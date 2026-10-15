package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.Database;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteConfig;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.Clock;
import java.util.UUID;

/** SQLite snapshots include all tracking tables, independent of journal mode. */
public final class BackupService {
    public record RestoreResult(Path restoredFrom, Path recoveryBackup) { }
    private final Database database;
    private final Clock clock;
    public BackupService(Database database, Clock clock) { this.database = database; this.clock = clock; }
    public Path defaultDirectory() { return database.path().getParent().resolve("backups"); }

    public Path backup(Path directory) throws IOException, SQLException {
        Path outputDirectory = directory.toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);
        Path temporary = Files.createTempFile(outputDirectory, ".backup-", ".db");
        try {
            try (var connection = database.open()) {
                int result = ((SQLiteConnection) connection).getDatabase().backup("main", temporary.toString(), null);
                if (result != 0) throw new SQLException("SQLite backup failed (code " + result + ").");
            }
            validate(temporary);
            Path output = outputDirectory.resolve("tracker-backup-" + java.time.LocalDate.now(clock) + "-" + UUID.randomUUID() + ".db");
            Files.move(temporary, output);
            return output;
        } finally { Files.deleteIfExists(temporary); }
    }

    public RestoreResult restore(Path source) throws IOException, SQLException {
        Path input = source.toAbsolutePath().normalize();
        if (!Files.isRegularFile(input)) throw new IOException("Choose an existing backup file.");
        if (Files.isSameFile(input, database.path())) throw new IOException("The current database cannot be its own restore source.");
        Files.createDirectories(defaultDirectory());
        Path staged = Files.createTempFile(defaultDirectory(), ".restore-", ".db");
        try {
            // Work on a stable private copy, never edit the user's backup.
            Files.copy(input, staged, StandardCopyOption.REPLACE_EXISTING);
            validate(staged);
            Path recovery = backup(defaultDirectory());
            try (var connection = database.open()) {
                int result = ((SQLiteConnection) connection).getDatabase().restore("main", staged.toString(), null);
                if (result != 0) throw new SQLException("SQLite restore failed (code " + result + "). Recovery copy: " + recovery);
            }
            return new RestoreResult(input, recovery);
        } finally { Files.deleteIfExists(staged); }
    }

    private static void validate(Path file) throws SQLException {
        var config = new SQLiteConfig(); config.setReadOnly(true);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file, config.toProperties());
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("PRAGMA quick_check")) {
                if (!result.next() || !"ok".equals(result.getString(1))) throw new SQLException("Backup integrity check failed.");
            }
            try (var result = statement.executeQuery("PRAGMA user_version")) {
                if (!result.next() || result.getInt(1) != 1) throw new SQLException("Backup schema is not supported by this version.");
            }
            String[] queries = {
                "SELECT id,student_name,title,status,started_on FROM projects LIMIT 0",
                "SELECT id,project_id,update_date,summary,next_steps FROM updates LIMIT 0",
                "SELECT id,project_id,description,due_date,completed_on FROM tasks LIMIT 0",
                "SELECT id,project_id,description,requested_on,due_date,resolved_on FROM followups LIMIT 0",
                "SELECT id,imported_at,source_file,row_count FROM import_runs LIMIT 0"
            };
            for (String query : queries) try (var ignored = statement.executeQuery(query)) { /* Verify required columns. */ }
            try (var result = statement.executeQuery("PRAGMA foreign_key_check")) {
                if (result.next()) throw new SQLException("Backup contains invalid project references.");
            }
        }
    }
}
