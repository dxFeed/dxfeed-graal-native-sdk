
![DXFEED-GRAAL-NATIVE-SDK](./docs/images/logo_dark.svg#gh-dark-mode-only)
![DXFEED-GRAAL-NATIVE-SDK](./docs/images/logo_light.svg#gh-light-mode-only)

This package grants you access to [dxFeed market data](https://dxfeed.com/market-data/). The library
is designed as a C library and was
compiled using [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
and our flagship [dxFeed Java API](https://docs.dxfeed.com/dxfeed/api/overview-summary.html), making
it easily integrable
into your projects.

![QDS](https://img.shields.io/badge/QDS-v3.357-yellow)
![MDD](https://img.shields.io/badge/MDD-v549-red)
![GraalVM](https://img.shields.io/badge/GraalVM-25.5-orange)
![Platform](https://img.shields.io/badge/platform-win--x64%20%7C%20linux--x64%20%7C%20linux--arm64%20%7C%20osx--x64%20%7C%20osx--arm64%20%7C%20ios%20%7C%20ios--simulator-lightgrey)
[![License](https://img.shields.io/badge/license-MPL--2.0-orange)](./LICENSE)

## Table of Contents

- [Overview](#overview)
  * [Reasons](#reasons)
  * [Benefits](#benefits)
  * [Future development](#future-development)
- [Installation](#installation)
  * [Windows archives](#windows-archives)
  * [iOS and macOS](#ios-and-macos)
  * [Debug builds](#debug-builds)
  * [Windows on ARM and 32-bit systems](#windows-on-arm-and-32-bit-systems)
- [Using the C API](#using-the-c-api)
  * [Conventions](#conventions)
  * [Example](#example)
  * [Samples and wrappers](#samples-and-wrappers)
  * [Wrapper guide and conformance kit](#wrapper-guide-and-conformance-kit)
  * [Version and features](#version-and-features)
  * [API description](#api-description)
  * [Isolates](#isolates)
  * [Listeners](#listeners)
  * [Event queue](#event-queue)
  * [Message connectors](#message-connectors)
  * [Shutdown](#shutdown)
  * [Network activity](#network-activity)
- [Documentation](#documentation)
- [Current State](#current-state)
- [Contribution](#contribution)
  * [Requirements](#requirements)
  * [Building](#building)
  * [IntelliJ IDEA](#intellij-idea)
  * [CLion](#clion)
  * [Scripts](#scripts)
  * [To regenerate META-INF/native-image](#to-regenerate-meta-infnative-image)
  * [To release a new version](#to-release-a-new-version)
- [Support](#support)
- [License](#license)

## Overview

### Reasons

Our Java API serves as the cornerstone of our technology, and with our SDK, you can seamlessly
integrate it into any language, leveraging it as a native library, even on iOS platforms.

### Benefits

- :rocket: Increased performance
- :milky_way: Wider functionality
- :gemini: Identical programming interfaces to our best API
- :thumbsup: Higher quality of support and service

### Future development

Our team is committed to continuously improving the library by regularly releasing updates to our
Java API and implementing them in the native library.

## Installation

The archives of every version are attached to its
[GitHub release](https://github.com/dxFeed/dxfeed-graal-native-sdk/releases):

| Archive                                                   | Platform                                                                      |
|-----------------------------------------------------------|-------------------------------------------------------------------------------|
| `graal-native-sdk-<version>-amd64-linux.zip`              | Linux x64, glibc 2.17+                                                        |
| `graal-native-sdk-<version>-aarch64-linux.zip`            | Linux arm64, glibc 2.17+                                                      |
| `graal-native-sdk-<version>-amd64-windows.zip`            | Windows x64, needs the Visual C++ Redistributable (VCRUNTIME140.dll)          |
| `graal-native-sdk-<version>-amd64-windows-static-crt.zip` | Windows x64, linked with the static C runtime: no Visual C++ Redistributable  |
| `graal-native-sdk-<version>-amd64-windows-static-lib.zip` | Windows x64, the static library: the SDK inside the executable, any C runtime |
| `graal-native-sdk-<version>-x86_64-osx.zip`               | macOS x64, 11.0+                                                              |
| `graal-native-sdk-<version>-aarch64-osx.zip`              | macOS arm64, 14.0+                                                            |
| `graal-native-sdk-<version>-aarch64-ios.zip`              | iOS arm64, 12.0+                                                              |
| `graal-native-sdk-<version>-ios-simulator.zip`            | iOS Simulator x64 (12.0+) and arm64 (14.0+)                                   |
| `graal-native-sdk-<version>-xcframework.zip`              | `DxFeedGraalNativeSdk.xcframework` for iOS, the iOS Simulator and macOS       |
| `graal-native-sdk-<version>-c-api-docs-html.zip`          | The C API documentation (HTML)                                                |

The Linux and Windows archives have `-debug` variants (`amd64-windows-debug-static-crt`,
`amd64-windows-debug-static-lib`), see [Debug builds](#debug-builds). Which Windows archive to take: see
[Windows archives](#windows-archives). There are no archives for Windows on ARM and 32-bit systems: see
[Windows on ARM and 32-bit systems](#windows-on-arm-and-32-bit-systems).

An archive contains the library, the headers (`dxfg_api.h` includes the others) and the license. The desktop archives
also contain `CMakeLists.txt`, which imports the library as the CMake target `DxFeedGraalNativeSdk`:

```cmake
add_subdirectory(<the unpacked archive> graal-native-sdk)
target_link_libraries(<your target> PRIVATE DxFeedGraalNativeSdk)
```

Every archive has `docs/graal-native-sdk-build-info.properties`, which records how it was built: the SDK version, the
GraalVM that built the library (its `release` file), what native-image resolved (the optimization level, the target
machine of `-march`, the garbage collector, PGO, the C compiler) and the options the build passed to native-image.
The iOS archives have no resolved values: their build stops before native-image reports them.

The libraries for .NET are published to nuget.org as the package
[DxFeed.Graal.Native](https://www.nuget.org/packages/DxFeed.Graal.Native). Inside Devexperts, the archives are also in
the Maven repository `qd` of Nexus: `com.dxfeed:graal-native-sdk:<version>`, type `zip`, the classifier is the
platform of the archive name (`amd64-linux`, `xcframework`, etc.).

### Windows archives

| Archive                    | The library                                                           | The application needs                      |
|----------------------------|-----------------------------------------------------------------------|--------------------------------------------|
| `amd64-windows`            | `DxFeedGraalNativeSdk.dll`, linked with the dynamic C runtime (`/MD`) | the DLL and the Visual C++ Redistributable |
| `amd64-windows-static-crt` | `DxFeedGraalNativeSdk.dll`, linked with the static C runtime (`/MT`)  | the DLL                                    |
| `amd64-windows-static-lib` | `DxFeedGraalNativeSdk_static.lib`, the static library                 | nothing: the SDK is inside the executable  |

Each of them fits an application built with `/MT`, `/MTd`, `/MD` or `/MDd`: the DLLs do not share their C runtime with
the application, and the static library uses the C runtime of the application (one library for all four, linked
without `/NODEFAULTLIB`).

The `-static-lib` archive contains `DxFeedGraalNativeSdk_static.lib`, the headers, `link-flags.txt` (the Windows
libraries that the static library needs, one per line) and `CMakeLists.txt`, which imports it as the CMake target
`DxFeedGraalNativeSdk` with these libraries: the same `add_subdirectory` and `target_link_libraries` as above link it.
Without CMake, link the library and the libraries of `link-flags.txt`:

```shell
cl /MT /I <the unpacked archive> app.c <the unpacked archive>\DxFeedGraalNativeSdk_static.lib psapi.lib winhttp.lib ncrypt.lib crypt32.lib version.lib advapi32.lib ws2_32.lib secur32.lib iphlpapi.lib userenv.lib mswsock.lib
```

Only the `dxfg_*` and `graal_*` functions of the static library are visible to the application: the JDK inside it (its
zlib, `JNU_*`) does not clash with the symbols of the application, and the executable exports nothing. The libraries
of GraalVM in it are built with MSVC 19.43 (Visual Studio 2022 17.13): link the application with this toolset or a
newer one.

### iOS and macOS

The archives for iOS (`aarch64-ios`), the iOS Simulator (`ios-simulator`) and macOS (`aarch64-osx`, `x86_64-osx`)
contain `link-flags.txt`: the linker flags of an application (the files and the system libraries), the platforms and
the minimum versions. The build makes it from the files of the archive and checks that the iOS image links with these
flags only before the archive is published.

`graal-native-sdk-<version>-xcframework.zip` (in the GitHub release and in the Maven repository, classifier
`xcframework`) contains `DxFeedGraalNativeSdk.xcframework` made from these archives: static frameworks for iOS (arm64)
and the iOS Simulator (arm64, x86_64), a dynamic framework for macOS (arm64, x86_64), with the headers and the Clang
module `DxFeedGraalNativeSdk`. With Swift Package Manager:

```swift
.binaryTarget(
    name: "DxFeedGraalNativeSdk",
    url: "https://github.com/dxFeed/dxfeed-graal-native-sdk/releases/download/v<version>/graal-native-sdk-<version>-xcframework.zip",
    checksum: "<swift package compute-checksum of the zip>"
)
```

and `import DxFeedGraalNativeSdk` in Swift (`#include <DxFeedGraalNativeSdk/dxfg_api.h>` in C and Objective-C). The
frameworks link the system libraries that they need themselves (autolinking).

### Debug builds

The `*-debug.zip` archives contain the SDK built with `-g -O0`.

On Linux, `libDxFeedGraalNativeSdk.so` is stripped, its debug info (DWARF, including the unwind info of the SDK code)
is in `libDxFeedGraalNativeSdk.so.debug`. Keep it next to the `.so` (gdb finds it by `.gnu_debuglink`;
`install()` of the bundled `CMakeLists.txt` copies it), otherwise set `debug-file-directory` in gdb.
`gdb-debughelpers.py` is the GraalVM gdb extension (pretty-printers of Java objects), load it with
`source <path>/gdb-debughelpers.py` after the SDK library is loaded (e.g. at a breakpoint in the application).
The Java sources are not included.

The SDK code keeps frame pointers in the debug builds, so the sanitizers (LSan/ASan with the default fast unwinder)
unwind the stack through the SDK back to the application code. Build the application code with
`-fno-omit-frame-pointer` for that.

gdb unwinds through the SDK using `libDxFeedGraalNativeSdk.so.debug`. The unwind info that GraalVM generates
for the SDK entry points does not describe the saved `rbp`, so gdb stops at the first application frame
that is addressed by `rbp` (`previous frame inner to this frame`). To get the full stack in gdb, build the calling
code with `-fomit-frame-pointer`.

### Windows on ARM and 32-bit systems

**Windows on ARM.** The SDK is built with GraalVM Native Image, which does not build for Windows on ARM yet
([oracle/graal#9215](https://github.com/oracle/graal/issues/9215)). The arm64 archive will appear when it does. Until
then, an arm64 process cannot load the SDK. An x64 application runs on Windows on ARM under the x64 emulation of
Windows 11 and can load the x64 library, but we do not test this configuration.

**32-bit systems.** There will be no 32-bit archives: GraalVM Native Image builds only 64-bit libraries, and the JDK no
longer has the 32-bit x86 ports (removed in JDK 24 on Windows and in JDK 25 on Linux). This also applies to 32-bit
applications on a 64-bit system. Such an application can receive the data over
[dxLink](https://kb.dxfeed.com/en/market-data-api/dxlink.html), the WebSocket protocol of dxFeed, without a native
library.

## Using the C API

### Conventions

- **Names.** A function is named `dxfg_<Java class>_<method>` after the class and the method of the
  [dxFeed Java API](https://docs.dxfeed.com/dxfeed/api/overview-summary.html) that it calls, so the Java API
  documentation describes what it does. A number at the end tells the overloads apart (`dxfg_DXFeed_getLastEvent2`).
  The functions of the SDK itself have lower-case names (`dxfg_system_set_property`).
- **Threads.** Every function takes the `graal_isolatethread_t *` of the calling thread first, see
  [Isolates](#isolates).
- **Handles.** The structures with the `handler` field (`dxfg_endpoint_t`, `dxfg_subscription_t`, the listeners, etc.)
  are handles of Java objects: the Java object is not collected until its handle is released with
  `dxfg_v4_JavaObjectHandler_release(thread, &object->handler)`. The other structures (events, symbols, instrument
  profiles, lists) are copies of the data of Java objects.
- **Memory.** What a function returns belongs to the caller, who releases it with the function that its description
  names: `dxfg_v4_JavaObjectHandler_release` for handles, `dxfg_v4_EventType_release` and
  `dxfg_v4_event_type_list_release` for events, `dxfg_v4_String_release` for strings, etc. What the caller passes
  stays the caller's: the SDK copies what it needs during the call. The data passed to a callback (the events of a
  listener) belongs to the SDK and is released when the callback returns, so copy what is needed later.
- **Errors.** A function that returns a pointer returns `NULL` on error, the functions that return `int32_t` return
  `DXFG_EXECUTE_SUCCESSFULLY` (0) or `DXFG_EXECUTE_FAIL` (-1), and the ones that return `int64_t` return -1 on error.
  A boolean is an `int32_t`: 1 is true, 0 is false, -1 is an error. Results that are not returned are written to the
  parameters marked `DXFG_OUT`. A failed call stores the Java exception in the calling thread:
  `dxfg_v4_get_and_clear_thread_exception` writes it (the class, the message, the stack trace and the cause) to its
  out parameter and clears it, `dxfg_v4_Exception_release` releases it.
- **Deprecation.** The deprecated functions are marked `DXFG_DEPRECATED`: the compilers warn where they are used, and
  their descriptions name the replacements. To build code that still uses them without the warnings, define the macro
  empty before the headers: `#define DXFG_DEPRECATED(message)` (or disable the warning C4996 of MSVC).
  The deprecated functions are removed in 4.0.0, the next major release: keep the warnings on to find their calls and
  move each call to the function that its warning names.
- **The v4 functions.** The `dxfg_v4_` functions (`dxfg_v4_<area>.h`, included by `dxfg_api.h`) do what the older
  functions do in one form: they return the `int32_t` status only, write the result to the last parameter, marked
  `DXFG_OUT`, take the exception with `dxfg_v4_get_and_clear_thread_exception`, return collections as
  `dxfg_v4_<element>_list` and have no digit suffixes. An older function that has a v4 twin is deprecated. The rules
  are in the "The v4 functions" group of the C API reference. The older and the v4 functions can be mixed.
- **The v4 generation.** The `dxfg_v4_` prefix names a generation of the C API, not a release of the SDK: later
  releases, major ones included, add functions with the same prefix. A new generation appears only when the rules of
  the v4 functions change. It is named after the major release in which it becomes the only one: the v4 functions
  become the only ones in 4.0.0.
- **Stable and experimental functions.** A stable function changes only by deprecation and is removed only in a major
  release. An experimental function is marked `DXFG_EXPERIMENTAL` and says so in its description (the "Experimental
  declarations" page of the C API reference lists them): it may change or be removed in a minor release, and an
  incompatible change gives it a new name. The experimental functions are the twins of the experimental Java API
  (`IndexedEventTxModel`, `SubscriptionController`, `DXFeedSubscription.getTotalDroppedEvents`, the transaction
  models `IndexedTxModel` and `TimeSeriesTxModel`) and the functions of the advanced headers, which `dxfg_api.h` does
  not include. The twins came out in 3.12.0 and are experimental since 3.13.0; their older functions stay until 4.0.0
  and their deprecation messages say that the replacement is experimental. The marker expands to nothing; to see where the experimental functions are used, define it before the
  headers, e.g. `#define DXFG_EXPERIMENTAL [[deprecated("experimental")]]`.

### Example

Prints the quotes of AAPL until Enter is pressed:

```c
#include <stdio.h>

#include "dxfg_api.h"

// Called on the threads of the SDK. The events are valid only during the call.
static void printQuotes(graal_isolatethread_t *thread, dxfg_v4_event_type_list *events, void *userData) {
    (void)thread;
    (void)userData;

    for (int32_t i = 0; i < events->size; i++) {
        if (events->elements[i]->clazz == DXFG_EVENT_QUOTE) {
            const dxfg_v4_quote_t *quote = (const dxfg_v4_quote_t *)events->elements[i];

            printf("%s bid %g x %g, ask %g x %g\n", quote->market_event.event_symbol, quote->bid_price,
                   quote->bid_size, quote->ask_price, quote->ask_size);
        }
    }
}

// Prints and releases the exception of the last failed call on this thread.
static int printException(graal_isolatethread_t *thread) {
    dxfg_v4_exception_t *exception = NULL;

    if (dxfg_v4_get_and_clear_thread_exception(thread, &exception) == DXFG_EXECUTE_SUCCESSFULLY && exception != NULL) {
        fprintf(stderr, "%s: %s\n", exception->class_name, exception->message != NULL ? exception->message : "");
        dxfg_v4_Exception_release(thread, exception);
    }

    return 1;
}

int main(void) {
    graal_isolate_t *isolate = NULL;
    graal_isolatethread_t *thread = NULL;

    if (graal_create_isolate(NULL, &isolate, &thread) != 0) {
        return 1;
    }

    dxfg_v4_endpoint_t *endpoint = NULL;
    if (dxfg_v4_DXEndpoint_create(thread, &endpoint) != DXFG_EXECUTE_SUCCESSFULLY ||
        dxfg_v4_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300") != DXFG_EXECUTE_SUCCESSFULLY) {
        return printException(thread);
    }

    dxfg_v4_feed_t *feed = NULL;
    dxfg_v4_subscription_t *subscription = NULL;
    dxfg_v4_feed_event_listener_t *listener = NULL;
    if (dxfg_v4_DXEndpoint_getFeed(thread, endpoint, &feed) != DXFG_EXECUTE_SUCCESSFULLY ||
        dxfg_v4_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE, &subscription) != DXFG_EXECUTE_SUCCESSFULLY ||
        dxfg_v4_DXFeedEventListener_new(thread, &printQuotes, NULL, &listener) != DXFG_EXECUTE_SUCCESSFULLY ||
        dxfg_v4_DXFeedSubscription_addEventListener(thread, subscription, listener) != DXFG_EXECUTE_SUCCESSFULLY) {
        return printException(thread);
    }

    // The symbol is copied by the call, so it can be on the stack.
    dxfg_v4_string_symbol_t symbol = {{STRING}, "AAPL"};
    if (dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) != DXFG_EXECUTE_SUCCESSFULLY) {
        return printException(thread);
    }

    printf("Press Enter to stop\n");
    getchar();

    // Releasing the last handle of the listener waits for its calls in progress: no calls after it.
    dxfg_v4_DXFeedSubscription_close(thread, subscription);
    dxfg_v4_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_v4_DXEndpoint_close(thread, endpoint);
    dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);

    // Stops the listeners and the endpoints that are still open, otherwise graal_tear_down_isolate waits for their
    // threads.
    dxfg_v4_system_shutdown(thread);
    graal_tear_down_isolate(thread);

    return 0;
}
```

Add the listeners before the symbols: adding a listener to an attached subscription that already has symbols fails
(`IllegalStateException`).

### Samples and wrappers

[DxfgClient](src/main/c/src/apps/DxfgClient) is a console application with a sample for every part of the API:
`DxfgClient --list` lists them with their options and examples, `DxfgClient <case> [-D<name>=<value>...] [<options>]`
runs one (the `-D` options set system properties).

| Area                       | Cases (short names)                                                                                                                    |
|----------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Endpoints, subscriptions   | `DxEndpointSubscriptionCase` (`es`), `DxEndpointTimeSeriesSubscriptionCase` (`ets`), `DxEndpointMonitoringCase` (`em`), `MessageConnectorsCase` (`mc`), `DxLinkCase` (`dxl`) |
| Last events, promises      | `GetLastEventCase` (`gle`), `LastEventIfSubscribedCase` (`le`), `PromiseCase` (`p`), `PromisesAllOfCase` (`pao`), `IndexedEventsPromiseCase` (`iep`) |
| Models                     | `OrderBookModelCase` (`obm`), `IndexedEventModelCase` (`iem`), `TxIndexedEventModelCase` (`txiem`), `IndexedEventTxModelCase` (`ietxm`) |
| Historical data            | `OnDemandServiceCase` (`ods`), `HistoryEndpointCase` (`he`)                                                                            |
| Instrument profiles        | `LiveIpfCase` (`li`), `ReaderIpfCase` (`ri`), `InstrumentProfileReaderBench` (`ipf`), `InstrumentProfileFieldCase` (`ipfi`), `InstrumentProfileCustomFieldsCase` (`ipcf`) |
| Schedule                   | `ScheduleCase` (`sch`), `Schedule2Case` (`sch2`)                                                                                       |
| Glossary                   | `AdditionalUnderlyingsCase` (`au`), `CfiCase` (`cfi`), `PriceIncrementsCase` (`pi`)                                                    |
| Price levels (ORCS)        | `OrcsCase` (`orcs`)                                                                                                                    |
| System                     | `SystemPropertiesCase` (`sp`), `LoggingCase` (`l`), `ExceptionCase` (`ex`), `ExecutorBaseOnConcurrentLinkedQueueCase` (`eb`), `FinalizeListenerCase` (`fl`) |
| QDS tools                  | `Qds`: the QDS tools of the Java API (`DxfgClient qds connect demo.dxfeed.com:7300 Quote AAPL`)                                         |

[NetTest](src/main/c/src/apps/NetTest) publishes generated events at a fixed rate and prints the statistics of the QD
tool `NetTest p`, for publishing benchmarks over the C API.

The [C tests](src/main/c/tests) are small programs too. Our ready-made wrappers:

- .NET: [dxfeed-graal-net-api](https://github.com/dxFeed/dxfeed-graal-net-api)
- C++: [dxfeed-graal-cxx-api](https://github.com/dxFeed/dxfeed-graal-cxx-api)
- Swift: [dxfeed-graal-swift-api](https://github.com/dxFeed/dxfeed-graal-swift-api)
- Go: [dxfeed-graal-go-api](https://github.com/dxFeed/dxfeed-graal-go-api)

### Wrapper guide and conformance kit

For a wrapper of your own (3.14.0 and newer):

- The [wrapper guide](docs/wrapper-guide/index.md) states the contract between the SDK and a wrapper: threads and
  attachment, ownership, return codes and outcomes, tiers and the start-up check, which delivery to use for what. It
  is the "Wrapper guide" of the C API reference, and every archive has it in `docs/wrapper-guide/`.
- The conformance kit checks a wrapper by running it: a runner (C++17 source) sends the requests of scenarios to an
  adapter that you write in the wrapper's language, and checks the responses against an endpoint with the role
  `DXFG_ENDPOINT_ROLE_LOCAL_HUB`, with no network. The archives with a dynamic library and `CMakeLists.txt` have it in
  `kit/`: the runner, the [protocol](src/main/kit/protocol.md), the scenarios and a reference adapter on the C API,
  see [src/main/kit/README.md](src/main/kit/README.md). What it proves and what it cannot is on
  [its page](docs/wrapper-guide/kit.md) of the guide.

### Version and features

A wrapper checks the library before it binds the other functions, with three functions of `dxfg_v4_system.h`
(3.13.0 and newer; a library without them is older):

- `dxfg_v4_system_get_version` writes the release version (`3.13.0`), released with `dxfg_v4_String_release`;
- `dxfg_v4_system_get_advanced_revision` writes the revision of the experimental functions;
- `dxfg_v4_system_is_feature_supported` tells whether a feature is supported (`DXFG_V4_FEATURE_EVENT_QUEUE`,
  `DXFG_V4_FEATURE_SHARED_EVENT_QUEUE`, `DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL`, `DXFG_V4_FEATURE_FEED_FILTER`,
  `DXFG_V4_FEATURE_TIME_SERIES_TX_MODEL_EVENT_PEEK`, `DXFG_V4_FEATURE_MESSAGE_CONNECTORS` and the others of
  `dxfg_v4_system.h`); an unknown name is not supported, not an error.

Bind the stable functions always, and the experimental ones only when the revision equals
`DXFG_V4_ADVANCED_REVISION` of the headers the wrapper was built with; otherwise treat the experimental part as
missing. A wrapper that links to the library checks the same at start-up and stops before it calls an experimental
function.

```c
const char *version = NULL;
int32_t revision = 0;
int32_t supported = 0;

if (dxfg_v4_system_get_version(thread, &version) == DXFG_EXECUTE_SUCCESSFULLY) {
    printf("dxFeed Graal Native SDK %s\n", version);
    dxfg_v4_String_release(thread, version);
}

const int experimental = dxfg_v4_system_get_advanced_revision(thread, &revision) == DXFG_EXECUTE_SUCCESSFULLY &&
                         revision == DXFG_V4_ADVANCED_REVISION;

dxfg_v4_system_is_feature_supported(thread, DXFG_V4_FEATURE_SHARED_EVENT_QUEUE, &supported);
```

### API description

Every archive has `dxfg-api.json` next to the library (3.13.0 and newer): the C API in a form a wrapper generator
reads instead of parsing the headers. It is generated from the headers by the build, sorted and with LF line endings,
so it is the same on every platform:

- `schema_version`: the version of the format, now 1. It grows only when a field changes its meaning or goes away;
  a new field does not change it, so a reader ignores the fields it does not know;
- `sdk_version`, `advanced_revision` and `features`: what the three functions above report at runtime;
- `headers`: every header with its tier, `basic` or `advanced` (an advanced header declares experimental functions
  only and `dxfg_v4_api.h` does not include it);
- `functions`, `types` and `macros`, sorted by name: each with its header, its tier (`stable` or `experimental`) and
  `since`, the version that added it: `x.y.z` of a release (`3.12.0`, the first release of the v4 API, for its
  declarations of that release), or `before 3.12.0` for the older ones. A pre-release such as `3.13.0-rc1` or
  `3.13.0-SNAPSHOT` comes before `3.13.0`, so a declaration with `since` 3.13.0 is already in it;
- a function has its return type and its parameters with their types and `out` (`DXFG_OUT`). A returned pointer and
  an out parameter that receives a pointer (`**`) have `release`, the function that releases it, from its description
  or by the `<type>_release` convention. `deprecated` is null, or the message and the `replacements` of
  `DXFG_DEPRECATED`;
- a struct has its fields, an enum its enumerators, a function pointer type its parameters. Types, values and
  sizes are C text as the header writes it, not evaluated: an enumerator `value` is null when the header gives none,
  and then it is the previous value plus 1 (0 for the first), as in C;
- a field that has no machine-readable source is `unknown`: `threads` (reserved; the thread rules are in the
  descriptions) and a `release` that the description does not name.

The description covers the `dxfg_*` API only: `graal_isolate.h` and `graal_isolate_dynamic.h`, the isolate functions
that GraalVM declares (see [Isolates](#isolates)), are outside it, and a wrapper binds them by hand.

The build also checks the headers against the description of the last release,
[scripts/check-api-headers/baseline/dxfg-api.json](scripts/check-api-headers/baseline/dxfg-api.json): a stable
declaration is not changed or removed (a deprecated function may be removed from 4.0.0 on), an experimental one is
changed or removed only together with a higher `DXFG_V4_ADVANCED_REVISION`, and a new declaration has `@since` in its
description with a version later than the baseline. Parameter names may change; an enum may get new enumerators as
long as every enumerator keeps its value. So that the description is complete, the headers declare nothing inside a
conditional block other than the include guard and `#ifdef __cplusplus`, and no function bodies.

### Isolates

The SDK runs in an isolate (`graal_isolate.h`): a GraalVM instance with its own heap and Java state, created by
`graal_create_isolate`. Every thread that calls the SDK must be attached to it (`graal_create_isolate` attaches the
calling thread, `graal_attach_thread` the others) and passes its own `graal_isolatethread_t` to the SDK functions;
a thread detaches with `graal_detach_thread` before it exits. The handles of one isolate cannot be used in another.

The isolate arguments are passed in `graal_create_isolate_params_t.argv`, parsed like a command line (`argv[0]`, the
program name, is skipped):

```c
char *argv[] = {"app", "-Dlog.level=OFF", "-Xmx512m", "-XX:TearDownWarningSeconds=10"};
graal_create_isolate_params_t params;
memset(&params, 0, sizeof(params));
params.version = __graal_create_isolate_params_version;
params.argc = 4;
params.argv = argv;

graal_isolate_t *isolate = NULL;
graal_isolatethread_t *thread = NULL;
if (graal_create_isolate(&params, &isolate, &thread) != 0) {
    // the reason is printed to stderr
}
```

- `-D<name>=<value>`: a system property, as `dxfg_system_set_property` sets it after the creation.
- `-Xmx`, `-Xms`, `-Xmn`: the maximum, the initial and the young generation sizes of the heap.
- `-XX:` options of the isolate: `-XX:TearDownWarningSeconds=<seconds>` (see [Shutdown](#shutdown)),
  `-XX:-EnableSignalHandling` (on Linux and macOS the isolate does not install its no-op handlers of `SIGPIPE` and
  `SIGXFSZ`).

An unknown `-XX:` option or a wrong value fails `graal_create_isolate`, unless `ignore_unrecognized_args` is 1.
`graal_isolate_dynamic.h` declares the same functions as pointer types (`graal_create_isolate_fn_t`, etc.) for loading
the library at run time (`dlopen`/`dlsym`, `LoadLibrary`/`GetProcAddress`); include it before `dxfg_api.h`, as both
headers have the same include guard. The C API documentation describes both headers.

### Listeners

A listener created by a `dxfg_*Listener_new` function calls its function with the user data until it is deactivated:
when the last handle of the listener is released (`dxfg_v4_JavaObjectHandler_release`, the clones made by
`dxfg_v4_JavaObjectHandler_clone` count) or by `dxfg_v4_NativeListener_deactivate`. Both wait for the calls that are
in progress on other threads, so the user data can be freed right after they return. Do not hold the locks that
the listener's function takes while releasing it. A listener may be released or deactivated from its own callback.

QD may call a listener once more after `dxfg_v4_DXEndpoint_close`, `dxfg_v4_DXFeedSubscription_close` or
`dxfg_*_remove*Listener` returns, so remove the listener first and then release it. The state changes
of an endpoint are coalesced (e.g. `NOT_CONNECTED -> CONNECTED` without `CONNECTING`).

### Event queue

An application that cannot take calls from the threads of the SDK (e.g. a wrapper for a language with its own runtime)
receives the events of a subscription on its own thread with an event queue (`dxfg_v4_event_queue.h`):

1. Create the queue with `dxfg_v4_EventQueue_new`. As with a listener, do it before adding the symbols to a
   subscription attached to a feed; a detached subscription may already have symbols, but then add its listeners
   before the queue (QD refuses a listener on a subscription with symbols once a feed or a queue is attached).
2. On a thread of your own, call `dxfg_v4_EventQueue_poll` in a loop: it waits for the events and returns them as a
   `dxfg_v4_event_type_list` (release it with `dxfg_v4_event_type_list_release`). It returns
   `DXFG_EVENT_QUEUE_NO_EVENTS` after the timeout or `dxfg_v4_EventQueue_wakeUp`, and `DXFG_EVENT_QUEUE_CLOSED` once
   the queue is closed (`dxfg_v4_EventQueue_close`, the release of its handle, the close of the subscription,
   `dxfg_v4_system_shutdown`).
3. After a poll, compare `dxfg_v4_EventQueue_getDroppedEvents` with its previous value: a larger one means that events
   were lost before the returned ones.

The SDK retrieves the events only when the queue is polled. Meanwhile the lasting events (e.g. Quote) are conflated,
and the other ones (e.g. TimeAndSale) wait in a buffer of the subscription: up to 1000000 events by default (the system
property `com.devexperts.qd.impl.matrix.Agent.MaxBufferSize`, set before the first endpoint), beyond that the oldest
ones are dropped and counted. A `STREAM_FEED` endpoint drops nothing: it stops receiving data for all its
subscriptions until the full queue is polled. Poll a queue from one thread at a time. Closing an endpoint does not
end a poll, so poll with a finite timeout if the application closes the endpoints itself. The rules are in the
"Event queue" group of the C API reference.

One thread can serve many subscriptions and the models: create a shared queue with `dxfg_v4_EventQueue_create`, attach
each subscription with `dxfg_v4_EventQueue_attach` and a tag of your choice, and give the queue's executor
(`dxfg_v4_EventQueue_getExecutor`) to the models and the instrument profile collector. `dxfg_v4_EventQueue_pollTagged`
returns the events with the tag of their subscription (release the list with `dxfg_v4_tagged_event_list_release`), and
calls the listeners of the models on the polling thread:

```c
dxfg_v4_event_queue_t *queue = NULL;
dxfg_v4_EventQueue_create(thread, &queue);
dxfg_v4_EventQueue_attach(thread, queue, quotes, 1);  // before the symbols are added
dxfg_v4_EventQueue_attach(thread, queue, trades, 2);

dxfg_v4_executor_t *executor = NULL;
dxfg_v4_EventQueue_getExecutor(thread, queue, &executor);
dxfg_v4_IndexedTxModel_Builder_withExecutor(thread, builder, executor, &builder2);  // the model's listener runs in polls

for (;;) {
    dxfg_v4_tagged_event_list *events = NULL;
    int32_t result = dxfg_v4_EventQueue_pollTagged(thread, queue, 1000, 0, &events);
    if (result == DXFG_EVENT_QUEUE_CLOSED || result < 0) {
        break;
    }
    if (result == DXFG_EXECUTE_SUCCESSFULLY) {
        for (int32_t i = 0; i < events->size; i++) {
            handle(events->tags[i], events->elements[i]);
        }
        dxfg_v4_tagged_event_list_release(thread, events);
    }
}
```

Closing a subscription detaches only it; closing the queue or releasing the last handle of the queue and of its
executor stops every listener on the queue. Keep the queue or executor handle until the models are closed.

A host with an event loop (Python asyncio, an `epoll` loop, libuv) can poll without a reader thread: the experimental
`dxfg_v4_EventQueue_setReadySignal` gives the queue an OS object of the host (check
`DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL`), which the SDK makes ready whenever the queue has work. The host creates,
owns and closes the object, resets it before each poll, and polls with the timeout 0 when the loop reports it ready.
On Linux, macOS and iOS the object is a non-blocking file descriptor, an `eventfd` or the write end of a pipe; a
descriptor that is blocking or read-only is refused:

```c
int fd = eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC);
dxfg_v4_EventQueue_setReadySignal(thread, queue, DXFG_V4_READY_SIGNAL_FD, fd);

int epfd = epoll_create1(EPOLL_CLOEXEC);
struct epoll_event ev = { .events = EPOLLIN, .data.fd = fd };
epoll_ctl(epfd, EPOLL_CTL_ADD, fd, &ev);

for (bool running = true; running;) {
    struct epoll_event ready;
    if (epoll_wait(epfd, &ready, 1, -1) > 0) {
        uint64_t counter;
        read(fd, &counter, sizeof counter);  // reset before the poll
        dxfg_v4_event_type_list *events = NULL;
        int32_t result = dxfg_v4_EventQueue_poll(thread, queue, 0, 0, &events);
        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            handle(events);
            dxfg_v4_event_type_list_release(thread, events);
        } else if (result == DXFG_EVENT_QUEUE_CLOSED) {
            running = false;
        }  // DXFG_EVENT_QUEUE_NO_EVENTS: wait again
    }
}
close(fd);  // the loop closes the object, after the queue is closed
```

On Windows the object is an event: pass `(int64_t)(intptr_t)handle` of an auto-reset event with
`DXFG_V4_READY_SIGNAL_WINDOWS_EVENT` and wait for it with `RegisterWaitForSingleObject` or `WaitForSingleObject`.
Loops that cannot wait for a foreign object (the asyncio proactor loop and libuv on Windows) keep a reader thread that
polls with a timeout. A signal without work is possible (its poll returns `DXFG_EVENT_QUEUE_NO_EVENTS`); work is never
left without a signal. The contract is in the "Event queue" group of the C API reference.

### Message connectors

The message connectors of an endpoint (`dxfg_v4_message_connector.h`, since 3.14.0) tell what the endpoint is connected
to and how much data flows: one connector per address of the address string. Check
`dxfg_v4_system_is_feature_supported(thread, DXFG_V4_FEATURE_MESSAGE_CONNECTORS, &supported)` before binding the
functions. A sketch without error handling:

```c
#include <inttypes.h>
#include <stdio.h>

#include "dxfg_api.h"

// Called on the SDK thread `dxfg-MessageConnectorListener`; the handle is valid during the call only.
static void onStateChanged(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector, void *userData) {
    int32_t state = -1;
    dxfg_v4_MessageConnector_getState(thread, connector, &state);
    printf("state %d\n", state); // the current state, which may be newer than the change that caused the call
}

void printConnectors(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint) {
    dxfg_v4_message_connector_list *connectors = NULL;
    dxfg_v4_DXEndpoint_getConnectors(thread, endpoint, &connectors);

    for (int32_t i = 0; i < connectors->size; i++) {
        dxfg_v4_message_connector_t *connector = connectors->elements[i];
        const char *address = NULL;
        dxfg_v4_string_list *current = NULL;
        int32_t state = -1;
        dxfg_v4_message_connector_stats_t stats;

        dxfg_v4_MessageConnector_getAddress(thread, connector, &address); // the credentials are masked
        dxfg_v4_MessageConnector_getCurrentAddresses(thread, connector, &current);
        dxfg_v4_MessageConnector_getState(thread, connector, &state); // DXFG_V4_MESSAGE_CONNECTOR_STATE_*
        dxfg_v4_MessageConnector_getStatistics(thread, connector, &stats);
        printf("%s: state %d, %d current address(es), %" PRId64 " data records read\n", address, state,
               current->size, stats.read_data_records);

        dxfg_v4_String_release(thread, address);
        dxfg_v4_string_list_release(thread, current);
    }

    dxfg_v4_message_connector_list_release(thread, connectors); // releases the connector handles too
}

void watchConnectors(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint) {
    // The listener is experimental: bind it only when the library has the revision of the headers.
    int32_t revision = -1;
    dxfg_v4_system_get_advanced_revision(thread, &revision);

    if (revision != DXFG_V4_ADVANCED_REVISION) {
        return;
    }

    dxfg_v4_message_connector_listener_t *listener = NULL;
    dxfg_v4_MessageConnectorListener_new(thread, &onStateChanged, NULL, &listener);
    dxfg_v4_DXEndpoint_addMessageConnectorListener(thread, endpoint, listener);
    printConnectors(thread, endpoint); // the listener gets the later changes only, not the current states
    // ...
    dxfg_v4_DXEndpoint_removeMessageConnectorListener(thread, endpoint, listener);
    dxfg_v4_JavaObjectHandler_release(thread, &listener->handler); // waits for a call in progress
}
```

- Lifetimes. `dxfg_v4_message_connector_list_release` releases the whole list with its handles; keep a connector
  longer with `dxfg_v4_JavaObjectHandler_clone`. The handles may be released in any order, also after the endpoint
  is closed. A connect to another address replaces the connectors (a replaced one answers as `DISCONNECTED`), so read
  the list anew rather than keeping it.
- Polling or a listener. Reading the list on a timer is enough for a status display. The listener is called on one
  SDK thread for all the endpoints of the isolate, one call at a time; fast changes may come as one call, so it reads
  the current state. It should return quickly; it may call any function of the SDK, `dxfg_v4_DXEndpoint_connect`
  included.
- Data flow. Data arrives when `read_data_records` grows between two reads of the statistics; `read_bytes` grows on
  heartbeats too. The counters are never reset: compute the rates from the differences.
- Shutdown. `dxfg_v4_system_shutdown` stops the listener thread; release the handles after it.

The case `DxfgClient mc` ([MessageConnectorsCase](src/main/c/src/apps/DxfgClient/MessageConnectorsCase.hpp)) prints the
connectors every second with the rates and the state changes from a listener.

### Shutdown

The threads of an open endpoint do not stop on `Thread.interrupt()`, so `graal_tear_down_isolate` waits
for them forever while any endpoint is open. Before tearing down the isolate (or exiting the process):

1. Call `dxfg_v4_system_shutdown`. It deactivates all the listeners (`dxfg_v4_NativeListener_deactivateAll`: no
   listener is called after it returns, the ones created later are never called) and closes the event queues (the
   polls return `DXFG_EVENT_QUEUE_CLOSED`), then closes all the endpoints
   (including the default ones of `dxfg_v4_DXEndpoint_getInstance`, `dxfg_v4_DXFeed_getInstance`, etc.) and instrument
   profile connections created through the SDK and waits for the termination of the endpoints
   (`dxfg_v4_system_close_all_and_await_termination`). It cannot be undone; call it in the explicit shutdown of the
   application, not from an exit handler of a DLL.
2. Release the remaining handles and call `graal_tear_down_isolate`.

To find the threads that block the tear-down, pass `-XX:TearDownWarningSeconds=<seconds>` in
`graal_create_isolate_params_t.argv` (the debug builds use 10 seconds by default).

### Network activity

The QD monitoring started with the first endpoint includes the MARS plugin `TimeSyncTracker`, which sends UDP multicast
packets (`239.192.51.45:5145` by default) from every process to compare the clocks of the hosts. The SDK disables it
by default. To enable it, set the system property `com.dxfeed.sdk.TimeSyncTracker.enable` to `true` with
`dxfg_system_set_property` before creating the first endpoint.

## Documentation

The C API reference (generated from the headers by Doxygen) of the latest release is at
https://dxfeed.github.io/dxfeed-graal-native-sdk/latest/, the reference of every release from 3.9.0 on at
`https://dxfeed.github.io/dxfeed-graal-native-sdk/<version>/` (the list of the versions:
https://dxfeed.github.io/dxfeed-graal-native-sdk/). It is also attached to every
[GitHub release](https://github.com/dxFeed/dxfeed-graal-native-sdk/releases) as
`graal-native-sdk-<version>-c-api-docs-html.zip`.

Find useful information in our self-service dxFeed Knowledge Base:

- [dxFeed Knowledge Base](https://kb.dxfeed.com/index.html?lang=en)
  * [Getting Started](https://kb.dxfeed.com/en/getting-started.html)
  * [Troubleshooting](https://kb.dxfeed.com/en/troubleshooting-guidelines.html)
  * [Market Events](https://kb.dxfeed.com/en/data-model/dxfeed-api-market-events.html)
  * [Event Delivery contracts](https://kb.dxfeed.com/en/data-model/model-of-event-publishing.html#event-delivery-contracts)
  * [dxFeed API Event classes](https://kb.dxfeed.com/en/data-model/model-of-event-publishing.html#dxfeed-api-event-classes)
  * [Exchange Codes](https://kb.dxfeed.com/en/data-model/exchange-codes.html)
  * [Order Sources](https://kb.dxfeed.com/en/data-model/qd-model-of-market-events.html#order-x)
  * [Order Book reconstruction](https://kb.dxfeed.com/en/data-model/dxfeed-order-book/order-book-reconstruction.html)
  * [Symbology Guide](https://kb.dxfeed.com/en/data-model/symbology-guide.html)

## Current State

### Event Types

- [x] [Order](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Order.html) is a snapshot
  of the full available
  market depth for a symbol
- [x] [SpreadOrder](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/SpreadOrder.html) is
  a snapshot of the
  full available market depth for all spreads
- [x] [AnalyticOrder](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/AnalyticOrder.html)
  represents an
  extension of Order introducing analytic information, e.g., adding iceberg-related
  information to this order
- [x] [Trade](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Trade.html) is a snapshot
  of the price and size
  of the last trade during regular trading hours and an overall day
  volume and day turnover
- [x] [TradeETH](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/TradeETH.html) is a
  snapshot of the price
  and size of the last trade during extended trading hours and the extended
  trading hours day volume and day turnover
- [x] [Candle](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/Candle.html) - event with
  open, high, low, and
  close prices and other information for a specific period
- [x] [Quote](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Quote.html) is a snapshot
  of the best bid and
  ask prices and other fields that change with each quote
- [x] [Profile](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Profile.html) is a
  snapshot that contains the
  security instrument description
- [x] [Summary](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Summary.html) is a
  snapshot of the trading
  session, including session highs, lows, etc.
- [x] [TimeAndSale](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/TimeAndSale.html) -
  represents a trade or
  other market event with price, like market open/close price, etc.
- [x] [Greeks](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/option/Greeks.html) is a snapshot
  of the option
  price, Black-Scholes volatility, and Greeks
- [x] [Series](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/option/Series.html) is a snapshot
  of computed values
  available for all options series for a given underlying symbol based on options market prices
- [x] [TheoPrice](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/option/TheoPrice.html) is a
  snapshot of the
  theoretical option price computation that is periodically performed
  by [dxPrice](http://www.devexperts.com/en/products/price.html) model-free computation
- [x] [Underlying](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/option/Underlying.html) is a
  snapshot of computed
  values available for an option underlying symbol based on the market’s option prices
- [x] [OptionSale](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/OptionSale.html) is a
  represents a trade or another market event with the price (for example, market open/close price,
  etc.) for each option symbol listed under the specified Underlying.
- [x] [Configuration](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/misc/Configuration.html)
  is an event with an
  application-specific attachment. In the C API the attachment is only a string (see `dxfg_message_t`):
  arbitrary Java attachments cannot be created or read in C, for a string payload prefer `TextConfiguration`
- [x] [Message](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/misc/Message.html) is an event
  with an
  application-specific attachment. In the C API the attachment is only a string (see `dxfg_message_t`):
  arbitrary Java attachments cannot be created or read in C, for a string payload prefer `TextMessage`
- [x] [TextConfiguration](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/misc/TextConfiguration.html)
  is an event with an application-specific text
- [x] [TextMessage](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/misc/TextMessage.html) is an event
  with an application-specific text
- [x] [OtcMarketsOrder](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/OtcMarketsOrder.html) is
  an extension of Order for the symbols traded on the OTC Markets
- [x] [MarketMaker](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/MarketMaker.html) is a
  snapshot of the aggregated top quotes of the market participants (such as market makers) for a symbol
- [x] [OrderImbalance](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/OrderImbalance.html) is the
  order book statistics during auctions: the imbalance between the buy and sell orders
- [x] [NuamOrder](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/custom/NuamOrder.html),
  [NuamTrade](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/custom/NuamTrade.html),
  [NuamTimeAndSale](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/custom/NuamTimeAndSale.html) are
  the extensions of Order, Trade and TimeAndSale for the symbols traded on the Nuam Exchange
- [x] [DailyCandle](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/DailyCandle.html) (deprecated
  in the Java API, use Candle)

### Subscription Symbols

- [x] String
- [x] [TimeSeriesSubscriptionSymbol](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/osub/TimeSeriesSubscriptionSymbol.html) -
  represents subscription to time-series events
- [x] [IndexedSubscriptionSymbol](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/osub/IndexedEventSubscriptionSymbol.html) -
  represents subscription to a specific source of indexed events
- [x] [WildcardSymbol.ALL](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/osub/WildcardSymbol.html) -
  represents a
  *wildcard* subscription to all events of the specific event type
- [x] [CandleSymbol](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandleSymbol.html) -
  symbol used
  with [DXFeedSubscription](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedSubscription.html)
  class to
  subscribe for [Candle](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/Candle.html)
  events

### Subscriptions & Models

- [x] [CreateSubscription](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedSubscription.html)
  creates a new
  subscription for multiple event types *attached* to a specified feed
- [x] [CreateTimeSeriesSubscription](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedTimeSeriesSubscription.html)
  extends DXFeedSubscription to conveniently subscribe to time series of events for a set of symbols
  and event
  types ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/api/DXFeedConnect.java))
- [x] [GetLastEvent](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getLastEvent-E-)
  returns the last
  event for the specified event
  instance ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/api/DXFeedSample.java))
- [x] [GetLastEvents](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getLastEvents-java.util.Collection-)
  returns the last events for the specified event instances list
- [x] [GetLastEventPromise](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getLastEventPromise-java.lang.Class-java.lang.Object-)
  requests the last event for the specified event type and
  symbol ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/console/LastEventsConsole.java))
- [x] [GetLastEventsPromises](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getLastEventsPromises-java.lang.Class-java.util.Collection-)
  requests the last events for the specified event type and symbol collection
- [x] [GetLastEventIfSubscribed](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getLastEventIfSubscribed-java.lang.Class-java.lang.Object-)
  returns the last event for the specified event type and symbol if there’s a subscription for it
- [x] [GetIndexedEventsPromise](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getIndexedEventsPromise-java.lang.Class-java.lang.Object-com.dxfeed.event.IndexedEventSource-)
  requests an indexed events list for the specified event type, symbol, and source
- [x] [GetIndexedEventsIfSubscribed](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getIndexedEventsIfSubscribed-java.lang.Class-java.lang.Object-com.dxfeed.event.IndexedEventSource-)
  requests an indexed events list for the specified event type, symbol, and source if there’s a
  subscription for it
- [x] [GetTimeSeriesPromise](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getTimeSeriesPromise-java.lang.Class-java.lang.Object-long-long-)
  requests time series of events for the specified event type, symbol, and time
  range ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/_simple_/FetchDailyCandles.java))
- [x] [GetTimeSeriesIfSubscribed](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getTimeSeriesIfSubscribed-java.lang.Class-java.lang.Object-long-long-)
  requests time series of events for the specified event type, symbol, and time range if there’s a
  subscription for it
- [x] [TimeSeriesEventModel](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/TimeSeriesEventModel.html) -
  is a model
  of a list of time series
  events ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/ui/swing/DXFeedCandleChart.java))
- [x] [IndexedEventModel](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/IndexedEventModel.html)
  is a model of a
  list of indexed
  events ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/ui/swing/DXFeedTimeAndSales.java))
- [x] [OrderBookModel](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/market/OrderBookModel.html)
  is a model of
  convenient Order Book
  management ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/ui/swing/DXFeedMarketDepth.java))
- [x] IndexedTxModel and TimeSeriesTxModel (`com.dxfeed.sdk.model.tx`) are the models of the indexed
  and time series events that deliver the transactions and the snapshots in whole

### IPF & Schedule

- [x] [InstrumentProfile](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/InstrumentProfile.html)
  represents basic
  profile information about a market
  instrument ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/ipf/DXFeedIpfConnect.java))
- [x] [InstrumentProfileCollector](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/live/InstrumentProfileCollector.html)
  collects instrument profile updates and provides the live instrument profiles
  list ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/ipf/DXFeedLiveIpfSample.java))
- [x] [Schedule](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/schedule/Schedule.html) provides API
  to retrieve and
  explore various exchanges’ trading schedules and different financial instrument
  classes ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/schedule/ScheduleSample.java))
- [x] [InstrumentProfileReader](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/InstrumentProfileReader.html)
  and [InstrumentProfileConnection](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ipf/live/InstrumentProfileConnection.html)
  read the instrument profiles from files and URLs, InstrumentProfileField and InstrumentProfileCustomFields
  access their fields
- [x] [PriceIncrements](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/glossary/PriceIncrements.html),
  [AdditionalUnderlyings](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/glossary/AdditionalUnderlyings.html),
  [CFI](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/glossary/CFI.html): the price increments, the additional
  underlyings of an option and the CFI code (ISO 10962) of an instrument

### Services

- [x] [OnDemandService](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ondemand/OnDemandService.html)
  provides on-demand
  historical tick data replay
  controls ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/ondemand/OnDemandSample.java))
- [x] PriceLevelService (`com.dxfeed.orcs.api`) requests the price levels of the order books from ORCS
- [x] HistoryEndpoint requests the candles from the candle web service of dxFeed (`dxfg_candlewebservice.h`)
- [x] [DXPublisher](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXPublisher.html) publishes the events
  to an endpoint

### Endpoint Roles

- [x] [FEED](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Role.html#FEED) connects
  to the remote data
  feed provider and is optimized for real-time or delayed data processing (**this is a default role
  **)
- [x] [STREAM_FEED](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Role.html#STREAM_FEED)
  is similar to
  FEED and also connects to the remote data feed provider but is designed for bulk data parsing from
  files
- [x] [LOCAL_HUB](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Role.html#LOCAL_HUB)
  is a local hub
  without the ability to establish network connections. Events published via publisher are delivered
  to local feed only.
- [x] [PUBLISHER](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Role.html#PUBLISHER)
  connects to the
  remote publisher hub (also known as multiplexor) or creates a publisher on the local
  host ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/_simple_/WriteTapeFile.java))
- [x] [STREAM_PUBLISHER](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Role.html#STREAM_PUBLISHER)
  is
  similar to PUBLISHER and also connects to the remote publisher hub, but is designed for bulk data
  publishing
- [x] [ON_DEMAND_FEED](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Role.html#ON_DEMAND_FEED)
  is similar
  to FEED, but it is designed to be used with OnDemandService for historical data replay only

## Contribution

[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)

[Semantic Versioning](https://semver.org/)

### Requirements

- GraalVM `graalvm.version` of `pom.xml` (now GraalVM 25 Innovation 5, `graal-25.5.5.0.3`) in `JAVA_HOME`. The
  macOS x64 and iOS Simulator libraries are built with GraalVM `jdk-25.0.1`, the last one published for macOS x64.
  See the [GraalVM versions](.teamcity/README.MD#graalvm-versions) that the build supports.
- Maven 3.8 or later (the CI uses 3.8.9).
- The C toolchain that Native Image needs: GCC with the glibc and zlib headers on Linux, the Xcode Command Line Tools on
  macOS (Xcode with the iOS and iOS Simulator SDKs for iOS), Build Tools for Visual Studio 2022 on Windows (run Maven
  from the Developer Command Prompt; the resource compiler is found by [scripts/rc.cmd](scripts/rc.cmd) itself).
- Access to the Devexperts Maven repositories of `pom.xml`: QD and the other dependencies are taken from there.

### Building

```shell
mvn clean package
```

builds the library of the current platform in `target/native-image` and its archive
`target/graal-native-sdk-<platform>.zip`. Options:

- `-P buildDebug`: the debug build, see [Debug builds](#debug-builds);
- `-Dc-api-docs`: also the C API documentation in `target/docs/c-api` and its archive (Doxygen must be in
  `PATH`); with `-Dc-api-docs.html=<absolute path>`, the archive packs that prebuilt `html` directory instead, and the
  build fails unless its `index.html` has the version of the project (the release builds pack the reference generated
  by the Doxygen of the pull request check, see [.teamcity/README.MD](.teamcity/README.MD#deploying-artifacts)). The
  check of the documentation, as in the pull request build, is
  `mkdir -p target/c-api-docs-check && doxygen docs/Doxyfile.check`: every undocumented declaration or parameter is a
  warning in `target/c-api-docs-check/warnings.log`, and any warning fails the pull request build. The check needs
  Doxygen 1.13 or later (older versions report the described parameters as undocumented); the pull request build runs
  Doxygen 1.18.0 in the image of [.teamcity/README.MD](.teamcity/README.MD#testing);
- `-DmacIos=true` on a Mac with Apple silicon: the iOS library. The iOS Simulator library is built after it without
  `clean`, with the x64 GraalVM under Rosetta: `arch -x86_64 mvn -DmacIosSimulator=true package` (its arm64 slice is
  the object of the iOS build). The JDK and Substrate VM libraries for iOS are in [jre-ios](jre-ios/README.MD).
- `-DwindowsStaticRuntime=true` on Windows: `DxFeedGraalNativeSdk.dll` is linked with the static C runtime (`/MT`) and
  does not need the Visual C++ Redistributable (no `VCRUNTIME140.dll`, `api-ms-win-crt-*.dll`); the archive is
  `amd64-windows-static-crt` (`amd64-windows-debug-static-crt` with `-P buildDebug`). The JDK libraries of GraalVM are
  built with `/MD`, so [New-CrtShim.ps1](scripts/windows-static-runtime/New-CrtShim.ps1) assembles the pointers of
  their CRT imports to the static CRT (`dumpbin` and `ml64` of the MSVC environment). With the phase
  `integration-test` or a later one (`verify` checks the archives), the same build packs the image object into the
  static library of the SDK, the archive `amd64-windows-static-lib` (`amd64-windows-debug-static-lib`), see
  [Windows archives](#windows-archives).

### IntelliJ IDEA

- Open `pom.xml` as a project. Set the GraalVM of the [requirements](#requirements) as the project SDK, as the JDK for
  the importer (Settings | Build, Execution, Deployment | Build Tools | Maven | Importing) and as the JRE of the runner
  (... | Maven | Runner).
- The TeamCity settings (`.teamcity/settings.kts`, Kotlin DSL) are a separate Maven project with JDK 21: until it is
  added (right-click `.teamcity/pom.xml` | Add as Maven Project), everything in them is red. See
  [IntelliJ IDEA](.teamcity/README.MD#intellij-idea) in `.teamcity/README.MD`.

### CLion

The C/C++ samples ([DxfgClient](#samples-and-wrappers)) and the C tests are a CMake project in `src/main/c` that links
the library built by Maven:

1. Build the library: `mvn clean package` (`-P buildDebug` to debug the SDK code too).
2. Open `src/main/c` in CLion. It takes the presets of `CMakePresets.json`: enable `conf-debug` or `conf-release`
   (Settings | Build, Execution, Deployment | CMake). On Windows, use the Visual Studio toolchain: the library is built
   by MSVC.
3. The project imports the library from `target/native-image` (`GRAAL_CUSTOM_BIN_LOCATION` in
   `src/main/c/CMakeLists.txt`) and copies it next to the executables after the build, so they run and debug as they
   are. CLion runs the tests with CTest.

Rebuild the library with Maven after changing the Java code. [build.sh and build.cmd](src/main/c) configure, build,
test, install and pack the project from the command line, as the CI does.

### Scripts

**Native Image metadata (`scripts/native-image-metadata`)**

| Script                             | Purpose                                                                                                                                                                                                                                                                             |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `update-native-image-metadata.ps1` | Collects the Native Image metadata with the native-image-agent (GraalVM 23+) by running the scenario classes (`NewCases` by default) and merges it into `src/main/resources/META-INF/native-image`. See [To regenerate META-INF/native-image](#to-regenerate-meta-infnative-image). |
| `merge-agent-metadata.py`          | Merges the agent output into `reachability-metadata.json` and the legacy `*-config.json` files (existing entries are never removed). Used by `update-native-image-metadata.ps1`.                                                                                                    |

**Build**

| Script                                                         | Purpose                                                                                                                                                                                                                                                                        |
|----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `scripts/rc.cmd`                                               | Wrapper for the Windows resource compiler used by the `windows` Maven profile to compile `src/main/windows/version.rc` (version info of `DxFeedGraalNativeSdk.dll`) into `target/version.res`. If `rc.exe` is not in `PATH`, it initializes the Visual Studio environment (`vcvars64.bat`). |
| `scripts/windows-static-runtime/New-CrtShim.ps1`               | Assembles the pointers of the CRT imports of the JDK libraries of GraalVM to the static CRT, so that the `windowsStaticRuntime` Maven profile links `DxFeedGraalNativeSdk.dll` without the Visual C++ Redistributable (`/MT`), see [Building](#building).                      |
| `scripts/windows-static-runtime/New-StaticLibrary.ps1`         | Packs the image object of native-image and the libraries it is linked with into `DxFeedGraalNativeSdk_static.lib`, the static library of the `-static-lib` archive (the `windowsStaticRuntime` Maven profile, see [Windows archives](#windows-archives)).                      |
| `scripts/windows-static-runtime/Hide-StaticLibrarySymbols.ps1` | Hides the internal symbols of the static library (the prefix `__dxfg_`) and removes its export and C runtime directives, so that it links with any C runtime of the application. The COFF code is in `StaticLibrary.cs`, which Windows PowerShell compiles.                    |
| `scripts/windows-static-runtime/Test-StaticLibrary.ps1`        | Checks the `-static-lib` archive before the deploy: a program links with it with `/MT`, `/MTd`, `/MD` and `/MDd`, with and without `/DEBUG`, without linker warnings, exports nothing and works.                                                                               |
| `scripts/windows-static-runtime/Test-StaticRuntime.ps1`        | Checks the `-static-crt` archive before the deploy: its DLL and a program built with `/MT` against it do not import the dynamic C runtime, and the program works, also in a Windows container without the Visual C++ Redistributable (`-Dwindows.static.runtime.check.image`). |
| `src/main/c/build.cmd`, `src/main/c/build.sh`                  | Configure, build, test (ctest), install and pack the C/C++ samples (`DxfgClient`) and tests against the library in `target/native-image`.                                                                                                                                      |
| `scripts/check-doc-names/CheckDocNames.java`                   | Checks in the `package` phase that the wrapper guide and the conformance kit name only what `dxfg-api.json` has: every `dxfg_`/`DXFG_` name exists and is not deprecated, the features and event fields of the scenarios exist (skip: `-Ddoc.names.check.skip`).               |
| `jre-ios/build.py`                                             | Builds the JRE libraries for iOS, declares the platform of the iOS images, checks that they link and assembles the XCFramework, see [jre-ios](jre-ios/README.MD).                                                                                                              |

**CI (`.teamcity`)**, see also [.teamcity/README.MD](.teamcity/README.MD)

| Script                                                                           | Purpose                                                                                                                                                                                                                                                                 |
|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `settings.kts`                                                                   | TeamCity project configuration (Kotlin DSL).                                                                                                                                                                                                                            |
| `Common.kt`                                                                      | Its shared parts: the Maven options with the credentials, the Docker images, the steps and the agents.                                                                                                                                                                  |
| `scripts/install.sh`, `scripts/install.ps1`                                      | Download and install Maven, GraalVM (see the [supported version formats](.teamcity/README.MD#graalvm-versions)) and Build Tools for Visual Studio. Used by the Dockerfiles and the macOS and Windows builds.                                                            |
| `docker/graalvm-linux-x64.Dockerfile`, `docker/graalvm-linux-aarch64.Dockerfile` | Linux build images (Oracle Linux 7, glibc 2.17).                                                                                                                                                                                                                        |
| `docker/cpp-test-linux-x64.Dockerfile`                                           | Linux test image (Oracle Linux 9): CMake, GCC 13 and 11, Clang, the sanitizer runtimes, GDB, Valgrind. The "Test [Linux, x64]" build runs the C tests in it, see [Testing](.teamcity/README.MD#testing).                                                                |
| `docker/docs-check-linux-x64.Dockerfile`                                         | Image of the C API documentation check (Ubuntu 24.04, the official Doxygen release binary). The "Test [Linux, x64]" build checks the headers with `docs/Doxyfile.check` in it, see [Testing](.teamcity/README.MD#testing).                                              |
| `docker/graalvm-win-x64-v2.Dockerfile`                                           | Windows build image.                                                                                                                                                                                                                                                    |
| `docker/docker-entrypoint.cmd`                                                   | Entry point of the Windows build image: initializes the Visual Studio environment and runs the passed command.                                                                                                                                                          |
| `docker/graalvm-win-x64.Dockerfile`                                              | Previous Windows build image based on MSYS2 (not used by the CI).                                                                                                                                                                                                       |
| `docker/run-mvn-vs.ps1`                                                          | Initializes the Visual Studio environment (`VsDevCmd.bat`) and runs Maven with the passed arguments; `-SetupOnly` only initializes the environment.                                                                                                                     |
| `docker/build.ps1`                                                               | Diagnostic build in the Windows build image: prints the environment and runs `mvn clean package`.                                                                                                                                                                       |
| `docker/nuget.Dockerfile`                                                        | Image with the NuGet CLI to pack and publish `NuGet/DxFeed.Graal.Native.nuspec`.                                                                                                                                                                                        |
| `jira-sync/cxx_api_jira_sync.py`                                                 | Moves the MDAPI tickets of the C++ API through the Jira workflow by its GitHub pull requests and releases its Jira versions, see [Syncing the C++ API with Jira](.teamcity/README.MD#syncing-the-c-api-with-jira).                                                      |
| `jira-sync/sdk_jira_release.py`                                                  | Releases the Jira version of a GitHub release of the SDK after "Publish GitHub Release", see [Releasing the SDK in Jira](.teamcity/README.MD#releasing-the-sdk-in-jira).                                                                                                |
| `jira-sync/jira_sync_common.py`                                                  | The parts that both Jira scripts share: the REST clients of GitHub and Jira, the build log, the release mail and the dry run.                                                                                                                                           |
| `gh-pages/publish_c_api_docs.py`                                                 | Publishes the C API reference of a release to the branch `gh-pages` of the GitHub mirror (`<version>/`, `latest/`, the list of the versions), see [Publishing the C API Reference to GitHub Pages](.teamcity/README.MD#publishing-the-c-api-reference-to-github-pages). |

### To regenerate META-INF/native-image

`src/main/resources/META-INF/native-image` contains the metadata in both formats:
`reachability-metadata.json` (read by GraalVM 23+) and the legacy `*-config.json` files (read by all versions,
required for GraalVM < 23). GraalVM 23+ merges both.

After changing dependency versions (e.g. `qd.version`), collect the metadata with the native-image-agent:

```powershell
# JAVA_HOME must point to GraalVM 23+, Maven and Python 3 are required.
.\scripts\native-image-metadata\update-native-image-metadata.ps1
# Scenarios that need an address or credentials:
.\scripts\native-image-metadata\update-native-image-metadata.ps1 -MainClass com.dxfeed.NewCases,com.dxfeed.NativeLibMain -JavaArgs "-Dtoken=<TOKEN>"
```

The script runs the scenario classes (`src/test/java/com/dxfeed/NewCases.java` by default) under the agent,
accumulates the result in `reachability-metadata.json` and merges the new entries into the legacy files
(`merge-agent-metadata.py`, existing entries are never removed). Add a new case to `NewCases.java` to cover a new
feature, then review the diff. `-JavaArgs "-Dcases=<case1>,<case2>"` runs only the given cases.

`connectorPropertiesCase` sets every property of the QD connectors, codecs and file parameters the way QD does it
for `address[property=value]` (by reflection), so that all their setters get into the metadata, not only the used ones.

### To release a new version

The releases are built by the Release chain of
[TeamCity](https://dxcity.in.devexperts.com/project/Mdapi_DxfeedGraalNativeSdk), see
[.teamcity/README.MD](.teamcity/README.MD). Only the first build is run by hand; each next build starts by itself when
the one before it succeeds:

1. Run "Release (Major.Minor.Patch[-rcN])" with `env.RELEASE_VERSION` (`3.14.0`, `3.14.0-rc1`), or "Release The Next
   Patch\RC", which releases the version of `pom.xml` without `-SNAPSHOT` (`3.13.1-SNAPSHOT`: `3.13.1`). It tags the
   version and deploys the Linux x64 archives and the C API documentation.
2. "Build & Deploy [All]" builds and deploys the archives of the other platforms and the XCFramework from the tag.
3. "Pack NuGet" packs the `DxFeed.Graal.Native` package.
4. "Publish GitHub Release" pushes `main` and the tag to GitHub, publishes the release with the archives, the package
   and the notes of the version from `ReleaseNotes.md`, and publishes the package to nuget.org with a GitHub Actions
   workflow (see [Creating and Publishing NuGet Package](.teamcity/README.MD#creating-and-publishing-nuget-package)).
5. After it, "Release in Jira" releases the Jira version of the SDK and resolves its tickets (see
   [Releasing the SDK in Jira](.teamcity/README.MD#releasing-the-sdk-in-jira); a pre-release changes nothing there),
   and "Publish C API Docs to GitHub Pages" puts the C API reference of the release on
   [GitHub Pages](.teamcity/README.MD#publishing-the-c-api-reference-to-github-pages).

For a release (not a pre-release), the Linux x64 release build also makes the description of its headers the
baseline of the [API description](#api-description) check and commits it to `main` ("Update the API baseline to
vX.Y.Z"). If that step failed, run it by hand with the `java` of the GraalVM of the build (the generator is a
multi-file source program, which needs Java 22 or newer) and commit the result:

```shell
git fetch --tags
java scripts/check-api-headers/GenerateApiJson.java --update-baseline v3.13.0 scripts/check-api-headers/baseline/dxfg-api.json
```

Until the baseline is updated, the build warns that it is older than the last release in `ReleaseNotes.md`; it keeps
the `since` of the declarations from the old baseline.

#### Pre-releases

A pre-release (e.g. a release candidate) is released by "Release (Major.Minor.Patch[-rcN])" with a version with a
qualifier: `env.RELEASE_VERSION=3.6.0-rc1`.

- Use a qualifier that Maven orders before the release: `rc1`, `beta-1`, `alpha1`, `M1` (`3.6.0-rc1` < `3.6.0`).
  Maven orders unknown qualifiers such as `pre` or `draft` after the release (`3.6.0-pre` > `3.6.0`), so they look
  newer than the release in the repositories.
- After a pre-release, the release plugin increments the number of the qualifier for the next development version:
  `3.6.0-rc1` -> `3.6.0-rc2-SNAPSHOT` ("Release The Next Patch\RC" would release `3.6.0-rc2` then). Release the final
  version with "Release (Major.Minor.Patch[-rcN])" as well: `env.RELEASE_VERSION=3.6.0`, the next development version
  is `3.6.1-SNAPSHOT`.
- `ReleaseNotes.md`: the header of the new version replaces the headers of the pre-releases of the same version at
  the top (`## v3.6.0-rc2` replaces `## v3.6.0-rc1`, `## v3.6.0` replaces both), so the section of the version, and
  the notes of its GitHub release, list all the changes since the previous release. The GitHub release of a version
  with a qualifier is marked as a pre-release.
- Windows: the file version of `DxFeedGraalNativeSdk.dll` is `MAJOR.MINOR.PATCH.N`, where `N` is the number of the
  pre-release (the last number of the qualifier: `3.6.0-rc2` -> `3.6.0.2`), 0 for a release and for a qualifier
  without a number. The product version (the string) is the full version: `3.6.0-rc2`. Note that the file version of
  the release (`3.6.0.0`) is lower than the ones of its pre-releases.
- NuGet: the package `DxFeed.Graal.Native` of a version with a qualifier is a pre-release package, NuGet clients take
  it only when pre-releases are allowed (`-Prerelease`, `Version="3.*-*"`). NuGet compares the qualifiers as strings
  (`3.6.0-rc10` < `3.6.0-rc2`) and ignores their case (`rc1` and `RC1` are the same version). A qualifier after a dot
  (`3.6.0.rc1`) is not a valid NuGet version.

## Support

Our support team on
our [customer portal](https://jira.in.devexperts.com/servicedesk/customer/portal/1) is
ready to answer any questions and help with the transition.

## License

MPL-2.0