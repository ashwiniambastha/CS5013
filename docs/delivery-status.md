# Delivery status

The implementation is prepared for local demonstration and Windows installation.
No stakeholder feedback has been supplied as of 2 November 2026.

## Implemented

- Local Java/Swing application and command-line interface.
- Persistent projects, weekly updates, tasks, deadlines and follow-ups.
- Missing-update detection, due-today/overdue classification and active board.
- Deterministic dated meeting reports.
- CSV/XLSX whole-file validation, transactional import and unchanged-row skipping.
- Native SQLite backups, validated restore and pre-restore recovery copies.
- Bundled-runtime Windows package, one-click install and optional weekly task.
- Separate, repeatable twelve-week demo and step-by-step rehearsal guide.

## Workflow assumptions to confirm at handover

The SQLite database is currently the working record. Spreadsheet imports add new
updates and reject conflicting existing IDs; they do not continuously synchronise
an authoritative spreadsheet. This differs from the proposal's strict
spreadsheet-source-of-truth criterion and requires explicit stakeholder agreement
or further adaptation after the real file layout is provided.

The initial staleness threshold is 14 days and can be changed for the current
session/command. The professor must review the report format and threshold.
The package targets Windows 64-bit; other devices require a matching build.

## Acceptance evidence still external

- Stakeholder's actual/anonymised file imports without manual correction.
- Professor agrees that attention flags and summary match his workflow.
- Installation and complete workflow on his laptop.
- Two consecutive unattended weekly runs on that laptop.
- The proposal's comparison against five alternative tools using the same fixture.

No meetings, approvals, alternative-tool benchmark results or two-week scheduled
runs have been fabricated. These items do not prevent a local demo; they prevent
claiming stakeholder acceptance or full proposal compliance today.
