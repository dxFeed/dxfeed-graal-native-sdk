@echo off
call "%VS_BUILD_TOOLS_INSTALL_PATH%\Common7\Tools\VsDevCmd.bat" -arch=amd64 -no_logo
if errorlevel 1 (
    echo docker-entrypoint: VsDevCmd initialization failed 1>&2
    exit /b 1
)
cmd.exe /S /C %*