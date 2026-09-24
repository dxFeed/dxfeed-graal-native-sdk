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
- [Deploying Artifacts](#deploying-artifacts)

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
docker build -m 8GB --build-arg GRAALVM_VERSION="graal-25.4.4.1.1" -t dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-graal-25.4.4.1.1 -f graalvm-win-x64-v2.Dockerfile .
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-graal-25.4.4.1.1
```

Example build Linux docker image:
```shell
docker build -m 8GB --build-arg GRAALVM_VERSION="graal-25.4.4.1.1" -t dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-graal-25.4.4.1.1 -f graalvm-linux-x64.Dockerfile .
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-graal-25.4.4.1.1
```

## GraalVM versions

The `GRAALVM_VERSION` value (the `env.GRAALVM_VERSION` TeamCity parameter, the Docker build argument,
and the argument of `install.sh graalvm` / `Install-GraalVM`) supports the following formats:

| Format                         | Example                   | Distribution                                               |
|--------------------------------|---------------------------|------------------------------------------------------------|
| `java<java_version>-<version>` | `java11-22.3.1`           | GraalVM CE, legacy numbering (before 2023)                 |
| `jdk-<jdk_version>`            | `jdk-25.0.2`              | GraalVM Community, JDK-aligned releases                    |
| `graal-<graal_version>`        | `graal-25.4.4.1.1`        | GraalVM Community, innovation releases (GraalVM 25.1+)     |
| `oracle-jdk-<jdk_version>`     | `oracle-jdk-25.0.4`       | Oracle GraalVM (GFTC license), JDK-aligned releases        |
| `oracle-graal-<graal_version>` | `oracle-graal-25.4.4.1.1` | Oracle GraalVM (GFTC license), innovation releases         |

An innovation release `graal-25.4.4.1.1` is GraalVM 25 Innovation 4 based on JDK 25.0.4.1.1.

GraalVM for macOS x64 is no longer published after `jdk-25.0.1`, so the macOS x64 and iOS simulator
builds use a separate `env.GRAALVM_VERSION_MACOS_X64` TeamCity parameter.

The project detects the GraalVM layout during the build: if the GraalVM contains `lib/svm/builder/svm-shared.jar`
(as GraalVM 25.4 does), the `svm-shared` Maven profile and `src/main/java-svm-shared` sources are used,
otherwise (e.g. `jdk-25.0.x` and older) the `svm-core` Maven profile and `src/main/java-svm-core` sources are used.

# Creating and Publishing NuGet Package

The `nuget.Dockerfile` is used to build the docker image to create and publish NuGet packages.
Documentation and examples of how to run the image can be found in the corresponding Dockerfile.

# Deploying Artifacts

`mvn deploy` publishes the pom and the platform zips to the internal repositories
(`qd` for releases, `qd-snapshot` for snapshots, see `distributionManagement` in `pom.xml`).

Releases (not snapshots) are additionally deployed to the public repository
https://repo.devexperts.com/repository/mdd-maven/ by the `deploy-mdd-maven` execution of
`maven-deploy-plugin` in `pom.xml`. The deploy commands in `settings.kts` pass
`-Drepo.user=%dxcity.namecode.nexus% -Drepo.password=%dxcity.passcode.nexus%`,
`.teamcity/settings.xml` uses them for the `mdd-maven` server.
`release:perform` passes the resolved settings to its forked build, so this works there as well.

**Currently disabled** (`<mdd-maven.deploy.skip>true</mdd-maven.deploy.skip>` in `pom.xml`):
repo.devexperts.com rejects the passed `%dxcity.namecode.nexus%` account (`dcsi-wrp`, the internal Nexus
account) with 401, the credentials of the `dxcity` bot are needed there. To enable deploying again,
set the property to `snapshots` and pass the `dxcity` credentials as `repo.user`/`repo.password`.

Every platform build deploys the same pom, so the repository must allow redeploying (as `qd` does).

The C API documentation (`graal-native-sdk-<version>-c-api-docs-html.zip`) is generated by Doxygen
(`docs/Doxyfile`, the `c-api-docs` Maven profile) only in the Linux x64 release builds:
their `release:perform` passes `-Darguments=-Dc-api-docs` to the forked release build.
Doxygen is installed from the Oracle Linux 7 repository in `graalvm-linux-x64.Dockerfile`
(the official Doxygen binaries require glibc 2.34+). The zip is deployed like the other artifacts,
and "Publish GitHub Release" attaches all the zips of the version from `qd` to the GitHub release.
To build the documentation locally: `mvn package -Dc-api-docs` (Doxygen must be in `PATH`).
