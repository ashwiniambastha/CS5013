package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.model.Project;
import in.iitm.cs5013.tracker.model.Task;
import in.iitm.cs5013.tracker.model.Followup;
import in.iitm.cs5013.tracker.repository.WorkRepository;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class WorkService {
    private final WorkRepository repository;
    private final ProjectService projects;
    private final Clock clock;
    public WorkService(WorkRepository repository, ProjectService projects, Clock clock) {
        this.repository = repository;
        this.projects = projects;
        this.clock = clock;
    }

    public void addTask(String id, String projectId, String description, LocalDate due) throws SQLException, IOException {
        Task task = new Task(id, projectId, description, due, null);
        Project project = activeProject(task.projectId());
        if (due != null && due.isBefore(project.startedOn()))
            throw new IllegalArgumentException("Due date is before the project start date.");
        if (repository.task(task.id()).isPresent()) throw new IllegalArgumentException("Task ID already exists: " + task.id());
        repository.addTask(task);
    }

    public List<Task> tasks(String projectId, boolean all) throws SQLException, IOException {
        return repository.tasks(projects.get(projectId).id(), all);
    }

    public void completeTask(String id) throws SQLException, IOException {
        String normalized = Project.required(id, "Task ID");
        Task task = repository.task(normalized).orElseThrow(() -> new IllegalArgumentException("Unknown task: " + normalized));
        // A repeated click/command preserves the original completion date.
        if (task.isOpen()) repository.completeTask(task.id(), LocalDate.now(clock));
    }

    public void addFollowup(String id, String projectId, String description, LocalDate requested, LocalDate due)
            throws SQLException, IOException {
        Followup item = new Followup(id, projectId, description, requested, due, null);
        Project project = activeProject(item.projectId());
        if (requested.isBefore(project.startedOn()) || requested.isAfter(LocalDate.now(clock)))
            throw new IllegalArgumentException("Request date must be between project start and today.");
        if (due != null && due.isBefore(requested)) throw new IllegalArgumentException("Due date is before the request date.");
        if (repository.followup(item.id()).isPresent()) throw new IllegalArgumentException("Follow-up ID already exists: " + item.id());
        repository.addFollowup(item);
    }

    public List<Followup> followups(String projectId, boolean all) throws SQLException, IOException {
        return repository.followups(projects.get(projectId).id(), all);
    }

    public void resolveFollowup(String id) throws SQLException, IOException {
        String normalized = Project.required(id, "Follow-up ID");
        Followup item = repository.followup(normalized).orElseThrow(() -> new IllegalArgumentException("Unknown follow-up: " + normalized));
        if (item.isOpen()) repository.resolveFollowup(item.id(), LocalDate.now(clock));
    }

    private Project activeProject(String id) throws SQLException, IOException {
        Project project = projects.get(id);
        if (project.status() != Project.Status.ACTIVE) throw new IllegalArgumentException("Project is completed: " + project.id());
        return project;
    }
}
