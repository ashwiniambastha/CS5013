package in.iitm.cs5013.tracker.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** A read-only snapshot. Dates and classifications are calculated, never stored. */
public record StatusBoard(LocalDate asOf, int staleDays, List<ProjectStatus> projects) {
    public StatusBoard { projects = List.copyOf(projects); }

    public enum DeadlineState { NO_DEADLINE, OVERDUE, DUE_TODAY, UPCOMING }
    public record TaskStatus(Task task, DeadlineState deadlineState) { }
    public record FollowupStatus(Followup followup, DeadlineState deadlineState) { }
    public record ProjectStatus(Project project, Optional<WeeklyUpdate> latestUpdate,
                                LocalDate freshnessDate, long daysSinceUpdate, boolean stale,
                                Optional<LocalDate> nextDeadline,
                                List<TaskStatus> tasks, List<FollowupStatus> followups) {
        public ProjectStatus {
            tasks = List.copyOf(tasks);
            followups = List.copyOf(followups);
        }
    }
}
