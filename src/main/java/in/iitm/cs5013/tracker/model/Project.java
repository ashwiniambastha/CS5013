package in.iitm.cs5013.tracker.model;

import java.time.LocalDate;
import java.util.Objects;

public record Project(String id, String studentName, String title, Status status, LocalDate startedOn) {
    public enum Status { ACTIVE, COMPLETED }

    public Project {
        id = required(id, "Project ID");
        studentName = required(studentName, "Student name");
        title = required(title, "Project title");
        Objects.requireNonNull(status, "Status is required");
        Objects.requireNonNull(startedOn, "Start date is required");
    }

    public static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }
}
