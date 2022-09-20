# Installation GraalVM JDK 11 with native-image

[graalvm-ce-builds](https://github.com/graalvm/graalvm-ce-builds/releases)
Version JDK 11:  26 Apr 2022 vm-22.1.0 3f79371

## On MacOS M1

### Prepare

We recommend using the tool [jenv](https://github.com/jenv/jenv) to switch between JDKs.

```shell
brew install jenv
jenv enable-plugin maven
```

### Installing

https://www.graalvm.org/22.1/reference-manual/native-image/


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