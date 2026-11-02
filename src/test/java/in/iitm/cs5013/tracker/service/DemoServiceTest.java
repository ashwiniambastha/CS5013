package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class DemoServiceTest {
    @TempDir Path temporary;
    @Test void twelveWeekDemoHasExpectedAttentionFlagsAndPreservesEdits() throws Exception {
        var database = new Database(temporary.resolve("demo/tracker.db"));
        var clock = Clock.fixed(Instant.parse("2026-11-02T06:00:00Z"),ZoneOffset.UTC);
        var demo = new DemoService(database,clock);
        assertTrue(demo.seed());
        var projects = new ProjectService(new SqliteProjectRepository(database),clock);
        var work = new WorkService(new SqliteWorkRepository(database),projects,clock);
        var board = new StatusService(projects,work,clock).board(14);
        assertEquals(5,projects.list(true).size()); assertEquals(4,board.projects().size());
        assertEquals(12,projects.updates("D001").size());
        assertFalse(board.projects().get(0).stale()); assertTrue(board.projects().get(1).stale());
        assertFalse(board.projects().get(2).stale()); assertTrue(board.projects().get(3).stale());
        assertEquals(2,board.projects().get(2).tasks().size());
        assertTrue(board.projects().get(2).followups().isEmpty());
        work.completeTask("DT2");
        assertFalse(demo.seed()); assertTrue(work.tasks("D002",false).isEmpty());
        assertEquals(12,projects.updates("D001").size());
    }
}
