# Part 6: Dated meeting summaries

## What changed

The status board is useful while the app is open. A report is a saved snapshot
the professor can read during a meeting or keep for later reference.
Every active project includes its latest update, next steps, open tasks,
earliest open-task deadline, pending follow-ups and attention flags.
Completed projects and closed work are excluded by the same rules as the board.

## Code flow

report command -> ReportService -> StatusService -> StatusBoard
-> TextSummaryRenderer -> UTF-8 text file.

StatusService owns all date classifications. The renderer assembles readable
text from those classifications. ReportService handles directories and writing.
There is no new database table or new database write for report generation.

## Run

After `mvn package`, from the repository directory in PowerShell:

```powershell
$trackerJar = 'target/student-project-tracker-0.1.0-SNAPSHOT.jar'
java -jar $trackerJar report
java -jar $trackerJar report --output "target/meeting reports"
java -jar $trackerJar report --output "target/meeting reports" --stale-days 7
```

With no output option, reports go in a reports directory beside tracker.db,
under the user's application-data folder. `--output` selects a directory, not
a filename. Relative paths resolve against the current working directory.
For earlier demo data:

```powershell
java -Dtracker.dataDir=target/part4-demo -jar $trackerJar report
```

The saved file is named supervision-summary-yyyy-MM-dd.txt using the laptop's
current local date. The command prints the absolute path after success.
Use an absolute --output directory when configuring the scheduler later.

## Repeat runs and failure handling

The same day's report is replaced, including when the threshold changes. The
following day gets a different file. Repeating the command creates no database
records and does not complete any work. With no active projects it still saves
a report explicitly stating that there are none.

The service writes a temporary UTF-8 file in the destination directory, then
replaces the dated report with an atomic move when the filesystem supports it.
If atomic moves are unsupported it falls back to a regular replacement. Temporary
files are cleaned up; write failures propagate to the CLI as a nonzero exit code.
The normal scheduler will be configured in Part 9, not in this part.

## Text format

Ordering follows the status board: projects by ID, work by due date then ID.
Report text uses LF line endings for consistent comparison across machines.
Whitespace in user notes is collapsed to spaces in the summary; stored notes
retain their original content. This keeps one item from breaking the layout.
No AI service is used. The text is assembled from the professor's own records.

## Verification

An anonymised fixture compares the full generated text with an expected report
in src/test/resources/reports/expected-summary.txt. Additional checks verify
same-day replacement with updated content, Unicode, completed-item exclusion,
no duplicate records, separate next-day files, empty projects, invalid thresholds,
output errors, and invocation through the CLI.

The expected format is a development baseline; the professor still needs to
review it before it meets the proposal's stakeholder acceptance criterion.

## Next

Part 7 brings existing records in through CSV, then Excel, with whole-file
validation and duplicate-safe transactional writes.
