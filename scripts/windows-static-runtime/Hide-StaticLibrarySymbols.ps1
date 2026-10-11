<#
.SYNOPSIS
Hides the internal symbols of a static library of the SDK: only dxfg_* and graal_* stay visible to the application.

.DESCRIPTION
The static library holds the image object of native-image and the libraries of GraalVM linked with it (the JDK with its
zlib, JNU_*, the SVM C helpers). For an application that links it, this script (StaticLibrary.cs):

  1. prefixes with __dxfg_ every symbol that an object of the library defines, in all objects (the definitions and the
     references), except dxfg_*, graal_*, the COMDAT constants __real@*, __xmm@*, ??_C@*, the CRT inline functions
     __local_stdio_* and both names of each /alternatename: the application may define its own deflate, inflate or
     JNU_ThrowByName;
  2. blanks in place, in the linker directives, /EXPORT (an exe would export the whole SDK), /DEFAULTLIB of MSVCRT,
     msvcprt and LIBCMT and /FAILIFMISMATCH of RuntimeLibrary and _ITERATOR_DEBUG_LEVEL: the application chooses the C
     runtime (/MT, /MTd, /MD, /MDd), the CRT shim of the library binds to it;
  3. excludes from the link the debug information of the objects whose types are in a PDB of their build that is not
     shipped (libchelper, jvm: vc140.pdb), otherwise any link with /DEBUG warns LNK4099;
  4. archives the rewritten objects with lib.exe, which builds the symbol index again.

An object that is not a regular COFF x64 object, or an /INCLUDE of a renamed symbol, fails the script. The renaming map
is written next to the output (.map). Requires the MSVC environment (lib).

.PARAMETER InputLibrary
The static library with the objects as they are.

.PARAMETER OutputLibrary
The static library with the hidden symbols.
#>
param(
    [Parameter(Mandatory = $true)][string]$InputLibrary,
    [Parameter(Mandatory = $true)][string]$OutputLibrary
)

$ErrorActionPreference = 'Stop'

function Get-Tool([string]$Name) {
    $tool = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $tool) {
        throw "$Name is not in PATH: run in the MSVC environment (vcvars64.bat, VsDevCmd.bat -arch=amd64)"
    }
    return $tool.Source
}

$lib = Get-Tool 'lib.exe'
if (-not ('DxFeed.StaticLibrary.SymbolHider' -as [type])) {
    Add-Type -Path (Join-Path $PSScriptRoot 'StaticLibrary.cs')
}

$started = Get-Date
$InputLibrary = (Resolve-Path $InputLibrary).Path
$OutputLibrary = [System.IO.Path]::GetFullPath($OutputLibrary)
$members = [DxFeed.StaticLibrary.Archive]::Read($InputLibrary)
$result = [DxFeed.StaticLibrary.SymbolHider]::Hide($members)

$objects = "$OutputLibrary.objects"
if (Test-Path $objects) {
    Remove-Item -Recurse -Force $objects
}
New-Item -ItemType Directory -Force $objects | Out-Null
Push-Location $objects
try {
    # The names of the objects repeat in the archive (lib /EXTRACT could not tell them apart): numbers instead,
    # relative, so that the archive does not keep the build directory.
    $files = for ($i = 0; $i -lt $result.Objects.Count; $i++) {
        $file = '{0:D4}.obj' -f $i
        [System.IO.File]::WriteAllBytes((Join-Path $objects $file), $result.Objects[$i])
        $file
    }
    # lib stores the objects in the reverse order of its arguments: the order of the input archive is kept.
    [array]::Reverse($files)
    $files | Set-Content -Encoding ascii objects.rsp
    if (Test-Path $OutputLibrary) {
        Remove-Item -Force $OutputLibrary
    }
    $output = & $lib /NOLOGO /MACHINE:X64 "/OUT:$OutputLibrary" '@objects.rsp'
    $output | Out-Host
    if ($LASTEXITCODE -ne 0 -or ($output | Where-Object { $_ -match 'warning' })) {
        throw "lib failed (exit code $LASTEXITCODE)"
    }
} finally {
    Pop-Location
    Remove-Item -Recurse -Force $objects
}

$result.Renamed.GetEnumerator() | ForEach-Object { "$($_.Key) $($_.Value)" } |
        Set-Content -Encoding ascii "$OutputLibrary.map"

$seconds = [int]((Get-Date) - $started).TotalSeconds
Write-Host ("[INFO] $(Split-Path $OutputLibrary -Leaf): $($result.Objects.Count) objects, $($result.Public) public " +
        "symbols, $($result.Renamed.Count) renamed, $($result.Exports) /EXPORT and $($result.RuntimeDirectives) C " +
        "runtime directives removed, debug information excluded in $($result.DebugExcluded) objects ($seconds s)")
