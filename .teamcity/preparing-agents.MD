# Preparing the Windows agent

winAgent4450 host: winbuilder4450.in.devexperts.com

 * https://medium.com/graalvm/using-graalvm-and-native-image-on-windows-10-9954dc071311
   * GraalVM https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-windows-amd64-22.3.1.zip and remove from PATH "C:\Program Files (x86)\Common Files\Oracle\Java\javapath"
   * GraalVM native-image
   * Visual Studio Build Tools
   * Windows 10 SDK

additional info https://www.graalvm.org/22.1/docs/getting-started/windows/

# Preparing the Linux agent

dxfeedAgent5919-1 dxfeedbuilder5919.in.devexperts.com

```shell
cd ~
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-linux-amd64-22.3.1.tar.gz
tar -xzvf graalvm-ce-java11-linux-amd64-22.3.1.tar.gz
sudo mv graalvm-ce-java11-22.3.1 /opt/jdks/
/opt/jdks/graalvm-ce-java11-22.3.1/bin/gu install native-image
#export JAVA_HOME=/opt/jdks/graalvm-ce-java11-22.3.1
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
#JFrog Platform URL: https://dxfeed.jfrog.io
#JFrog Artifactory URL: https://dxfeed.jfrog.io/artifactory
#JFrog access token:
#14:28:27 [🟠Warn] couldn't extract payload from Access Token
#The provided access token is not a valid JWT, probably a reference token.
#Some package managers only support basic authentication which requires also a username.
#If you plan to work with one of those package managers, please provide a username.
#JFrog username (optional):
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
# disabling SIP to copy the artifact to the share using the script https://teamcity-support.jetbrains.com/hc/en-us/community/posts/360010834480-Mac-build-agent-unable-to-access-external-drive-Operation-not-permitted-error
#ARCH64
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-darwin-aarch64-22.3.1.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-ce-java11-darwin-aarch64-22.3.1.tar.gz
tar -xzvf graalvm-ce-java11-darwin-aarch64-22.3.1.tar.gz
sudo mv graalvm-ce-java11-22.3.1 /Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1-arm64
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1-arm64/Contents/Home/bin/gu install native-image
#IOS https://www.graalvm.org/22.2/reference-manual/native-image/LLVMBackend/
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1-arm64/Contents/Home/bin/gu install llvm-toolchain
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-darwin-amd64-22.3.1.tar.gz
#AMD64
sudo xattr -rd com.apple.quarantine graalvm-ce-java11-darwin-amd64-22.3.1.tar.gz
tar -xzvf graalvm-ce-java11-darwin-amd64-22.3.1.tar.gz
sudo mv graalvm-ce-java11-22.3.1 /Library/Java/JavaVirtualMachines/
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1/Contents/Home/bin/gu install native-image
```