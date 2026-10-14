# Design and implementation roadmap

Build one understandable part at a time. Each part has an explanation, a working
example, appropriate verification, and a Git commit pushed to GitHub. Git commits
are checkpoints; they do not mean the whole application is finished.

## Part 1 — Application foundation

Purpose: establish where code lives and how the program starts.
Deliverable: Maven project and TrackerApplication.main.
Verification: compile for Java 17 and launch the program.

## Part 2 — Local storage

Purpose: keep records after the application closes.
Deliverable: SQLite connection, schema, repository interfaces and implementations.
Tables: projects, updates, tasks, followups, import_runs.
Use foreign keys and transactions. Store dates as ISO dates. Use stable IDs rather
than student names. Keep the database in the user's application-data directory,
separate from installation files. Verify persistence by closing and reopening it.

## Part 3 — Projects and weekly updates

Purpose: represent who is supervised and what they have reported.
Deliverable: add/list/complete projects; record and retrieve dated updates.
Fields: project ID, student name, title, start date and active/completed status;
update ID, project ID, date, summary and next steps.
Verify a project with multiple updates and retrieval of its latest update.

## Part 4 — Tasks and follow-ups

Purpose: track work and requests separately.
Tasks have descriptions, optional due dates and completion dates. Follow-ups have
request dates, optional due dates and resolution dates. Completing one must not
change another. Verify these state transitions and invalid project references.

## Part 5 — Status rules

Purpose: explain what needs attention using deterministic rules.
An unfinished item is overdue when its due date is before today. Due today is
separate. An active project is stale when its latest update is older than a
configurable threshold (initially 14 days). With no updates, use its start date.
Exactly 14 days is not stale. Completed projects are excluded by default.
Next deadline is the earliest due date of an unfinished task. Inject Clock and
verify date boundaries, same-day deadlines and completed-project exclusion.

## Part 6 — Reports

Purpose: produce a meeting-ready summary from the stored records.
Include each active project, latest update, next steps, open tasks, next deadline,
pending follow-ups and attention flags. Use stable ordering and UTF-8 text.
Write a dated report; rerunning on the same day replaces that day's file and
does not create database entries. Verify against a reviewed expected report.

## Part 7 — CSV, then Excel imports

Purpose: bring existing records into the tracker safely.
Publish a template with project_id, student_name, project_title, started_on,
update_id, update_date, summary and next_steps. Each row is one update.
Validate the entire file before a transaction writes anything; report row numbers.
Existing identical IDs are skipped; conflicting content is rejected initially.
Verify invalid-row rollback and repeat-import idempotence. Excel reuses the same
validation and persistence logic after CSV works.

Working assumption: SQLite is authoritative after import, with CSV export for
portability. The proposal's spreadsheet-source-of-truth requirement must be
confirmed with the stakeholder before recurring synchronisation is designed.

## Part 8 — Desktop interface

Purpose: let the professor use the app without terminal commands.
Swing screens: Dashboard, Projects, Follow-ups and Reports. Provide forms for
updates and tasks, import preview/errors, completion actions and report generation.
UI calls services; it contains no SQL or date-rule logic. Keep slow imports off
the Swing event thread. Verify the stakeholder's complete workflow through the UI.

## Part 9 — Packaging, scheduling and handover

Purpose: make installation and weekly use dependable.
Confirm laptop OS. Use jpackage with a bundled runtime and desktop shortcut.
Provide a headless report command for the OS scheduler, plus backup/restore.
Test a packaged installation, upgrade preserving data, restore, and scheduled
reports. Complete a stakeholder dry-run and document remaining limitations.

## Architecture

CLI / Swing -> services -> repositories -> SQLite.
Import parsers -> shared validation -> import service -> repositories.
Report service -> status rules + stored records -> text renderer.

Keep services independent of Swing and picocli so both interfaces use the same
behaviour. Add dependencies when the corresponding part needs them.

## Proposal commitments

Before finalising the custom build, record the promised comparison with existing
tools using the same anonymised workflow. Confirm import layout, staleness
threshold, report format, OS and spreadsheet ownership with the professor.
Use anonymised data in GitHub. The proposal's milestones are plans, not evidence
that those features already exist.
