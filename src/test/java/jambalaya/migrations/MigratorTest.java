package jambalaya.migrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MigratorTest {

	private DataSource _dataSource;

	@BeforeEach
	public void createDatabase() {
		final JdbcDataSource ds = new JdbcDataSource();
		ds.setURL( "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL" );
		_dataSource = ds;
	}

	private Migrator migrator() {
		return new Migrator( _dataSource, "migrations-test" );
	}

	@Test
	public void discoversScriptsInOrderAndIgnoresOtherFiles() {
		final List<Migration> available = migrator().available();
		assertEquals( 2, available.size() );
		assertEquals( 1, available.get( 0 ).version() );
		assertEquals( "001-create-widget-2026-09-07.sql", available.get( 0 ).name() );
		assertEquals( 2, available.get( 1 ).version() );
		assertEquals( 64, available.get( 0 ).checksum().length() );
	}

	@Test
	public void appliesPendingScriptsOnceAndRecordsThem() throws SQLException {
		final Migrator migrator = migrator();
		assertEquals( 2, migrator.pending().size() );

		final List<Migration> applied = migrator.migrate();
		assertEquals( 2, applied.size() );
		assertEquals( "red", query( "SELECT colour FROM widget WHERE id = 1" ) );

		final List<AppliedMigration> history = migrator.applied();
		assertEquals( 2, history.size() );
		assertEquals( "002-widget-colour-2026-09-07.sql", history.get( 1 ).name() );
		assertEquals( migrator.available().get( 1 ).checksum(), history.get( 1 ).checksum() );

		assertTrue( migrator.pending().isEmpty() );
		assertTrue( migrator.migrate().isEmpty() );
	}

	@Test
	public void baselineRecordsWithoutRunning() throws SQLException {
		final Migrator migrator = migrator();
		assertEquals( 1, migrator.baseline( 1 ).size() );

		final List<Migration> pending = migrator.pending();
		assertEquals( 1, pending.size() );
		assertEquals( 2, pending.get( 0 ).version() );
		assertEquals( "baseline", migrator.applied().get( 0 ).appliedBy() );

		// The widget table was never created, so applying 002 must fail and leave the history untouched
		assertThrows( MigrationException.class, migrator::migrate );
		assertEquals( 1, migrator.applied().size() );
	}

	@Test
	public void refusesAnEditedAppliedScript() throws SQLException {
		final Migrator migrator = migrator();
		migrator.migrate();

		try( Connection c = _dataSource.getConnection(); Statement s = c.createStatement() ) {
			s.execute( "UPDATE schema_migration SET checksum = 'tampered' WHERE version = 2" );
		}

		final MigrationException e = assertThrows( MigrationException.class, migrator::pending );
		assertTrue( e.getMessage().contains( "was changed after being applied" ) );
	}

	@Test
	public void refusesAPendingScriptOlderThanTheNewestApplied() throws SQLException {
		final Migrator migrator = migrator();

		try( Connection c = _dataSource.getConnection(); Statement s = c.createStatement() ) {
			s.execute( "CREATE TABLE schema_migration (version integer PRIMARY KEY, name varchar(255) NOT NULL, checksum varchar(64) NOT NULL, applied_at timestamp NOT NULL, applied_by varchar(255), duration_millis bigint NOT NULL)" );
			s.execute( "INSERT INTO schema_migration VALUES (2, '002-widget-colour-2026-09-07.sql', '" + Migrator.checksum( migrator.available().get( 1 ).sql() ) + "', CURRENT_TIMESTAMP, 'test', 0)" );
		}

		final MigrationException e = assertThrows( MigrationException.class, migrator::pending );
		assertTrue( e.getMessage().contains( "already been applied" ) );
	}

	@Test
	public void checksumIgnoresLineEndings() {
		assertEquals( Migrator.checksum( "a\nb\n" ), Migrator.checksum( "a\r\nb\r\n" ) );
	}

	private String query( final String sql ) throws SQLException {
		try( Connection c = _dataSource.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery( sql ) ) {
			rs.next();
			return rs.getString( 1 );
		}
	}
}
