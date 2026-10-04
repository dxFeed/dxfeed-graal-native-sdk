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
   static JDK libraries), and the files of `svm-sources.txt` with the flags of their mx projects. The objects that
   need a system library name it for the linker (`LIBRARY_DEPENDENCIES` of `build.py`): the objects of `libzip` add
   `-lz`, those of `libnet` add `-framework CFNetwork`, so an application links them without knowing about them, and
   the libraries do not embed their own zlib, which would conflict with the one of the application. `build.py`
   includes a generated header (`-include`) with `__asm__(".linker_option ...")` into these objects, which writes
   `LC_LINKER_OPTION`.
6. Makes the libraries with `libtool` and `lipo` and writes `build-info.txt`.

`build.py check` links a dynamic library from `libDxFeedGraalNativeSdk.o` and the `.a` files of an archive for every
architecture of the image with only what any application links (`-framework Foundation`), so every symbol that
neither the libraries nor the system libraries that they name define is reported by the linker, as in the
application. It also checks the platform that each architecture of the image declares (see below). The SDK build for
iOS and for the simulator runs it before making the archive, so an archive that does not link is not published
(`-Dios.check.skip=true` skips it: **Build JRE Libraries [iOS]** checks both archives in its own step).

## The platform of the image

native-image writes the object of the iOS image itself and declares no platform in it (`LC_BUILD_VERSION`), and the
arm64 object of the simulator archive is the object of the device (GraalVM has no simulator platform: the build for
the simulator adds the x86_64 object to it with `lipo`), so the linker could not check that the object is built for
the platform of the application. The SDK build declares it with `build.py set-build-version`, as clang does for the
objects of the libraries:

| Archive                                | Architecture | Platform       | Minimum version |
|----------------------------------------|--------------|----------------|-----------------|
| `graal-native-sdk-*-aarch64-ios.zip`   | arm64        | `ios`          | 12.0            |
| `graal-native-sdk-*-ios-simulator.zip` | arm64        | `iossimulator` | 14.0            |
| `graal-native-sdk-*-ios-simulator.zip` | x86_64       | `iossimulator` | 12.0            |

The SDK version is the one of Xcode (`xcrun --show-sdk-version`). The command is written into the free space after the
load commands (the data of the sections starts at a page boundary), so no offset changes and the size of the object
stays the same; `set-build-version` fails if there is no free space. The linked application does not change either:
the binary of the Swift API built with the declared object is the same size, section by section.

## The manifest of the linker flags

`build.py manifest` writes `link-flags.txt` into the archives for iOS, the simulator and macOS: a comment with the
platform and the minimum version of each architecture (`LC_BUILD_VERSION`) and the linker flags of an application on
the last line, relative to the directory of the archive:

```
libDxFeedGraalNativeSdk.o graal-svm-arm64-ios-r.a jdk-arm64-ios-r.a -framework Foundation -framework CFNetwork -lz
```

It is made from the files themselves: the system libraries are the ones that the objects name in `LC_LINKER_OPTION`
(see `LIBRARY_DEPENDENCIES`), plus `-framework Foundation` (`CHECK_LINK_FLAGS`), so it cannot get out of date.
`build.py check` links the image with these flags only, without autolinking (`-Wl,-ignore_auto_link`), as well as
with autolinking, so an archive whose manifest is not enough is not published. The macOS manifest names the library,
its install name (`@rpath/libDxFeedGraalNativeSdk.dylib`) and the system libraries that it loads.

## The XCFramework

`build.py xcframework` makes `DxFeedGraalNativeSdk.xcframework` from the files of the archives for iOS, the simulator
and macOS (arm64 and x86_64), and zips it with `ditto` (which keeps the links of the macOS framework):

| Library identifier           | Framework                                                                            |
|------------------------------|--------------------------------------------------------------------------------------|
| `ios-arm64`                  | static: `libtool -static` of the image object and the libraries                     |
| `ios-arm64_x86_64-simulator` | static: the same for the simulator                                                   |
| `macos-arm64_x86_64`         | dynamic: the libraries of the SDK for arm64 and x86_64 (`lipo`), re-signed ad hoc after `install_name_tool` |

Each framework has the headers of the SDK and the Clang module `DxFeedGraalNativeSdk` (umbrella header `dxfg_api.h`,
`link framework "Foundation"`); the objects of the static frameworks keep their `LC_BUILD_VERSION` and
`LC_LINKER_OPTION`, so the frameworks link their system libraries themselves. The image object must declare its
platform (see above).

The TeamCity build configuration **Build & Deploy [XCFramework]** runs it after **Build & Deploy [macOS, iOS]**: it
downloads the four archives of the release from Nexus, makes the XCFramework and deploys
`graal-native-sdk-<version>-xcframework.zip` next to them (classifier `xcframework`), from where **Publish GitHub
Release** takes it with the other archives. **Build & Deploy [All]** waits for it.

```shell
arch -arm64 python3 jre-ios/build.py xcframework --ios out/aarch64-ios --simulator out/ios-simulator \
    --macos out/aarch64-osx out/x86_64-osx --version 3.7.1 --work out/xcframework \
    --zip out/graal-native-sdk-3.7.1-xcframework.zip
```

## When a new GraalVM version does not build

* A file of the JDK does not compile for iOS: exclude the code that iOS does not have with `TARGET_OS_IPHONE` in
  `jdk-ios.patch` (`#include <TargetConditionals.h>`). To update the patch, make the changes in
  `~/.graal/jre-ios/<version>/labs-openjdk` and run `git diff > jre-ios/jdk-ios.patch` there.
* A file of `jdk-sources.txt` no longer exists: remove it from the list.
* The check reports undefined symbols: add the file that defines them to `jdk-sources.txt` (`Java_*`, `JNI_OnLoad_*`;
  `grep -rl <symbol> src` in labs-openjdk) or to `svm-sources.txt` (`svm_*` and other functions of
  `substratevm/src/com.oracle.svm.native.*`).
