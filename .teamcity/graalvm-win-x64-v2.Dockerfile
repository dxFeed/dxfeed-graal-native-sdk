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
# docker build -m 8GB --build-arg GRAALVM_VERSION="java11-22.3.1" -t graalvm:win-x64-java11-22.3.1 -f graalvm-win-x64.Dockerfile .
# docker build -m 8GB --build-arg GRAALVM_VERSION="jdk-22.0.1" -t graalvm:win-x64-jdk-22.0.1 -f graalvm-win-x64.Dockerfile .
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
# docker run -m 8GB -v %cd%:C:\mnt\ --rm <name:tag> mvn clean package
#
# How to deploy a mvn project to the JFrog artifactory (run from project root):
# docker run -m 8GB -v %cd%:C:\mnt\ --rm <name:tag> mvn --settings ".teamcity/settings.xml" -Djfrog.user=<user> -Djfrog.password=<pass> deploy
#
# How to run an interactive:
# docker run -m 8GB --rm -it <name:tag> cmd

# We need to specify the container arch, otherwise we may have problems with multi-arch images on Windows.
FROM mcr.microsoft.com/windows/servercore:ltsc2019-amd64

ARG TARGETPLATFORM="win-x64"

# Define arguments for versions
ARG GRAALVM_VERSION="java11-22.3.1"
ARG MVN_VERSION="3.8.8"
ARG VS_BUILD_TOOLS_VERSION="17"

# Define installation paths
ARG MVN_INSTALL_PATH="C:/mvn"
ARG GRAALVM_INSTALL_PATH="C:/graalvm"
ARG VS_BUILD_TOOLS_INSTALL_PATH="C:/BuildTools"

# Copy PowerShell helper script into the container
COPY install.ps1 C:/install.ps1

# Run installation steps using PowerShell
RUN powershell -Command \
    Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; \
    . C:/install.ps1; \
    Install-VSBuildTools -Version ${VS_BUILD_TOOLS_VERSION} -InstallPath ${VS_BUILD_TOOLS_INSTALL_PATH}; \
    Install-Maven -Version ${MVN_VERSION} -InstallPath ${MVN_INSTALL_PATH}; \
    Install-GraalVM -Version ${GRAALVM_VERSION} -Platform ${TARGETPLATFORM -InstallPath ${GRAALVM_INSTALL_PATH}; \
    Remove-Item -Path C:/install.ps1

# Update environment variables
ENV JAVA_HOME="${GRAALVM_INSTALL_PATH}"
ENV PATH="${MVN_INSTALL_PATH}/bin;${GRAALVM_INSTALL_PATH}/bin;%PATH%"

# Define the entry point for the container
# Start the Visual Studio Developer Command Prompt for easier build tools access
ENTRYPOINT ["C:/BuildTools/Common7/Tools/VsDevCmd.bat", "-arch=amd64", "&&", "cmd.exe", "/C"]

# Set the default working directory for the container
WORKDIR "C:/mnt/"
