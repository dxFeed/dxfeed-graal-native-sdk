<#
.SYNOPSIS
Collects Native Image metadata with the native-image-agent and merges it into src/main/resources/META-INF/native-image.

.DESCRIPTION
Run it after changing dependency versions (e.g. qd.version, auther-api.version), then review the diff.
Requirements: JAVA_HOME points to GraalVM 23+ (its agent writes reachability-metadata.json), Maven and Python 3.

Steps:
  1. Compiles the tests and builds the classpath (target/classpath.txt).
  2. Runs the scenario classes under the agent. The agent works in target/native-image-agent seeded with
     the current reachability-metadata.json, so the new metadata accumulates on top of the existing one.
  3. Updates reachability-metadata.json (GraalVM 23+) and merges the new entries into the legacy
     reflect-config.json, serialization-config.json, jni-config.json, resource-config.json (GraalVM < 23).
     Existing entries are never removed.

.PARAMETER MainClass
Scenario classes to run (they must be offline-friendly or get addresses/credentials via JavaArgs).

.PARAMETER JavaArgs
Additional JVM arguments for the scenarios, e.g. "-Dtoken=...".

.PARAMETER NoLegacy
Updates only reachability-metadata.json.

.EXAMPLE
.\update-native-image-metadata.ps1

.EXAMPLE
.\update-native-image-metadata.ps1 -MainClass com.dxfeed.NewCases,com.dxfeed.NativeLibMain -JavaArgs "-Dtoken=<token>"
#>
param(
    [string[]]$MainClass = @("com.dxfeed.NewCases"),
    [string[]]$JavaArgs = @(),
    [switch]$NoLegacy
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$metadataDir = Join-Path $PSScriptRoot 'src/main/resources/META-INF/native-image'
$workDir = Join-Path $PSScriptRoot 'target/native-image-agent'
$classpathFile = Join-Path $PSScriptRoot 'target/classpath.txt'

if (-not $env:JAVA_HOME) {
    throw "JAVA_HOME is not set, it must point to GraalVM 23+"
}
$release = Get-Content (Join-Path $env:JAVA_HOME 'release') -Raw
if ($release -notmatch 'JAVA_VERSION="(\d+)' -or [int]$Matches[1] -lt 23) {
    throw "JAVA_HOME must point to GraalVM 23+ (found: $env:JAVA_HOME)"
}
$java = Join-Path $env:JAVA_HOME 'bin/java'
# On Windows, python3/python may be Microsoft Store stubs, so pick the first interpreter that actually runs.
$python = Get-Command python3, python -All -ErrorAction SilentlyContinue |
        Where-Object { & $_.Source --version 2>$null; $LASTEXITCODE -eq 0 } |
        Select-Object -First 1 -ExpandProperty Source
if (-not $python) {
    throw "Python 3 is required"
}

function Invoke-Checked([string]$Name, [scriptblock]$Command) {
    Write-Host "[INFO] $Name" -ForegroundColor Cyan
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

# exec.skip skips the Windows resource compilation (rc), which is not needed to run the scenarios.
Invoke-Checked "Compiling tests" { mvn -q -B test-compile "-Dexec.skip=true" }
Invoke-Checked "Building classpath" { mvn -q -B dependency:build-classpath "-Dmdep.outputFile=$classpathFile" }
$classpath = @((Get-Content $classpathFile -Raw).Trim(), 'target/test-classes', 'target/classes') -join [IO.Path]::PathSeparator

Remove-Item $workDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $workDir | Out-Null
$current = Join-Path $metadataDir 'reachability-metadata.json'
if (Test-Path $current) {
    Copy-Item $current $workDir
}

foreach ($class in $MainClass) {
    # The scenarios report their own failures, so the exit code is not checked here.
    Write-Host "[INFO] Running $class with the agent" -ForegroundColor Cyan
    & $java "-agentlib:native-image-agent=config-merge-dir=$workDir" -cp $classpath @JavaArgs $class
    Write-Host "[INFO] $class finished with exit code $LASTEXITCODE" -ForegroundColor Gray
}

$mergeArgs = @((Join-Path $PSScriptRoot 'merge-agent-metadata.py'), (Join-Path $workDir 'reachability-metadata.json'), $metadataDir)
if ($NoLegacy) {
    $mergeArgs += '--no-legacy'
}
Invoke-Checked "Merging metadata" { & $python @mergeArgs }

Write-Host "[INFO] Done. Review the changes:" -ForegroundColor Green
git status --short -- $metadataDir
