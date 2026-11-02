# Faculty tools: no commands required

Version 1.1 adds a Faculty tools tab to the desktop window.

## Edit and delete

Select a project at the top, then choose Faculty tools > Edit / delete. Select
Project, Update, Task or Follow-up and a record ID. Edit its fields, including
dates, descriptions and project status. IDs stay fixed. Use ACTIVE to reopen a
completed project. Blank completion/resolution dates reopen tasks/requests.
Invalid date relationships are rejected together without saving partial edits.

Delete asks for confirmation. Deleting a project also deletes its updates, tasks
and follow-ups in one transaction. A SQLite recovery backup is created before
deletion. Restore that file through the normal Restore button to undo the change.

## Export and email drafts

Export / reminders saves either an Excel workbook with five sheets, or a directory
with five CSV files: projects, updates, tasks, follow-ups and import history.
Export is archival, not the weekly-update import template. Use a database backup
for a full restore. CSV values that could execute spreadsheet formulas are
prefixed with an apostrophe; Excel cells are written as text.

Save the student's email for the selected project, then choose Open reminder
draft. The default email application opens a draft addressed to that student.
The professor reviews and sends it. Nothing is emailed automatically. A configured
default email handler is required; otherwise the app reports the failure.

## Preferences and weekly reporting

Change the update threshold above, then use Settings / online updates > Save
preferences. It applies on later launches and to scheduled report generation.
Preferences belong to the current data directory, so demo settings stay separate.

Choose a weekday and HH:mm time, then Enable / update weekly reports. Confirm the
Windows task change. Disable weekly reports removes it. Use the installed Windows
package; source-only mode has no bundled reporting runner. Demo mode cannot create
a live schedule. The job runs under the current logged-in Windows user; the laptop
must be available. Confirm the Windows task on the faculty device during handover.

## Hosted student submissions

The default hosted server is shown in Settings / online updates. Add at least one
active project, save preferences, then Create / refresh online classroom.
The server registers project IDs, not the local roster's student names/titles.
Share the displayed student link and each student's project ID. Students need no
account and cannot browse other responses. Sharing the link allows submissions;
it does not authenticate student identity, so review is important.

The desktop generates a random classroom access key and protects it using Windows
DPAPI under the current user's account. It is not included in the student link.
No fixed administrator email or email password is required. Email drafts use the
professor's default mail account. Keep preferences.properties with the application
data: database-only backups do not include the encrypted classroom connection.
Connections are per Windows user/device; moving to another device requires a new
classroom connection. Do not delete preferences while responses are pending.

After students submit, choose Review / import student responses. The app downloads
them and shows a selectable table with full notes. Choose Import selected or
Discard selected; discards require another confirmation and keep a local CSV copy.
Select valid rows separately from unknown/completed projects or invalid dates.
Imports match IDs to local projects and validate the selected rows transactionally.
Only then are updates imported into SQLite. Imported online copies
are removed after successful import; cancelled responses remain online. If cleanup
fails, retry later: identical IDs are skipped locally before cleanup is retried.
Downloaded CSVs remain in the local inbox folder. Completed/unknown projects are
rejected rather than silently reassigned. Refresh the online classroom after
adding, deleting or completing local projects to update allowed IDs.

Online submissions require internet and store progress notes on the hosted server
until imported. The local tracker, drafts, exports and reports continue to work
offline. Native email handlers, Windows task registration on the professor's
account, and his file/privacy requirements still need a handover dry-run.
