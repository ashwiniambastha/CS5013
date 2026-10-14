package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.model.StatusBoard.DeadlineState;
import in.iitm.cs5013.tracker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class StatusServiceTest {
    @TempDir Path temporary;
    private static final LocalDate TODAY = LocalDate.parse("2026-10-14");
    private ProjectService projects;
    private WorkService work;
    private StatusService status;

    @BeforeEach void setup() throws Exception {
        var database = new Database(temporary.resolve("tracker.db"));
        database.initialize();
        var clock = Clock.fixed(Instant.parse("2026-10-14T06:00:00Z"), ZoneOffset.UTC);
        projects = new ProjectService(new SqliteProjectRepository(database), clock);
        work = new WorkService(new SqliteWorkRepository(database), projects, clock);
        status = new StatusService(projects, work, clock);
    }

    @Test void exactlyFourteenDaysIsFreshAndFifteenDaysIsStale() throws Exception {
        projects.add("P001", "Student A", "Boundary", TODAY.minusDays(30));
        projects.add("P002", "Student B", "Stale", TODAY.minusDays(30));
        projects.addUpdate("U001", "P001", TODAY.minusDays(14), "Boundary note", "");
        projects.addUpdate("U002", "P002", TODAY.minusDays(15), "Old note", "");
        var board = status.board(14);
        assertEquals(TODAY, board.asOf());
        assertFalse(board.projects().get(0).stale());
        assertEquals(14, board.projects().get(0).daysSinceUpdate());
        assertTrue(board.projects().get(1).stale());
        assertTrue(status.board(7).projects().get(0).stale());
    }

    @Test void noUpdatesUsesStartDateAndCompletedProjectsDisappear() throws Exception {
        projects.add("P001", "Student A", "New", TODAY.minusDays(14));
        projects.add("P002", "Student B", "Quiet", TODAY.minusDays(15));
        projects.add("P003", "Student C", "Finished", TODAY.minusDays(30));
        projects.complete("P003");
        var rows = status.board(14).projects();
        assertEquals(2, rows.size());
        assertTrue(rows.get(0).latestUpdate().isEmpty());
        assertEquals(TODAY.minusDays(14), rows.get(0).freshnessDate());
        assertFalse(rows.get(0).stale());
        assertTrue(rows.get(1).stale());
    }

    @Test void classifiesDatesAndIgnoresClosedItemsWhilePreservingSameDayDeadlines() throws Exception {
        projects.add("P001", "Student A", "Study", TODAY.minusDays(30));
        work.addTask("T001", "P001", "Past", TODAY.minusDays(1));
        work.addTask("T002", "P001", "Today A", TODAY);
        work.addTask("T003", "P001", "Today B", TODAY);
        work.addTask("T004", "P001", "Future", TODAY.plusDays(1));
        work.addTask("T005", "P001", "Undated", null);
        work.addTask("T006", "P001", "Closed", TODAY.minusDays(2));
        work.completeTask("T006");
        work.addFollowup("F001", "P001", "Past request", TODAY.minusDays(5), TODAY.minusDays(1));
        work.addFollowup("F002", "P001", "Due today", TODAY.minusDays(5), TODAY);
        work.addFollowup("F003", "P001", "Undated request", TODAY.minusDays(5), null);
        work.addFollowup("F004", "P001", "Answered", TODAY.minusDays(5), TODAY.minusDays(2));
        work.resolveFollowup("F004");
        var row = status.board(14).projects().get(0);
        assertEquals(5, row.tasks().size());
        assertEquals(DeadlineState.OVERDUE, row.tasks().get(0).deadlineState());
        assertEquals(DeadlineState.DUE_TODAY, row.tasks().get(1).deadlineState());
        assertEquals(DeadlineState.DUE_TODAY, row.tasks().get(2).deadlineState());
        assertEquals(DeadlineState.UPCOMING, row.tasks().get(3).deadlineState());
        assertEquals(DeadlineState.NO_DEADLINE, row.tasks().get(4).deadlineState());
        assertEquals(TODAY.minusDays(1), row.nextDeadline().orElseThrow());
        assertEquals(3, row.followups().size());
        assertEquals(DeadlineState.OVERDUE, row.followups().get(0).deadlineState());
        assertEquals(DeadlineState.DUE_TODAY, row.followups().get(1).deadlineState());
        assertEquals(DeadlineState.NO_DEADLINE, row.followups().get(2).deadlineState());
        // Repeated board reads never create or mutate work.
        assertEquals(row, status.board(14).projects().get(0));
        assertEquals(6, work.tasks("P001", true).size());
        assertEquals(4, work.followups("P001", true).size());
    }

    @Test void latestUpdateWinsAndNoTaskDeadlineIsRepresentedExplicitly() throws Exception {
        assertTrue(status.board(14).projects().isEmpty());
        projects.add("P001", "Student A", "Study", TODAY.minusDays(30));
        projects.addUpdate("U002", "P001", TODAY, "Latest", "Proof");
        projects.addUpdate("U001", "P001", TODAY.minusDays(20), "Older entered later", "");
        work.addTask("T001", "P001", "Undated", null);
        var row = status.board(14).projects().get(0);
        assertEquals("Latest", row.latestUpdate().orElseThrow().summary());
        assertFalse(row.stale());
        assertTrue(row.nextDeadline().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> status.board(0));
        assertThrows(IllegalArgumentException.class, () -> status.board(-1));
    }
}
