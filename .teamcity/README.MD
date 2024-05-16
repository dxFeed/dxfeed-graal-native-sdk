# Table of Contents

- [Building on Windows](#building-on-windows)
  * [Requirements for a TeamCity Agent](#requirements-for-a-teamcity-agent)
  * [How to install docker on Windows](#how-to-install-docker-on-windows)
- [Building on Linux](#building-on-linux)
  * [Requirements for a TeamCity Agent](#requirements-for-a-teamcity-agent-1)
- [Building on macOS](#building-on-macos)
  * [Requirements for a TeamCity Agent](#requirements-for-a-teamcity-agent-2)
- [Creating and Publishing GraalVM Docker Image for Project Build](#creating-and-publishing-graalvm-docker-image-for-project-build)
- [Creating and Publishing NuGet Package](#creating-and-publishing-nuget-package)

# Building on Windows

This project is built using Docker on Windows. The `graalvm-win-x64.Dockerfile` is utilized for
constructing the Docker image. Detailed documentation and usage examples for running the image are
available in the corresponding Dockerfile.

## Requirements for a TeamCity Agent:

- The `docker` must be installed on the agent to containerize Windows environments.
- The `dxcity` user should have permissions to run Docker containers without admin privileges.
- Establish `TeamCity Connections` to the Docker Registry
  at https://dxfeed.jfrog.io/artifactory/docker/dxfeed-api/.

## How to install docker on Windows

* The agent must support Hyper-V (might have to enable virtualization in BIOS or something else).
* If the agent is running in a hypervisor, nested-virtualization must be enabled and the
  virtualization type must be set to `host` (not `KVM`, `QEMU`, `EPIC`, etc.)
* It may be necessary to temporarily disable the security policy before running next
  script `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`
* Run this
  [script](https://github.com/microsoft/Windows-Containers/blob/Main/helpful_tools/Install-DockerCE/install-docker-ce.ps1)
  in powershell as admin. This script is in Microsoft GitHub, it is possible to do the same thing
  manually, but it is just longer.
* Add `dxcity` to `docker-users` group `net localgroup docker-users "dxcity" /ADD`, if this group
  does not exist, create its.
* Create `C:\ProgramData\docker\config\daemon.json` with the following content:
  ```json
  {
    "dns": [
      "8.8.8.8",
      "8.8.4.4",
      "1.1.1.1"
    ],
    "hosts": [
      "npipe://"
    ],
    "group": "docker-users",
    "experimental": true
  }
  ```
* Sometimes it's necessary to manually start the dockerd service:
  ```shell
  dockerd.exe --register-service
  Start-Service docker
  ```
* Delete `credsStore` line from `C:\Users\dxcity\.docker\config.json`
* Reboot

# Building on Linux

This project is built using Docker on Linux. The `graalvm-linux-x64.Dockerfile` is utilized for
constructing the Docker image. Detailed documentation and usage examples for running the image are
available in the corresponding Dockerfile.

## Requirements for a TeamCity Agent:

- The `docker` must be installed on the agent.
- The `dxcity` user should be capable of running containers without sudo.
- Establish `TeamCity Connections` to the Docker Registry
  at https://dxfeed.jfrog.io/artifactory/docker/dxfeed-api/.

# Building on macOS

The project is built directly on the macOS host system.

## Requirements for a TeamCity Agent:

* The agent must be equipped with an Apple Silicon processor.
* Rosetta 2 must be installed on the agent (`softwareupdate --install-rosetta`).
* Xcode Command Line Tools must be installed on the agent (`xcode-select --install`).

# Creating and Publishing GraalVM Docker Image for Project Build

```shell
# Authenticate with the Docker Registry
docker login dxfeed-docker.jfrog.io
# Build and push the Docker image
docker build --build-arg GRAALVM_VERSION="<version>" -t graalvm:<os>-<arch>-<version> -f .\.teamcity\graalvm-<os>-<arch>.Dockerfile .\.teamcity\
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:<os>-<arch>-<version>
```

Example build Windows docker image:
```shell
docker build -m 8GB --build-arg GRAALVM_VERSION="jdk-22.0.1" -t dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-jdk-22.0.1 -f graalvm-win-x64.Dockerfile .
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-jdk-22.0.1
```

Example build Linux docker image:
```shell
docker build -m 8GB --build-arg GRAALVM_VERSION="jdk-22.0.1" -t dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-jdk-22.0.1 -f graalvm-linux-x64.Dockerfile .
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-jdk-22.0.1
```

# Creating and Publishing NuGet Package

The `nuget.Dockerfile` is used to build the docker image to create and publish NuGet packages.
Documentation and examples of how to run the image can be found in the corresponding Dockerfile.
