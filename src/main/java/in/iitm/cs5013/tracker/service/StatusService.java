package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.model.StatusBoard;
import in.iitm.cs5013.tracker.model.StatusBoard.*;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

public final class StatusService {
    public static final int DEFAULT_STALE_DAYS = 14;
    private final ProjectService projects;
    private final WorkService work;
    private final Clock clock;

    public StatusService(ProjectService projects, WorkService work, Clock clock) {
        this.projects = projects;
        this.work = work;
        this.clock = clock;
    }

    public StatusBoard board(int staleDays) throws SQLException, IOException {
        if (staleDays < 1) throw new IllegalArgumentException("Staleness threshold must be at least 1 day.");
        LocalDate today = LocalDate.now(clock);
        var rows = new ArrayList<ProjectStatus>();
        for (var project : projects.list(false)) {
            var updates = projects.updates(project.id());
            var latest = updates.isEmpty() ? Optional.<in.iitm.cs5013.tracker.model.WeeklyUpdate>empty()
                : Optional.of(updates.get(0));
            LocalDate freshnessDate = latest.map(update -> update.date()).orElse(project.startedOn());
            long age = ChronoUnit.DAYS.between(freshnessDate, today);
            var tasks = work.tasks(project.id(), false).stream()
                .map(task -> new TaskStatus(task, classify(task.dueDate(), today))).toList();
            var followups = work.followups(project.id(), false).stream()
                .map(item -> new FollowupStatus(item, classify(item.dueDate(), today))).toList();
            var nextDeadline = tasks.stream().map(item -> item.task().dueDate())
                .filter(java.util.Objects::nonNull).min(LocalDate::compareTo);
            rows.add(new ProjectStatus(project, latest, freshnessDate, age, age > staleDays,
                nextDeadline, tasks, followups));
        }
        return new StatusBoard(today, staleDays, rows);
    }

    private static DeadlineState classify(LocalDate due, LocalDate today) {
        if (due == null) return DeadlineState.NO_DEADLINE;
        if (due.isBefore(today)) return DeadlineState.OVERDUE;
        if (due.equals(today)) return DeadlineState.DUE_TODAY;
        return DeadlineState.UPCOMING;
    }
}
