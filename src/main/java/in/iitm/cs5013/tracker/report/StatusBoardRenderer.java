package in.iitm.cs5013.tracker.report;

import in.iitm.cs5013.tracker.model.StatusBoard;
import java.io.PrintWriter;

/** Presentation only: all date decisions come from StatusService. */
public final class StatusBoardRenderer {
    public void render(StatusBoard board, PrintWriter out) {
        out.printf("PROJECT STATUS BOARD - %s%n", board.asOf());
        out.printf("Missing-update threshold: more than %d days%n", board.staleDays());
        if (board.projects().isEmpty()) out.println("No active projects.");
        for (var row : board.projects()) {
            var project = row.project();
            out.printf("%n%s | %s | %s%n", project.id(), project.studentName(), project.title());
            if (row.latestUpdate().isPresent()) {
                var update = row.latestUpdate().get();
                out.printf("  Latest update: %s - %s%n", update.date(), update.summary());
                if (!update.nextSteps().isBlank()) out.printf("  Next steps: %s%n", update.nextSteps());
            } else {
                out.printf("  No updates yet; project started %s%n", row.freshnessDate());
            }
            out.printf("  Update check: %s (%d days since %s)%n", row.stale() ? "MISSING UPDATE" : "WITHIN THRESHOLD",
                row.daysSinceUpdate(), row.latestUpdate().isPresent() ? "latest update" : "project start");
            out.printf("  Next deadline (earliest open task): %s%n", row.nextDeadline().map(Object::toString).orElse("none"));
            out.printf("  Open tasks: %d%n", row.tasks().size());
            for (var item : row.tasks()) out.printf("    %s | %s | due %s | %s%n", item.task().id(),
                item.task().description(), item.task().dueDate() == null ? "not set" : item.task().dueDate(), item.deadlineState());
            out.printf("  Pending follow-ups: %d%n", row.followups().size());
            for (var item : row.followups()) out.printf("    %s | %s | requested %s | due %s | %s%n", item.followup().id(),
                item.followup().description(), item.followup().requestedOn(),
                item.followup().dueDate() == null ? "not set" : item.followup().dueDate(), item.deadlineState());
        }
        out.flush();
    }
}
