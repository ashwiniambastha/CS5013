# Student Project Progress Tracker

A local Java application that helps a supervisor track student progress and prepare for review meetings.

| Course | Developer | Stakeholder |
| --- | --- | --- |
| CS5013 — Programming with AI, IIT Madras | Ashwini · MA25M006 | Prof. Neelesh Shankar Upadhye |

[Quick start](#quick-start) · [Try a demo](#try-a-demo) · [Commands](#commands) · [Design guides](#design-guides)

## Why this project?

Student updates, pending work, and deadlines can become scattered across notes, emails, and spreadsheets. This tracker brings those records together and highlights what needs attention before a meeting.

The workflow is:

**Import or enter records → record progress → check the status board → save a meeting summary**

Records stay on the supervisor's laptop in a SQLite database. The application requires no student accounts, hosted server, or external AI service.

## What works today?

| Feature | What it does |
| --- | --- |
| Projects | Add projects, list active projects, and mark them completed |
| Weekly updates | Record dated progress notes and next steps |
| Tasks | Track work, optional deadlines, and completion dates |
| Follow-ups | Track requests awaiting a response, independently of tasks |
| Status board | Show missing updates, overdue work, items due today, and upcoming deadlines |
| Meeting reports | Save a dated text summary of every active project |
| CSV / Excel imports | Preview, validate, and import projects and updates without duplicates |
| Desktop window | Use forms, tables, file pickers, and report previews through Java Swing |
| Backup and restore | Save SQLite snapshots and restore them with a pre-restore recovery copy |
| Windows handover | Package a bundled runtime, desktop shortcut, and optional weekly reporting task |

**Progress:** Parts 1–9 are implemented. The stakeholder's laptop dry-run, preferred settings, and two-week unattended reporting check remain pending.

## Quick start

**For the stakeholder:** use the extracted Windows package and double-click `StudentProjectTracker.exe`. It includes Java. See the [installation and handover guide](docs/part-09-handover.md).

The steps below are for building and running from source.

### 1. Check the requirements

- **JDK 17 or later**
- **Maven** available as `mvn`
- **Git**, if cloning the repository

The first build needs internet access to download dependencies. Normal tracking and reporting run locally afterward.

### 2. Clone and build

```powershell
git clone https://github.com/ashwiniambastha/CS5013.git
cd CS5013
mvn package
```

The build runs the tests and creates an executable JAR with its dependencies included.

### 3. Open the status board

Run these commands from the repository directory in PowerShell:

```powershell
$trackerJar = 'target/student-project-tracker-0.1.0-SNAPSHOT.jar'
java -jar $trackerJar
```

A new database starts with no active projects. To see the available commands:

```powershell
java -jar $trackerJar --help
```

### 4. Open the desktop window

```powershell
java -jar $trackerJar gui
```

Use **Dashboard**, **Projects**, **Follow-ups**, and **Reports** to work through forms and buttons. Start with Projects → Add project, or click Import CSV / Excel to bring in existing updates. See the [desktop walkthrough](docs/part-08-desktop.md).

## Try a demo

Use the included [anonymised sample file](fixtures/updates-template.csv) to try the workflow. This example keeps demo data under `target/demo-data`, separate from the normal database.

```powershell
$trackerJar = 'target/student-project-tracker-0.1.0-SNAPSHOT.jar'
$demoData = 'target/demo-data'

# Preview the file without saving
java "-Dtracker.dataDir=$demoData" -jar $trackerJar import fixtures/updates-template.csv --dry-run

# Import two projects and three weekly updates
java "-Dtracker.dataDir=$demoData" -jar $trackerJar import fixtures/updates-template.csv

# View progress and attention flags
java "-Dtracker.dataDir=$demoData" -jar $trackerJar status

# Save a meeting summary; the command prints its location
java "-Dtracker.dataDir=$demoData" -jar $trackerJar report
```

Importing the same file again skips the unchanged updates. The sample uses October 2026 dates; the status flags depend on the date when you run it. Files under `target` are build/demo files and may be removed by `mvn clean`.

## Commands

Set `$trackerJar` as shown above, then run `java -jar $trackerJar` followed by a command:

| Action | Command and options |
| --- | --- |
| Add a project | `project-add --id P001 --student "Student A" --title "Graph Theory" --start 2026-10-01` |
| List active projects | `project-list` |
| Include completed projects | `project-list --all` |
| Record progress | `update-add --project P001 --date 2026-10-14 --summary "Read first paper" --next "Draft proof"` |
| View update history | `update-list --project P001` |
| Add a task | `task-add --id T001 --project P001 --text "Submit draft" --due 2026-10-20` |
| List open tasks | `task-list --project P001` |
| Complete a task | `task-done --id T001` |
| Add a follow-up | `followup-add --id F001 --project P001 --text "Send revised proof" --requested 2026-10-14 --due 2026-10-18` |
| List pending follow-ups | `followup-list --project P001` |
| Resolve a follow-up | `followup-resolve --id F001` |
| Complete a project | `project-complete --id P001` |
| Check the status board | `status` |
| Open the desktop window | `gui` |
| Back up tracking records | `backup` |
| Restore a saved backup | `restore "backup.db" --confirm` |
| Use a seven-day update threshold | `status --stale-days 7` |
| Save a report to a directory | `report --output "reports"` |
| Preview an import | `import "updates.csv" --dry-run` |
| Import CSV or Excel | `import "updates.csv"` or `import "updates.xlsx"` |

Dates use **`yyyy-MM-dd`**. Use `--all` with task or follow-up lists to include closed history. When working with demo data, include the same `-Dtracker.dataDir` option before `-jar` on every command.

## How the rules work

- **Missing update:** the latest update is more than 14 days old by default. Exactly 14 days is within the threshold. With no updates, the project start date is used.
- **Overdue:** an open item's deadline is before today. A deadline equal to today is shown as **due today**.
- **Next deadline:** the earliest deadline among unfinished tasks, including an overdue one.
- **Completed projects:** hidden from the active board and reports, with their history retained.
- **Reports:** rerunning replaces the same day's file; a new day produces a separate file.
- **Imports:** all rows must pass validation before saving. Identical stored updates are skipped; conflicting IDs are rejected.

The current import format covers **projects and weekly updates**. Tasks and follow-ups are entered separately. Imports do not continuously synchronise a spreadsheet with the database.

## Where is the data saved?

| Item | Default location on Windows |
| --- | --- |
| Database | `%LOCALAPPDATA%\StudentProjectTracker\tracker.db` |
| Reports | `%LOCALAPPDATA%\StudentProjectTracker\reports\` |
| Backups | `%LOCALAPPDATA%\StudentProjectTracker\backups\` |
| Built application | `target/student-project-tracker-0.1.0-SNAPSHOT.jar` |

Application data lives separately from the installed code. Use `-Dtracker.dataDir=<folder>` to choose another data directory, or `report --output <folder>` to choose a report directory. Reports are named `supervision-summary-yyyy-MM-dd.txt`.

## Technology and structure

| Component | Technology |
| --- | --- |
| Language and build | Java 17, Maven |
| Command-line interface | picocli |
| Local storage | SQLite via JDBC |
| File imports | Apache Commons CSV, Apache POI |
| Tests | JUnit 5 |
| Desktop interface | Java Swing |

The application follows **interface → services → repositories → SQLite**. Services hold the rules, repositories handle storage, and renderers format the board and reports.

```text
src/main/java/in/iitm/cs5013/tracker/
├── config/       Data and report locations
├── importer/     CSV and Excel readers
├── model/        Projects, updates, tasks, and follow-ups
├── repository/   Database access and transactions
├── service/      Validation and tracking rules
├── report/       Status board and summary formatting
└── ui/           Desktop tabs, forms, and dialogs

src/test/         Automated tests and expected report fixture
fixtures/         Anonymised sample import
docs/             Design roadmap and implementation walkthroughs
scripts/          Windows packaging, installation, and weekly reporting
```

## Tests

```powershell
mvn test
```

Tests cover persistence, date boundaries, task/follow-up independence, report output, import validation, duplicate handling, and transaction rollback. They use temporary databases and fixed dates rather than real student records.

## Design guides

| Part | Focus | Guide |
| --- | --- | --- |
| 1 | Application foundation | [Design roadmap](docs/design-roadmap.md) |
| 2 | SQLite storage | [How records persist](docs/part-02-storage.md) |
| 3 | Projects and updates | [Record student progress](docs/part-03-projects-updates.md) |
| 4 | Tasks and follow-ups | [Track work and responses](docs/part-04-tasks-followups.md) |
| 5 | Status board | [Calculate attention flags](docs/part-05-status-board.md) |
| 6 | Meeting reports | [Generate dated summaries](docs/part-06-reports.md) |
| 7 | CSV and Excel imports | [Validate and import records](docs/part-07-imports.md) |
| 8 | Desktop interface | [Use the desktop window](docs/part-08-desktop.md) |
| 9 | Packaging and handover | [Install, schedule, back up and restore](docs/part-09-handover.md) |

The stakeholder's file layout, preferred update threshold, and report format still need confirmation through a dry-run. Build the self-contained Windows package with `scripts/build-windows.ps1`. Scheduling is opt-in during handover; no recurring task is activated by the build.
