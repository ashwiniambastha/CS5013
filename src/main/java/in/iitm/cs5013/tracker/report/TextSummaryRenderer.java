package in.iitm.cs5013.tracker.report;

import in.iitm.cs5013.tracker.model.StatusBoard;
import java.util.ArrayList;

/** Deterministic meeting summary; no date rules or database access here. */
public final class TextSummaryRenderer {
    public String render(StatusBoard board) {
        var text = new StringBuilder();
        text.append("SUPERVISION SUMMARY - ").append(board.asOf()).append('\n');
        text.append("Active projects: ").append(board.projects().size()).append('\n');
        text.append("Missing-update threshold: more than ").append(board.staleDays()).append(" days\n");
        if (board.projects().isEmpty()) text.append("\nNo active projects.\n");
        for (var row : board.projects()) {
            text.append('\n').append(line(row.project().studentName())).append(" - ")
                .append(line(row.project().title())).append(" [").append(line(row.project().id())).append("]\n");
            if (row.latestUpdate().isPresent()) {
                var update = row.latestUpdate().get();
                text.append("Latest update: ").append(update.date()).append(" - ").append(line(update.summary())).append('\n');
                text.append("Next steps: ").append(update.nextSteps().isBlank() ? "Not recorded" : line(update.nextSteps())).append('\n');
            } else {
                text.append("Latest update: None; project started ").append(row.freshnessDate()).append('\n');
                text.append("Next steps: Not recorded\n");
            }
            text.append("Next deadline (earliest open task): ").append(row.nextDeadline().map(Object::toString).orElse("None")).append('\n');
            text.append("Open tasks: ").append(row.tasks().size()).append('\n');
            for (var item : row.tasks()) text.append("  - ").append(line(item.task().id())).append(": ")
                .append(line(item.task().description())).append(" | due ")
                .append(item.task().dueDate() == null ? "not set" : item.task().dueDate())
                .append(" | ").append(item.deadlineState()).append('\n');
            text.append("Pending follow-ups: ").append(row.followups().size()).append('\n');
            for (var item : row.followups()) text.append("  - ").append(line(item.followup().id())).append(": ")
                .append(line(item.followup().description())).append(" | requested ").append(item.followup().requestedOn())
                .append(" | due ").append(item.followup().dueDate() == null ? "not set" : item.followup().dueDate())
                .append(" | ").append(item.deadlineState()).append('\n');
            var attention = new ArrayList<String>();
            if (row.stale()) attention.add("Missing update (" + row.daysSinceUpdate() + " days since "
                + (row.latestUpdate().isPresent() ? "latest update" : "project start") + ")");
            for (var item : row.tasks()) {
                if (item.deadlineState() == StatusBoard.DeadlineState.OVERDUE) attention.add("Overdue task " + line(item.task().id()));
                if (item.deadlineState() == StatusBoard.DeadlineState.DUE_TODAY) attention.add("Task due today " + line(item.task().id()));
            }
            if (!row.followups().isEmpty()) attention.add(row.followups().size() + " pending follow-up(s)");
            for (var item : row.followups()) {
                if (item.deadlineState() == StatusBoard.DeadlineState.OVERDUE) attention.add("Overdue follow-up " + line(item.followup().id()));
                if (item.deadlineState() == StatusBoard.DeadlineState.DUE_TODAY) attention.add("Follow-up due today " + line(item.followup().id()));
            }
            text.append("Attention: ").append(attention.isEmpty() ? "None" : String.join("; ", attention)).append('\n');
        }
        return text.toString();
    }

    private static String line(String value) { return value.replaceAll("\\s+", " ").trim(); }
}
