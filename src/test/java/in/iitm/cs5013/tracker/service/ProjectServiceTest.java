package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.model.Project;
import in.iitm.cs5013.tracker.repository.Database;
import in.iitm.cs5013.tracker.repository.SqliteProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest {
    @TempDir Path temporary;
    private ProjectService service() throws Exception {
        var database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        return new ProjectService(new SqliteProjectRepository(database),
            Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC));
    }

    @Test void persistsUpdatesAndUsesDatesRatherThanInsertionOrder() throws Exception {
        var service = service();
        service.add("P001", "Student A", "Graph Theory", LocalDate.parse("2026-10-01"));
        service.addUpdate("U002", "P001", LocalDate.parse("2026-10-14"), "Latest", "Proof");
        service.addUpdate("U001", "P001", LocalDate.parse("2026-10-07"), "Earlier", "Read");
        var reopened = service();
        assertEquals(2, reopened.updates("P001").size());
        assertEquals("Latest", reopened.updates("P001").get(0).summary());
        assertEquals("Student A", reopened.get("P001").studentName());
    }

    @Test void completionRetainsHistoryAndExcludesProjectFromActiveList() throws Exception {
        var service = service();
        service.add("P001", "Student A", "Study", LocalDate.parse("2026-10-01"));
        service.addUpdate("U001", "P001", LocalDate.parse("2026-10-07"), "Done", "");
        service.complete("P001");
        assertTrue(service.list(false).isEmpty());
        assertEquals(Project.Status.COMPLETED, service.list(true).get(0).status());
        assertEquals(1, service.updates("P001").size());
        assertThrows(IllegalArgumentException.class, () -> service.addUpdate("U002", "P001", LocalDate.parse("2026-10-14"), "Late", ""));
    }

    @Test void invalidInputDoesNotWriteRecords() throws Exception {
        var service = service();
        var start = LocalDate.parse("2026-10-01");
        assertThrows(IllegalArgumentException.class, () -> service.add(" ", "Student", "Study", start));
        service.add("P001", "Student", "Study", start);
        assertThrows(IllegalArgumentException.class, () -> service.add("P001", "Other", "Study", start));
        assertThrows(IllegalArgumentException.class, () -> service.addUpdate("U001", "missing", start, "Note", ""));
        assertThrows(IllegalArgumentException.class, () -> service.addUpdate("U001", "P001", start.minusDays(1), "Note", ""));
        assertThrows(IllegalArgumentException.class, () -> service.addUpdate("U001", "P001", LocalDate.parse("2026-10-15"), "Note", ""));
        assertTrue(service.updates("P001").isEmpty());
        assertEquals(1, service.list(true).size());
    }
}
