# Student Project Progress Tracker

CS5013 Programming with AI project for Prof. Neelesh Shankar Upadhye.
A local Java application for weekly student updates, tasks, deadlines,
follow-ups, and supervision reports.

## Current progress

Parts 1 through 4 are complete: Maven structure, versioned SQLite storage, and
commands to add/list/complete projects and record/list weekly updates.
Startup creates the five tables without erasing existing data.
Tasks and follow-ups can be added, listed and closed independently, with optional
deadlines and retained history. Status warnings, reports and the desktop interface
are not implemented yet.
See [the design roadmap](docs/design-roadmap.md) and [Part 2 explanation](docs/part-02-storage.md).
For commands and a worked example, read [Part 3](docs/part-03-projects-updates.md).
For tasks and follow-ups, read [Part 4](docs/part-04-tasks-followups.md).

## Run

Requires JDK 17 or later and Maven for development:

```powershell
mvn package
java -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar
```

The package includes its SQLite dependency. Run tests with `mvn test`.
On Windows, persistent data defaults to `%LOCALAPPDATA%/StudentProjectTracker/tracker.db`.
For an isolated demonstration, run:

```powershell
java -Dtracker.dataDir=target/demo-data -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar
```

The final desktop package will include a Java runtime. Generated databases and
reports are excluded from Git; use anonymised fixtures for demonstrations.
