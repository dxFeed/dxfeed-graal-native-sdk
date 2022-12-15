# Preparing the Windows agent

winAgent4450 host: winbuilder4450.in.devexperts.com

 * maven https://maven.apache.org/install.html
   * https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.zip
   * "C:\Users\dxcity\.m2\settings.xml" add
```xml
<settings>
  <servers>
    <server>
      <id>jfrog</id>
      <username>${jfrog.user}</username>
      <password>${jfrog.password}</password>
    </server>
  </servers>
</settings>
```

 * https://medium.com/graalvm/using-graalvm-and-native-image-on-windows-10-9954dc071311
   * GraalVM https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-windows-amd64-22.1.0.zip and remove from PATH "C:\Program Files (x86)\Common Files\Oracle\Java\javapath"
   * GraalVM native-image
   * Visual Studio Build Tools
   * Windows 10 SDK

additional info https://www.graalvm.org/22.1/docs/getting-started/windows/

# Preparing the Linux agent

dxAgent1707-1 host: dxbuilder1707.in.devexperts.com

```shell
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-linux-amd64-22.1.0.tar.gz
tar -xzvf graalvm-ce-java11-linux-amd64-22.1.0.tar.gz
sudo mv graalvm-ce-java11-22.1.0 /opt/jdks/
/opt/jdks/graalvm-ce-java11-22.1.0/bin/gu install native-image
export JAVA_HOME=/opt/jdks/graalvm-ce-java11-22.1.0
```

# Preparing the OSX agent

macbuilder10 host: macbuilder10.in.devexperts.com

```shell
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-darwin-amd64-22.1.0.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-ce-java11-darwin-amd64-22.1.0.tar.gz
tar -xzvf graalvm-ce-java11-darwin-amd64-22.1.0.tar.gz
sudo mv graalvm-ce-java11-22.1.0 /Library/Java/JavaVirtualMachines/
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0/Contents/Home/bin/gu install native-image
export JAVA_HOME=$(/usr/libexec/java_home -v "11.0.15")
xcode-select --install
```

# Preparing the OSX M1

```shell
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-darwin-aarch64-22.1.0.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-ce-java11-darwin-aarch64-22.1.0.tar.gz
tar -xzvf graalvm-ce-java11-darwin-aarch64-22.1.0.tar.gz
sudo mv graalvm-ce-java11-22.1.0 /Library/Java/JavaVirtualMachines/
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0/Contents/Home/bin/gu install native-image
export JAVA_HOME=$(/usr/libexec/java_home -v "11.0.15")
xcode-select --install
```