# We use

[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)

[Semantic Versioning](https://semver.org/)

# Installation GraalVM JDK 11 with native-image

[graalvm-ce-builds](https://github.com/graalvm/graalvm-ce-builds/releases)
Version JDK 11:  26 Apr 2022 vm-22.1.0 3f79371

[Install GraalVM](https://www.graalvm.org/22.1/docs/getting-started/#install-graalvm)

[Install Native Image](https://www.graalvm.org/22.1/docs/getting-started/#native-image)

# Settings Inetllij Idea

 * File | Project structure | SDKs - add JDK graalvm-11
 * File | Project structure | Project / SDK - choose graalvm-11
 * Preferences | Build, Execution, Deployment | Build Tools | Maven | Importing | JDK for importer - choose graalvm-11
 * Preferences | Build, Execution, Deployment | Build Tools | Maven | Runner | JRE - choose graalvm-11

# To regenerate META-INF/native-image

add a new case to src/test/java/com/dxfeed/NativeLibMain.java and execute it

```shell
export M2_HOME=~/.m2
$JAVA_HOME/bin/java -Dtoken=<TOKEN> -agentlib:native-image-agent=config-output-dir=META-INF/native-image -Dfile.encoding=UTF-8 -classpath ./target/test-classes:./target/classes:$M2_HOME/repository/com/devexperts/qd/qds/3.312/qds-3.312.jar:$M2_HOME/repository/com/devexperts/qd/dxlib/3.312/dxlib-3.312.jar:$M2_HOME/repository/com/devexperts/qd/dxfeed-api/3.312/dxfeed-api-3.312.jar:$M2_HOME/repository/com/devexperts/mdd/auther-api/441/auther-api-441.jar:$M2_HOME/repository/com/devexperts/qd/qds-tools/3.313/qds-tools-3.313.jar:$M2_HOME/repository/com/devexperts/qd/qds-file/3.313/qds-file-3.313.jar:$M2_HOME/repository/org/graalvm/sdk/graal-sdk/22.1.0/graal-sdk-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/svm/22.1.0/svm-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/objectfile/22.1.0/objectfile-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/pointsto/22.1.0/pointsto-22.1.0.jar:$M2_HOME/repository/org/graalvm/compiler/compiler/22.1.0/compiler-22.1.0.jar:$M2_HOME/repository/org/graalvm/truffle/truffle-api/22.1.0/truffle-api-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/native-image-base/22.1.0/native-image-base-22.1.0.jar com.dxfeed.NativeLibMain


```

# To release a new version

We use [teamcity](https://dxcity.in.devexperts.com/project/Eugenics_DxfeedGraalNativeApi).

1. Run the "build PATCH and deploy linux" configuration to release a version of PATCH or "build MAJOR.MINOR.PATCH and deploy linux" (set env.RELEASE_VERSION in run parameters) to release a version of MAJOR or MINOR. A linux version of the artifact will also be deployed.
1. The "deploy osx", "deploy windows", "deploy nuget" builds will start automatically, after which you can see the new artifacts in [artifactory](https://dxfeed.jfrog.io/ui/repos/tree/General/maven-open/com/dxfeed/graal-native-api). You can also see them in Jazz at the path: /Projects/mdd/DXFG/Release/. The "deploy osx" configuration builds and deploys artifacts under amd, arm architecture osx and arm under IOS.

# IOS

https://www.graalvm.org/22.2/reference-manual/native-image/LLVMBackend/

```shell
/Library/Java/JavaVirtualMachines/graalvm-ce-java11-22.1.0/Contents/Home/bin/gu install llvm-toolchain
```

deploy

```shell
mvn -DmacIos=true deploy
```

# License
MPL-2.0