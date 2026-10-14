package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.*;
import in.iitm.cs5013.tracker.report.TextSummaryRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {
    @TempDir Path temporary;
    private static final LocalDate TODAY = LocalDate.parse("2026-10-14");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC);

    @Test void goldenSummaryAndRerunsPreserveRecordsAndReplaceOneFile() throws Exception {
        var database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        var projects = new ProjectService(new SqliteProjectRepository(database), clock);
        var work = new WorkService(new SqliteWorkRepository(database), projects, clock);
        projects.add("P002", "Student B", "Algebra", TODAY);
        projects.add("P001", "Student A", "Graph Theory", LocalDate.parse("2026-09-01"));
        projects.addUpdate("U001", "P001", LocalDate.parse("2026-09-29"), "Read the paper", "Draft proof");
        work.addTask("T001", "P001", "Submit draft", TODAY.minusDays(1));
        work.addFollowup("F001", "P001", "Send revised proof", TODAY.minusDays(4), TODAY);
        work.addTask("T002", "P001", "Already finished", TODAY.minusDays(2));
        work.completeTask("T002");
        projects.add("P003", "Student C", "Completed", TODAY);
        projects.complete("P003");
        var reports = new ReportService(new StatusService(projects, work, clock), new TextSummaryRenderer());
        Path directory = temporary.resolve("nested/reports");
        Path output = reports.generate(directory, 14);
        String expected;
        try (var stream = getClass().getResourceAsStream("/reports/expected-summary.txt")) {
            assertNotNull(stream);
            expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
        assertEquals(expected, Files.readString(output));
        assertEquals("supervision-summary-2026-10-14.txt", output.getFileName().toString());
        assertEquals(output, reports.generate(directory, 14));
        projects.addUpdate("U002", "P001", TODAY, "New progress: α", "Write\nconclusion");
        reports.generate(directory, 14);
        String revised = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(revised.contains("New progress: α"));
        assertTrue(revised.contains("Next steps: Write conclusion"));
        assertFalse(revised.contains("Missing update (15 days"));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
        assertEquals(2, projects.updates("P001").size());
        assertEquals(2, work.tasks("P001", true).size());
        assertEquals(1, work.followups("P001", true).size());
    }

    @Test void emptyReportAndNextDayUseDistinctFiles() throws Exception {
        var database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        var projects = new ProjectService(new SqliteProjectRepository(database), clock);
        var work = new WorkService(new SqliteWorkRepository(database), projects, clock);
        var renderer = new TextSummaryRenderer();
        var reports = new ReportService(new StatusService(projects, work, clock), renderer);
        Path directory = temporary.resolve("reports");
        Path today = reports.generate(directory, 14);
        assertTrue(Files.readString(today).contains("No active projects."));
        Clock tomorrow = Clock.offset(clock, Duration.ofDays(1));
        Path next = new ReportService(new StatusService(projects, work, tomorrow), renderer).generate(directory, 14);
        assertNotEquals(today, next);
        assertTrue(Files.exists(today));
        assertTrue(Files.exists(next));
    }

    @Test void invalidThresholdAndOutputFailuresAreReported() throws Exception {
        var database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        var projects = new ProjectService(new SqliteProjectRepository(database), clock);
        var work = new WorkService(new SqliteWorkRepository(database), projects, clock);
        var reports = new ReportService(new StatusService(projects, work, clock), new TextSummaryRenderer());
        Path invalidDirectory = temporary.resolve("invalid");
        assertThrows(IllegalArgumentException.class, () -> reports.generate(invalidDirectory, 0));
        assertFalse(Files.exists(invalidDirectory));
        Path file = temporary.resolve("file.txt");
        Files.writeString(file, "Keep this file");
        assertThrows(java.io.IOException.class, () -> reports.generate(file, 14));
        assertEquals("Keep this file", Files.readString(file));
    }
}
