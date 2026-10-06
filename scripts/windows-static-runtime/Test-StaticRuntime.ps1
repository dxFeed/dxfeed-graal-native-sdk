<#
.SYNOPSIS
Checks a Windows archive of the SDK linked with the static C runtime (-static-mt): no Visual C++ Redistributable needed.

.DESCRIPTION
  1. DxFeedGraalNativeSdk.dll of the archive must not import the dynamic C runtime (VCRUNTIME*, MSVCP*,
     api-ms-win-crt-*, ucrtbase).
  2. static-runtime-check.c is compiled with /MT against the archive (DxFeedGraalNativeSdk.lib) and must not import it
     either.
  3. The check runs on this machine: a local hub publishes a profile, a listener receives it, the last event is read
     back, a system property is set and read, the isolate is torn down (no network).
  4. With -Image, the check also runs in a Windows container of that image, which must not have the Visual C++
     Redistributable (servercore has none): the proof that the SDK works on a machine without it. The image is pulled
     if needed and runs in process isolation when its Windows build is the one of this machine, in Hyper-V isolation
     otherwise. "auto" takes mcr.microsoft.com/windows/servercore of the version of this Windows Server (ltsc2019,
     ltsc2022, ltsc2025).

Requires the MSVC environment (cl, dumpbin); docker with Windows containers for -Image.

.PARAMETER Archive
The archive: graal-native-sdk-<version>-amd64-windows[-debug]-static-mt.zip (or the one in target).

.PARAMETER WorkDirectory
Where the archive is unpacked and the check is built.

.PARAMETER Image
none (default), auto, or a Windows container image to run the check in.
#>
param(
    [Parameter(Mandatory = $true)][string]$Archive,
    [Parameter(Mandatory = $true)][string]$WorkDirectory,
    [string]$Image = 'none'
)

$ErrorActionPreference = 'Stop'
$dynamicRuntime = '^(vcruntime|msvcp|msvcr|api-ms-win-crt|ucrtbase)'

function Get-Tool([string]$Name) {
    $tool = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $tool) {
        throw "$Name is not in PATH: run in the MSVC environment (vcvars64.bat, VsDevCmd.bat -arch=amd64)"
    }
    return $tool.Source
}

function Get-Dependencies([string]$File) {
    $dependencies = & $script:dumpbin /nologo /dependents $File | Where-Object { $_ -match '^\s+(\S+\.dll)$' } |
            ForEach-Object { $Matches[1] }
    if ($LASTEXITCODE -ne 0) {
        throw "dumpbin failed for $File"
    }
    return @($dependencies)
}

function Assert-StaticRuntime([string]$File) {
    $dependencies = Get-Dependencies $File
    $dynamic = @($dependencies | Where-Object { $_ -match $dynamicRuntime })
    Write-Host "[INFO] $(Split-Path $File -Leaf): $($dependencies -join ', ')"
    if ($dynamic.Count -gt 0) {
        throw "$File imports the dynamic C runtime: $($dynamic -join ', ')"
    }
}

$script:dumpbin = Get-Tool 'dumpbin.exe'
$cl = Get-Tool 'cl.exe'

$sdk = Join-Path $WorkDirectory 'sdk'
if (Test-Path $sdk) {
    Remove-Item -Recurse -Force $sdk
}
New-Item -ItemType Directory -Force $sdk | Out-Null
Expand-Archive -Path $Archive -DestinationPath $sdk

# 1. The library.
Assert-StaticRuntime (Join-Path $sdk 'DxFeedGraalNativeSdk.dll')

# 2. The check, built with /MT against the archive.
Copy-Item (Join-Path $PSScriptRoot 'static-runtime-check.c') $sdk
Push-Location $sdk
try {
    & $cl /nologo /MT /W4 /O2 static-runtime-check.c DxFeedGraalNativeSdk.lib | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "cl failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
$check = Join-Path $sdk 'static-runtime-check.exe'
Assert-StaticRuntime $check

# The program prints "OK: ..." last: an exit code 0 without it is not a passed check (e.g. a command that did not run).
function Assert-Passed([string]$Where, [object[]]$Output, [int]$ExitCode) {
    $Output | Out-Host
    if ($ExitCode -ne 0 -or -not ($Output | Where-Object { $_ -match '^OK: ' })) {
        throw "The check failed $Where (exit code $ExitCode, no OK line)"
    }
}

# 3. On this machine.
$output = & $check
Assert-Passed 'on this machine' $output $LASTEXITCODE

# 4. In a container without the Visual C++ Redistributable.
if ($Image -ne 'none') {
    $build = [Environment]::OSVersion.Version.Build
    if ($Image -eq 'auto') {
        $tags = @{ 17763 = 'ltsc2019'; 20348 = 'ltsc2022'; 26100 = 'ltsc2025' }
        if (-not $tags.ContainsKey($build)) {
            throw "No servercore image for Windows build ${build}: pass -Image <image>"
        }
        $Image = "mcr.microsoft.com/windows/servercore:$($tags[$build])"
    }

    # Not "docker image inspect": the "No such image" of its stderr stops Windows PowerShell 5.1 (ErrorActionPreference).
    $dockerOs = & docker version --format '{{.Server.Os}}'
    if ($LASTEXITCODE -ne 0 -or $dockerOs -ne 'windows') {
        throw "docker must run Windows containers (the server OS is '$dockerOs', exit code $LASTEXITCODE)"
    }
    if (-not (& docker image ls --quiet $Image)) {
        Write-Host "[INFO] Pulling $Image"
        & docker pull $Image | Out-Host
        if ($LASTEXITCODE -ne 0) {
            throw "docker pull $Image failed with exit code $LASTEXITCODE"
        }
    }
    # Process isolation needs the same Windows build as this one; another build runs in Hyper-V isolation.
    $imageBuild = [int](((& docker image inspect --format '{{.OsVersion}}' $Image) -split '\.')[2])
    $isolation = if ($imageBuild -eq $build) { 'process' } else { 'hyperv' }

    Write-Host "[INFO] The check in $Image (Windows build $imageBuild, this one $build, $isolation isolation)"
    # "else": a command after "&" that follows the block of "if" runs only with the condition.
    $command = 'if exist C:\Windows\System32\vcruntime140.dll (echo The image has the Visual C++ Redistributable& exit /b 2) ' +
            'else (C:\check\static-runtime-check.exe)'
    $output = & docker run --rm --isolation=$isolation --volume "$((Resolve-Path $sdk).Path):C:\check" $Image cmd /c $command
    Assert-Passed "in $Image" $output $LASTEXITCODE
}

Write-Host "[INFO] $(Split-Path $Archive -Leaf): the static C runtime is checked"
