# Student Project Progress Tracker

CS5013 Programming with AI project for Prof. Neelesh Shankar Upadhye.
A local Java application for weekly student updates, tasks, deadlines,
follow-ups, and supervision reports.

## Current progress

Part 1 is complete: Maven project structure and a runnable Java entry point.
There is no database or desktop interface yet. See [the design roadmap](docs/design-roadmap.md).

## Run

Requires JDK 17 or later and Maven for development:

```powershell
mvn package
java -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar
```

Without Maven, verify the initial entry point using a JDK:

```powershell
javac --release 17 -d target/classes src/main/java/in/iitm/cs5013/tracker/TrackerApplication.java
java -cp target/classes in.iitm.cs5013.tracker.TrackerApplication
```

The final desktop package will include a Java runtime. Generated databases and
reports are excluded from Git; use anonymised fixtures for demonstrations.
