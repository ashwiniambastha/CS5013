# Part 3: Projects and weekly updates

## What to understand first

A project identifies the work being supervised. A weekly update is a dated note
linked to that project. One project can have many updates.

Project P001: Student A, Graph Theory, started 1 October.
Update U001: P001, 7 October, finished reading a paper.
Update U002: P001, 14 October, drafted a proof.

## How one command moves through the application

1. picocli parses the command and converts its date to LocalDate.
2. ProjectService checks required fields, IDs, project status and dates.
3. Project/WeeklyUpdate records carry validated values.
4. SqliteProjectRepository uses prepared statements to write SQLite records.
5. The command prints confirmation only after the write succeeds.

Prepared statements pass notes as data, so quotes in a title do not become SQL.
The ProjectRepository interface describes storage operations; the SQLite class
implements them. Swing will call the same service later.

## Try the workflow

Build with `mvn package`. From the repository folder, run these PowerShell commands.
The demo directory is separate from the professor's normal application-data folder.

```powershell
java -Dtracker.dataDir=target/part3-demo -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar project-add --id P001 --student "Student A" --title "Graph Theory" --start 2026-10-01
java -Dtracker.dataDir=target/part3-demo -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar update-add --id U001 --project P001 --date 2026-10-07 --summary "Read the first paper" --next "Draft a proof"
java -Dtracker.dataDir=target/part3-demo -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar project-list
java -Dtracker.dataDir=target/part3-demo -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar update-list --project P001
java -Dtracker.dataDir=target/part3-demo -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar project-complete --id P001
java -Dtracker.dataDir=target/part3-demo -jar target/student-project-tracker-0.1.0-SNAPSHOT.jar project-list --all
```

Use a fresh demo directory when repeating the initial add commands: duplicate IDs
are intentionally rejected. Omit --id on update-add to generate a UUID.

## Rules introduced here

- Required text cannot be blank; surrounding whitespace is trimmed.
- Project IDs and update IDs are unique (also enforced by database primary keys).
- Start/update dates cannot be in the future; updates cannot predate the project.
- Updates require an existing active project.
- Completing a project retains its updates and hides it from the default list.
- Updates sort by date descending, then ID descending for a deterministic tie.
  Same-day IDs do not imply time order; they are only the tie-breaker.

The CLI currently uses flat command names (project-add, update-list) to keep the
initial command structure small. The default status board and GUI come later.

## Verification

Integration tests use real temporary SQLite files and a fixed clock. They verify
persisted update history, ordering by date instead of insertion order, completion
without data loss, and invalid input causing no writes. CLI tests verify parsing,
execution and rejection of malformed dates. A packaged-JAR smoke test exercises
the commands in separate processes.

## Next part

Tasks represent work to finish. Follow-ups represent requests waiting for a
response. Part 4 adds these two independent workflows.
