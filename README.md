
![DXFEED-GRAAL-NATIVE-SDK](./docs/images/logo_dark.svg#gh-dark-mode-only)
![DXFEED-GRAAL-NATIVE-SDK](./docs/images/logo_light.svg#gh-light-mode-only)

This package grants you access to [dxFeed market data](https://dxfeed.com/market-data/). The library
is designed as a C library and was
compiled using [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
and our flagship [dxFeed Java API](https://docs.dxfeed.com/dxfeed/api/overview-summary.html), making
it easily integrable
into your projects.

![qds](https://img.shields.io/badge/qds-v3.355-yellow)
![mdd](https://img.shields.io/badge/mdd-v548-red)
![Platform](https://img.shields.io/badge/platform-win--x64%20%7C%20linux--x64%20%7C%20linux--arm64%20%7C%20osx--x64%20%7C%20osx--arm64%20%7C%20ios%20%7C%20ios--simulator-lightgrey)
[![License](https://img.shields.io/badge/license-MPL--2.0-orange)](./LICENSE)

## Table of Contents

- [Overview](#overview)
  * [Reasons](#reasons)
  * [Benefits](#benefits)
  * [Future development](#future-development)
- [Installation](#installation)
  * [iOS and macOS](#ios-and-macos)
  * [Debug builds](#debug-builds)
- [Using the C API](#using-the-c-api)
  * [Conventions](#conventions)
  * [Example](#example)
  * [Samples and wrappers](#samples-and-wrappers)
  * [Isolates](#isolates)
  * [Listeners](#listeners)
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

| Archive                                          | Platform                                                                       |
|--------------------------------------------------|--------------------------------------------------------------------------------|
| `graal-native-sdk-<version>-amd64-linux.zip`     | Linux x64, glibc 2.17+                                                         |
| `graal-native-sdk-<version>-aarch64-linux.zip`   | Linux arm64, glibc 2.17+                                                       |
| `graal-native-sdk-<version>-amd64-windows.zip`   | Windows x64                                                                    |
| `graal-native-sdk-<version>-x86_64-osx.zip`      | macOS x64, 11.0+                                                               |
| `graal-native-sdk-<version>-aarch64-osx.zip`     | macOS arm64, 14.0+                                                             |
| `graal-native-sdk-<version>-aarch64-ios.zip`     | iOS arm64, 12.0+                                                               |
| `graal-native-sdk-<version>-ios-simulator.zip`   | iOS Simulator x64 (12.0+) and arm64 (14.0+)                                    |
| `graal-native-sdk-<version>-xcframework.zip`     | `DxFeedGraalNativeSdk.xcframework` for iOS, the iOS Simulator and macOS        |
| `graal-native-sdk-<version>-c-api-docs-html.zip` | The C API documentation (HTML)                                                 |

The Linux and Windows archives have `-debug` variants, see [Debug builds](#debug-builds).

An archive contains the library, the headers (`dxfg_api.h` includes the others) and the license. The desktop archives
also contain `CMakeLists.txt`, which imports the library as the CMake target `DxFeedGraalNativeSdk`:

```cmake
add_subdirectory(<the unpacked archive> graal-native-sdk)
target_link_libraries(<your target> PRIVATE DxFeedGraalNativeSdk)
```

The libraries for .NET are published to nuget.org as the package
[DxFeed.Graal.Native](https://www.nuget.org/packages/DxFeed.Graal.Native). Inside Devexperts, the archives are also in
the Maven repository `qd` of Nexus: `com.dxfeed:graal-native-sdk:<version>`, type `zip`, the classifier is the
platform of the archive name (`amd64-linux`, `xcframework`, etc.).

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
  `dxfg_JavaObjectHandler_release(thread, &object->handler)`. The other structures (events, symbols, instrument
  profiles, lists) are copies of the data of Java objects.
- **Memory.** What a function returns belongs to the caller, who releases it with the function that its description
  names: `dxfg_JavaObjectHandler_release` for handles, `dxfg_EventType_release` and `dxfg_CList_EventType_release`
  for events, `dxfg_String_release` for strings, etc. What the caller passes stays the caller's: the SDK copies what it
  needs during the call. The data passed to a callback (the events of a listener) belongs to the SDK and is released
  when the callback returns, so copy what is needed later.
- **Errors.** A function that returns a pointer returns `NULL` on error, the functions that return `int32_t` return
  `DXFG_EXECUTE_SUCCESSFULLY` (0) or `DXFG_EXECUTE_FAIL` (-1), and the ones that return `int64_t` return -1 on error.
  A boolean is an `int32_t`: 1 is true, 0 is false, -1 is an error. Results that are not returned are written to the
  parameters marked `DXFG_OUT`. A failed call stores the Java exception in the calling thread:
  `dxfg_get_and_clear_thread_exception_t` returns it (the class, the message, the stack trace and the cause) and
  clears it, `dxfg_Exception_release` releases it.
- **Deprecation.** The deprecated functions are marked `DXFG_DEPRECATED`: the compilers warn where they are used, and
  their descriptions name the replacements.

### Example

Prints the quotes of AAPL until Enter is pressed:

```c
#include <stdio.h>

#include "dxfg_api.h"

// Called on the threads of the SDK. The events are valid only during the call.
static void printQuotes(graal_isolatethread_t *thread, dxfg_event_type_list *events, void *userData) {
    (void)thread;
    (void)userData;

    for (int32_t i = 0; i < events->size; i++) {
        if (events->elements[i]->clazz == DXFG_EVENT_QUOTE) {
            const dxfg_quote_t *quote = (const dxfg_quote_t *)events->elements[i];

            printf("%s bid %g x %g, ask %g x %g\n", quote->market_event.event_symbol, quote->bid_price,
                   quote->bid_size, quote->ask_price, quote->ask_size);
        }
    }
}

// Prints and releases the exception of the last failed call on this thread.
static int printException(graal_isolatethread_t *thread) {
    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

    if (exception != NULL) {
        fprintf(stderr, "%s: %s\n", exception->class_name, exception->message != NULL ? exception->message : "");
        dxfg_Exception_release(thread, exception);
    }

    return 1;
}

int main(void) {
    graal_isolate_t *isolate = NULL;
    graal_isolatethread_t *thread = NULL;

    if (graal_create_isolate(NULL, &isolate, &thread) != 0) {
        return 1;
    }

    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(thread);
    if (endpoint == NULL ||
        dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300") != DXFG_EXECUTE_SUCCESSFULLY) {
        return printException(thread);
    }

    dxfg_feed_t *feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_subscription_t *subscription = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &printQuotes, NULL);
    if (feed == NULL || subscription == NULL || listener == NULL ||
        dxfg_DXFeedSubscription_addEventListener(thread, subscription, listener) != DXFG_EXECUTE_SUCCESSFULLY) {
        return printException(thread);
    }

    // The symbol is copied by the call, so it can be on the stack.
    dxfg_string_symbol_t symbol = {{STRING}, "AAPL"};
    if (dxfg_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) != DXFG_EXECUTE_SUCCESSFULLY) {
        return printException(thread);
    }

    printf("Press Enter to stop\n");
    getchar();

    // Releasing the last handle of the listener waits for its calls in progress: no calls after it.
    dxfg_DXFeedSubscription_close(thread, subscription);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &subscription->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);

    // Stops the endpoints that are still open, otherwise graal_tear_down_isolate waits for their threads.
    dxfg_system_close_all_and_await_termination(thread);
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
| Endpoints, subscriptions   | `DxEndpointSubscriptionCase` (`es`), `DxEndpointTimeSeriesSubscriptionCase` (`ets`), `DxEndpointMonitoringCase` (`em`), `DxLinkCase` (`dxl`) |
| Last events, promises      | `GetLastEventCase` (`gle`), `LastEventIfSubscribedCase` (`le`), `PromiseCase` (`p`), `PromisesAllOfCase` (`pao`), `IndexedEventsPromiseCase` (`iep`) |
| Models                     | `OrderBookModelCase` (`obm`), `IndexedEventModelCase` (`iem`), `TxIndexedEventModelCase` (`txiem`), `IndexedEventTxModelCase` (`ietxm`) |
| Historical data            | `OnDemandServiceCase` (`ods`), `HistoryEndpointCase` (`he`)                                                                            |
| Instrument profiles        | `LiveIpfCase` (`li`), `ReaderIpfCase` (`ri`), `InstrumentProfileReaderBench` (`ipf`), `InstrumentProfileFieldCase` (`ipfi`), `InstrumentProfileCustomFieldsCase` (`ipcf`) |
| Schedule                   | `ScheduleCase` (`sch`), `Schedule2Case` (`sch2`)                                                                                       |
| Glossary                   | `AdditionalUnderlyingsCase` (`au`), `CfiCase` (`cfi`), `PriceIncrementsCase` (`pi`)                                                    |
| Price levels (ORCS)        | `OrcsCase` (`orcs`)                                                                                                                    |
| System                     | `SystemPropertiesCase` (`sp`), `LoggingCase` (`l`), `ExceptionCase` (`ex`), `ExecutorBaseOnConcurrentLinkedQueueCase` (`eb`), `FinalizeListenerCase` (`fl`) |
| QDS tools                  | `Qds`: the QDS tools of the Java API (`DxfgClient qds connect demo.dxfeed.com:7300 Quote AAPL`)                                         |

The [C tests](src/main/c/tests) are small programs too. Our ready-made wrappers:

- .NET: [dxfeed-graal-net-api](https://github.com/dxFeed/dxfeed-graal-net-api)
- C++: [dxfeed-graal-cxx-api](https://github.com/dxFeed/dxfeed-graal-cxx-api)
- Swift: [dxfeed-graal-swift-api](https://github.com/dxFeed/dxfeed-graal-swift-api)
- Go: [dxfeed-graal-go-api](https://github.com/dxFeed/dxfeed-graal-go-api)

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
when the last handle of the listener is released (`dxfg_JavaObjectHandler_release`, the clones made by
`dxfg_JavaObjectHandler_clone` count) or by `dxfg_NativeListener_deactivate`. Both wait for the calls that are
in progress on other threads, so the user data can be freed right after they return. Do not hold the locks that
the listener's function takes while releasing it. A listener may be released or deactivated from its own callback.

QD may call a listener once more after `dxfg_DXEndpoint_close`, `dxfg_DXFeedSubscription_close` or
`dxfg_*_remove*Listener` returns, so remove the listener first and then release it. The state changes
of an endpoint are coalesced (e.g. `NOT_CONNECTED -> CONNECTED` without `CONNECTING`).

### Shutdown

The threads of an open endpoint do not stop on `Thread.interrupt()`, so `graal_tear_down_isolate` waits
for them forever while any endpoint is open. Before tearing down the isolate (or exiting the process):

1. Release the listeners (or deactivate them with `dxfg_NativeListener_deactivate`).
2. Call `dxfg_system_close_all_and_await_termination`: it closes all the endpoints (including the default ones
   of `dxfg_DXEndpoint_getInstance`, `dxfg_DXFeed_getInstance`, etc.) and instrument profile connections created
   through the SDK and waits for the termination of the endpoints.
3. Release the remaining handles and call `graal_tear_down_isolate`.

To find the threads that block the tear-down, pass `-XX:TearDownWarningSeconds=<seconds>` in
`graal_create_isolate_params_t.argv` (the debug builds use 10 seconds by default).

### Network activity

The QD monitoring started with the first endpoint includes the MARS plugin `TimeSyncTracker`, which sends UDP multicast
packets (`239.192.51.45:5145` by default) from every process to compare the clocks of the hosts. The SDK disables it
by default. To enable it, set the system property `com.dxfeed.sdk.TimeSyncTracker.enable` to `true` with
`dxfg_system_set_property` before creating the first endpoint.

## Documentation

The C API reference (generated from the headers by Doxygen) is attached to every
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
- [x] IndexedTxModel and TimeSeriesTxModel (`com.dxfeed.api.experimental.model`) are the models of the indexed
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

- GraalVM `graalvm.version` of `pom.xml` (now GraalVM 25 Innovation 4, `graal-25.4.4.1.1`) in `JAVA_HOME`. The
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
  `PATH`);
- `-DmacIos=true` on a Mac with Apple silicon: the iOS library. The iOS Simulator library is built after it without
  `clean`, with the x64 GraalVM under Rosetta: `arch -x86_64 mvn -DmacIosSimulator=true package` (its arm64 slice is
  the object of the iOS build). The JDK and Substrate VM libraries for iOS are in [jre-ios](jre-ios/README.MD).
- `-DwindowsStaticRuntime=true` on Windows: `DxFeedGraalNativeSdk.dll` is linked with the static C runtime (`/MT`) and
  does not need the Visual C++ Redistributable (no `VCRUNTIME140.dll`, `api-ms-win-crt-*.dll`); the archive is
  `amd64-windows-static-mt` (`amd64-windows-debug-static-mt` with `-P buildDebug`). The JDK libraries of GraalVM are
  built with `/MD`, so [New-CrtShim.ps1](scripts/windows-static-runtime/New-CrtShim.ps1) assembles the pointers of
  their CRT imports to the static CRT (`dumpbin` and `ml64` of the MSVC environment).

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

| Script                                           | Purpose                                                                                                                                                                                                                                                   |
|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `scripts/rc.cmd`                                 | Wrapper for the Windows resource compiler used by the `windows` Maven profile to compile `version.rc` (version info of `DxFeedGraalNativeSdk.dll`). If `rc.exe` is not in `PATH`, it initializes the Visual Studio environment (`vcvars64.bat`).          |
| `scripts/windows-static-runtime/New-CrtShim.ps1` | Assembles the pointers of the CRT imports of the JDK libraries of GraalVM to the static CRT, so that the `windowsStaticRuntime` Maven profile links `DxFeedGraalNativeSdk.dll` without the Visual C++ Redistributable (`/MT`), see [Building](#building). |
| `src/main/c/build.cmd`, `src/main/c/build.sh`    | Configure, build, test (ctest), install and pack the C/C++ samples (`DxfgClient`) and tests against the library in `target/native-image`.                                                                                                                 |
| `jre-ios/build.py`                               | Builds the JRE libraries for iOS, declares the platform of the iOS images, checks that they link and assembles the XCFramework, see [jre-ios](jre-ios/README.MD).                                                                                         |

**CI (`.teamcity`)**, see also [.teamcity/README.MD](.teamcity/README.MD)

| Script                                                                           | Purpose                                                                                                                                                                                                            |
|----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `settings.kts`                                                                   | TeamCity project configuration (Kotlin DSL).                                                                                                                                                                       |
| `Common.kt`                                                                      | Its shared parts: the Maven options with the credentials, the Docker images, the steps and the agents.                                                                                                             |
| `scripts/install.sh`, `scripts/install.ps1`                                      | Download and install Maven, GraalVM (see the [supported version formats](.teamcity/README.MD#graalvm-versions)) and Build Tools for Visual Studio. Used by the Dockerfiles and the macOS and Windows builds.       |
| `docker/graalvm-linux-x64.Dockerfile`, `docker/graalvm-linux-aarch64.Dockerfile` | Linux build images (Oracle Linux 7, glibc 2.17).                                                                                                                                                                   |
| `docker/cpp-test-linux-x64.Dockerfile`                                           | Linux test image (Oracle Linux 9): CMake, GCC 13 and 11, Clang, the sanitizer runtimes, GDB, Valgrind. The "Test [Linux, x64]" build runs the C tests in it, see [Testing](.teamcity/README.MD#testing).           |
| `docker/graalvm-win-x64-v2.Dockerfile`                                           | Windows build image.                                                                                                                                                                                               |
| `docker/docker-entrypoint.cmd`                                                   | Entry point of the Windows build image: initializes the Visual Studio environment and runs the passed command.                                                                                                     |
| `docker/graalvm-win-x64.Dockerfile`                                              | Previous Windows build image based on MSYS2 (not used by the CI).                                                                                                                                                  |
| `docker/run-mvn-vs.ps1`                                                          | Initializes the Visual Studio environment (`VsDevCmd.bat`) and runs Maven with the passed arguments; `-SetupOnly` only initializes the environment.                                                                |
| `docker/build.ps1`                                                               | Diagnostic build in the Windows build image: prints the environment and runs `mvn clean package`.                                                                                                                  |
| `docker/nuget.Dockerfile`                                                        | Image with the NuGet CLI to pack and publish `NuGet/DxFeed.Graal.Native.nuspec`.                                                                                                                                   |
| `jira-sync/cxx_api_jira_sync.py`                                                 | Moves the MDAPI tickets of the C++ API through the Jira workflow by its GitHub pull requests and releases its Jira versions, see [Syncing the C++ API with Jira](.teamcity/README.MD#syncing-the-c-api-with-jira). |

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

The releases are built in [TeamCity](https://dxcity.in.devexperts.com/project/Mdapi_DxfeedGraalNativeSdk), see
[.teamcity/README.MD](.teamcity/README.MD):

1. Run "Build PATCH & Deploy [Linux, x64]" to release the next patch version, or "Build MAJOR.MINOR.PATCH & Deploy
   [Linux, x64]" with `env.RELEASE_VERSION` (`3.9.0`, `3.9.0-rc1`) to release another one. It tags the version and
   deploys the Linux x64 archives and the C API documentation.
2. Then "Build & Deploy [All]" builds and deploys the archives of the other platforms and the XCFramework from the
   tag.
3. Then "Deploy NuGet" publishes the `DxFeed.Graal.Native` package, and "Publish GitHub Release" pushes `main` and
   the tag to GitHub and publishes the release with the archives and the notes of the version from
   `ReleaseNotes.md`.

#### Pre-releases

A pre-release (e.g. a release candidate) is released by "Build MAJOR.MINOR.PATCH & Deploy [Linux, x64]" with a
version with a qualifier: `env.RELEASE_VERSION=3.6.0-rc1`.

- Use a qualifier that Maven orders before the release: `rc1`, `beta-1`, `alpha1`, `M1` (`3.6.0-rc1` < `3.6.0`).
  Maven orders unknown qualifiers such as `pre` or `draft` after the release (`3.6.0-pre` > `3.6.0`), so they look
  newer than the release in the repositories.
- After a pre-release, the release plugin increments the number of the qualifier for the next development version:
  `3.6.0-rc1` -> `3.6.0-rc2-SNAPSHOT` ("Build PATCH & Deploy [Linux, x64]" would release `3.6.0-rc2` then). Release
  the final version with "Build MAJOR.MINOR.PATCH & Deploy [Linux, x64]" as well: `env.RELEASE_VERSION=3.6.0`, the next
  development version is `3.6.1-SNAPSHOT`.
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