param([string[]]$Goals = @('package'))
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
if ($mavenCommand) { $maven = $mavenCommand.Source }
else {
    $toolsDirectory = Join-Path $env:LOCALAPPDATA 'StudentProjectTrackerTools'
    $maven = Join-Path $toolsDirectory 'apache-maven-3.9.9/bin/mvn.cmd'
    if (-not (Test-Path -LiteralPath $maven)) {
        New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null
        $archive = Join-Path $toolsDirectory 'apache-maven-3.9.9-bin.zip'
        $uri = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip'
        Invoke-WebRequest -Uri $uri -OutFile $archive
        $expected = (Invoke-WebRequest -Uri ($uri + '.sha512')).Content.Trim().Split(' ')[0]
        if ((Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash -ne $expected) { throw 'Maven download checksum does not match.' }
        Expand-Archive -LiteralPath $archive -DestinationPath $toolsDirectory -Force
    }
}
Push-Location -LiteralPath $repositoryRoot
try {
    & $maven -B -ntp @Goals
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
}
finally { Pop-Location }
