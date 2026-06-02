#
# Dockerfile for GraalVM on Windows
#
# This Dockerfile is designed to create a containerized Windows based development environment
# with the pre-installed GraalVM, Apache Maven and Build Tools for Visual Studio.
# It's tailored for building and deploying Java projects.
# This container must be run from Windows.
#
# BUILD
#
# How to build an image:
# docker build -m 8GB -t <name:tag> -f <Dockerfile> .
#
# For example:
# docker build -m 8GB --build-arg GRAALVM_VERSION="java11-22.3.1" -t graalvm:win-x64-java11-22.3.1 -f graalvm-win-x64-v2.Dockerfile .
# docker build -m 8GB --build-arg GRAALVM_VERSION="jdk-23.0.2" -t graalvm:win-x64-jdk-23.0.2 -f graalvm-win-x64-v2.Dockerfile .
#
# MOUNTING DIRECTORIES
#
# To mount the current directory into a container, use ${PWD} in PowerShell, %cd% in cmd.
# It is expected that the project root will be mounted into container.
#
# NOTES
#
# If you're using Windows PowerShell is very weird about handling arguments that contain a dot,
# so it's probably better to use cmd.
#
# EXAMPLES
#
# How to build a mvn project (run from project root):
# cmd:
# docker run -m 8GB -v %cd%:C:\mnt\ --rm <name:tag> powershell -NoProfile -ExecutionPolicy Bypass -File C:\run-mvn-vs.ps1 clean package
# powershell:
# docker run -m 8GB -v ${PWD}:C:\mnt\ --rm <name:tag> powershell -NoProfile -ExecutionPolicy Bypass -File C:\run-mvn-vs.ps1 clean package
#
# How to deploy a mvn project to the JFrog artifactory (run from project root):
# docker run -m 8GB -v %cd%:C:\mnt\ --rm <name:tag> mvn --settings ".teamcity/settings.xml" -Djfrog.user=<user> -Djfrog.password=<pass> deploy
# docker run -m 8GB -v %cd%:C:\mnt\ --rm <name:tag> powershell -NoProfile -ExecutionPolicy Bypass -File C:\run-mvn-vs.ps1 --settings ".teamcity/settings.xml" -Djfrog.user=<user> -Djfrog.password=<pass> deploy
#
# How to run an interactive:
# docker run -m 8GB --rm -it <name:tag> powershell -NoProfile -ExecutionPolicy Bypass -Command "& { . C:\run-mvn-vs.ps1 -SetupOnly; powershell }"

# We need to specify the container arch, otherwise we may have problems with multi-arch images on Windows.
FROM mcr.microsoft.com/windows/servercore:ltsc2019-amd64

ARG TARGETPLATFORM="win-x64"

# Define arguments for versions
ARG GRAALVM_VERSION="jdk-25.0.1"
ARG MVN_VERSION="3.8.9"
ARG VS_BUILD_TOOLS_VERSION="17"

ENV TARGETPLATFORM="${TARGETPLATFORM}" \
    GRAALVM_VERSION="${GRAALVM_VERSION}" \
    MVN_VERSION="${MVN_VERSION}" \
    VS_BUILD_TOOLS_VERSION="${VS_BUILD_TOOLS_VERSION}" \
    MVN_INSTALL_PATH="C:/mvn" \
    GRAALVM_INSTALL_PATH="C:/graalvm" \
    VS_BUILD_TOOLS_INSTALL_PATH="C:/BuildTools"

# Copy PowerShell helper script into the container
COPY install.ps1 C:/install.ps1

# Run installation steps using PowerShell
RUN powershell -Command \
    Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; \
    . C:/install.ps1; \
    Install-Maven -Version "$Env:MVN_VERSION" -InstallPath "$Env:MVN_INSTALL_PATH";

RUN powershell -Command \
    Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; \
    . C:/install.ps1; \
    Install-VSBuildTools -Version "$Env:VS_BUILD_TOOLS_VERSION" -InstallPath "$Env:VS_BUILD_TOOLS_INSTALL_PATH";

RUN powershell -Command \
    Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; \
    . C:/install.ps1; \
    Install-GraalVM -Version "$Env:GRAALVM_VERSION" -Platform "$Env:TARGETPLATFORM" -InstallPath "$Env:GRAALVM_INSTALL_PATH";

RUN powershell -Command \
    if (Test-Path -Path C:/install.ps1) { Remove-Item C:/install.ps1 -Force }

# Update environment variables
ENV JAVA_HOME="${GRAALVM_INSTALL_PATH}"
ENV PATH="${MVN_INSTALL_PATH}/bin;${GRAALVM_INSTALL_PATH}/bin;C:/Windows/System32;C:/Windows"

# PowerShell wrapper
COPY run-mvn-vs.ps1 C:/run-mvn-vs.ps1
COPY build.ps1 C:/build.ps1

ENTRYPOINT ["cmd.exe", "/S", "/C"]

# Set the default working directory for the container
WORKDIR "C:/mnt/"
