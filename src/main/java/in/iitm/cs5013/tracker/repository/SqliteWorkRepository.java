package in.iitm.cs5013.tracker.repository;

import in.iitm.cs5013.tracker.model.Task;
import in.iitm.cs5013.tracker.model.Followup;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SqliteWorkRepository implements WorkRepository {
    private final Database database;
    public SqliteWorkRepository(Database database) { this.database = database; }

    public void addTask(Task task) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement(
            "INSERT INTO tasks(id,project_id,description,due_date,completed_on) VALUES(?,?,?,?,?)")) {
            statement.setString(1, task.id());
            statement.setString(2, task.projectId());
            statement.setString(3, task.description());
            statement.setString(4, text(task.dueDate()));
            statement.setString(5, text(task.completedOn()));
            statement.executeUpdate();
        }
    }

    public Optional<Task> task(String id) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement("SELECT * FROM tasks WHERE id=?")) {
            statement.setString(1, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(readTask(result)) : Optional.empty();
            }
        }
    }

    public List<Task> tasks(String projectId, boolean all) throws SQLException, IOException {
        String sql = "SELECT * FROM tasks WHERE project_id=?" + (all ? "" : " AND completed_on IS NULL")
            + " ORDER BY due_date IS NULL, due_date, id";
        try (var connection = database.open(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectId);
            try (var result = statement.executeQuery()) {
                List<Task> items = new ArrayList<>();
                while (result.next()) items.add(readTask(result));
                return List.copyOf(items);
            }
        }
    }

    public void completeTask(String id, LocalDate date) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement(
            "UPDATE tasks SET completed_on=? WHERE id=? AND completed_on IS NULL")) {
            statement.setString(1, date.toString());
            statement.setString(2, id);
            statement.executeUpdate();
        }
    }

    public void addFollowup(Followup followup) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement(
            "INSERT INTO followups(id,project_id,description,requested_on,due_date,resolved_on) VALUES(?,?,?,?,?,?)")) {
            statement.setString(1, followup.id());
            statement.setString(2, followup.projectId());
            statement.setString(3, followup.description());
            statement.setString(4, followup.requestedOn().toString());
            statement.setString(5, text(followup.dueDate()));
            statement.setString(6, text(followup.resolvedOn()));
            statement.executeUpdate();
        }
    }

    public Optional<Followup> followup(String id) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement("SELECT * FROM followups WHERE id=?")) {
            statement.setString(1, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(readFollowup(result)) : Optional.empty();
            }
        }
    }

    public List<Followup> followups(String projectId, boolean all) throws SQLException, IOException {
        String sql = "SELECT * FROM followups WHERE project_id=?" + (all ? "" : " AND resolved_on IS NULL")
            + " ORDER BY due_date IS NULL, due_date, id";
        try (var connection = database.open(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectId);
            try (var result = statement.executeQuery()) {
                List<Followup> items = new ArrayList<>();
                while (result.next()) items.add(readFollowup(result));
                return List.copyOf(items);
            }
        }
    }

    public void resolveFollowup(String id, LocalDate date) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement(
            "UPDATE followups SET resolved_on=? WHERE id=? AND resolved_on IS NULL")) {
            statement.setString(1, date.toString());
            statement.setString(2, id);
            statement.executeUpdate();
        }
    }

    private static String text(LocalDate date) { return date == null ? null : date.toString(); }
    private static LocalDate date(ResultSet result, String field) throws SQLException {
        String value = result.getString(field);
        return value == null ? null : LocalDate.parse(value);
    }
    private static Task readTask(ResultSet result) throws SQLException {
        return new Task(result.getString("id"), result.getString("project_id"), result.getString("description"),
            date(result,"due_date"), date(result,"completed_on"));
    }
    private static Followup readFollowup(ResultSet result) throws SQLException {
        return new Followup(result.getString("id"), result.getString("project_id"), result.getString("description"),
            date(result,"requested_on"), date(result,"due_date"), date(result,"resolved_on"));
    }
}
