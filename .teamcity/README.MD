# Table of Contents

- [Building on Windows](#building-on-windows)
  * [Requirements for a TeamCity Agent](#requirements-for-a-teamcity-agent)
  * [How to install docker on Windows](#how-to-install-docker-on-windows)
- [Building on Linux](#building-on-linux)
  * [Requirements for a TeamCity Agent](#requirements-for-a-teamcity-agent-1)
- [Testing](#testing)
- [Building on macOS](#building-on-macos)
  * [Requirements for a TeamCity Agent](#requirements-for-a-teamcity-agent-2)
- [Creating and Publishing GraalVM Docker Image for Project Build](#creating-and-publishing-graalvm-docker-image-for-project-build)
- [Creating and Publishing NuGet Package](#creating-and-publishing-nuget-package)
- [Deploying Artifacts](#deploying-artifacts)
- [Syncing the C++ API with Jira](#syncing-the-c-api-with-jira)

# Building on Windows

This project is built using Docker on Windows. The `docker/graalvm-win-x64-v2.Dockerfile` is utilized for
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

This project is built using Docker on Linux. The `docker/graalvm-linux-x64.Dockerfile` is utilized for
constructing the Docker image. Detailed documentation and usage examples for running the image are
available in the corresponding Dockerfile.

## Requirements for a TeamCity Agent:

- The `docker` must be installed on the agent.
- The `dxcity` user should be capable of running containers without sudo.
- Establish `TeamCity Connections` to the Docker Registry
  at https://dxfeed.jfrog.io/artifactory/docker/dxfeed-api/.

# Testing

The `Test [Linux, x64]` build builds the library in the Linux build image (`docker/graalvm-linux-x64.Dockerfile`) and runs
the C tests of `src/main/c` (CTest) in the test image (`docker/cpp-test-linux-x64.Dockerfile`, Oracle Linux 9). The library is
built for glibc 2.17, so the tests also check that it works on a newer distribution.

- The build runs for every pull request to `main` (the branches `pull-requests/<N>`) and for every change of `main`.
- The status of the build is published to the pull request in Bitbucket (with a link to the build). The results of the
  tests are in the build in TeamCity (the JUnit report of CTest).
- The build configuration appears in TeamCity when `.teamcity/settings.kts` with it gets to `main`.

One-time setup:

1. Run `Build & Push a Test Docker Image [Linux, x64]` to publish `nexus-docker-graalvm.in.devexperts.com/cpp-test:linux-x64-ol9`.
   Run it again when `docker/cpp-test-linux-x64.Dockerfile` changes.
2. To block merging a pull request without a successful build, enable the `Minimum successful builds` merge check in the
   settings of the repository in Bitbucket.

The same locally (after `mvn clean package` in the Linux build image):

```shell
docker build --pull -t cpp-test:linux-x64-ol9 -f .teamcity/docker/cpp-test-linux-x64.Dockerfile .teamcity
docker run --rm -v "$(pwd):/mnt" cpp-test:linux-x64-ol9 "cd src/main/c && cmake --preset=conf-release . && cmake --build --preset=build-release -j\$(nproc) && ctest --test-dir build/conf-release --output-on-failure"
```

# Building on macOS

The project is built directly on the macOS host system.

## Requirements for a TeamCity Agent:

* The agent must be equipped with an Apple Silicon processor.
* Rosetta 2 must be installed on the agent (`softwareupdate --install-rosetta`).
* Xcode Command Line Tools must be installed on the agent (`xcode-select --install`).
* **Build JRE Libraries [iOS]**, which builds the libraries of [jre-ios](../jre-ios/README.MD), also needs Xcode with
  the iOS and iOS Simulator SDKs, selected with `sudo xcode-select -s /Applications/Xcode.app`: the Command Line
  Tools have only the macOS SDK.

# Creating and Publishing GraalVM Docker Image for Project Build

The Dockerfiles are in `docker`, the build context is `.teamcity` (the images take `scripts/install.sh` or
`scripts/install.ps1` from it).

```shell
# Authenticate with the Docker Registry
docker login dxfeed-docker.jfrog.io
# Build and push the Docker image
docker build --build-arg GRAALVM_VERSION="<version>" -t graalvm:<os>-<arch>-<version> -f .\.teamcity\docker\graalvm-<os>-<arch>.Dockerfile .\.teamcity\
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:<os>-<arch>-<version>
```

Example build Windows docker image (from `.teamcity`):
```shell
docker build -m 8GB --build-arg GRAALVM_VERSION="graal-25.4.4.1.1" -t dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-graal-25.4.4.1.1 -f docker/graalvm-win-x64-v2.Dockerfile .
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:win-x64-graal-25.4.4.1.1
```

Example build Linux docker image (from `.teamcity`):
```shell
docker build -m 8GB --build-arg GRAALVM_VERSION="graal-25.4.4.1.1" -t dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-graal-25.4.4.1.1 -f docker/graalvm-linux-x64.Dockerfile .
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:linux-x64-graal-25.4.4.1.1
```

## GraalVM versions

The `GRAALVM_VERSION` value (the `env.GRAALVM_VERSION` TeamCity parameter, the Docker build argument,
and the argument of `scripts/install.sh graalvm` / `Install-GraalVM`) supports the following formats:

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

The `docker/nuget.Dockerfile` is used to build the docker image to create and publish NuGet packages.
Documentation and examples of how to run the image can be found in the corresponding Dockerfile.

# Deploying Artifacts

`mvn deploy` publishes the pom and the platform zips to the internal repositories
(`qd` for releases, `qd-snapshot` for snapshots, see `distributionManagement` in `pom.xml`).

Releases (not snapshots) are additionally deployed to the public repository
https://repo.devexperts.com/repository/mdd-maven/ by the `deploy-mdd-maven` execution of
`maven-deploy-plugin` in `pom.xml`. The deploy commands in `settings.kts` pass
`-Drepo.user=%dxcity.namecode.nexus% -Drepo.password=%dxcity.passcode.nexus%` (`Mvn.REPO` in `Common.kt`),
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
Doxygen is installed from the Oracle Linux 7 repository in `docker/graalvm-linux-x64.Dockerfile`
(the official Doxygen binaries require glibc 2.34+). The zip is deployed like the other artifacts,
and "Publish GitHub Release" attaches all the zips of the version from `qd` to the GitHub release.
To build the documentation locally: `mvn package -Dc-api-docs` (Doxygen must be in `PATH`).

# Syncing the C++ API with Jira

The C++ API (`dxfeed-graal-cxx-api`) is on GitHub, so the Jira triggers of Bitbucket do not move its MDAPI tickets. The
`C++ API: GitHub to Jira` build (Etc.) does it every 10 minutes with `jira-sync/cxx_api_jira_sync.py`, as the `dxcity`
bot (`%dxcity.token.jira%`):

- a pull request is opened (not a draft): In development -> Waiting for review;
- a draft pull request is opened (the work goes on): Confirmed -> In development, Waiting for build -> Waiting for
  review -> In development (there is no direct transition), Waiting for review -> In development if no other pull
  request of the ticket is in review;
- a pull request is merged into `main` or `release/*`: Waiting for review -> Waiting for build once no other pull
  request of the ticket is open, or -> In development if only drafts are open. While another pull request is in
  review, the ticket waits: the last of them merged, or closed without a merge, moves it. A ticket moved to Waiting
  for build gets the fix version of the next release of its line (see below), unless it has an unreleased one of that
  line or a later one.
- a pull request is closed without a merge (or merged into another feature branch), and no pull request of the
  ticket has been merged: Waiting for review -> In development, unless another pull request of the ticket is in
  review.

The lines of the versions: `release/vN` collects the breaking changes of the next major, `main` has the patches of the
current major until `release/vN` is merged into it and `vN.0.0` is released; the earlier majors are not maintained
after that. A pull request merged into `release/vN` gives its ticket the unreleased `graal-cxx-api vN...` version
(`vN.0.0` is created if there is none), one merged into `main` the unreleased version of the major of the last release
(the patch after it is created if there is none). A ticket with parts in both gets the later line.

The tickets are the MDAPI keys of the title and the branch of the pull request. Each pull request is applied to a
ticket once (the ticket keeps the applied pull requests in the issue property `cxx-api-github-sync`), so a ticket moved
back by hand stays where it is. The pull request is linked from the ticket.

A published GitHub release `vX.Y.Z` (not a draft, not a pre-release) releases the Jira version
`graal-cxx-api vX.Y.Z`:

- if there is no version of that name, the only unreleased `graal-cxx-api` version of the major of the tag (the
  placeholder of the next release of the line) is renamed to it, or else the version is created;
- the unreleased versions of the earlier majors (a new major is released) give their tickets to it and are archived;
- its tickets in Waiting for build are resolved (the transition `Resolve`, as the Release Steps of MDAPI do), as well
  as those in Waiting for test (left from the time with the testing step); the tickets in Testing stay as they are;
- its unfinished tickets (In development, Waiting for review and the other statuses not of the category "done") move
  to the next version `graal-cxx-api vX.Y.(Z+1)`, which is created if needed;
- the release mail is sent (see below), then the version is released with the date of the GitHub release (the
  Release Steps of MDAPI are not used, so there is no mail of Jira);
- the build warns about the tickets of the commits of the release without the version, and the other way round.

So a finished ticket gets the fix version of the placeholder of its line in Waiting for build (by the build, or by
hand for a ticket moved there by hand), and the tag of the release decides the number of the version. A ticket that is not
finished goes back to In development (by hand, or by a draft pull request): the release moves it to the next version.

The release mail goes from `dxcity@bots.devexperts.com` through `mxeu0.devexperts.com` (the relay of the mail of Jira,
which takes the mail of the agents without a login) to `env.MAIL_TO`, with a copy to `env.MAIL_CC` (comma-separated
addresses; no `env.MAIL_TO`: no mail). It has the notes of the GitHub release as GitHub renders them, with the MDAPI
keys linked to the tickets, and the links to the release, the documentation and the Jira version. A failed mail leaves
the version unreleased: the next run tries again. To send the mail of a release again, run the build with
`env.RESEND_MAIL` set to its tag (`v8.1.0`) in the Run dialog, where `env.MAIL_TO` and `env.MAIL_CC` can be changed
for that run too; the recipients of every release are the values in `settings.kts`.

`env.DRY_RUN` of the build is `0`: the build makes the changes. Set it to `1` to only read GitHub and Jira (including
the transitions available to `dxcity`) and log the changes that it would make. The tests (no network):
`python3 -m unittest` in `jira-sync`.
