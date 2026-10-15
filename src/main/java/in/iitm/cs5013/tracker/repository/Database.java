package in.iitm.cs5013.tracker.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens short-lived connections and creates the initial versioned schema. */
public final class Database {
    private final Path path;

    public Database(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public Path path() { return path; }

    public Connection open() throws SQLException, IOException {
        Files.createDirectories(path.getParent());
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
        return connection;
    }

    public void initialize() throws SQLException, IOException {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            int version;
            try (var result = statement.executeQuery("PRAGMA user_version")) {
                version = result.getInt(1);
            }
            if (version > 1) {
                throw new SQLException("Database was created by a newer application version.");
            }
            if (version == 1) { return; }
            connection.setAutoCommit(false);
            try {
                statement.execute("""
                    CREATE TABLE projects (
                        id TEXT PRIMARY KEY NOT NULL,
                        student_name TEXT NOT NULL,
                        title TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','COMPLETED')),
                        started_on TEXT NOT NULL
                    )
                    """);
                statement.execute("""
                    CREATE TABLE updates (
                        id TEXT PRIMARY KEY NOT NULL,
                        project_id TEXT NOT NULL REFERENCES projects(id),
                        update_date TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        next_steps TEXT NOT NULL DEFAULT ''
                    )
                    """);
                statement.execute("""
                    CREATE TABLE tasks (
                        id TEXT PRIMARY KEY NOT NULL,
                        project_id TEXT NOT NULL REFERENCES projects(id),
                        description TEXT NOT NULL,
                        due_date TEXT,
                        completed_on TEXT
                    )
                    """);
                statement.execute("""
                    CREATE TABLE followups (
                        id TEXT PRIMARY KEY NOT NULL,
                        project_id TEXT NOT NULL REFERENCES projects(id),
                        description TEXT NOT NULL,
                        requested_on TEXT NOT NULL,
                        due_date TEXT,
                        resolved_on TEXT
                    )
                    """);
                statement.execute("""
                    CREATE TABLE import_runs (
                        id INTEGER PRIMARY KEY,
                        imported_at TEXT NOT NULL,
                        source_file TEXT NOT NULL,
                        row_count INTEGER NOT NULL CHECK(row_count >= 0)
                    )
                    """);
                statement.execute("CREATE INDEX updates_project_date ON updates(project_id, update_date)");
                statement.execute("CREATE INDEX tasks_project ON tasks(project_id)");
                statement.execute("CREATE INDEX followups_project ON followups(project_id)");
                statement.execute("PRAGMA user_version = 1");
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }
}
