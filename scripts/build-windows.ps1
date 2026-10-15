param(
    [string]$Maven = 'mvn',
    [ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version = '0.1.0',
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
Push-Location -LiteralPath $repositoryRoot
try {
    if (-not $SkipBuild) {
        & $Maven -B -ntp package
        if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
    }
    $jarFile = Join-Path $repositoryRoot 'target/student-project-tracker-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jarFile)) { throw 'Build the application first.' }
    $javaPath = (Get-Command java -ErrorAction Stop).Source
    $jdkBin = Split-Path $javaPath -Parent
    $jlink = Join-Path $jdkBin 'jlink.exe'
    $jpackage = Join-Path $jdkBin 'jpackage.exe'
    if (-not (Test-Path -LiteralPath $jlink) -or -not (Test-Path -LiteralPath $jpackage)) { throw 'A Windows JDK with jlink and jpackage is required.' }
    # Each build uses a new directory, without deleting earlier packages.
    $workDirectory = Join-Path $repositoryRoot ('target/package-' + [guid]::NewGuid().ToString('N'))
    $inputDirectory = Join-Path $workDirectory 'input'
    $runtimeDirectory = Join-Path $workDirectory 'runtime'
    $distributionDirectory = Join-Path $workDirectory 'distribution'
    New-Item -ItemType Directory -Path $inputDirectory -Force | Out-Null
    Copy-Item -LiteralPath $jarFile -Destination (Join-Path $inputDirectory 'tracker.jar')
    & $jlink --add-modules 'java.se,jdk.unsupported,jdk.crypto.ec,jdk.charsets' --strip-debug --no-header-files --no-man-pages --output $runtimeDirectory
    if ($LASTEXITCODE -ne 0) { throw 'Runtime packaging failed.' }
    & $jpackage --type app-image --name StudentProjectTracker --app-version $Version --vendor 'CS5013 IIT Madras' --input $inputDirectory --main-jar tracker.jar --main-class in.iitm.cs5013.tracker.TrackerApplication --arguments gui --runtime-image $runtimeDirectory --dest $distributionDirectory --java-options '--enable-native-access=ALL-UNNAMED'
    if ($LASTEXITCODE -ne 0) { throw 'Application packaging failed.' }
    $packageDirectory = Join-Path $distributionDirectory 'StudentProjectTracker'
    Copy-Item -LiteralPath (Join-Path $repositoryRoot 'scripts') -Destination $packageDirectory -Recurse
    Copy-Item -LiteralPath (Join-Path $repositoryRoot 'docs') -Destination $packageDirectory -Recurse
    Copy-Item -LiteralPath (Join-Path $repositoryRoot 'fixtures') -Destination $packageDirectory -Recurse
    Copy-Item -LiteralPath (Join-Path $repositoryRoot 'README.md') -Destination $packageDirectory
    Set-Content -LiteralPath (Join-Path $packageDirectory 'VERSION.txt') -Value $Version -Encoding ASCII
    Copy-Item -LiteralPath (Join-Path $repositoryRoot 'scripts/TrackerCLI.cmd') -Destination $packageDirectory
    $archive = Join-Path $repositoryRoot "target/StudentProjectTracker-windows-$env:PROCESSOR_ARCHITECTURE-$Version.zip"
    Compress-Archive -LiteralPath $packageDirectory -DestinationPath $archive -Force
    Write-Output "App folder: $packageDirectory"
    Write-Output "ZIP package: $archive"
}
finally { Pop-Location }
