# Part 9: Packaging, weekly reports and recovery

## What is delivered

A Windows portable application folder and ZIP containing a Java runtime, desktop
launcher, command-line launcher, sample import, documentation, installation script
and weekly report scripts. The professor does not need Maven or a separate Java
installation. This is a portable app package, not an MSI/setup installer.

The package is built for the build machine's Windows architecture. Confirm the
professor's OS and architecture before handover. Other operating systems require
a package built on that OS; this project does not claim cross-platform packaging.

## Build the Windows package (developer)

Requires Windows, Maven, and a JDK containing jpackage and jlink.

```powershell
.\scripts\build-windows.ps1
```

With a Maven executable at another location:

```powershell
.\scripts\build-windows.ps1 -Maven 'C:\tools\apache-maven\bin\mvn.cmd'
```

The script runs the tests, bundles a runtime, builds an app-image and prints its
folder and ZIP paths under target. Each build gets its own staging folder; old
packages are not deleted. -SkipBuild is for packaging an already verified JAR.
The runtime is from the build JDK (currently JDK 25); source compatibility remains
Java 17. The ZIP is a generated artifact, not committed to GitHub.

## Install and open (stakeholder)

1. Extract the entire ZIP into a folder. Do not move only the .exe.
2. Double-click StudentProjectTracker.exe to open the desktop application.
3. To install a permanent copy and desktop shortcut, open PowerShell in the
   extracted StudentProjectTracker folder and run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-windows.ps1
```

The installation is per-user under
%LOCALAPPDATA%\Programs\StudentProjectTracker\0.1.0. No administrator installation
is needed. The script will not overwrite an existing version directory.
Run it as the professor's own Windows user so the shortcut and data belong to him.
The scripts are unsigned; university device policy may restrict their execution.

## Data locations

| Contents | Location under %LOCALAPPDATA%\StudentProjectTracker |
| --- | --- |
| Tracking database | tracker.db |
| Dated summaries | reports\ |
| Database backups and pre-restore recovery copies | backups\ |
| Weekly job log | logs\weekly-report.log |

Application installation and user records are separate. Replacing the application
does not replace the database. Keep demo data separate using -Dtracker.dataDir.

## Backup and restore

Desktop: click Backup to save a uniquely named snapshot. Copy important backups
to a separate drive. Click Restore, choose a tracker .db backup and confirm to
replace current records. Close other tracker windows before restoring.

From the installed folder:

```powershell
.\TrackerCLI.cmd backup
.\TrackerCLI.cmd backup --output 'D:\TrackerBackups'
.\TrackerCLI.cmd restore 'D:\TrackerBackups\tracker-backup-example.db' --confirm
```

Restore validates SQLite integrity, schema version, required columns and foreign
keys before applying changes. It saves the previous records as a recovery backup
first. The result prints that recovery path; it can itself be restored.
The backup/restore API operates through SQLite, including committed WAL data.
Backups contain tracking records and import history, not exported report files,
application code or scheduled-task settings. Restore leaves existing reports in
place and clears the desktop report preview. Backups are local, unencrypted files;
no automatic deletion/retention policy is applied.

## Weekly reporting

From the installed folder, inspect the proposed task first:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\schedule-weekly-report.ps1 -Preview
```

Activate it explicitly under the professor's account:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\schedule-weekly-report.ps1 -Day Monday -Time '09:00'
```

The time is the laptop's local time. The task uses the installed runtime and
absolute script paths; no Java on PATH is needed. It runs hidden, first backs up
records, then generates a dated summary and appends a log. Failures return a
nonzero exit code. The task retries three times at five-minute intervals and
prevents overlapping task runs. No passwords are stored.

This per-user task runs while that user is logged in. StartWhenAvailable permits
catch-up when eligible after a missed run; it does not guarantee reporting while
the laptop is off or the user is logged out. It is allowed to run on battery.
The time limit is ten minutes. Installation alone does not activate a schedule.

Manual check without waiting a week:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-weekly-report.ps1
Get-ScheduledTaskInfo -TaskName StudentProjectTracker-WeeklyReport
```

The manual command does not require a registered task. To remove the task:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\schedule-weekly-report.ps1 -Remove
```

The script refuses to overwrite an existing task. Remove it explicitly before
changing the schedule or the installed application's path, then register again.

## Updating the application

Back up first and close the tracker. Build/install a new version into a new version
folder, update the desktop shortcut and recreate the scheduled task from that
installation. Data stays in the same application-data folder. This version uses
schema 1; a database with a newer schema is rejected. Future schema changes must
ship an explicit migration. Old installation folders may be kept for rollback.

## Verification and acceptance still needed

Automated tests cover backup round-trips, recovery backups, corrupt/newer-schema
rejection, WAL snapshots, CLI confirmation, and desktop restore cancellation.
Packaging checks exercise the bundled runtime, isolated installation, weekly
runner, task XML preview and data persistence across application copies.
The development dry-run uses anonymised data and does not activate a real schedule.

Before final acceptance, confirm the stakeholder's OS/file layout/report format,
run the complete workflow on his laptop, and observe two consecutive unattended
weekly jobs. The promised alternative-tool benchmark and stakeholder review
remain separate proposal commitments; building this package does not establish
that those acceptance steps have happened.
