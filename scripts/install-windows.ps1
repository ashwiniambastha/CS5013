param(
    [string]$InstallRoot = (Join-Path $env:LOCALAPPDATA 'Programs/StudentProjectTracker'),
    [switch]$NoShortcut
)
$ErrorActionPreference = 'Stop'
$packageDirectory = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path -LiteralPath (Join-Path $packageDirectory 'StudentProjectTracker.exe'))) { throw 'Run this script from an extracted Windows package.' }
$version = (Get-Content -LiteralPath (Join-Path $packageDirectory 'VERSION.txt') -Raw).Trim()
if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Invalid package version.' }
$destination = [IO.Path]::GetFullPath((Join-Path $InstallRoot $version))
$sourcePath = [IO.Path]::GetFullPath($packageDirectory).TrimEnd('\')
if ($destination.StartsWith($sourcePath + '\', [StringComparison]::OrdinalIgnoreCase) -or $destination -eq $sourcePath) { throw 'Install outside the extracted package directory.' }
if (Test-Path -LiteralPath $destination) { throw "Installation already exists: $destination. Keep it or select another InstallRoot." }
New-Item -ItemType Directory -Path $InstallRoot -Force | Out-Null
Copy-Item -LiteralPath $packageDirectory -Destination $destination -Recurse
if (-not $NoShortcut) {
    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut((Join-Path ([Environment]::GetFolderPath('Desktop')) 'Student Project Tracker.lnk'))
    $shortcut.TargetPath = Join-Path $destination 'StudentProjectTracker.exe'
    $shortcut.WorkingDirectory = $destination
    $shortcut.Save()
}
Write-Output "Installed: $destination"
Write-Output 'Existing tracking data is kept in your application-data folder.'
