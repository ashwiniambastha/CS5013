package in.iitm.cs5013.tracker;

import in.iitm.cs5013.tracker.config.AppPaths;
import in.iitm.cs5013.tracker.repository.Database;

/** Entry point. Tracking features will be added in the next implementation parts. */
public final class TrackerApplication {
    private TrackerApplication() {
    }

    public static void main(String[] args) throws Exception {
        var databasePath = AppPaths.database();
        new Database(databasePath).initialize();
        System.out.println("Student Project Progress Tracker");
        System.out.println("Part 2: local database is ready at " + databasePath);
        System.out.println("Project entry and the status board are not implemented yet.");
    }
}
