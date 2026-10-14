# Part 5: The status board

## From stored facts to useful decisions

Earlier parts saved dates and notes. This part reads those facts and compares them
with today's date. It never saves a stale/overdue flag to the database: those flags
change as time passes, so every board display recalculates them.

Example on 14 October 2026:

| Fact | Result |
| --- | --- |
| Latest update on 30 September (14 days ago) | Within the 14-day threshold |
| Latest update on 29 September (15 days ago) | Missing update |
| Open task due 13 October | Overdue |
| Open task due 14 October | Due today |
| Open task due 20 October | Upcoming |
| Request with no deadline and no response | Pending, no deadline |

## Rules

- Only active projects appear.
- The newest update date controls freshness, regardless of insertion order.
- With no updates, count days from the project start date.
- Missing update means age is strictly greater than the threshold, initially 14 days.
- The threshold is configurable per status invocation; it must be positive.
- Only incomplete tasks and unresolved follow-ups are included.
- A due date before today is overdue; equal to today is due today.
- Undated work is shown explicitly rather than labelled overdue.
- The next deadline is the earliest dated open task, including an overdue one.
  Follow-up due dates are displayed separately.
- Every pending follow-up is listed, even if it is not overdue.

The 14-day threshold is an initial assumption to confirm with the professor.
It is not a claim that two weeks is his preferred reporting interval.

## Code flow

Default launch or status command -> StatusService -> ProjectService/WorkService
-> repositories -> SQLite -> StatusBoard snapshot -> StatusBoardRenderer.

StatusService captures today's date once using an injected Clock and calculates
the classifications. StatusBoard contains the structured result. The renderer
only prints it, so the desktop interface and meeting report can reuse the same
rules later. There is no database schema change in this part.

Clock lets tests fix today to a known date. Normal use follows the laptop's local
date. The board is read-only; repeated runs do not insert or close anything.
For this single-user first version, reads use separate connections rather than
one atomic database snapshot; simultaneous changes may appear on the next refresh.

## Run it

After `mvn package`, from the repository folder in PowerShell:

```powershell
$trackerJar = 'target/student-project-tracker-0.1.0-SNAPSHOT.jar'
java -jar $trackerJar
java -jar $trackerJar status
java -jar $trackerJar status --stale-days 7
```

The first two commands show the same board using 14 days. Running --help still
shows command help. To view earlier demo data rather than the normal database:

```powershell
java -Dtracker.dataDir=target/part4-demo -jar $trackerJar status
```

## Verification

Tests cover exactly 14 versus 15 days, no-update projects, custom thresholds,
latest update selection, completed projects, overdue/today/future/undated items,
same-day deadlines, closed-item exclusion, no next deadline, and repeated reads.
CLI tests verify default launch, explicit status, configurable output, and rejection
of a zero threshold. Dates in these tests are fixed independently of the laptop.

## Next

Part 6 generates dated meeting summaries as files using these same status rules.
The current board prints to the terminal; exporting a report is not implemented yet.
