# We use

https://www.conventionalcommits.org/en/v1.0.0/
https://semver.org/

# Installation GraalVM JDK 11 with native-image

[graalvm-ce-builds](https://github.com/graalvm/graalvm-ce-builds/releases)
Version JDK 11:  26 Apr 2022 vm-22.1.0 3f79371

## On MacOS M1

### Prepare

We recommend using the tool [jenv](https://github.com/jenv/jenv) or [sdkman](https://sdkman.io/) to switch between JDKs.

```shell
brew install jenv
jenv enable-plugin maven
```

### Installing

https://www.graalvm.org/22.1/reference-manual/native-image/

# Preparing the Windows agent

winAgent4450 host: winbuilder4450.in.devexperts.com

 * maven https://maven.apache.org/install.html
   * https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.zip
 * https://medium.com/graalvm/using-graalvm-and-native-image-on-windows-10-9954dc071311
   * GraalVM https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-windows-amd64-22.1.0.zip and remove from PATH "C:\Program Files (x86)\Common Files\Oracle\Java\javapath"
   * GraalVM native-image
   * Visual Studio Build Tools
   * Windows 10 SDK

additional info https://www.graalvm.org/22.1/docs/getting-started/windows/

linux
dxAgent1707-1 host: dxbuilder1707.in.devexperts.com

```shell
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-linux-amd64-22.1.0.tar.gz
tar -xzvf graalvm-ce-java11-linux-amd64-22.1.0.tar.gz
sudo mv graalvm-ce-java11-22.1.0 /opt/jdks/
java -version
#openjdk version "11.0.15" 2022-04-19
#OpenJDK Runtime Environment GraalVM CE 22.1.0 (build 11.0.15+10-jvmci-22.1-b06)
#OpenJDK 64-Bit Server VM GraalVM CE 22.1.0 (build 11.0.15+10-jvmci-22.1-b06, mixed mode)
/opt/jdks/graalvm-ce-java11-22.1.0/Contents/Home/bin/gu install native-image
```

macos
macbuilder10 host: macbuilder10.in.devexperts.com

```shell
cd ~/Downloads
curl -LO https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.1.0/graalvm-ce-java11-darwin-aarch64-22.1.0.tar.gz
sudo xattr -rd com.apple.quarantine graalvm-ce-java11-darwin-aarch64-22.1.0.tar.gz
tar -xzvf graalvm-ce-java11-darwin-aarch64-22.1.0.tar.gz
sudo mv graalvm-ce-java11-22.1.0 /Library/Java/JavaVirtualMachines/
jenv add /Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0/Contents/Home
jenv shell graalvm64-11.0.15
java -version
#openjdk version "11.0.15" 2022-04-19
#OpenJDK Runtime Environment GraalVM CE 22.1.0 (build 11.0.15+10-jvmci-22.1-b06)
#OpenJDK 64-Bit Server VM GraalVM CE 22.1.0 (build 11.0.15+10-jvmci-22.1-b06, mixed mode)
xcode-select --install
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0/Contents/Home/bin/gu install native-image
```

# Settings Inetllij Idea

 * File | Project structure | SDKs - add JDK graalvm-11
 * File | Project structure | Project / SDK - choose graalvm-11
 * Preferences | Build, Execution, Deployment | Build Tools | Maven | Importing | JDK for importer - choose graalvm-11
 * Preferences | Build, Execution, Deployment | Build Tools | Maven | Runner | JRE - choose graalvm-11

```shell
jenv shell graalvm64-11.0.15
mvn -version
#Apache Maven 3.8.6 (84538c9988a25aec085021c365c560670ad80f63)
#Maven home: /opt/homebrew/Cellar/maven/3.8.6/libexec
#Java version: 11.0.15, vendor: GraalVM Community, runtime: /Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0/Contents/Home
#Default locale: en_DE, platform encoding: UTF-8
#OS name: "mac os x", version: "12.5.1", arch: "aarch64", family: "mac"
mvn clean package -DskipTests
```

# To generate META-INF/native-image

```shell
export M2_HOME=~/.m2
$JAVA_HOME/bin/java -Dtoken=<TOKEN> -agentlib:native-image-agent=config-output-dir=META-INF/native-image -Dfile.encoding=UTF-8 -classpath ./target/test-classes:./target/classes:$M2_HOME/repository/com/devexperts/qd/qds/3.312/qds-3.312.jar:$M2_HOME/repository/com/devexperts/qd/dxlib/3.312/dxlib-3.312.jar:$M2_HOME/repository/com/devexperts/qd/dxfeed-api/3.312/dxfeed-api-3.312.jar:$M2_HOME/repository/com/devexperts/mdd/auther-api/441/auther-api-441.jar:$M2_HOME/repository/com/devexperts/qd/qds-tools/3.313/qds-tools-3.313.jar:$M2_HOME/repository/com/devexperts/qd/qds-file/3.313/qds-file-3.313.jar:$M2_HOME/repository/org/graalvm/sdk/graal-sdk/22.1.0/graal-sdk-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/svm/22.1.0/svm-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/objectfile/22.1.0/objectfile-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/pointsto/22.1.0/pointsto-22.1.0.jar:$M2_HOME/repository/org/graalvm/compiler/compiler/22.1.0/compiler-22.1.0.jar:$M2_HOME/repository/org/graalvm/truffle/truffle-api/22.1.0/truffle-api-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/native-image-base/22.1.0/native-image-base-22.1.0.jar com.dxfeed.NativeLibMain


```

# License
MPL-2.0