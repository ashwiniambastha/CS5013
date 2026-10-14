package in.iitm.cs5013.tracker.service;

import in.iitm.cs5013.tracker.model.Project;
import in.iitm.cs5013.tracker.model.WeeklyUpdate;
import in.iitm.cs5013.tracker.repository.ProjectRepository;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class ProjectService {
    private final ProjectRepository repository;
    private final Clock clock;
    public ProjectService(ProjectRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void add(String id, String student, String title, LocalDate start) throws SQLException, IOException {
        Project project = new Project(id, student, title, Project.Status.ACTIVE, start);
        if (start.isAfter(LocalDate.now(clock))) throw new IllegalArgumentException("Start date cannot be in the future.");
        if (repository.find(project.id()).isPresent()) throw new IllegalArgumentException("Project ID already exists: " + project.id());
        repository.add(project);
    }

    public List<Project> list(boolean all) throws SQLException, IOException { return repository.list(all); }

    public Project get(String id) throws SQLException, IOException {
        String normalized = Project.required(id, "Project ID");
        return repository.find(normalized).orElseThrow(() -> new IllegalArgumentException("Unknown project: " + normalized));
    }

    public void complete(String id) throws SQLException, IOException { repository.complete(get(id).id()); }

    public void addUpdate(String id, String projectId, LocalDate date, String summary, String next) throws SQLException, IOException {
        WeeklyUpdate update = new WeeklyUpdate(id, projectId, date, summary, next);
        Project project = get(update.projectId());
        if (project.status() != Project.Status.ACTIVE) throw new IllegalArgumentException("Cannot add updates to a completed project.");
        if (date.isBefore(project.startedOn())) throw new IllegalArgumentException("Update date is before the project start date.");
        if (date.isAfter(LocalDate.now(clock))) throw new IllegalArgumentException("Update date cannot be in the future.");
        if (repository.updates(project.id()).stream().anyMatch(existing -> existing.id().equals(update.id())))
            throw new IllegalArgumentException("Update ID already exists: " + update.id());
        repository.addUpdate(update);
    }

    public List<WeeklyUpdate> updates(String id) throws SQLException, IOException { return repository.updates(get(id).id()); }
}
