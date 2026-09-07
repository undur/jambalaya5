package jambalaya.migrations;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies ordered SQL migration scripts from the classpath to a database and keeps a history of
 * what has been applied — the "ordered SQL files plus a history table" model of Flyway, Rails and
 * friends, reduced to what a single-database project needs.
 *
 * <p><b>Scripts</b> live in a classpath folder (say {@code migrations}) and are named
 * {@code NNN-description.sql}: a leading number gives the order, the rest is documentation. The
 * folder is scanned both as a directory on disk (development) and inside a jar (deployment).
 *
 * <p><b>History</b> is a table (default {@code schema_migration}) with one row per applied
 * script: version, name, checksum, when, by whom and how long it took. That row is the answer to
 * "has this run here?" — nothing else needs to be remembered.
 *
 * <p><b>Applying</b> runs each pending script in its own transaction, in order, and records it in
 * the same transaction, so a failing script leaves nothing behind. Before applying, the history is
 * checked against the scripts: an applied script whose content has since changed, or a pending
 * script older than one already applied, stops the run with an error rather than silently doing
 * something surprising. On PostgreSQL an advisory lock keeps two instances starting at once from
 * both migrating; the lock dies with the connection, so a crash leaves nothing to clean up.
 *
 * <p><b>Baseline:</b> when introducing the migrator to a database that already received some
 * scripts by hand, {@link #baseline(int)} records those as applied without running them.
 *
 * <p>Typical use at application startup: {@code new Migrator( dataSource, "migrations" ).migrate();}
 * and let a {@link MigrationException} stop the application.
 */
public class Migrator {

	private static final Logger logger = LoggerFactory.getLogger( Migrator.class );

	private static final Pattern SCRIPT_NAME = Pattern.compile( "^(\\d+)[-_].*\\.sql$" );

	/**
	 * Key of the PostgreSQL advisory lock held while migrating
	 */
	private static final long LOCK_KEY = 0x6a616d62616c6179L; // "jambalay"

	private final DataSource _dataSource;
	private final String _folder;
	private final String _tableName;
	private final ClassLoader _classLoader;

	public Migrator( final DataSource dataSource, final String folder ) {
		this( dataSource, folder, "schema_migration", Thread.currentThread().getContextClassLoader() );
	}

	public Migrator( final DataSource dataSource, final String folder, final String tableName, final ClassLoader classLoader ) {
		_dataSource = Objects.requireNonNull( dataSource );
		_folder = folder.replaceAll( "^/|/$", "" );
		_tableName = tableName;
		_classLoader = classLoader;
	}

	// ---------------------------------------------------------------------------------------------
	// Discovery
	// ---------------------------------------------------------------------------------------------

	/**
	 * @return Every script in the folder, in version order
	 */
	public List<Migration> available() {
		final List<Migration> result = new ArrayList<>();

		try {
			final Enumeration<URL> urls = _classLoader.getResources( _folder );

			while( urls.hasMoreElements() ) {
				final URL url = urls.nextElement();

				for( final String fileName : listFolder( url ) ) {
					final Matcher matcher = SCRIPT_NAME.matcher( fileName );

					if( !matcher.matches() ) {
						continue;
					}

					final String sql = readResource( _folder + "/" + fileName );
					result.add( new Migration( Integer.parseInt( matcher.group( 1 ) ), fileName, sql, checksum( sql ) ) );
				}
			}
		}
		catch( final IOException e ) {
			throw new MigrationException( "Could not read migration scripts from classpath folder '%s'".formatted( _folder ), e );
		}

		Collections.sort( result );

		for( int i = 1; i < result.size(); i++ ) {
			if( result.get( i ).version() == result.get( i - 1 ).version() ) {
				throw new MigrationException( "Two migration scripts carry version %s: '%s' and '%s'".formatted( result.get( i ).version(), result.get( i - 1 ).name(), result.get( i ).name() ) );
			}
		}

		return result;
	}

	/**
	 * @return The scripts that have not been applied to the database
	 */
	public List<Migration> pending() {
		try( Connection connection = _dataSource.getConnection() ) {
			ensureHistoryTable( connection );
			final List<AppliedMigration> applied = applied( connection );
			final List<Migration> available = available();
			validate( available, applied );
			return pending( available, applied );
		}
		catch( final SQLException e ) {
			throw new MigrationException( "Could not read migration history", e );
		}
	}

	/**
	 * @return The history: what has been applied, oldest first
	 */
	public List<AppliedMigration> applied() {
		try( Connection connection = _dataSource.getConnection() ) {
			ensureHistoryTable( connection );
			return applied( connection );
		}
		catch( final SQLException e ) {
			throw new MigrationException( "Could not read migration history", e );
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Applying
	// ---------------------------------------------------------------------------------------------

	/**
	 * Applies every pending script, in order, each in its own transaction.
	 *
	 * @return The migrations applied by this call
	 */
	public List<Migration> migrate() {
		try( Connection connection = _dataSource.getConnection() ) {
			connection.setAutoCommit( false );
			ensureHistoryTable( connection );
			lock( connection );

			try {
				final List<AppliedMigration> applied = applied( connection );
				final List<Migration> available = available();
				validate( available, applied );
				final List<Migration> pending = pending( available, applied );

				if( pending.isEmpty() ) {
					logger.info( "Database schema is up to date ({} migrations applied)", applied.size() );
					return pending;
				}

				for( final Migration migration : pending ) {
					apply( connection, migration );
				}

				return pending;
			}
			finally {
				unlock( connection );
			}
		}
		catch( final SQLException e ) {
			throw new MigrationException( "Could not connect for migration", e );
		}
	}

	/**
	 * Records every script up to and including the given version as applied, without running
	 * it — for a database that received those scripts by hand before the migrator was introduced.
	 *
	 * @return The migrations recorded by this call
	 */
	public List<Migration> baseline( final int version ) {
		try( Connection connection = _dataSource.getConnection() ) {
			connection.setAutoCommit( false );
			ensureHistoryTable( connection );

			final List<AppliedMigration> applied = applied( connection );
			final List<Migration> recorded = new ArrayList<>();

			for( final Migration migration : available() ) {
				if( migration.version() > version || applied.stream().anyMatch( a -> a.version() == migration.version() ) ) {
					continue;
				}

				record( connection, migration, "baseline", 0 );
				recorded.add( migration );
				logger.info( "Migration {} recorded as applied (baseline)", migration.name() );
			}

			connection.commit();
			return recorded;
		}
		catch( final SQLException e ) {
			throw new MigrationException( "Could not record baseline", e );
		}
	}

	private void apply( final Connection connection, final Migration migration ) {
		logger.info( "Applying migration {}", migration.name() );
		final long start = System.currentTimeMillis();

		try {
			try( Statement statement = connection.createStatement() ) {
				statement.execute( migration.sql() );
			}

			record( connection, migration, System.getProperty( "user.name" ), System.currentTimeMillis() - start );
			connection.commit();
			logger.info( "Migration {} applied in {} ms", migration.name(), System.currentTimeMillis() - start );
		}
		catch( final SQLException e ) {
			try {
				connection.rollback();
			}
			catch( final SQLException rollbackFailure ) {
				e.addSuppressed( rollbackFailure );
			}

			throw new MigrationException( "Migration %s failed and was rolled back: %s".formatted( migration.name(), e.getMessage() ), e );
		}
	}

	private void record( final Connection connection, final Migration migration, final String appliedBy, final long durationMillis ) throws SQLException {
		try( PreparedStatement statement = connection.prepareStatement( "INSERT INTO " + _tableName + " (version, name, checksum, applied_at, applied_by, duration_millis) VALUES (?, ?, ?, ?, ?, ?)" ) ) {
			statement.setInt( 1, migration.version() );
			statement.setString( 2, migration.name() );
			statement.setString( 3, migration.checksum() );
			statement.setTimestamp( 4, java.sql.Timestamp.from( Instant.now() ) );
			statement.setString( 5, appliedBy );
			statement.setLong( 6, durationMillis );
			statement.executeUpdate();
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Consistency between scripts and history
	// ---------------------------------------------------------------------------------------------

	/**
	 * The history and the scripts must tell the same story: an applied script must still exist
	 * unchanged, and no pending script may be older than one already applied.
	 */
	private static void validate( final List<Migration> available, final List<AppliedMigration> applied ) {
		for( final AppliedMigration a : applied ) {
			final Migration script = available.stream().filter( m -> m.version() == a.version() ).findFirst().orElse( null );

			if( script == null ) {
				logger.warn( "Migration {} is recorded as applied but no script for version {} exists on the classpath", a.name(), a.version() );
				continue;
			}

			if( !script.checksum().equals( a.checksum() ) ) {
				throw new MigrationException( "Migration %s was changed after being applied (checksum %s recorded, %s now). Applied scripts must not be edited; add a new migration instead.".formatted( script.name(), a.checksum(), script.checksum() ) );
			}
		}

		final int newestApplied = applied.stream().mapToInt( AppliedMigration::version ).max().orElse( Integer.MIN_VALUE );

		for( final Migration m : pending( available, applied ) ) {
			if( m.version() < newestApplied ) {
				throw new MigrationException( "Migration %s is pending but version %s has already been applied; migrations must be added with increasing versions".formatted( m.name(), newestApplied ) );
			}
		}
	}

	private static List<Migration> pending( final List<Migration> available, final List<AppliedMigration> applied ) {
		return available.stream()
				.filter( m -> applied.stream().noneMatch( a -> a.version() == m.version() ) )
				.toList();
	}

	// ---------------------------------------------------------------------------------------------
	// History table
	// ---------------------------------------------------------------------------------------------

	private void ensureHistoryTable( final Connection connection ) throws SQLException {
		try( Statement statement = connection.createStatement() ) {
			statement.execute( """
					CREATE TABLE IF NOT EXISTS %s (
						version integer PRIMARY KEY,
						name varchar(255) NOT NULL,
						checksum varchar(64) NOT NULL,
						applied_at timestamp NOT NULL,
						applied_by varchar(255),
						duration_millis bigint NOT NULL
					)""".formatted( _tableName ) );
		}

		if( !connection.getAutoCommit() ) {
			connection.commit();
		}
	}

	private List<AppliedMigration> applied( final Connection connection ) throws SQLException {
		final List<AppliedMigration> result = new ArrayList<>();

		try( Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery( "SELECT version, name, checksum, applied_at, applied_by, duration_millis FROM " + _tableName + " ORDER BY version" ) ) {
			while( rs.next() ) {
				result.add( new AppliedMigration( rs.getInt( 1 ), rs.getString( 2 ), rs.getString( 3 ), rs.getTimestamp( 4 ).toInstant(), rs.getString( 5 ), rs.getLong( 6 ) ) );
			}
		}

		return result;
	}

	// ---------------------------------------------------------------------------------------------
	// Locking (PostgreSQL only; other databases migrate unlocked)
	// ---------------------------------------------------------------------------------------------

	private static boolean isPostgreSQL( final Connection connection ) throws SQLException {
		return connection.getMetaData().getDatabaseProductName().toLowerCase().contains( "postgres" );
	}

	private static void lock( final Connection connection ) throws SQLException {
		if( isPostgreSQL( connection ) ) {
			try( Statement statement = connection.createStatement() ) {
				statement.execute( "SELECT pg_advisory_lock(" + LOCK_KEY + ")" );
			}
		}
	}

	private static void unlock( final Connection connection ) throws SQLException {
		if( isPostgreSQL( connection ) ) {
			try( Statement statement = connection.createStatement() ) {
				statement.execute( "SELECT pg_advisory_unlock(" + LOCK_KEY + ")" );
			}

			connection.commit();
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Classpath access
	// ---------------------------------------------------------------------------------------------

	/**
	 * @return The file names directly inside the folder the URL points at — a directory on disk or an entry inside a jar
	 */
	private List<String> listFolder( final URL url ) throws IOException {
		final List<String> names = new ArrayList<>();

		if( "file".equals( url.getProtocol() ) ) {
			final Path dir = Path.of( java.net.URI.create( url.toString() ) );

			if( Files.isDirectory( dir ) ) {
				try( Stream<Path> files = Files.list( dir ) ) {
					files.filter( Files::isRegularFile ).forEach( f -> names.add( f.getFileName().toString() ) );
				}
			}

			return names;
		}

		if( "jar".equals( url.getProtocol() ) ) {
			final JarURLConnection jarConnection = (JarURLConnection)url.openConnection();
			final String prefix = _folder + "/";

			try( JarFile jar = jarConnection.getJarFile() ) {
				final Enumeration<JarEntry> entries = jar.entries();

				while( entries.hasMoreElements() ) {
					final String entryName = entries.nextElement().getName();

					if( entryName.startsWith( prefix ) && !entryName.endsWith( "/" ) && entryName.indexOf( '/', prefix.length() ) == -1 ) {
						names.add( entryName.substring( prefix.length() ) );
					}
				}
			}

			return names;
		}

		throw new MigrationException( "Unsupported classpath location for migrations: " + url );
	}

	private String readResource( final String path ) throws IOException {
		try( InputStream stream = _classLoader.getResourceAsStream( path ) ) {
			if( stream == null ) {
				throw new IOException( "Resource not found: " + path );
			}

			return new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
		}
	}

	/**
	 * @return SHA-256 of the script with line endings normalised, as lower-case hex
	 */
	static String checksum( final String sql ) {
		try {
			final byte[] digest = MessageDigest.getInstance( "SHA-256" ).digest( sql.replace( "\r\n", "\n" ).getBytes( StandardCharsets.UTF_8 ) );
			return HexFormat.of().formatHex( digest );
		}
		catch( final NoSuchAlgorithmException e ) {
			throw new IllegalStateException( e );
		}
	}
}
