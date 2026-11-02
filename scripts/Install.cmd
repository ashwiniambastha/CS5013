@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-windows.ps1"
if errorlevel 1 (
  echo Installation did not finish. See the message above and START-HERE.txt.
) else (
  echo Installation complete. Use the desktop shortcuts to open the tracker or demo.
)
pause
