package in.iitm.cs5013.tracker;

import in.iitm.cs5013.tracker.config.AppPaths;
import in.iitm.cs5013.tracker.repository.Database;
import in.iitm.cs5013.tracker.repository.SqliteProjectRepository;
import in.iitm.cs5013.tracker.service.ProjectService;
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
    public TrackerApplication(ProjectService service) { this.service = service; }
    public void run() {
        System.out.println("Student Project Progress Tracker - Part 3");
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

    public static void main(String[] args) throws Exception {
        Database database = new Database(AppPaths.database());
        database.initialize();
        var service = new ProjectService(new SqliteProjectRepository(database), Clock.systemDefaultZone());
        var cli = new CommandLine(new TrackerApplication(service));
        cli.setExecutionExceptionHandler((exception, command, parseResult) -> {
            command.getErr().println("Error: " + exception.getMessage());
            return 1;
        });
        System.exit(cli.execute(args));
    }
}
