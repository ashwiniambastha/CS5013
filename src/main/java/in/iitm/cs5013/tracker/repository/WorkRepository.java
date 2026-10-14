package in.iitm.cs5013.tracker.repository;

import in.iitm.cs5013.tracker.model.Task;
import in.iitm.cs5013.tracker.model.Followup;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkRepository {
    void addTask(Task task) throws SQLException, IOException;
    Optional<Task> task(String id) throws SQLException, IOException;
    List<Task> tasks(String projectId, boolean all) throws SQLException, IOException;
    void completeTask(String id, LocalDate date) throws SQLException, IOException;
    void addFollowup(Followup followup) throws SQLException, IOException;
    Optional<Followup> followup(String id) throws SQLException, IOException;
    List<Followup> followups(String projectId, boolean all) throws SQLException, IOException;
    void resolveFollowup(String id, LocalDate date) throws SQLException, IOException;
}
