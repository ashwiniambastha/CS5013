package in.iitm.cs5013.tracker.model;

import java.time.LocalDate;

public record Task(String id, String projectId, String description, LocalDate dueDate, LocalDate completedOn) {
    public Task {
        id = Project.required(id, "Task ID");
        projectId = Project.required(projectId, "Project ID");
        description = Project.required(description, "Description");
    }
    public boolean isOpen() { return completedOn == null; }
}
