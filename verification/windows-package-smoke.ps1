param([Parameter(Mandatory = $true)][string]$PackageDirectory)
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$package = [IO.Path]::GetFullPath($PackageDirectory)
$smokeRoot = Join-Path $repositoryRoot ('target/handover-smoke-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $smokeRoot -Force | Out-Null
$dataDirectory = Join-Path $smokeRoot 'data'
if (-not (Test-Path -LiteralPath (Join-Path $package 'StudentProjectTracker.exe'))) { throw 'Packaged launcher is missing.' }
if ((Get-Content -LiteralPath (Join-Path $package 'app/StudentProjectTracker.cfg') -Raw) -notmatch 'arguments=gui') { throw 'Desktop launch arguments are missing.' }
if (-not (Test-Path -LiteralPath (Join-Path $package 'TrackerDemo.exe'))) { throw 'Demo launcher is missing.' }
if ((Get-Content -LiteralPath (Join-Path $package 'app/TrackerDemo.cfg') -Raw) -notmatch 'arguments=demo') { throw 'Demo launch arguments are missing.' }
if (-not (Test-Path -LiteralPath (Join-Path $package 'Install.cmd'))) { throw 'One-click installation entry point is missing.' }
function Invoke-PackagedTracker {
    param([string]$ApplicationDirectory, [string[]]$CommandArgs)
    $javaPath = Join-Path $ApplicationDirectory 'runtime/bin/java.exe'
    $jarPath = Join-Path $ApplicationDirectory 'app/tracker.jar'
    $result = & $javaPath --enable-native-access=ALL-UNNAMED "-Dtracker.dataDir=$dataDirectory" -jar $jarPath @CommandArgs
    if ($LASTEXITCODE -ne 0) { throw 'Packaged command failed.' }
    return $result
}
$firstRoot = Join-Path $smokeRoot 'installed'
& (Join-Path $package 'scripts/install-windows.ps1') -InstallRoot $firstRoot -NoShortcut
$version = (Get-Content -LiteralPath (Join-Path $package 'VERSION.txt') -Raw).Trim()
$first = Join-Path $firstRoot $version
$demoBoard = Invoke-PackagedTracker -ApplicationDirectory $first -CommandArgs @('demo','--no-gui')
if (($demoBoard -join "`n") -notmatch 'MISSING UPDATE' -or ($demoBoard -join "`n") -notmatch 'OVERDUE') { throw 'Demo attention cases are missing.' }
if (Test-Path -LiteralPath (Join-Path $dataDirectory 'tracker.db')) { throw 'Demo created a normal working database.' }
Invoke-PackagedTracker -ApplicationDirectory $first -CommandArgs @('demo','--no-gui') | Out-Null
Invoke-PackagedTracker -ApplicationDirectory $first -CommandArgs @('import',(Join-Path $first 'fixtures/updates-template.csv'))
Invoke-PackagedTracker -ApplicationDirectory $first -CommandArgs @('backup')
$backup = Get-ChildItem -LiteralPath (Join-Path $dataDirectory 'backups') -Filter '*.db' | Select-Object -First 1
Invoke-PackagedTracker -ApplicationDirectory $first -CommandArgs @('project-complete','--id','P001')
# A second application copy uses the same data without rebuilding or installing Java.
$secondRoot = Join-Path $smokeRoot 'updated-copy'
& (Join-Path $package 'scripts/install-windows.ps1') -InstallRoot $secondRoot -NoShortcut
$second = Join-Path $secondRoot $version
$history = Invoke-PackagedTracker -ApplicationDirectory $second -CommandArgs @('project-list','--all')
if (($history -join "`n") -notmatch 'COMPLETED') { throw 'Data was not preserved between application copies.' }
Invoke-PackagedTracker -ApplicationDirectory $second -CommandArgs @('restore',$backup.FullName,'--confirm')
$active = Invoke-PackagedTracker -ApplicationDirectory $second -CommandArgs @('project-list')
if (($active -join "`n") -notmatch 'P001' -or ($active -join "`n") -notmatch 'P002') { throw 'Restore did not recover both active projects.' }
Set-Content -LiteralPath (Join-Path $dataDirectory 'preferences.properties') -Value 'staleDays=7' -Encoding ASCII
& (Join-Path $second 'scripts/run-weekly-report.ps1') -DataDirectory $dataDirectory
& (Join-Path $second 'scripts/run-weekly-report.ps1') -DataDirectory $dataDirectory
$reports = @(Get-ChildItem -LiteralPath (Join-Path $dataDirectory 'reports') -Filter '*.txt')
if ($reports.Count -ne 1 -or (Get-Content -LiteralPath $reports[0].FullName -Raw) -notmatch 'Student A') { throw 'Weekly report output is incorrect.' }
if((Get-Content -LiteralPath $reports[0].FullName -Raw) -notmatch 'more than 7 days'){throw 'Weekly runner ignored the saved update threshold.'}
[xml]$taskXml = (& (Join-Path $second 'scripts/schedule-weekly-report.ps1') -Preview | Out-String)
$command = $taskXml.SelectSingleNode("//*[local-name()='Exec']/*[local-name()='Command']").InnerText
$arguments = $taskXml.SelectSingleNode("//*[local-name()='Exec']/*[local-name()='Arguments']").InnerText
$settings = $taskXml.SelectSingleNode("//*[local-name()='StartWhenAvailable']").InnerText
if (-not [IO.Path]::IsPathRooted($command) -or -not $arguments.Contains($second) -or $settings -ne 'true') { throw 'Weekly task definition is invalid.' }
$taskFile = Join-Path $smokeRoot 'weekly-task-preview.xml'
$taskXml.Save($taskFile)
Write-Output 'PASS: separate demo, bundled runtime, isolated installation, persistence, backup/restore, weekly runner and task XML.'
Write-Output "Evidence directory: $smokeRoot"
