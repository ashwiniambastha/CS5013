package in.iitm.cs5013.tracker.importer;

import in.iitm.cs5013.tracker.model.Project;
import in.iitm.cs5013.tracker.model.WeeklyUpdate;
import java.util.List;

public final class ImportData {
    private ImportData() { }
    public static final List<String> COLUMNS = List.of("project_id", "student_name", "project_title",
        "started_on", "update_id", "update_date", "summary", "next_steps");
    public record Row(long number, List<String> values) { }
    public record Parsed(List<Row> rows, List<String> errors) { }
    public record Entry(long row, Project project, WeeklyUpdate update) { }
    public record Result(int projectsAdded, int updatesAdded, int updatesSkipped, boolean dryRun) { }

    public static IllegalArgumentException invalid(List<String> errors) {
        return new IllegalArgumentException("Import rejected; no records saved:\n" + String.join("\n", errors));
    }
}
