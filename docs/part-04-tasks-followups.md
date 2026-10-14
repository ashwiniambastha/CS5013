# Part 4: Tasks and follow-ups

## Two different questions

A task asks: what work needs to be finished?
Example T001: submit the literature-review draft.

A follow-up asks: what response are we waiting for?
Example F001: send the revised proof requested at the last meeting.

Both belong to a project, but they have independent states. Completing T001 does
not resolve F001. A progress update records a note; it also does not silently
complete work or resolve a request.

## Java representation

Task: id, projectId, description, optional dueDate, optional completedOn.
Followup: id, projectId, description, requestedOn, optional dueDate, optional resolvedOn.

An empty completion/resolution date means the item is open. Closing the item sets
that date to today, using the service's Clock. A repeated command leaves the
original date intact. The first interface supports closing today; entering a
historical completion date is not implemented yet.

## Flow through the application

CLI command -> WorkService -> WorkRepository -> SQLite.

The model carries fields. WorkService checks the project and dates.
WorkRepository defines storage operations. SqliteWorkRepository implements
prepared SQL queries against the tables created in Part 2. No schema migration
is needed for this part.

## Rules

- New items require an existing active project and nonblank ID/description.
- IDs are unique within their respective table.
- Task deadlines cannot predate the project start date.
- Follow-up request dates must be between project start and today.
- Follow-up deadlines cannot predate the request date.
- A deadline can be in the past: existing overdue work must be recordable.
- Listing shows open items by default; --all retains closed history.
- Lists order by deadline, then ID; items with no deadline appear last.
- Existing items can still be closed after their project is completed.
- Completion does not automatically close all unfinished work.

## Try it

Build with `mvn package`. From the repository directory in PowerShell:

```powershell
$trackerJar = 'target/student-project-tracker-0.1.0-SNAPSHOT.jar'
$demoData = 'target/part4-demo'
java "-Dtracker.dataDir=$demoData" -jar $trackerJar project-add --id P001 --student "Student A" --title "Graph Theory" --start 2026-10-01
java "-Dtracker.dataDir=$demoData" -jar $trackerJar task-add --id T001 --project P001 --text "Submit literature review" --due 2026-10-20
java "-Dtracker.dataDir=$demoData" -jar $trackerJar followup-add --id F001 --project P001 --text "Send revised proof" --requested 2026-10-10 --due 2026-10-18
java "-Dtracker.dataDir=$demoData" -jar $trackerJar task-list --project P001
java "-Dtracker.dataDir=$demoData" -jar $trackerJar followup-list --project P001
java "-Dtracker.dataDir=$demoData" -jar $trackerJar task-done --id T001
java "-Dtracker.dataDir=$demoData" -jar $trackerJar followup-list --project P001
java "-Dtracker.dataDir=$demoData" -jar $trackerJar followup-resolve --id F001
java "-Dtracker.dataDir=$demoData" -jar $trackerJar task-list --project P001 --all
java "-Dtracker.dataDir=$demoData" -jar $trackerJar followup-list --project P001 --all
```

Run the project-add step only once per demo directory. Omit --due for an undated
item. Use ISO dates (yyyy-MM-dd). The above sample request date is historical.

## Verification

Tests use temporary SQLite databases and fixed clocks. They verify independent
task/follow-up states, persistence after reopening, repeated closure preserving
dates, deadline ordering, invalid requests, unknown IDs, duplicate task IDs and
completed-project restrictions. Command tests cover creation and closure.

## Next

Part 5 turns stored facts into a status board: overdue tasks, unanswered follow-ups,
missing weekly updates, and the next deadline. This part stores and lists work;
it does not yet classify deadlines as overdue or due today.
