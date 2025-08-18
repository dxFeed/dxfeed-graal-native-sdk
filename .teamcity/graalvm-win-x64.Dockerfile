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

# Download and install latest MSYS2,
# ARG MSYS2_DOWNLOAD_URL="https://github.com/msys2/msys2-installer/releases/download/nightly-x86_64/msys2-base-x86_64-latest.sfx.exe"
ARG MSYS2_DOWNLOAD_URL="https://github.com/msys2/msys2-installer/releases/download/2024-05-07/msys2-base-x86_64-20240507.sfx.exe"
RUN curl -SL --output msys2.exe %MSYS2_DOWNLOAD_URL% && \
    msys2.exe -y -oC:/ && \
    del msys2.exe && \
    setx /M TZ UTC && \
    C:/msys64/usr/bin/bash -lc " " && \
    setx /M PATH "C:/msys64/usr/local/bin;C:/msys64/usr/bin;C:/msys64/bin;%PATH%"

# Copy the helper script to the PATH directory with correct permissions.
COPY install.sh "C:/msys64/usr/local/bin/"
RUN C:/msys64/usr/bin/bash -lc "chmod +x /usr/local/bin/install.sh"

# Download and install Build Tools for Visual Studio.
ARG VS_BUILD_TOOLS_VERSION="17"
ARG VS_BUILD_TOOLS_INSTALL_PATH="C:/BuildTools/"
RUN	bash -lc "install.sh vs_build_tools %VS_BUILD_TOOLS_VERSION% %VS_BUILD_TOOLS_INSTALL_PATH%"

# Download and install Apache Maven.
ARG MVN_VERSION="3.8.9"
ARG MVN_INSTALL_PATH="C:/mvn/"
RUN	bash -lc "install.sh maven %MVN_VERSION% %MVN_INSTALL_PATH%" && \
    setx /M PATH "%MVN_INSTALL_PATH%/bin;%PATH%"

# Download and install GraalVM.
ARG GRAALVM_VERSION="java11-22.3.1"
ARG GRAALVM_INSTALL_PATH="C:/graalvm/"
RUN	bash -lc "install.sh graalvm %GRAALVM_VERSION% %TARGETPLATFORM% %GRAALVM_INSTALL_PATH%" && \
    setx /M JAVA_HOME "%GRAALVM_INSTALL_PATH%" && \
    setx /M PATH "%GRAALVM_INSTALL_PATH%/bin;%PATH%"

# Define the entry point for the docker container.
# This entry point starts the developer command prompt. Don't use powershell here.
# Powershell handles commands with a dot (e.g. -Dprop.subprop=value) very weird.
ENTRYPOINT ["C:/BuildTools/Common7/Tools/VsDevCmd.bat", "-arch=amd64", "&&", "cmd.exe", "/C"]

# Default working directory.
# It is expected that the project root will be mounted in this directory.
WORKDIR "C:/mnt/"
