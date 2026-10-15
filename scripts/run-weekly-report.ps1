param([string]$DataDirectory = (Join-Path $env:LOCALAPPDATA 'StudentProjectTracker'))
$ErrorActionPreference = 'Stop'
$packageDirectory = Split-Path $PSScriptRoot -Parent
$javaPath = Join-Path $packageDirectory 'runtime/bin/java.exe'
$jarPath = Join-Path $packageDirectory 'app/tracker.jar'
$logDirectory = Join-Path $DataDirectory 'logs'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$logFile = Join-Path $logDirectory 'weekly-report.log'
try {
    Add-Content -LiteralPath $logFile -Value "$(Get-Date -Format o) Starting weekly backup and report."
    & $javaPath --enable-native-access=ALL-UNNAMED "-Dtracker.dataDir=$DataDirectory" -jar $jarPath backup 2>&1 | Out-File -LiteralPath $logFile -Append -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw 'Weekly backup failed.' }
    & $javaPath --enable-native-access=ALL-UNNAMED "-Dtracker.dataDir=$DataDirectory" -jar $jarPath report 2>&1 | Out-File -LiteralPath $logFile -Append -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw 'Weekly report failed.' }
    Add-Content -LiteralPath $logFile -Value "$(Get-Date -Format o) Completed."
}
catch {
    Add-Content -LiteralPath $logFile -Value "$(Get-Date -Format o) FAILED: $_"
    throw
}
