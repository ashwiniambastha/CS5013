package in.iitm.cs5013.tracker.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    @TempDir Path temporary;

    @Test void recordsSurviveReopeningAndRepeatedInitialization() throws Exception {
        Path file = temporary.resolve("data/tracker.db");
        Database database = new Database(file);
        database.initialize();
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO projects VALUES ('P001','Student A','Graph Theory','ACTIVE','2026-10-01')");
            statement.executeUpdate("INSERT INTO updates VALUES ('U001','P001','2026-10-14','Read paper','Draft proof')");
        }
        Database reopened = new Database(file);
        reopened.initialize();
        try (var connection = reopened.open(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT summary FROM updates WHERE id='U001'")) {
            assertTrue(result.next());
            assertEquals("Read paper", result.getString(1));
            assertFalse(result.next());
        }
    }

    @Test void rejectsUpdatesForUnknownProjects() throws Exception {
        Database database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        try (var connection = database.open(); var statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                "INSERT INTO updates VALUES ('U001','missing','2026-10-14','Read paper','')"));
        }
    }

    @Test void rejectsNewerSchemaWithoutChangingIt() throws Exception {
        Database database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        try (var connection = database.open(); var statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = 2");
        }
        assertThrows(SQLException.class, database::initialize);
    }
}
