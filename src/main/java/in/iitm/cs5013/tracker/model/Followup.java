package in.iitm.cs5013.tracker.model;

import java.time.LocalDate;
import java.util.Objects;

public record Followup(String id, String projectId, String description, LocalDate requestedOn,
                       LocalDate dueDate, LocalDate resolvedOn) {
    public Followup {
        id = Project.required(id, "Follow-up ID");
        projectId = Project.required(projectId, "Project ID");
        description = Project.required(description, "Description");
        Objects.requireNonNull(requestedOn, "Request date is required");
    }
    public boolean isOpen() { return resolvedOn == null; }
}
