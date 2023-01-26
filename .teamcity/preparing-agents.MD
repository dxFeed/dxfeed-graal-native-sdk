# Preparing the Windows agent

winAgent4450 host: winbuilder4450.in.devexperts.com

 * https://medium.com/graalvm/using-graalvm-and-native-image-on-windows-10-9954dc071311
   * GraalVM https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-windows-amd64-22.3.1.zip and remove from PATH "C:\Program Files (x86)\Common Files\Oracle\Java\javapath"
   * GraalVM native-image
   * Visual Studio Build Tools
   * Windows 10 SDK

additional info https://www.graalvm.org/22.1/docs/getting-started/windows/

# Preparing the Linux agent

dxAgent1707-1 host: dxbuilder1707.in.devexperts.com

```shell
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-linux-amd64-22.3.1.tar.gz
tar -xzvf graalvm-ce-java11-linux-amd64-22.3.1.tar.gz
sudo mv graalvm-ce-java11-22.3.1 /opt/jdks/
/opt/jdks/graalvm-ce-java11-22.3.1/bin/gu install native-image
export JAVA_HOME=/opt/jdks/graalvm-ce-java11-22.3.1
sudo apt install nuget -y
sudo apt-get install mono-complete -y
sudo nuget update -self
wget -qO - https://releases.jfrog.io/artifactory/jfrog-gpg-public/jfrog_public_gpg.key | sudo apt-key add -
echo "deb https://releases.jfrog.io/artifactory/jfrog-debs xenial contrib" | sudo tee -a /etc/apt/sources.list;
apt update;
apt install -y jfrog-cli-v2-jf;
jf config add Artifactory --artifactory-url=https://dxfeed.jfrog.io/artifactory
JFrog Platform URL: https://dxfeed.jfrog.io
JFrog Artifactory URL: https://dxfeed.jfrog.io/artifactory
JFrog access token:
14:28:27 [🟠Warn] couldn't extract payload from Access Token
The provided access token is not a valid JWT, probably a reference token.
Some package managers only support basic authentication which requires also a username.
If you plan to work with one of those package managers, please provide a username.
JFrog username (optional):
Is the Artifactory reverse proxy configured to accept a client certificate? (y/n) [n]? n
```

# Preparing the OSX agent

macbuilder10 host: macbuilder10.in.devexperts.com

```shell
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-darwin-amd64-22.3.1.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-ce-java11-darwin-amd64-22.3.1.tar.gz
tar -xzvf graalvm-ce-java11-darwin-amd64-22.3.1.tar.gz
sudo mv graalvm-ce-java11-22.3.1 /Library/Java/JavaVirtualMachines/
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1/Contents/Home/bin/gu install native-image
export JAVA_HOME=$(/usr/libexec/java_home -v "11.0.15")
xcode-select --install
```

# Preparing the OSX M1

```shell
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-darwin-aarch64-22.3.1.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-ce-java11-darwin-aarch64-22.3.1.tar.gz
tar -xzvf graalvm-ce-java11-darwin-aarch64-22.3.1.tar.gz
sudo mv graalvm-ce-java11-22.3.1 /Library/Java/JavaVirtualMachines/
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.3.1/Contents/Home/bin/gu install native-image
export JAVA_HOME=$(/usr/libexec/java_home -v "11.0.15")
xcode-select --install
```