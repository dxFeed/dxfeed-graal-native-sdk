# DxFeed.Graal.Native.Oracle

The native libraries of the [dxFeed Graal Native SDK](https://github.com/dxFeed/dxfeed-graal-native-sdk) in the build
variant `oracle`: the dxFeed Java API compiled with Oracle GraalVM Native Image at `-O3` with the Serial GC into a
shared library with a C API. It is a drop-in replacement for
[DxFeed.Graal.Native](https://www.nuget.org/packages/DxFeed.Graal.Native/) of the same version: the same C API, library
names and layout, several isolates per process.

| Package                         | GraalVM        | Optimization | GC     | Isolates per process | Default maximum heap |
|---------------------------------|----------------|--------------|--------|----------------------|----------------------|
| `DxFeed.Graal.Native`           | GraalVM CE     | `-O2`        | Serial | several              | 80 % of RAM          |
| `DxFeed.Graal.Native.Oracle`    | Oracle GraalVM | `-O3`        | Serial | several              | 80 % of RAM          |
| `DxFeed.Graal.Native.Oracle.G1` | Oracle GraalVM | `-O3`        | G1     | one                  | 25 % of RAM          |

## Contents

| Runtime identifier | Library                           |
|--------------------|-----------------------------------|
| `win-x64`          | `DxFeedGraalNativeSdk.dll`        |
| `linux-x64`        | `libDxFeedGraalNativeSdk.so`      |
| `linux-arm64`      | `libDxFeedGraalNativeSdk.so`      |
| `osx-arm64`        | `libDxFeedGraalNativeSdk.dylib`   |

There is no `osx-x64` library: Oracle GraalVM has no macOS x64 build. The libraries are in
`runtimes/<runtime identifier>/native`. The Linux libraries need glibc 2.17 or later. The exact build configuration is
in `docs/graal-native-sdk-build-info.properties` of the archives of the
[release](https://github.com/dxFeed/dxfeed-graal-native-sdk/releases)
(`graal-native-sdk-<version>-<platform>-oracle.zip`).

## Using it with DxFeed.Graal.Net

[DxFeed.Graal.Net](https://www.nuget.org/packages/DxFeed.Graal.Net/) depends on `DxFeed.Graal.Native`. To use this
package instead, reference it with the version of `DxFeed.Graal.Native` that `DxFeed.Graal.Net` depends on, and exclude
the assets of `DxFeed.Graal.Native`:

```xml
<ItemGroup>
  <PackageReference Include="DxFeed.Graal.Net" Version="..." />
  <PackageReference Include="DxFeed.Graal.Native" Version="X.Y.Z" ExcludeAssets="all" />
  <PackageReference Include="DxFeed.Graal.Native.Oracle" Version="X.Y.Z" />
</ItemGroup>
```

A project that gets the assets of two native packages fails the build with an error that names them.

## How the library gets to the application

* .NET Core and .NET 5 or later: the .NET SDK copies the library of the runtime identifier of the application, as
  for any native asset of a package (into the output directory when the runtime identifier is set, into
  `runtimes/<runtime identifier>/native` of a portable application).
* .NET Framework 4.6.1 or later: the MSBuild targets of the package copy `DxFeedGraalNativeSdk.dll` (`win-x64`) into
  the output directory after the build.

## Links

* [The C API, the samples, the build and the build variants](https://github.com/dxFeed/dxfeed-graal-native-sdk)
* [The release notes](https://github.com/dxFeed/dxfeed-graal-native-sdk/blob/main/ReleaseNotes.md)

## License

The code of the SDK: [MPL-2.0](https://licenses.nuget.org/MPL-2.0). The libraries are built with Oracle GraalVM and
contain its runtime, which is distributed under the
[GraalVM Free Terms and Conditions](https://www.oracle.com/downloads/licenses/graal-free-license.html).
