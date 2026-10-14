package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkServiceTest {
    @TempDir Path temporary;
    private static final LocalDate TODAY = LocalDate.parse("2026-10-14");
    private static Clock clock(LocalDate day) { return Clock.fixed(day.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC); }
    private Database database() throws Exception {
        var database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        return database;
    }

    @Test void taskCompletionDoesNotResolveFollowupAndRerunsPreserveDates() throws Exception {
        var database = database();
        var projects = new ProjectService(new SqliteProjectRepository(database), clock(TODAY));
        projects.add("P001", "Student A", "Study", TODAY.minusDays(10));
        var repository = new SqliteWorkRepository(database);
        var work = new WorkService(repository, projects, clock(TODAY));
        work.addTask("T001", "P001", "Submit draft", TODAY.plusDays(3));
        work.addFollowup("F001", "P001", "Send proof", TODAY.minusDays(2), null);
        work.completeTask("T001");
        assertTrue(work.tasks("P001", false).isEmpty());
        assertEquals(1, work.followups("P001", false).size());
        work.resolveFollowup("F001");
        var reopened = new WorkService(new SqliteWorkRepository(database()), projects, clock(TODAY.plusDays(1)));
        reopened.completeTask("T001");
        reopened.resolveFollowup("F001");
        assertEquals(TODAY, reopened.tasks("P001", true).get(0).completedOn());
        assertEquals(TODAY, reopened.followups("P001", true).get(0).resolvedOn());
    }

    @Test void ordersDeadlinesAndKeepsUndatedTasksLast() throws Exception {
        var database = database();
        var projects = new ProjectService(new SqliteProjectRepository(database), clock(TODAY));
        projects.add("P001", "Student A", "Study", TODAY.minusDays(10));
        var work = new WorkService(new SqliteWorkRepository(database), projects, clock(TODAY));
        work.addTask("T003", "P001", "No date", null);
        work.addTask("T002", "P001", "Later", TODAY.plusDays(2));
        work.addTask("T001", "P001", "Already due", TODAY.minusDays(1));
        assertEquals(java.util.List.of("T001", "T002", "T003"), work.tasks("P001", false).stream().map(t -> t.id()).toList());
    }

    @Test void invalidRequestsAndUnknownIdsDoNotChangeRecords() throws Exception {
        var database = database();
        var projects = new ProjectService(new SqliteProjectRepository(database), clock(TODAY));
        projects.add("P001", "Student A", "Study", TODAY.minusDays(10));
        var work = new WorkService(new SqliteWorkRepository(database), projects, clock(TODAY));
        assertThrows(IllegalArgumentException.class, () -> work.addTask("T001", "missing", "Work", null));
        assertThrows(IllegalArgumentException.class, () -> work.addTask("T001", "P001", " ", null));
        assertThrows(IllegalArgumentException.class, () -> work.addFollowup("F001", "P001", "Request", TODAY.plusDays(1), null));
        assertThrows(IllegalArgumentException.class, () -> work.addFollowup("F001", "P001", "Request", TODAY, TODAY.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> work.completeTask("missing"));
        assertThrows(IllegalArgumentException.class, () -> work.resolveFollowup("missing"));
        assertTrue(work.tasks("P001", true).isEmpty());
        assertTrue(work.followups("P001", true).isEmpty());
        work.addTask("T001", "P001", "First", null);
        assertThrows(IllegalArgumentException.class, () -> work.addTask("T001", "P001", "Duplicate", null));
        projects.complete("P001");
        assertThrows(IllegalArgumentException.class, () -> work.addTask("T002", "P001", "New", null));
        // Existing work can be closed after project completion without losing history.
        work.completeTask("T001");
        assertEquals(1, work.tasks("P001", true).size());
    }
}
