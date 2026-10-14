package in.iitm.cs5013.tracker;

import in.iitm.cs5013.tracker.repository.Database;
import in.iitm.cs5013.tracker.repository.SqliteProjectRepository;
import in.iitm.cs5013.tracker.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class TrackerCommandTest {
    @TempDir Path temporary;

    @Test void commandsParseAndPersistRecordsWhileInvalidDatesAreRejected() throws Exception {
        var database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        var service = new ProjectService(new SqliteProjectRepository(database),
            Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC));
        var cli = new CommandLine(new TrackerApplication(service));
        cli.setErr(new PrintWriter(new StringWriter()));
        assertEquals(0, cli.execute("project-add", "--id", "P001", "--student", "Student A",
            "--title", "Graph Theory", "--start", "2026-10-01"));
        assertEquals(0, cli.execute("update-add", "--id", "U001", "--project", "P001",
            "--date", "2026-10-07", "--summary", "Read paper"));
        assertEquals("Read paper", service.updates("P001").get(0).summary());
        assertEquals(2, cli.execute("update-add", "--project", "P001",
            "--date", "not-a-date", "--summary", "Bad note"));
        assertEquals(1, service.updates("P001").size());
        assertEquals(0, cli.execute("project-complete", "--id", "P001"));
        assertTrue(service.list(false).isEmpty());
    }
}
