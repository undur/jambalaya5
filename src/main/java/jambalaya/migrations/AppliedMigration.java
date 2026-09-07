package jambalaya.migrations;

import java.time.Instant;

/**
 * A row of the history table: a migration that has been applied to the database.
 */
public record AppliedMigration( int version, String name, String checksum, Instant appliedAt, String appliedBy, long durationMillis ) {}
