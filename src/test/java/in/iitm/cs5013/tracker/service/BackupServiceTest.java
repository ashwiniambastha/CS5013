package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {
    @TempDir Path temporary;
    private Database database;
    private ProjectService projects;
    private BackupService backups;
    @BeforeEach void setup() throws Exception {
        database = new Database(temporary.resolve("tracker.db")); database.initialize();
        var clock = Clock.fixed(Instant.parse("2026-10-15T06:00:00Z"), ZoneOffset.UTC);
        projects = new ProjectService(new SqliteProjectRepository(database),clock);
        backups = new BackupService(database,clock);
        projects.add("P001","Student A","Study",LocalDate.parse("2026-10-01"));
    }
    @Test void snapshotRestoresRecordsAndKeepsRecoveryCopy() throws Exception {
        Path original = backups.backup(temporary.resolve("archives"));
        projects.addUpdate("U001","P001",LocalDate.parse("2026-10-14"),"New update","");
        var restored = backups.restore(original);
        assertTrue(projects.updates("P001").isEmpty());
        assertTrue(Files.exists(restored.recoveryBackup()));
        backups.restore(restored.recoveryBackup());
        assertEquals(1,projects.updates("P001").size());
        assertNotEquals(original,backups.backup(temporary.resolve("archives")));
    }
    @Test void corruptAndUnsupportedFilesDoNotChangeCurrentRecords() throws Exception {
        Path broken = temporary.resolve("broken.db"); Files.writeString(broken,"not SQLite");
        assertThrows(java.sql.SQLException.class, () -> backups.restore(broken));
        Path future = backups.backup(temporary.resolve("archives"));
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:"+future); var statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = 99");
        }
        assertThrows(java.sql.SQLException.class, () -> backups.restore(future));
        assertThrows(java.io.IOException.class, () -> backups.restore(database.path()));
        assertThrows(java.io.IOException.class, () -> backups.restore(temporary.resolve("missing.db")));
        assertEquals(1,projects.list(true).size());
        try (var files = Files.list(backups.defaultDirectory())) { assertEquals(0,files.count()); }
    }
    @Test void snapshotsIncludeCommittedWalData() throws Exception {
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA wal_autocheckpoint = 0");
            projects.addUpdate("U001","P001",LocalDate.parse("2026-10-14"),"In WAL","");
            Path snapshot = backups.backup(temporary.resolve("archives"));
            var copy = new Database(snapshot);
            var restoredProjects = new SqliteProjectRepository(copy);
            assertEquals("In WAL",restoredProjects.updates("P001").get(0).summary());
        }
    }
}
