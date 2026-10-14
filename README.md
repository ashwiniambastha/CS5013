# Student Project Progress Tracker

CS5013 Programming with AI project for Prof. Neelesh Shankar Upadhye.
A local Java application for weekly student updates, tasks, deadlines,
follow-ups, and supervision reports.

## Current progress

Parts 1 and 2 are complete: Maven structure, a runnable entry point, and versioned
SQLite storage. Startup creates the five tables without erasing existing data.
Project entry and the desktop interface are not implemented yet.
See [the design roadmap](docs/design-roadmap.md) and [Part 2 explanation](docs/part-02-storage.md).

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
