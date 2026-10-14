# Part 7: CSV and Excel imports

## What to understand first

An import converts rows in a file into the same project/update records that the
manual commands create. Each row represents one weekly update. Project details
repeat when one project has multiple updates. Stable IDs determine identity,
not student names. Two students can share a name if their project IDs differ.

## Template

See fixtures/updates-template.csv for an anonymised working example.
Required columns (order may vary; additional named columns are ignored):

| Column | Meaning |
| --- | --- |
| project_id | Stable project ID, e.g. P001 |
| student_name | Student display name |
| project_title | Project title |
| started_on | Project start date |
| update_id | Stable, globally unique update ID, e.g. U001 |
| update_date | Date of the progress update |
| summary | Progress note |
| next_steps | Next work; value may be blank |

All eight column headings must exist. Other required values cannot be blank.
Text dates must use yyyy-MM-dd; ambiguous slash-separated dates are rejected.
Dates must be valid; updates must be between project start and today.

For Excel, put the same headers in row 1 of the first sheet. Genuine Excel date
cells are converted to ISO dates independently of the displayed date format.
Use literal values in the required columns: formula/error cells are rejected.
Only .xlsx is supported, not legacy .xls or macro-enabled files. Blank rows are
skipped. CSV is UTF-8, supports a BOM, quoted commas and quoted multiline fields.
CSV error row numbers refer to records, so a multiline field is one record.

## Code flow

UpdateFileReader (CSV or Excel) -> shared ImportService validation
-> SqliteImportRepository transaction -> SQLite.

Parsing discovers headers, row numbers and values. Validation constructs the
same Project and WeeklyUpdate models used by the manual workflow. The repository
checks IDs against existing records on the same connection used for the writes.
Only after validation succeeds are projects, updates and import history saved.
If a database write fails, all writes in that transaction roll back.

## Duplicate and conflict rules

- Multiple rows can use the same project ID if its details agree.
- Repeated update IDs within one file are errors, even if the rows are identical.
- A stored identical update is skipped. Existing matching projects are reused.
- Different content under an existing ID is a conflict; nothing is overwritten.
- A completed project can be reimported with identical updates; it stays completed.
- New updates cannot be imported into a completed project.
- A completely unchanged reimport adds neither records nor another import-run row.
- --dry-run validates existing IDs too, and saves nothing.

This is initial import plus explicit new updates, not continuous spreadsheet
synchronisation. SQLite remains the working record after import. The professor's
spreadsheet-source-of-truth preference still needs agreement; deleting a row from
a file does not delete it from the database. Tasks/follow-ups are not imported in
this version. Export and column mapping are not implemented yet.

## Try it

After `mvn package`, from the repository folder in PowerShell:

```powershell
$trackerJar = 'target/student-project-tracker-0.1.0-SNAPSHOT.jar'
$demoData = 'target/part7-demo'
java "-Dtracker.dataDir=$demoData" -jar $trackerJar import fixtures/updates-template.csv --dry-run
java "-Dtracker.dataDir=$demoData" -jar $trackerJar import fixtures/updates-template.csv
java "-Dtracker.dataDir=$demoData" -jar $trackerJar import fixtures/updates-template.csv
java "-Dtracker.dataDir=$demoData" -jar $trackerJar status
java "-Dtracker.dataDir=$demoData" -jar $trackerJar report
```

The first command previews two projects and three updates without saving.
The second saves them. The third skips three unchanged updates. For Excel,
replace the file argument with the path to an .xlsx using the same template.
The included example dates are historical as of 14 October 2026.

## Verification

Tests cover CSV quoting/BOM/blank rows, Excel date cells, formula rejection,
shared validation across formats, multiple row errors, malformed and missing
columns, duplicate/conflicting IDs, completed projects, dry runs, reimports and
import-history counts. A forced database failure verifies full rollback after
a project insert. CLI tests exercise preview and repeat import.

These parsers read the whole input into memory, appropriate for a small student
roster. The stakeholder must supply or approve the actual anonymised file layout
before the proposal's real-file acceptance criterion is satisfied.

## Next

Part 8 adds the Swing desktop interface over the same services, including
project/update forms, task/follow-up actions, import previews and reports.
