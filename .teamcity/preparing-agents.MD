# Preparing the Windows agent

winAgent5168 host: winbuilder5168.in.devexperts.com

 * https://medium.com/graalvm/using-graalvm-and-native-image-on-windows-10-9954dc071311
   * GraalVM https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-22.0.1/graalvm-community-jdk-22.0.1_windows-x64_bin.zip and remove from PATH "C:\Program Files (x86)\Common Files\Oracle\Java\javapath"
   * Visual Studio Build Tools
   * Windows 10 SDK

additional info https://www.graalvm.org/latest/docs/getting-started/windows/

# Preparing the Linux agent

dxfeedAgent5919-1 dxfeedbuilder5919.in.devexperts.com

```shell
sudo -i -u dxcity
cd ~
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-22.0.1/graalvm-community-jdk-22.0.1_linux-x64_bin.tar.gz
tar -xzvf graalvm-community-jdk-22.0.1_linux-x64_bin.tar.gz
sudo mv graalvm-community-openjdk-22.0.1+8.1 /opt/jdks/
#export JAVA_HOME=/opt/jdks/graalvm-community-openjdk-22.0.1+8.1
sudo apt install nuget -y
#ok
sudo apt-get install mono-complete -y
#ok
sudo nuget update -self
wget -qO - https://releases.jfrog.io/artifactory/jfrog-gpg-public/jfrog_public_gpg.key | sudo apt-key add -
echo "deb https://releases.jfrog.io/artifactory/jfrog-debs xenial contrib" | sudo tee -a /etc/apt/sources.list;
sudo apt update;
sudo apt install -y jfrog-cli-v2-jf;
jf config add Artifactory --artifactory-url=https://dxfeed.jfrog.io/artifactory
#confirm
#JFrog Platform URL: https://dxfeed.jfrog.io
#confirm
#JFrog username: <username>
#JFrog password or API key: <api_key>
#Is the Artifactory reverse proxy configured to accept a client certificate? (y/n) [n]? n
```

# Preparing the OSX M1 agent

macbuilder22 macbuilder22.in.devexperts.com

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