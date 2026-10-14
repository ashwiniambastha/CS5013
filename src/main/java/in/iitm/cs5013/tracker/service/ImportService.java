package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.importer.*;
import in.iitm.cs5013.tracker.model.*;
import in.iitm.cs5013.tracker.repository.SqliteImportRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

public final class ImportService {
    private final SqliteImportRepository repository;
    private final Clock clock;
    public ImportService(SqliteImportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public ImportData.Result importFile(Path file, boolean dryRun) throws IOException, SQLException {
        var parsed = new UpdateFileReader().read(file);
        var errors = new ArrayList<>(parsed.errors());
        var entries = new ArrayList<ImportData.Entry>();
        var projects = new HashMap<String, Project>();
        var updates = new HashSet<String>();
        LocalDate today = LocalDate.now(clock);
        for (var row : parsed.rows()) {
            try {
                var v = row.values();
                Project project = new Project(v.get(0), v.get(1), v.get(2), Project.Status.ACTIVE, iso(v.get(3), "started_on"));
                WeeklyUpdate update = new WeeklyUpdate(v.get(4), project.id(), iso(v.get(5), "update_date"), v.get(6), v.get(7));
                if (project.startedOn().isAfter(today)) throw new IllegalArgumentException("Start date cannot be in the future.");
                if (update.date().isBefore(project.startedOn()) || update.date().isAfter(today))
                    throw new IllegalArgumentException("Update date must be between project start and today.");
                Project previous = projects.putIfAbsent(project.id(), project);
                if (previous != null && !previous.equals(project)) throw new IllegalArgumentException("Conflicting project details for " + project.id());
                if (!updates.add(update.id())) throw new IllegalArgumentException("Duplicate update_id " + update.id());
                entries.add(new ImportData.Entry(row.number(), project, update));
            } catch (IllegalArgumentException exception) {
                errors.add("Row " + row.number() + ": " + exception.getMessage());
            }
        }
        if (parsed.rows().isEmpty() && errors.isEmpty()) errors.add("File: no update rows found.");
        if (!errors.isEmpty()) throw ImportData.invalid(errors);
        return repository.importEntries(entries, file.getFileName().toString(), clock.instant(), dryRun);
    }

    private static LocalDate iso(String value, String column) {
        if (!value.trim().matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException(column + " must use yyyy-MM-dd.");
        try { return LocalDate.parse(value.trim()); }
        catch (java.time.format.DateTimeParseException exception) { throw new IllegalArgumentException(column + " is not a valid date."); }
    }
}
