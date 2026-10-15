param(
    [ValidateSet('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday')]
    [string]$Day = 'Monday',
    [ValidatePattern('^([01]\d|2[0-3]):[0-5]\d$')][string]$Time = '09:00',
    [string]$TaskName = 'StudentProjectTracker-WeeklyReport',
    [switch]$Preview,
    [switch]$Remove
)
$ErrorActionPreference = 'Stop'
if ($Remove) {
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
    Write-Output "Removed task: $TaskName"
    return
}
$packageDirectory = Split-Path $PSScriptRoot -Parent
$runner = Join-Path $PSScriptRoot 'run-weekly-report.ps1'
if (-not (Test-Path -LiteralPath (Join-Path $packageDirectory 'runtime/bin/java.exe'))) { throw 'Schedule from the installed Windows package.' }
$powershellPath = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
$action = New-ScheduledTaskAction -Execute $powershellPath -Argument "-NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$runner`"" -WorkingDirectory $packageDirectory
$trigger = New-ScheduledTaskTrigger -Weekly -WeeksInterval 1 -DaysOfWeek $Day -At ([datetime]::Today.Add([timespan]::Parse($Time)))
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Minutes 10) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 5)
$principal = New-ScheduledTaskPrincipal -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive -RunLevel Limited
$task = New-ScheduledTask -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Description 'Save a local tracker backup and meeting summary each week.'
if ($Preview) { $task | Export-ScheduledTask; return }
if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) { throw "Task already exists: $TaskName. Remove it explicitly before changing its schedule or installation path." }
Register-ScheduledTask -TaskName $TaskName -InputObject $task | Out-Null
Write-Output "Scheduled $TaskName every $Day at $Time (this laptop's local time, while this user is logged in)."
