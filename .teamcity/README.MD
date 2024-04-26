## Create and publish GraalVM docker image for building project

```shell
# docker login dxfeed-docker.jfrog.io
docker build --build-arg GRAALVM_VERSION="<version>" -t graalvm:<os>-<arch>-<version> -f .\.teamcity\graalvm-<os>-<arch>.Dockerfile .\.teamcity\
docker push dxfeed-docker.jfrog.io/dxfeed-api/graalvm:<os>-<arch>-<version>
```

# Build on Windows

On windows, this project is built in the docker. The `graalvm-win-x64.Dockerfile` is used to build
the docker image. Documentation and examples of how to run the image can be found in the
corresponding Dockerfile.


### Requirements for a TeamCity agent:

* docker must be installed on agent (to containerize windows, not linux)
* `dxcity` user should be able to run docker containers without admin privileges
* `dxcity` user user must have r/w access to https://dxfeed.jfrog.io/artifactory/docker/dxfeed-api/
  (`docker login dxfeed-docker.jfrog.io`)

### The following agents are currently in use:

* winAgent5168 host: winbuilder5168.in.devexperts.com

### How to install docker on Windows

* The agent must support Hyper-V (might have to enable virtualization in BIOS or something else).
* If the agent is running in a hypervisor, nested-virtualization must be enabled and the processor
  must have the "valid" name.
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
* Reboot.

# Build on Linux

On linux, this project is built in the docker. The `graalvm-linux-x64.Dockerfile` is used to build
the docker image. Documentation and examples of how to run the image can be found in the
corresponding Dockerfile.

### Requirements for a TeamCity agent:

* docker must be installed on agent
* `dxcity` user should be able to run containers without sudo
* `dxcity` user user must have r/w access to https://dxfeed.jfrog.io/artifactory/docker/dxfeed-api/
  (`docker login dxfeed-docker.jfrog.io`)

# Build on macOS

On macOS, this project is built in the host system.

### Requirements for a TeamCity agent:

* The agent must have an Apple Silicon processor.

### Preparing the OSX M1 agent

```shell
cd ~
# xcode command line tools
xcode-select --install
# maven
sudo -i -u dxcity
curl -LO https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.tar.gz
sudo xattr -rd com.apple.quarantine apache-maven-3.8.8-bin.tar.gz
tar -xzvf apache-maven-3.8.8-bin.tar.gz
#ARCH64
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-22.0.1/graalvm-community-jdk-22.0.1_macos-aarch64_bin.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-community-jdk-22.0.1_macos-aarch64_bin.tar.gz
tar -xzvf graalvm-community-jdk-22.0.1_macos-aarch64_bin.tar.gz
sudo mv graalvm-community-openjdk-22.0.1+8.1 /Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22.0.1+8.1
#AMD64
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-22.0.1/graalvm-community-jdk-22.0.1_macos-x64_bin.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-community-jdk-22.0.1_macos-x64_bin.tar.gz
tar -xzvf graalvm-community-jdk-22.0.1_macos-x64_bin.tar.gz
sudo mv graalvm-community-openjdk-22.0.1+8.1 /Library/Java/JavaVirtualMachines/graalvm-community-openjdk-22.0.1+8.1-amd64
```

### The following agents are currently in use:

* macbuilder22 macbuilder22.in.devexperts.com

# Creating and publishing NuGet package

The `nuget.Dockerfile` is used to build the docker image to create and publish NuGet packages.
Documentation and examples of how to run the image can be found in the corresponding Dockerfile.
