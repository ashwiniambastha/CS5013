package in.iitm.cs5013.tracker;

import in.iitm.cs5013.tracker.repository.Database;
import in.iitm.cs5013.tracker.repository.SqliteProjectRepository;
import in.iitm.cs5013.tracker.service.ProjectService;
import in.iitm.cs5013.tracker.service.WorkService;
import in.iitm.cs5013.tracker.service.StatusService;
import in.iitm.cs5013.tracker.service.ImportService;
import in.iitm.cs5013.tracker.service.BackupService;
import in.iitm.cs5013.tracker.repository.SqliteImportRepository;
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
        var status = new StatusService(service, work, Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC));
        var importer = new ImportService(new SqliteImportRepository(database), Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC));
        var cli = new CommandLine(new TrackerApplication(service, work, status, importer,
            new BackupService(database, Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC))));
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

    @Test void defaultBoardAndStatusCommandUseConfiguredOutputAndThreshold() throws Exception {
        var database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        var clock = Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC);
        var projects = new ProjectService(new SqliteProjectRepository(database), clock);
        var work = new WorkService(new SqliteWorkRepository(database), projects, clock);
        projects.add("P001", "Student A", "Study", LocalDate.parse("2026-10-01"));
        work.addTask("T001", "P001", "Submit draft", LocalDate.parse("2026-10-13"));
        var cli = new CommandLine(new TrackerApplication(projects, work, new StatusService(projects, work, clock),
            new ImportService(new SqliteImportRepository(database), clock), new BackupService(database, clock)));
        var output = new StringWriter();
        cli.setOut(new PrintWriter(output));
        cli.setErr(new PrintWriter(new StringWriter()));
        cli.setExecutionExceptionHandler((exception, command, result) -> 1);
        assertEquals(0, cli.execute());
        assertTrue(output.toString().contains("WITHIN THRESHOLD"));
        assertTrue(output.toString().contains("OVERDUE"));
        output.getBuffer().setLength(0);
        assertEquals(0, cli.execute("status", "--stale-days", "7"));
        assertTrue(output.toString().contains("MISSING UPDATE"));
        output.getBuffer().setLength(0);
        assertEquals(1, cli.execute("status", "--stale-days", "0"));
        assertEquals("", output.toString());
        Path reportDirectory = temporary.resolve("exported reports");
        assertEquals(0, cli.execute("report", "--output", reportDirectory.toString(), "--stale-days", "7"));
        Path report = reportDirectory.resolve("supervision-summary-2026-10-14.txt");
        assertTrue(java.nio.file.Files.readString(report).contains("Missing update (13 days since project start)"));
        assertTrue(output.toString().contains("Saved report:"));
        Path importFile = temporary.resolve("import.csv");
        java.nio.file.Files.writeString(importFile, "project_id,student_name,project_title,started_on,update_id,update_date,summary,next_steps\n"
            + "P002,Student B,Algebra,2026-10-01,U002,2026-10-07,Read paper,Draft notes\n");
        assertEquals(0, cli.execute("import", importFile.toString(), "--dry-run"));
        assertEquals(1, projects.list(true).size());
        assertEquals(0, cli.execute("import", importFile.toString()));
        assertEquals(2, projects.list(true).size());
        assertEquals(0, cli.execute("import", importFile.toString()));
        assertEquals(1, projects.updates("P002").size());
        Path backupDirectory = temporary.resolve("backups");
        assertEquals(0, cli.execute("backup", "--output", backupDirectory.toString()));
        Path backup;
        try (var files = java.nio.file.Files.list(backupDirectory)) { backup = files.findFirst().orElseThrow(); }
        projects.complete("P002");
        assertEquals(1, cli.execute("restore", backup.toString()));
        assertEquals(1, projects.list(false).size());
        assertEquals(0, cli.execute("restore", backup.toString(), "--confirm"));
        assertEquals(2, projects.list(false).size());
    }
}
