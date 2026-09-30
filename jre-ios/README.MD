# JDK and Substrate VM libraries for iOS

The native image of the SDK for iOS is a relocatable object file (`libDxFeedGraalNativeSdk.o`) that contains the Java
code of the SDK, of the JDK and of Substrate VM. The native code of the JDK and of Substrate VM that it calls is in the
static libraries of this directory, which the pom puts into the iOS archives next to the object file:

| Library                       | Slices                                    | Archive                                |
|-------------------------------|-------------------------------------------|----------------------------------------|
| `jdk-arm64-ios-r.a`           | iOS arm64                                 | `graal-native-sdk-*-aarch64-ios.zip`   |
| `graal-svm-arm64-ios-r.a`     | iOS arm64                                 | `graal-native-sdk-*-aarch64-ios.zip`   |
| `jdk-ios-simulator-r.a`       | iOS Simulator arm64, iOS Simulator x86_64 | `graal-native-sdk-*-ios-simulator.zip` |
| `graal-svm-ios-simulator-r.a` | iOS Simulator arm64, iOS Simulator x86_64 | `graal-native-sdk-*-ios-simulator.zip` |

The libraries must be built from the same sources as the GraalVM that builds the image: every GraalVM version changes
the native functions of the JDK and of Substrate VM (for example, the image of GraalVM 25 needs
`Java_java_io_FileInputStream_isRegularFile0` and `svm_get_locale`, which the libraries of GraalVM 22 do not have, and
the application does not link). So the libraries are rebuilt whenever `env.GRAALVM_VERSION` or
`env.GRAALVM_VERSION_MACOS_X64` of the TeamCity project changes. `build-info.txt`, built with the libraries, says which
sources each slice is built from.

The slices come from two GraalVM versions, as the images do: the arm64 slices from `env.GRAALVM_VERSION`
(graal-25.4.4.1.1), the x86_64 slice of the simulator from `env.GRAALVM_VERSION_MACOS_X64` (jdk-25.0.1), since GraalVM
for macOS x64 is no longer published after jdk-25.0.1. The native functions of the two differ too (JDK 25.0.1 has
`Java_java_io_Console_istty`, JDK 25.0.4 has `Java_java_io_Console_ttyStatus` instead).

## Updating the libraries

1. Run the TeamCity build configuration **Build JRE Libraries [iOS]** on the branch with the new GraalVM version. It
   builds the libraries from the sources of `env.GRAALVM_VERSION` and `env.GRAALVM_VERSION_MACOS_X64` (the parameters
   `jre.ios.graal.tag` and `jre.ios.x64.graal.tag`), builds the SDK for iOS and the simulator with them and checks that
   the SDK links with them.
2. Download the artifact `jre-ios-libraries-<version>.zip` and replace the `*.a` files and `build-info.txt` of this
   directory with its content.
3. Commit them. The SDK archives of the same build (`graal-native-sdk-aarch64-ios.zip`,
   `graal-native-sdk-ios-simulator.zip`) can be used to check the libraries in an application (the Swift API).

The agent needs macOS on Apple Silicon with Xcode (the Command Line Tools have no iOS SDK). The first build on an agent
takes longer: it downloads the sources and the labs JDKs and builds the Java code of the JDKs, which are then kept
in `~/.graal/jre-ios/<version>`.

## Building the libraries on a Mac

```shell
arch -arm64 python3 jre-ios/build.py build --graal-tag graal-25.4.4.1.1 --x64-graal-tag jdk-25.0.1 --work ~/.graal/jre-ios --out out/jre-ios/libraries
cp out/jre-ios/libraries/* jre-ios/
```

`--graal-tag` is a tag of [oracle/graal](https://github.com/oracle/graal): `graal-<version>` for the GraalVM Community
innovation releases (`graal-25.4.4.1.1`), `jdk-<version>` for the others (`jdk-25.0.1`); only GraalVM Community is
supported. `--x64-graal-tag` is the version for the x86_64 slice of the simulator (`--graal-tag` by default).
`--ios-min-version` sets the minimum iOS version (12.0 by default).

To check that an SDK archive links with its libraries:

```shell
unzip target/graal-native-sdk-aarch64-ios.zip -d out/check-ios
arch -arm64 python3 jre-ios/build.py check --dir out/check-ios --platform ios
unzip target/graal-native-sdk-ios-simulator.zip -d out/check-simulator
arch -arm64 python3 jre-ios/build.py check --dir out/check-simulator --platform simulator
```

## How the libraries are built

`build.py build` prepares the sources of each GraalVM version in `<work>/<tag>` (steps 1-4) and builds each slice
from the sources of its version (steps 5-6):

1. Clones [oracle/graal](https://github.com/oracle/graal) at the tag and [mx](https://github.com/graalvm/mx) of the
   version that its suites need.
2. Downloads with `mx fetch-jdk` the labs JDK that this graal is built on (`labsjdk-ce-latest` of its
   `common.json`) and generates `JvmFuncsFallbacks.c` with `mx build --dependencies svm-jvmfuncs-fallback-builder`:
   stubs of the `JVM_*` functions that the JDK libraries call and Substrate VM does not implement.
3. Clones [labs-openjdk](https://github.com/graalvm/labs-openjdk) at the tag of this labs JDK and applies
   `jdk-ios.patch`, which excludes the parts of the JDK code that iOS does not have (proxy settings,
   `libproc.h`, ...).
4. Configures the JDK for macOS and runs `make compile-commands`: it generates the headers of the native code and
   `compile_commands.json` with the compiler flags of every file of the JDK (autoconf, which the configure needs, is
   built into the work directory when it is missing; `metal` and `metallib`, which Xcode 26 has only with the Metal
   Toolchain component, are replaced by a stub: they only compile the shaders of `java.desktop`).
5. Compiles the files of `jdk-sources.txt` with these flags for iOS arm64, iOS Simulator arm64 and iOS Simulator
   x86_64 (the macOS target, SDK and version flags are replaced, `STATIC_BUILD` and `LIBRARY_NAME` are added as for the
   static JDK libraries), and the files of `svm-sources.txt` with the flags of their mx projects.
6. Makes the libraries with `libtool` and `lipo` and writes `build-info.txt`.

`build.py check` links a dynamic library from `libDxFeedGraalNativeSdk.o` and the `.a` files of an archive for every
architecture of the image, so every symbol that the libraries do not define is reported by the linker.

## When a new GraalVM version does not build

* A file of the JDK does not compile for iOS: exclude the code that iOS does not have with `TARGET_OS_IPHONE` in
  `jdk-ios.patch` (`#include <TargetConditionals.h>`). To update the patch, make the changes in
  `~/.graal/jre-ios/<version>/labs-openjdk` and run `git diff > jre-ios/jdk-ios.patch` there.
* A file of `jdk-sources.txt` no longer exists: remove it from the list.
* The check reports undefined symbols: add the file that defines them to `jdk-sources.txt` (`Java_*`, `JNI_OnLoad_*`;
  `grep -rl <symbol> src` in labs-openjdk) or to `svm-sources.txt` (`svm_*` and other functions of
  `substratevm/src/com.oracle.svm.native.*`).
