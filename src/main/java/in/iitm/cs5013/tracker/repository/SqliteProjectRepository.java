package in.iitm.cs5013.tracker.repository;

import in.iitm.cs5013.tracker.model.Project;
import in.iitm.cs5013.tracker.model.WeeklyUpdate;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SqliteProjectRepository implements ProjectRepository {
    private final Database database;
    public SqliteProjectRepository(Database database) { this.database = database; }

    public void add(Project project) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement(
                "INSERT INTO projects(id,student_name,title,status,started_on) VALUES(?,?,?,?,?)")) {
            statement.setString(1, project.id());
            statement.setString(2, project.studentName());
            statement.setString(3, project.title());
            statement.setString(4, project.status().name());
            statement.setString(5, project.startedOn().toString());
            statement.executeUpdate();
        }
    }

    public Optional<Project> find(String id) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement("SELECT * FROM projects WHERE id=?")) {
            statement.setString(1, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(readProject(result)) : Optional.empty();
            }
        }
    }

    public List<Project> list(boolean includeCompleted) throws SQLException, IOException {
        String sql = "SELECT * FROM projects" + (includeCompleted ? "" : " WHERE status='ACTIVE'") + " ORDER BY id";
        try (var connection = database.open(); var statement = connection.prepareStatement(sql); var result = statement.executeQuery()) {
            List<Project> projects = new ArrayList<>();
            while (result.next()) projects.add(readProject(result));
            return List.copyOf(projects);
        }
    }

    public void complete(String id) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement("UPDATE projects SET status='COMPLETED' WHERE id=?")) {
            statement.setString(1, id);
            if (statement.executeUpdate() == 0) throw new IllegalArgumentException("Unknown project: " + id);
        }
    }

    public void addUpdate(WeeklyUpdate update) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement(
                "INSERT INTO updates(id,project_id,update_date,summary,next_steps) VALUES(?,?,?,?,?)")) {
            statement.setString(1, update.id());
            statement.setString(2, update.projectId());
            statement.setString(3, update.date().toString());
            statement.setString(4, update.summary());
            statement.setString(5, update.nextSteps());
            statement.executeUpdate();
        }
    }

    public List<WeeklyUpdate> updates(String projectId) throws SQLException, IOException {
        try (var connection = database.open(); var statement = connection.prepareStatement(
                "SELECT * FROM updates WHERE project_id=? ORDER BY update_date DESC, id DESC")) {
            statement.setString(1, projectId);
            try (var result = statement.executeQuery()) {
                List<WeeklyUpdate> updates = new ArrayList<>();
                while (result.next()) updates.add(new WeeklyUpdate(result.getString("id"), result.getString("project_id"),
                    LocalDate.parse(result.getString("update_date")), result.getString("summary"), result.getString("next_steps")));
                return List.copyOf(updates);
            }
        }
    }

    private static Project readProject(ResultSet result) throws SQLException {
        return new Project(result.getString("id"), result.getString("student_name"), result.getString("title"),
            Project.Status.valueOf(result.getString("status")), LocalDate.parse(result.getString("started_on")));
    }
}
