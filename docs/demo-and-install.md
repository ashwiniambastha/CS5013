# Demo and installation: final rehearsal

## The package to use

Use the Windows 64-bit version 1.0.0 ZIP printed by scripts/build-windows.ps1.
Extract the entire ZIP first. It contains two application launchers and Install.cmd.
Read START-HERE.txt before the rehearsal. A Windows package cannot be installed
on macOS or Linux; confirm the professor's device is Windows before copying it.

## Rehearse with sample data

Double-click TrackerDemo.exe. Its window is labelled DEMO. The working database
remains separate, under the normal data directory's demo/yyyy-MM-dd subdirectory.
Every new day gets a fresh fixture; same-day edits are preserved. Demo data never
gets imported automatically into the working database.

The fixture covers twelve weeks and the proposal's important cases:

| Project | What to show |
| --- | --- |
| D001, Student A | Twelve updates; current progress, future deadline, finished task |
| D002, Student B | Four weeks without updates, overdue task and overdue follow-up |
| D003, Student C | Exactly 14 days since update; two tasks due today; late resolved request |
| D004, Student D | No updates since project start |
| D005, Student E | Completed; available in history but absent from the active board |

Five-minute sequence:

1. Explain the dashboard's missing-update and deadline flags.
2. Select D002, save a weekly update and show that its missing-update flag clears.
3. Mark DT2 completed, resolve DF1, and refresh the dashboard.
4. Generate a report, showing the preview and actual file location.
5. Save a backup; change a record, then restore to show recovery.
6. Show Projects > Add project and the import preview/cancel flow.

No account, server, internet connection or external AI service is used at runtime.
Some Windows device policies may restrict unsigned executables or scripts.

## Install on the stakeholder's device

Under his own Windows account, extract the ZIP and double-click Install.cmd.
It installs in his per-user Programs folder and creates normal/demo shortcuts.
The normal StudentProjectTracker.exe opens his working database, initially empty.
Use his anonymised file first to confirm the template and report format.
His existing database stays separate from versioned application folders.

Scheduling is optional: follow part-09-handover.md only after he chooses a
reporting day and time. It runs under his logged-in Windows account and logs
backup/report results. No live task is created by packaging or demo mode.

## Build and verify from source

```powershell
# Uses existing Maven, or downloads Maven with SHA-512 verification
.\scripts\build.ps1

# Tests already ran; now package the verified JAR
.\scripts\build-windows.ps1 -SkipBuild

# Pass the App folder path printed by packaging
.\verification\windows-package-smoke.ps1 -PackageDirectory 'C:\path\StudentProjectTracker'
```

The packaging command also writes a SHA-256 checksum next to the ZIP.
Native file-picker interaction, desktop-shortcut creation on his actual account,
and two real unattended weekly runs remain handover checks. Automated tests and
demo runs are engineering evidence, not stakeholder acceptance.
