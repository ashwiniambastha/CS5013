package in.iitm.cs5013.tracker;

import in.iitm.cs5013.tracker.config.AppPaths;
import in.iitm.cs5013.tracker.repository.Database;
import in.iitm.cs5013.tracker.repository.SqliteProjectRepository;
import in.iitm.cs5013.tracker.service.ProjectService;
import in.iitm.cs5013.tracker.service.WorkService;
import in.iitm.cs5013.tracker.repository.SqliteWorkRepository;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Entry point. Tracking features will be added in the next implementation parts. */
@Command(name="tracker", mixinStandardHelpOptions=true, description="Local student project progress tracker")
public final class TrackerApplication implements Runnable {
    private final ProjectService service;
    private final WorkService work;
    public TrackerApplication(ProjectService service, WorkService work) {
        this.service = service;
        this.work = work;
    }
    public void run() {
        System.out.println("Student Project Progress Tracker - Part 4");
        System.out.println("Use --help for commands. Status board comes in Part 5.");
    }

    @Command(name="project-add", description="Create an active project")
    void add(@Option(names="--id", required=true) String id,
             @Option(names="--student", required=true) String student,
             @Option(names="--title", required=true) String title,
             @Option(names="--start", required=true, description="ISO date: yyyy-MM-dd") LocalDate start) throws Exception {
        service.add(id, student, title, start);
        System.out.println("Added project " + id);
    }

    @Command(name="project-list", description="List active projects")
    void list(@Option(names="--all", description="Include completed projects") boolean all) throws Exception {
        var projects = service.list(all);
        if (projects.isEmpty()) System.out.println("No projects found.");
        for (var project : projects) System.out.printf("%s | %s | %s | %s | started %s%n",
            project.id(), project.studentName(), project.title(), project.status(), project.startedOn());
    }

    @Command(name="project-complete", description="Mark a project completed; retain its history")
    void complete(@Option(names="--id", required=true) String id) throws Exception {
        service.complete(id);
        System.out.println("Completed project " + id);
    }

    @Command(name="update-add", description="Record a dated weekly update")
    void update(@Option(names="--id", description="Optional stable update ID") String id,
                @Option(names="--project", required=true) String project,
                @Option(names="--date", required=true, description="ISO date: yyyy-MM-dd") LocalDate date,
                @Option(names="--summary", required=true) String summary,
                @Option(names="--next", defaultValue="") String next) throws Exception {
        String updateId = id == null ? UUID.randomUUID().toString() : id;
        service.addUpdate(updateId, project, date, summary, next);
        System.out.println("Added update " + updateId);
    }

    @Command(name="update-list", description="Show updates, newest date first")
    void updates(@Option(names="--project", required=true) String project) throws Exception {
        var updates = service.updates(project);
        if (updates.isEmpty()) System.out.println("No updates recorded.");
        for (var update : updates) System.out.printf("%s | %s | %s%n  Next: %s%n",
            update.id(), update.date(), update.summary(), update.nextSteps());
    }

    @Command(name="task-add", description="Add work to finish; due date is optional")
    void taskAdd(@Option(names="--id", required=true) String id,
                 @Option(names="--project", required=true) String project,
                 @Option(names="--text", required=true) String text,
                 @Option(names="--due") LocalDate due) throws Exception {
        work.addTask(id, project, text, due);
        System.out.println("Added task " + id);
    }

    @Command(name="task-list", description="List open tasks for a project")
    void taskList(@Option(names="--project", required=true) String project,
                  @Option(names="--all") boolean all) throws Exception {
        var items = work.tasks(project, all);
        if (items.isEmpty()) System.out.println("No tasks found.");
        for (var item : items) System.out.printf("%s | %s | due %s | %s%n", item.id(), item.description(),
            item.dueDate() == null ? "not set" : item.dueDate(), item.isOpen() ? "OPEN" : "completed " + item.completedOn());
    }

    @Command(name="task-done", description="Complete a task today")
    void taskDone(@Option(names="--id", required=true) String id) throws Exception {
        work.completeTask(id);
        System.out.println("Task " + id + " is completed.");
    }

    @Command(name="followup-add", description="Record a request awaiting a response")
    void followupAdd(@Option(names="--id", required=true) String id,
                     @Option(names="--project", required=true) String project,
                     @Option(names="--text", required=true) String text,
                     @Option(names="--requested", required=true) LocalDate requested,
                     @Option(names="--due") LocalDate due) throws Exception {
        work.addFollowup(id, project, text, requested, due);
        System.out.println("Added follow-up " + id);
    }

    @Command(name="followup-list", description="List pending follow-ups for a project")
    void followupList(@Option(names="--project", required=true) String project,
                      @Option(names="--all") boolean all) throws Exception {
        var items = work.followups(project, all);
        if (items.isEmpty()) System.out.println("No follow-ups found.");
        for (var item : items) System.out.printf("%s | %s | requested %s | due %s | %s%n", item.id(), item.description(),
            item.requestedOn(), item.dueDate() == null ? "not set" : item.dueDate(),
            item.isOpen() ? "PENDING" : "resolved " + item.resolvedOn());
    }

    @Command(name="followup-resolve", description="Mark a request answered today")
    void followupResolve(@Option(names="--id", required=true) String id) throws Exception {
        work.resolveFollowup(id);
        System.out.println("Follow-up " + id + " is resolved.");
    }

    public static void main(String[] args) throws Exception {
        Database database = new Database(AppPaths.database());
        database.initialize();
        var service = new ProjectService(new SqliteProjectRepository(database), Clock.systemDefaultZone());
        var work = new WorkService(new SqliteWorkRepository(database), service, Clock.systemDefaultZone());
        var cli = new CommandLine(new TrackerApplication(service, work));
        cli.setExecutionExceptionHandler((exception, command, parseResult) -> {
            command.getErr().println("Error: " + exception.getMessage());
            return 1;
        });
        System.exit(cli.execute(args));
    }
}
