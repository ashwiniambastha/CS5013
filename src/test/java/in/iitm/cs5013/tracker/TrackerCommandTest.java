package in.iitm.cs5013.tracker;

import in.iitm.cs5013.tracker.repository.Database;
import in.iitm.cs5013.tracker.repository.SqliteProjectRepository;
import in.iitm.cs5013.tracker.service.ProjectService;
import in.iitm.cs5013.tracker.service.WorkService;
import in.iitm.cs5013.tracker.repository.SqliteWorkRepository;
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
        var work = new WorkService(new SqliteWorkRepository(database), service,
            Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC));
        var cli = new CommandLine(new TrackerApplication(service, work));
        cli.setErr(new PrintWriter(new StringWriter()));
        assertEquals(0, cli.execute("project-add", "--id", "P001", "--student", "Student A",
            "--title", "Graph Theory", "--start", "2026-10-01"));
        assertEquals(0, cli.execute("update-add", "--id", "U001", "--project", "P001",
            "--date", "2026-10-07", "--summary", "Read paper"));
        assertEquals("Read paper", service.updates("P001").get(0).summary());
        assertEquals(2, cli.execute("update-add", "--project", "P001",
            "--date", "not-a-date", "--summary", "Bad note"));
        assertEquals(1, service.updates("P001").size());
        assertEquals(0, cli.execute("task-add", "--id", "T001", "--project", "P001", "--text", "Submit draft", "--due", "2026-10-20"));
        assertEquals(0, cli.execute("followup-add", "--id", "F001", "--project", "P001", "--text", "Send proof", "--requested", "2026-10-10"));
        assertEquals(0, cli.execute("task-done", "--id", "T001"));
        assertTrue(work.tasks("P001", false).isEmpty());
        assertEquals(1, work.followups("P001", false).size());
        assertEquals(0, cli.execute("followup-resolve", "--id", "F001"));
        assertTrue(work.followups("P001", false).isEmpty());
        assertEquals(0, cli.execute("project-complete", "--id", "P001"));
        assertTrue(service.list(false).isEmpty());
    }
}
