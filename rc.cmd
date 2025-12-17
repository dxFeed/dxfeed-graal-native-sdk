@echo off
setlocal enabledelayedexpansion

where rc.exe >nul 2>&1
if not errorlevel 1 (
    echo [INFO] rc.exe found in PATH, running directly...
    rc.exe %*
    exit /b %errorlevel%
)

echo [INFO] rc.exe not found in PATH, setting up environment...

if not exist "C:\jdk\graalvm-community-openjdk-22.0.1+8.1\" (
    echo [ERROR] Java directory not found
    exit /b 1
)

if not exist "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" (
    echo [ERROR] vcvars64.bat not found
    exit /b 1
)

call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"

where rc.exe >nul 2>&1
if errorlevel 1 (
    echo [ERROR] rc.exe not found in PATH
    exit /b 1
)

echo [INFO] Running rc.exe with parameters: %*
rc.exe %*