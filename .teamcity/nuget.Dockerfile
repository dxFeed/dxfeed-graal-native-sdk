#
# Dockerfile for NuGet CLI
#
# This Dockerfile creates an environment for running the NuGet Command Line Interface (CLI)
# inside a container. It's designed to perform NuGet operations such as package creation
# or publishing without needing NuGet installed locally.
# This container can be run from Linux or Windows (using containerd and nerdctl).
#
# BUILD
#
# How to build an image with the latest version of NuGet CLI:
# docker build -t nuget:latest -f nuget.Dockerfile .
#
# How to build an image with the specified version of NuGet CLI:
# docker build --build-arg="NUGET_VERSION=v6.8.1" -t nuget:6.8.1 -f nuget.Dockerfile .
#
# MOUNTING DIRECTORIES
#
# To mount the current directory into a container, use ${PWD} in PowerShell, %cd% in cmd,
# or $(pwd) if you are using Linux with bash (sh, zsh, fish, etc).
# It is expected that the project root will be mounted into container.
#
# NOTES
#
# If you're using Windows PowerShell is very weird about handling arguments that contain a dot,
# so it's probably better to use cmd.
#
# EXAMPLES
#
# How to create a NuGet package based on the specified nuspec:
# docker run -v $(pwd):/mnt --rm nuget pack <project.nuspec> -Version X.Y.Z
#
# How to deploy a created package to the NuGet repository:
# docker run -v $(pwd):/mnt --rm nuget push <package.nupkg> -Source <path> -ApiKey <key>
#
# How to run an interactive:
# docker run --entrypoint=/bin/bash --rm -it nuget

# We use the base mono image as the simplest way to run nuget.exe of a specified version.
FROM mono:6

# A list of NuGet versions can be found at https://www.nuget.org/downloads.
# Use latest, for the current "latest" version, or use "vX.Y.Z", for the specified version.
ARG NUGET_VERSION=latest
RUN download_url="https://dist.nuget.org/win-x86-commandline/${NUGET_VERSION}/nuget.exe"; \
    install_path="/usr/lib/nuget/nuget.exe"; \
    curl -SL --create-dirs --output "${install_path}" "${download_url}" && \
    chmod +x "${install_path}"

# Run NuGet CLI directly.
ENTRYPOINT ["nuget"]

# Default working directory.
# It is expected that the project root will be mounted in this directory.
WORKDIR /mnt/
