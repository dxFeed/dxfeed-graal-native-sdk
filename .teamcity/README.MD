# Table of Contents

- [Editing the Settings](#editing-the-settings)
  * [IntelliJ IDEA](#intellij-idea)
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

# Editing the Settings

TeamCity takes the settings of the project from `.teamcity` of `main` (Kotlin DSL): the builds are in `settings.kts`,
their shared parts (the Maven options with the credentials, the Docker images, the steps, the agents, the VCS root)
are in `Common.kt`. A change takes effect when it gets to `main`. Make the changes there, not in the TeamCity UI: a
change made in the UI is kept by TeamCity as a patch (`.teamcity/patches`) that has to be moved to `settings.kts`.

- `mvn teamcity-configs:generate` in `.teamcity` (with JDK 21, see below) compiles the settings without a server and
  writes the XML of the builds to `target/generated-configs`. Comparing it with the XML generated from `main` shows
  what a change does to the builds (a refactoring must not change it).
- The server compiles the DSL with Kotlin 2.0, so `pom.xml` sets the language and API versions to 2.0: the newer
  syntax, such as the multi-dollar strings (`$$"..."`), does not compile on the server.
- The id of a build is `RootProjectId_<object name>` in any subproject (Release chain, Images & Libraries, Checks,
  Tests, Etc.), so moving a build to another subproject keeps it and its history. Renaming the object changes the
  id; `id("<old name>")` in the build keeps it.
- The functions declared in `settings.kts` capture the script, put the shared code in `Common.kt`.

## IntelliJ IDEA

`.teamcity` is a Maven project of its own (`.teamcity/pom.xml`), not a module of the root `pom.xml`. Until IntelliJ
IDEA imports it, `settings.kts` and `Common.kt` are plain files: everything in them is red.

1. Add the project: right-click `.teamcity/pom.xml` | Add as Maven Project (or the `+` of the Maven tool window). The
   project `Mdapi_DxfeedGraalNativeSdk_dsl` appears in the Maven tool window next to `graal-native-sdk`.
2. Set its module SDK to JDK 21 (File | Project Structure | Modules | `Mdapi_DxfeedGraalNativeSdk_dsl` | Dependencies).
   The TeamCity plugin of the DSL is built for Java 21: with JDK 17 `teamcity-configs:generate` fails with
   `UnsupportedClassVersionError`, with JDK 25 the Kotlin compiler fails to start (`processing of -javaagent failed`).
   Run it from the command line with `JAVA_HOME` of JDK 21 as well.
3. Reload the project (Reload All Maven Projects). The parent `configs-dsl-kotlin-parent` and the DSL of the plugins
   of our server are downloaded from the server itself (`https://dxcity.in.devexperts.com/app/dsl-plugins-repository`,
   no credentials, the internal network), the rest from the JetBrains repository. A mirror of `*` in
   `~/.m2/settings.xml` hides them: exclude them in it (`<mirrorOf>*,!teamcity-server,!jetbrains-all</mirrorOf>`).
   The DSL artifacts are snapshots: after an update of the server, reload with Update Snapshots (`mvn -U`).
4. If IntelliJ IDEA shows "Load script configuration" over `settings.kts`, load it.

The project compiles with the language and API versions of Kotlin 2.0 (see above), so IntelliJ IDEA does not suggest
the features of a newer Kotlin. `@file:Suppress("CanConvertToMultiDollarString")` in `settings.kts` turns off the
inspection that the language version does not turn off.

# Building on Windows

This project is built using Docker on Windows. The `docker/graalvm-win-x64-v2.Dockerfile` is utilized for
constructing the Docker image. Detailed documentation and usage examples for running the image are
available in the corresponding Dockerfile.

## Requirements for a TeamCity Agent:

- The `docker` must be installed on the agent to containerize Windows environments.
- The `dxcity` user should have permissions to run Docker containers without admin privileges.
- The images are in the Docker registry of Nexus `nexus-docker-graalvm.in.devexperts.com`: the project feature
  `Nexus` (`dockerRegistry` in `settings.kts`) logs the builds in to it.
- "Build & Deploy [Windows, x64][Release][Static MT]" and "[Debug][Static MT]" build the archives linked with the
  static C runtime (`-DwindowsStaticRuntime=true`, see `scripts/windows-static-runtime`) and check them before the
  deploy in `mcr.microsoft.com/windows/servercore:ltsc2019` (`Mvn.POWERSHELL_STATIC_RUNTIME` in `Common.kt`), which has
  no Visual C++ Redistributable: the base image of the Windows build image, already on the agents. The agents run
  Windows 11 (build 26300), so the container runs in Hyper-V isolation (process isolation only when the image has the
  build of the agent); the image is pulled if it is missing.

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
- The images are in the Docker registry of Nexus `nexus-docker-graalvm.in.devexperts.com`: the project feature
  `Nexus` (`dockerRegistry` in `settings.kts`) logs the builds in to it.

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

The builds "Build & Push a Docker Image [Linux, x64]", "[Linux, aarch64]" and "[Windows, x64]" build the image of
`env.GRAALVM_VERSION` and push it to `nexus-docker-graalvm.in.devexperts.com/graalvm:<os>-<arch>-<version>`, the
tag that the other builds use. The same by hand:

```shell
# Authenticate with the Docker Registry
docker login nexus-docker-graalvm.in.devexperts.com
# Build and push the Docker image
docker build --build-arg GRAALVM_VERSION="<version>" -t nexus-docker-graalvm.in.devexperts.com/graalvm:<os>-<arch>-<version> -f .\.teamcity\docker\graalvm-<os>-<arch>.Dockerfile .\.teamcity\
docker push nexus-docker-graalvm.in.devexperts.com/graalvm:<os>-<arch>-<version>
```

Example build Windows docker image (from `.teamcity`):
```shell
docker build -m 8GB --build-arg GRAALVM_VERSION="graal-25.4.4.1.1" -t nexus-docker-graalvm.in.devexperts.com/graalvm:win-x64-graal-25.4.4.1.1 -f docker/graalvm-win-x64-v2.Dockerfile .
docker push nexus-docker-graalvm.in.devexperts.com/graalvm:win-x64-graal-25.4.4.1.1
```

Example build Linux docker image (from `.teamcity`):
```shell
docker build -m 8GB --build-arg GRAALVM_VERSION="graal-25.4.4.1.1" -t nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-graal-25.4.4.1.1 -f docker/graalvm-linux-x64.Dockerfile .
docker push nexus-docker-graalvm.in.devexperts.com/graalvm:linux-x64-graal-25.4.4.1.1
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

- a branch with the MDAPI key is created (`feature/MDAPI-NNN-...`, `bugfix/MDAPI-NNN-...`, any name with the key) or a
  draft pull request is opened (the work starts or goes on): the development starts (Reported -> Confirmed, for a
  forgotten confirmation, -> In development; Confirmed -> In development), Waiting for build -> Waiting for review ->
  In development (there is no direct transition), Waiting for review -> In development if no other pull request of
  the ticket is in review;
- a pull request is opened (not a draft): the development starts if needed, then In development -> Waiting for
  review;
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

The checks (GitHub Actions) of the latest commits of the open pull requests are shown in their tickets: the status
icon at the link of a pull request follows them (passed, failed, running), and one comment per pull request shows the
result with the failed jobs, edited (not added again) when it changes. Running checks change the icon only, so Jira
mails the watchers for the results alone. A pull request merged (closed) right after its checks finished gets the
final checks once more.

The tickets are the MDAPI keys of the title and the branch of a pull request, of the name of a branch; the created
branches come from the events of the repository (GitHub may show them with a delay). Each pull request and branch is
applied to a ticket once (the ticket keeps them in the issue property `cxx-api-github-sync`), so a ticket moved back
by hand stays where it is. The pull requests and the branches are linked from the ticket, grouped by the branch
(the heading of a group is the distinct part of the name of the branch, as Jira cuts the heading at about 20
characters: `feature/MDAPI-427-warnings` -> `warnings`, `bugfix/MDAPI-411` -> `MDAPI-411`; the branch and its pull
requests are under it, the groups in the alphabetical order of Jira). The link of a pull request has its state in
the title (`(draft)`, `(merged)`, `(closed)`) and is struck through when it is merged or closed; the link of a branch
is struck through (`(deleted)`) when the branch is deleted, by the same events of the repository. To regroup the links
made before (or lost), run the build with `env.RELINK` set to the tickets (`MDAPI-427`) in the Run dialog.

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
hand for a ticket moved there by hand), and the tag of the release decides the number of the version. A ticket that
is not finished goes back to In development (by hand, by a draft pull request or a new branch): the release moves it
to the next version.

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
