package in.iitm.cs5013.tracker.repository;

import in.iitm.cs5013.tracker.importer.ImportData;
import in.iitm.cs5013.tracker.model.*;
import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Validates existing IDs and writes the complete batch on one connection. */
public final class SqliteImportRepository {
    private final Database database;
    public SqliteImportRepository(Database database) { this.database = database; }

    public ImportData.Result importEntries(List<ImportData.Entry> entries, String source, Instant time, boolean dryRun)
            throws SQLException, IOException {
        try (var connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                var errors = new ArrayList<String>();
                var newProjects = new LinkedHashMap<String, Project>();
                var newUpdates = new ArrayList<WeeklyUpdate>();
                int skipped = 0;
                for (var entry : entries) {
                    var project = entry.project();
                    Project existing = project(connection, project.id());
                    if (existing == null) newProjects.putIfAbsent(project.id(), project);
                    else if (!existing.studentName().equals(project.studentName()) || !existing.title().equals(project.title())
                            || !existing.startedOn().equals(project.startedOn())) {
                        errors.add("Row " + entry.row() + ": project_id conflicts with stored project " + project.id());
                    }
                    WeeklyUpdate previous = update(connection, entry.update().id());
                    if (previous != null) {
                        if (previous.equals(entry.update())) skipped++;
                        else errors.add("Row " + entry.row() + ": update_id conflicts with stored update " + entry.update().id());
                    } else if (existing != null && existing.status() == Project.Status.COMPLETED) {
                        errors.add("Row " + entry.row() + ": cannot add an update to completed project " + project.id());
                    } else newUpdates.add(entry.update());
                }
                if (!errors.isEmpty()) throw ImportData.invalid(errors);
                if (!dryRun) {
                    for (var project : newProjects.values()) insertProject(connection, project);
                    for (var update : newUpdates) insertUpdate(connection, update);
                    if (!newProjects.isEmpty() || !newUpdates.isEmpty()) {
                        try (var statement = connection.prepareStatement("INSERT INTO import_runs(imported_at,source_file,row_count) VALUES(?,?,?)")) {
                            statement.setString(1, time.toString());
                            statement.setString(2, source);
                            statement.setInt(3, entries.size());
                            statement.executeUpdate();
                        }
                    }
                    connection.commit();
                } else connection.rollback();
                return new ImportData.Result(newProjects.size(), newUpdates.size(), skipped, dryRun);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static Project project(Connection connection, String id) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT * FROM projects WHERE id=?")) {
            statement.setString(1,id);
            try (var row = statement.executeQuery()) {
                return !row.next() ? null : new Project(row.getString("id"), row.getString("student_name"), row.getString("title"),
                    Project.Status.valueOf(row.getString("status")), LocalDate.parse(row.getString("started_on")));
            }
        }
    }
    private static WeeklyUpdate update(Connection connection, String id) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT * FROM updates WHERE id=?")) {
            statement.setString(1,id);
            try (var row = statement.executeQuery()) {
                return !row.next() ? null : new WeeklyUpdate(row.getString("id"),row.getString("project_id"),
                    LocalDate.parse(row.getString("update_date")),row.getString("summary"),row.getString("next_steps"));
            }
        }
    }
    private static void insertProject(Connection connection, Project project) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO projects(id,student_name,title,status,started_on) VALUES(?,?,?,?,?)")) {
            statement.setString(1,project.id()); statement.setString(2,project.studentName()); statement.setString(3,project.title());
            statement.setString(4,project.status().name()); statement.setString(5,project.startedOn().toString()); statement.executeUpdate();
        }
    }
    private static void insertUpdate(Connection connection, WeeklyUpdate update) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO updates(id,project_id,update_date,summary,next_steps) VALUES(?,?,?,?,?)")) {
            statement.setString(1,update.id()); statement.setString(2,update.projectId()); statement.setString(3,update.date().toString());
            statement.setString(4,update.summary()); statement.setString(5,update.nextSteps()); statement.executeUpdate();
        }
    }
}
