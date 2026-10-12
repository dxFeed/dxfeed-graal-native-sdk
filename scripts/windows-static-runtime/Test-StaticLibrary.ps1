<#
.SYNOPSIS
Checks a Windows archive of the static library of the SDK (-static-lib): one library for any C runtime.

.DESCRIPTION
For each C runtime of the application (/MT, /MTd, /MD, /MDd), static-runtime-check.c is linked with the library of the
archive and the Windows libraries of its link-flags.txt, next to:
  - static-library-clash.c, the application's own deflate, inflate and JNU_ThrowByName, which the JDK in the library
    defines too (the library hides its internal symbols);
  - static-library-vector.cpp, C++ code with the /FAILIFMISMATCH directives of RuntimeLibrary and _ITERATOR_DEBUG_LEVEL.
The link runs without /DEBUG and with it (the CMake Debug and RelWithDebInfo builds): any linker warning fails the
check. The program must export nothing, with /MT and /MTd must not import the dynamic C runtime (VCRUNTIME*, MSVCP*,
api-ms-win-crt-*, ucrtbase), and must run: a local hub publishes a profile, a listener receives it (no network).

When cmake is in PATH, the same sources are also built through the CMakeLists.txt of the archive (add_subdirectory and
target_link_libraries(<app> DxFeedGraalNativeSdk), NMake Makefiles): Release with /MT and Debug with /MDd, without
linker warnings, then run, and cmake --install must put the library into lib. Without cmake this part is skipped with
a warning.

Requires the MSVC environment (cl, link, dumpbin).

.PARAMETER Archive
The archive: graal-native-sdk-<version>-amd64-windows[-debug]-static-lib.zip (or the one in target).

