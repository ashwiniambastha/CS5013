package in.iitm.cs5013.tracker.repository;

import in.iitm.cs5013.tracker.model.Project;
import in.iitm.cs5013.tracker.model.WeeklyUpdate;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    void add(Project project) throws SQLException, IOException;
    Optional<Project> find(String id) throws SQLException, IOException;
    List<Project> list(boolean includeCompleted) throws SQLException, IOException;
    void complete(String id) throws SQLException, IOException;
    void addUpdate(WeeklyUpdate update) throws SQLException, IOException;
    List<WeeklyUpdate> updates(String projectId) throws SQLException, IOException;
}
