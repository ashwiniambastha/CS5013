package in.iitm.cs5013.tracker.model;

import java.time.LocalDate;
import java.util.Objects;

public record WeeklyUpdate(String id, String projectId, LocalDate date, String summary, String nextSteps) {
    public WeeklyUpdate {
        id = Project.required(id, "Update ID");
        projectId = Project.required(projectId, "Project ID");
        summary = Project.required(summary, "Summary");
        Objects.requireNonNull(date, "Update date is required");
        nextSteps = nextSteps == null ? "" : nextSteps.trim();
    }
}