.PARAMETER WorkDirectory
Where the archive is unpacked and the checks are built.
#>
param(
    [Parameter(Mandatory = $true)][string]$Archive,
    [Parameter(Mandatory = $true)][string]$WorkDirectory
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

function Assert-NoExports([string]$File) {
    $exports = & $script:dumpbin /nologo /exports $File
    if ($LASTEXITCODE -ne 0) {
        throw "dumpbin failed for $File"
    }
    if ($exports -match 'following exports') {
        $exports | Out-Host
        throw "$File exports symbols of the static library"
    }
}

# The program prints "OK: ..." last: an exit code 0 without it is not a passed check (e.g. a command that did not run).
function Assert-Passed([string]$File) {
    # On a machine short of memory, an executable run right after link.exe wrote it was seen mapped with zero pages
    # instead of the written ones (0xC0000005 at the entry point, 0xC000001D, a hang or a Java exception, while the
    # file is correct). Opening the file for writing drops the cached image, so the run maps what is on the disk.
    $stream = [IO.File]::Open($File, 'Open', 'ReadWrite', 'None')
    $stream.Flush($true)
    $stream.Dispose()
    $output = & $File
    $exitCode = $LASTEXITCODE
    $output | Out-Host
    if ($exitCode -ne 0 -or -not ($output | Where-Object { $_ -match '^OK: ' })) {
        throw "$File failed (exit code $exitCode, no OK line)"
    }
}

# Builds the check through the CMake target of the archive, as an application does, and runs it.
function Test-CMakeTarget([string]$Cmake, [string]$Source, [string]$Configuration, [string]$Runtime) {
    $build = Join-Path $WorkDirectory "cmake-$Configuration"
    & $Cmake -S $Source -B $build -G 'NMake Makefiles' "-DCMAKE_BUILD_TYPE=$Configuration" `
            "-DCMAKE_MSVC_RUNTIME_LIBRARY=$Runtime" | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "cmake configure ($Configuration) failed with exit code $LASTEXITCODE"
    }
    $output = & $Cmake --build $build
    $output | Out-Host
    if ($LASTEXITCODE -ne 0 -or ($output -match 'warning LNK')) {
        throw "cmake build ($Configuration) failed or the linker warned (exit code $LASTEXITCODE)"
    }
    $exe = Join-Path $build 'static-library-cmake.exe'
    Assert-NoExports $exe
    if ($Runtime -notlike '*DLL') {
        Assert-StaticRuntime $exe
    }
    Assert-Passed $exe
    return $build
}

$script:dumpbin = Get-Tool 'dumpbin.exe'
$cl = Get-Tool 'cl.exe'
$link = Get-Tool 'link.exe'

$sdk = Join-Path $WorkDirectory 'sdk'
if (Test-Path $WorkDirectory) {
    Remove-Item -Recurse -Force $WorkDirectory
}
New-Item -ItemType Directory -Force $sdk | Out-Null
Expand-Archive -Path $Archive -DestinationPath $sdk

$library = Join-Path $sdk 'DxFeedGraalNativeSdk_static.lib'
foreach ($file in $library, (Join-Path $sdk 'link-flags.txt'), (Join-Path $sdk 'CMakeLists.txt')) {
    if (-not (Test-Path $file)) {
        throw "$(Split-Path $Archive -Leaf) has no $(Split-Path $file -Leaf)"
    }
}
$dlls = @(Get-ChildItem $sdk -Recurse -Filter *.dll)
if ($dlls.Count -gt 0) {
    throw "$(Split-Path $Archive -Leaf) has DLLs: $($dlls.Name -join ', ')"
}
$windowsLibraries = @(Get-Content (Join-Path $sdk 'link-flags.txt') | Where-Object { $_ })

Push-Location $WorkDirectory
try {
    foreach ($runtime in 'MT', 'MTd', 'MD', 'MDd') {
        $sources = 'static-runtime-check.c', 'static-library-clash.c', 'static-library-vector.cpp'
        $objects = foreach ($source in $sources) {
            $object = "$runtime-$([System.IO.Path]::GetFileNameWithoutExtension($source)).obj"
            & $cl /nologo "/$runtime" /W4 /EHsc /c "/I$sdk" "/Fo$object" (Join-Path $PSScriptRoot $source) | Out-Host
            if ($LASTEXITCODE -ne 0) {
                throw "cl /$runtime failed for $source with exit code $LASTEXITCODE"
            }
            $object
        }
        foreach ($debug in $false, $true) {
            $name = if ($debug) { "$runtime-debug" } else { $runtime }
            $arguments = @('/NOLOGO', "/OUT:$name.exe") + $objects + $library + $windowsLibraries
            if ($debug) {
                $arguments += '/DEBUG'
            }
            $output = & $link $arguments
            $output | Out-Host
            if ($LASTEXITCODE -ne 0 -or $output) {
                throw "link of $name.exe failed or warned (exit code $LASTEXITCODE)"
            }
            $exe = Join-Path $WorkDirectory "$name.exe"
            Assert-NoExports $exe
            if ($runtime -like 'MT*') {
                Assert-StaticRuntime $exe
            }
            Assert-Passed $exe
        }
    }
} finally {
    Pop-Location
}

$cmake = Get-Command 'cmake.exe' -ErrorAction SilentlyContinue
if ($cmake) {
    $source = Join-Path $WorkDirectory 'cmake'
    New-Item -ItemType Directory -Force $source | Out-Null
    $sdkPath = $sdk -replace '\\', '/'
    $scripts = $PSScriptRoot -replace '\\', '/'
    @"
cmake_minimum_required(VERSION 3.21)
project(StaticLibraryCMakeCheck C CXX)
add_subdirectory("$sdkPath" sdk)
add_executable(static-library-cmake "$scripts/static-runtime-check.c" "$scripts/static-library-clash.c"
        "$scripts/static-library-vector.cpp")
target_link_libraries(static-library-cmake PRIVATE DxFeedGraalNativeSdk)
"@ | Set-Content -Encoding ASCII (Join-Path $source 'CMakeLists.txt')
    $release = Test-CMakeTarget $cmake.Source $source 'Release' 'MultiThreaded'
    Test-CMakeTarget $cmake.Source $source 'Debug' 'MultiThreadedDebugDLL' | Out-Null
    $prefix = Join-Path $WorkDirectory 'cmake-install'
    & $cmake.Source --install $release --prefix $prefix | Out-Host
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path (Join-Path $prefix 'lib\DxFeedGraalNativeSdk_static.lib'))) {
        throw "cmake --install did not put DxFeedGraalNativeSdk_static.lib into lib (exit code $LASTEXITCODE)"
    }
    Write-Host "[INFO] $(Split-Path $Archive -Leaf): the CMake target is checked (Release /MT, Debug /MDd, install)"
} else {
    Write-Host "[WARN] cmake is not in PATH: the CMakeLists.txt of $(Split-Path $Archive -Leaf) is not checked"
}

Write-Host "[INFO] $(Split-Path $Archive -Leaf): the static library is checked with /MT, /MTd, /MD and /MDd"
