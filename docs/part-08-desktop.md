# Part 8: The desktop interface

Swing gives the professor forms, buttons and tables that call the existing
services. There is no second database or separate set of desktop deadline rules.

## Launch

From the repository directory in PowerShell, after `mvn package`:

```powershell
$trackerJar = 'target/student-project-tracker-0.1.0-SNAPSHOT.jar'
java -jar $trackerJar gui
```

For an isolated demo:

```powershell
java -Dtracker.dataDir=target/desktop-demo -jar $trackerJar gui
```

The gui command opens the window. Running without a command still prints the
CLI status board. A bundled runtime and installer come in Part 9.

## Four tabs

| Tab | What to do |
| --- | --- |
| Dashboard | Review active projects, missing updates, deadlines and pending requests |
| Projects | Add projects, record updates, add/complete tasks, and complete a project |
| Follow-ups | Add requests and mark selected requests resolved |
| Reports | Choose a directory, generate a summary and read it in the window |

The selector at the top controls the project-specific history and forms.
Selecting a project table row changes this selector too. The dashboard always
shows all active projects. Completed projects remain selectable for history;
services reject new updates, tasks or requests for them.

## Try the workflow

1. Open Projects, then Add project. Enter the student, title and start date.
   Leaving the optional ID blank generates one.
2. Select that project above. Open Weekly updates, enter a date, summary and next
   steps, then save.
3. Open Tasks and add work with an optional deadline. Select a row and mark it
   done when finished.
4. Open Follow-ups to record a request. Select its row and resolve it when answered.
5. Click Refresh and review Dashboard.
6. Open Reports and generate a report. The message area shows its saved path,
   and the report appears in the preview.

Dates use yyyy-MM-dd. Blank completion/resolution cells mean an item is open.
History tables include closed items; the dashboard and reports show open work.
Completing a project asks for confirmation and retains history.

## Import

Click Import CSV / Excel and choose a file using the Part 7 template. The app
validates it in the background and shows counts before saving. Confirm to commit,
or cancel to leave records unchanged. Saving validates the file again.

The scrollable message area displays row errors and form validation failures.
Invalid forms retain their values for correction. Successful forms reset.
The status threshold starts at 14 days: change it and click Refresh. Reports use
that selected threshold too; the preference is not persisted between launches.

## How the code works

- TrackerApplication exposes gui and keeps the process alive while the window is open.
- TrackerDesktop opens/sizes the frame and defers closing during an active action.
- TrackerPanel builds tabs/forms and coordinates background jobs.
- DesktopDialogs handles file selection and confirmations.
- Existing services handle rules, database access, imports and reports.

Swing's event dispatch thread handles clicks and drawing. SwingWorker performs
database/file work in the background and returns results to the event thread.
Inputs/actions are disabled during a job to prevent overlapping saves. A
successful change refreshes the data; failures show a readable message.

## Verification and remaining work

Desktop integration tests drive form actions on the event dispatch thread with
a temporary SQLite database and fixed clock. They cover projects, updates,
independent tasks/follow-ups, visible errors, reports, cancelled/confirmed imports,
unchanged reimports, and completed-project history. Panels are rendered as PNGs
under target/ui-preview for visual inspection.

Tests supply dialog responses. Native file-picker behaviour and the professor's
actual laptop still need a stakeholder dry-run. Packaging, scheduled reporting,
and backup/restore belong to Part 9.
