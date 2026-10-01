# DxFeed.Graal.Native

The native libraries of the [dxFeed Graal Native SDK](https://github.com/dxFeed/dxfeed-graal-native-sdk): the dxFeed
Java API compiled with GraalVM Native Image into a shared library with a C API, for Windows, Linux and macOS.

Applications usually do not reference this package: it is a dependency of
[DxFeed.Graal.Net](https://www.nuget.org/packages/DxFeed.Graal.Net/), the dxFeed .NET API
([dxfeed-graal-net-api](https://github.com/dxFeed/dxfeed-graal-net-api)). Reference it directly only to call the C
API with P/Invoke.

## Contents

| Runtime identifier | Library                           |
|--------------------|-----------------------------------|
| `win-x64`          | `DxFeedGraalNativeSdk.dll`        |
| `linux-x64`        | `libDxFeedGraalNativeSdk.so`      |
| `linux-arm64`      | `libDxFeedGraalNativeSdk.so`      |
| `osx-x64`          | `libDxFeedGraalNativeSdk.dylib`   |
| `osx-arm64`        | `libDxFeedGraalNativeSdk.dylib`   |

The libraries are in `runtimes/<runtime identifier>/native`. The Linux libraries need glibc 2.17 or later.

## How the library gets to the application

* .NET Core and .NET 5 or later: the .NET SDK copies the library of the runtime identifier of the application, as
  for any native asset of a package (into the output directory when the runtime identifier is set, into
  `runtimes/<runtime identifier>/native` of a portable application).
* .NET Framework 4.6.1 or later: the MSBuild targets of the package copy `DxFeedGraalNativeSdk.dll` (`win-x64`) into
  the output directory after the build.

## Links

* [The C API, the samples and the build](https://github.com/dxFeed/dxfeed-graal-native-sdk)
* [The release notes](https://github.com/dxFeed/dxfeed-graal-native-sdk/blob/main/ReleaseNotes.md)
* [The releases](https://github.com/dxFeed/dxfeed-graal-native-sdk/releases): the archives of the libraries with the
  C headers for every platform (including iOS) and the HTML documentation of the C API
  (`graal-native-sdk-<version>-c-api-docs-html.zip`)

## License

[MPL-2.0](https://licenses.nuget.org/MPL-2.0)
