package jambalaya.migrations;

/**
 * Thrown when migrations cannot be applied: a script failed, the history and the scripts disagree, or the setup is wrong.
 */
public class MigrationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MigrationException( final String message ) {
		super( message );
	}

	public MigrationException( final String message, final Throwable cause ) {
		super( message, cause );
	}
}
