package in.iitm.cs5013.tracker.config;

import java.nio.file.Path;

/** Keeps persistent data outside the installed application. */
public final class AppPaths {
    private AppPaths() { }

    public static Path reports() {
        return database().toAbsolutePath().getParent().resolve("reports");
    }

    public static Path database() {
        String override = System.getProperty("tracker.dataDir");
        if (override != null && !override.isBlank()) {
            return Path.of(override).resolve("tracker.db");
        }
        String local = System.getenv("LOCALAPPDATA");
        Path base = local != null && !local.isBlank()
                ? Path.of(local)
                : Path.of(System.getProperty("user.home"), ".local", "share");
        return base.resolve("StudentProjectTracker").resolve("tracker.db");
    }
}
