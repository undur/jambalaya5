package jambalaya.migrations;

/**
 * A migration script found on the classpath: {@code <folder>/NNN-description.sql}.
 *
 * @param version      The leading number of the file name; migrations run in this order
 * @param name         The file name without the folder
 * @param sql          The script's content
 * @param checksum     SHA-256 of the content with line endings normalised, so an applied script that is later edited is detected
 */
public record Migration( int version, String name, String sql, String checksum ) implements Comparable<Migration> {

	@Override
	public int compareTo( final Migration other ) {
		return Integer.compare( version, other.version );
	}
}
