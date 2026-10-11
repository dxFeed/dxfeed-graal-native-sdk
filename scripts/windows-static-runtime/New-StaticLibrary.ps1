<#
.SYNOPSIS
Packs the static library of the SDK for Windows (DxFeedGraalNativeSdk_static.lib) from the image object of native-image.

.DESCRIPTION
The windowsStaticRuntime profile of pom.xml builds the DLL with -H:TempDirectory, so the image object
(SVM-*\DxFeedGraalNativeSdk.obj) stays after the build. This script:

  1. archives with lib.exe the image object and the libraries native-image links it with: libchelper and jvm of
     Substrate VM, the static JDK libraries extnet, nio, net, management_ext, java, zip, security.obj of sunmscapi, and
     crt_shim.obj (New-CrtShim.ps1), which binds the CRT import pointers of the JDK objects to the C runtime of the
     application;
  2. hides its internal symbols and its export and C runtime directives (Hide-StaticLibrarySymbols.ps1): one library for
     the applications with /MT, /MTd, /MD and /MDd;
  3. writes link-flags.txt, the Windows libraries the application links with it, one per line;
  4. removes the temporary directory of native-image.

A library missing from GraalVM fails the script; a library the image needs that is not in the list fails the check of
the archive (Test-StaticLibrary.ps1) with LNK2019. Requires the MSVC environment (lib), as native-image does.

.PARAMETER GraalVmHome
The GraalVM that builds the image.

.PARAMETER TempDirectory
The temporary directory of native-image (-H:TempDirectory); removed at the end.

.PARAMETER CrtShim
crt_shim.obj of New-CrtShim.ps1.

.PARAMETER OutputDirectory
Where DxFeedGraalNativeSdk_static.lib and link-flags.txt are written.
#>
param(
    [Parameter(Mandatory = $true)][string]$GraalVmHome,
    [Parameter(Mandatory = $true)][string]$TempDirectory,
    [Parameter(Mandatory = $true)][string]$CrtShim,
    [Parameter(Mandatory = $true)][string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'

# The Windows libraries that the image links (the default libraries of its objects are the C runtime only).
$windowsLibraries = 'psapi.lib', 'winhttp.lib', 'ncrypt.lib', 'crypt32.lib', 'version.lib', 'advapi32.lib',
        'ws2_32.lib', 'secur32.lib', 'iphlpapi.lib', 'userenv.lib', 'mswsock.lib'

function Get-Tool([string]$Name) {
    $tool = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $tool) {
        throw "$Name is not in PATH: run in the MSVC environment (vcvars64.bat, VsDevCmd.bat -arch=amd64)"
    }
    return $tool.Source
}

function Get-File([string]$Path) {
    if (-not (Test-Path $Path -PathType Leaf)) {
        throw "$Path is not found"
    }
    return $Path
}

$lib = Get-Tool 'lib.exe'
if (-not ('DxFeed.StaticLibrary.SymbolHider' -as [type])) {
    Add-Type -Path (Join-Path $PSScriptRoot 'StaticLibrary.cs')
}

$images = @(Get-ChildItem (Join-Path $TempDirectory 'SVM-*\DxFeedGraalNativeSdk.obj') -ErrorAction SilentlyContinue)
if ($images.Count -ne 1) {
    throw "Expected one SVM-*\DxFeedGraalNativeSdk.obj in $TempDirectory, found $($images.Count): the image is built " +
            "with -H:TempDirectory=$TempDirectory"
}

$clibraries = Join-Path $GraalVmHome 'lib\svm\clibraries\windows-amd64'
$static = Join-Path $GraalVmHome 'lib\static\windows-amd64'
New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
$work = Join-Path $OutputDirectory 'work'
if (Test-Path $work) {
    Remove-Item -Recurse -Force $work
}
New-Item -ItemType Directory -Force $work | Out-Null
try {
    # security.obj, the only object of sunmscapi.lib, goes in as an object: lib.exe places the members of an input
    # library differently, and this order is the one checked with the prototype.
    $security = @([DxFeed.StaticLibrary.Archive]::Read((Get-File (Join-Path $static 'sunmscapi.lib'))) |
            Where-Object { $_.Name -match '(^|\\)security\.obj$' })
    if ($security.Count -ne 1) {
        throw "Expected one security.obj in $static\sunmscapi.lib, found $($security.Count)"
    }
    $securityObject = Join-Path $work 'security.obj'
    [System.IO.File]::WriteAllBytes($securityObject, $security[0].Data)

    $inputs = @($images[0].FullName, (Get-File (Join-Path $clibraries 'libchelper.lib'))) +
            @('extnet', 'nio', 'net', 'management_ext', 'java' |
                    ForEach-Object { Get-File (Join-Path $static "$_.lib") }) +
            @($securityObject, (Get-File (Join-Path $static 'zip.lib')), (Get-File (Join-Path $clibraries 'jvm.lib')),
            (Get-File $CrtShim))
    $original = Join-Path $work 'original.lib'
    & $lib /NOLOGO /MACHINE:X64 "/OUT:$original" @inputs | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "lib failed with exit code $LASTEXITCODE"
    }

    & (Join-Path $PSScriptRoot 'Hide-StaticLibrarySymbols.ps1') -InputLibrary $original `
            -OutputLibrary (Join-Path $OutputDirectory 'DxFeedGraalNativeSdk_static.lib')
    $windowsLibraries | Set-Content -Encoding ascii (Join-Path $OutputDirectory 'link-flags.txt')
} finally {
    Remove-Item -Recurse -Force $work
}

Remove-Item -Recurse -Force $TempDirectory
Write-Host "[INFO] The static library of the SDK: $OutputDirectory"
