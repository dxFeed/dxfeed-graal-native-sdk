
![DXFEED-GRAAL-NATIVE-SDK](./docs/images/logo_dark.svg#gh-dark-mode-only)
![DXFEED-GRAAL-NATIVE-SDK](./docs/images/logo_light.svg#gh-light-mode-only)

This package grants you access to [dxFeed market data](https://dxfeed.com/market-data/). The library
is designed as a С-library and was
compiled using [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
and our flagship [dxFeed Java API](https://docs.dxfeed.com/dxfeed/api/overview-summary.html), making
it easily integrable
into your projects.

![Platform](https://img.shields.io/badge/platform-win--x64%20%7C%20linux--x64%20%7C%20osx--x64%20%7C%20osx--arm64%20%7C%20ios-lightgrey)
[![License](https://img.shields.io/badge/license-MPL--2.0-orange)](https://github.com/dxFeed/dxfeed-graal-native-sdk/blob/master/LICENSE)

## Table of Contents

- [Overview](#overview)
  * [Reasons](#reasons)
  * [Benefits](#benefits)
  * [Future development](#future-development)
  * [Implementation details and usage](#Implementation-details-and-usage)
- [Installation](#installation)
- [Current State](#current-state)
- [Contribution](#Contribution)
- [Support](#Support)
- [License](#License)

## Overview

### Reasons

Our Java API serves as the cornerstone of our technology, and with our SDK, you can seamlessly
integrate it into any language, leveraging it as a native library, even on IOS platforms.

### Benefits

- :rocket: Increased performance
- :milky_way: Wider functionality
- :gemini: Identical programming interfaces to our best API
- :thumbsup: Higher quality of support and service

### Future development

Our team is committed to continuously improving the library by regularly releasing updates to our
Java API and implementing them in the native library.

### Implementation details and usage

To maintain consistency, we've established a naming convention for C functions that involves using
the dxfg prefix followed by the java-class and method name. However, there may be situations where
exceptions to this rule are necessary, such as when dealing with service functions or using generic
names for java-class names. If a method name can be used for different types of arguments, we add a
number to the end. It also helps to find the appropriate java-doc to get more information about the
use of a particular function.

C-structures can be classified into two categories: those that serve as reference holders for Java
objects and those that act as field containers for data transferred between Java and C. Reference
holders are only necessary for invoking methods of the associated Java object through corresponding
C functions. Furthermore, it is ensured that the garbage collector will not collect the Java object
until the appropriate release method for the reference holder is called.

When a C function returns a pointer to a c-structure, it indicates that the object's memory was
allocated in Java. It is essential to release this memory using the corresponding release method
after using the object. All memory allocated in C should be released in C.

It's important to keep in mind that if an object is passed to C code as a function parameter, it
will be automatically released after the function is executed. Additionally, when a listener is
added to dxFeed, it will be called from Java threads and the C code may be notified of its removal
in Java. To account for this, a finalizer function can be passed when adding a listener. This
ensures that the finalizer function will be called when the listener is no longer needed.

In case of any exceptions occurring when calling the method, the method returns a null-pointer (in
case of returned pointer type) or -1 (in case of returned int and long type). To retrieve and clear
an exception from the thread-local variable, use the corresponding method:
dxfg_get_and_clear_thread_exception_t.

To return boolean values, we use int where 0 represents false and 1 represents true. The value -1 is
reserved for exceptions in both int and long types.

```
    dxfg_endpoint_t* endpoint = dxfg_DXEndpoint_create(thread);
    dxfg_DXEndpoint_connect(thread, endpoint, "demo.dxfeed.com:7300");
    dxfg_feed_t* feed = dxfg_DXEndpoint_getFeed(thread, endpoint);
    dxfg_subscription_t* subscriptionQuote = dxfg_DXFeed_createSubscription(thread, feed, DXFG_EVENT_QUOTE);
    dxfg_feed_event_listener_t *listener = dxfg_DXFeedEventListener_new(thread, &c_print, nullptr);
    dxfg_DXFeedSubscription_addEventListener(thread,subscriptionQuote, listener, finalize, nullptr);
    dxfg_string_symbol_t symbolAAPL;
    symbolAAPL.supper.type = STRING;
    symbolAAPL.symbol = "AAPL";
    dxfg_DXFeedSubscription_setSymbol(thread, subscriptionQuote, &symbolAAPL.supper);
    usleep(2000000);
    dxfg_DXEndpoint_close(thread, endpoint);
    dxfg_JavaObjectHandler_release(thread, &subscriptionQuote->handler);
    dxfg_JavaObjectHandler_release(thread, &listener->handler);
    dxfg_JavaObjectHandler_release(thread, &feed->handler);
    dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
```

There are the following usage [examples](src/main/c/src/apps/DxfgClient/main.cpp):

* live IPF
* reader IPF
* finalize Listener
* executor Base On Concurrent Linked Queue
* dxEndpoint Subscription
* dxEndpoint TimeSeriesSubscription
* system Properties
* exception
* order Book Model
* indexed Event Model
* promise
* last Event If Subscribed
* promises All Of
* indexed Events Promise
* get Last Event
* schedule

Or our ready-made wrappers:

* https://github.com/dxFeed/dxfeed-graal-net-api

## Installation

You can find artifacts here:

* https://dxfeed.jfrog.io/artifactory/maven-open/com/dxfeed/graal-native-sdk/
* https://dxfeed.jfrog.io/artifactory/nuget-open/com/dxfeed/graal-native/

## Documentation

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
  application-specific attachment
- [x] [Message](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/misc/Message.html) is an event
  with an
  application-specific attachment

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

### Services

- [ ] [OnDemandService](https://docs.dxfeed.com/dxfeed/api/com/dxfeed/ondemand/OnDemandService.html)
  provides on-demand
  historical tick data replay
  controls ([Java API sample](https://github.com/devexperts/QD/blob/master/dxfeed-samples/src/main/java/com/dxfeed/sample/ondemand/OnDemandSample.java))

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

### Installation GraalVM JDK 11 with native-image

[GraalVM Community Edition 22.3.1](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-22.3.1)

[Install GraalVM](https://www.graalvm.org/22.3/docs/getting-started/#install-graalvm)

[Install Native Image](https://www.graalvm.org/22.3/docs/getting-started/#native-image)

### Settings IntelliJ Idea

* File | Project structure | SDKs - add JDK graalvm-11
* File | Project structure | Project / SDK - choose JDK graalvm-11
* Preferences | Build, Execution, Deployment | Build Tools | Maven | Importing | JDK for importer -
  choose JDK graalvm-11
* Preferences | Build, Execution, Deployment | Build Tools | Maven | Runner | JRE - choose JDK
  graalvm-11

### To regenerate META-INF/native-image

add a new case to src/test/java/com/dxfeed/NativeLibMain.java and execute it

```shell
export M2_HOME=~/.m2
$JAVA_HOME/bin/java -Dtoken=<TOKEN> -agentlib:native-image-agent=config-output-dir=META-INF/native-image -Dfile.encoding=UTF-8 -classpath ./target/test-classes:./target/classes:$M2_HOME/repository/com/devexperts/qd/qds/3.312/qds-3.312.jar:$M2_HOME/repository/com/devexperts/qd/dxlib/3.312/dxlib-3.312.jar:$M2_HOME/repository/com/devexperts/qd/dxfeed-api/3.312/dxfeed-api-3.312.jar:$M2_HOME/repository/com/devexperts/mdd/auther-api/441/auther-api-441.jar:$M2_HOME/repository/com/devexperts/qd/qds-tools/3.313/qds-tools-3.313.jar:$M2_HOME/repository/com/devexperts/qd/qds-file/3.313/qds-file-3.313.jar:$M2_HOME/repository/org/graalvm/sdk/graal-sdk/22.1.0/graal-sdk-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/svm/22.1.0/svm-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/objectfile/22.1.0/objectfile-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/pointsto/22.1.0/pointsto-22.1.0.jar:$M2_HOME/repository/org/graalvm/compiler/compiler/22.1.0/compiler-22.1.0.jar:$M2_HOME/repository/org/graalvm/truffle/truffle-api/22.1.0/truffle-api-22.1.0.jar:$M2_HOME/repository/org/graalvm/nativeimage/native-image-base/22.1.0/native-image-base-22.1.0.jar com.dxfeed.NativeLibMain

```

### To release a new version

We use [teamcity](https://dxcity.in.devexperts.com/project/Eugenics_DxfeedGraalNativeApi).

1. Run the "build PATCH and deploy linux" configuration to release a version of PATCH or "build
   MAJOR.MINOR.PATCH and deploy linux" (set env.RELEASE_VERSION in run parameters) to release a
   version of MAJOR or MINOR. A linux version of the artifact will also be deployed.
2. The "deploy osx", "deploy windows", "deploy nuget" builds will start automatically, after which
   you can see the new artifacts
   in [artifactory](https://dxfeed.jfrog.io/ui/repos/tree/General/maven-open/com/dxfeed/graal-native-api).
   The "deploy osx"
   configuration builds and deploys artifacts under amd, arm architecture osx and arm under IOS.

#### IOS

deploy

```shell
mvn -DmacIos=true deploy
```

## Support

Our support team on
our [customer portal](https://jira.in.devexperts.com/servicedesk/customer/portal/1/create/122) is
ready to answer any questions and help with the transition.

## License

MPL-2.0