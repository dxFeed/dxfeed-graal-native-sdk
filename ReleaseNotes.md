* **\[MDAPI-478]\[GRAAL]** The message connectors of an endpoint (`dxfg_v4_message_connector.h`): what an endpoint
  is connected to and how much data flows, as `com.devexperts.qd.qtp.MessageConnector` reports it.
  `dxfg_v4_DXEndpoint_getConnectors()` returns the current connectors, one per address of the address string, released
  by `dxfg_v4_message_connector_list_release()`. A connector reports its name, kind (`DXFG_V4_MESSAGE_CONNECTOR_KIND_*`:
  a client or a server socket, or another), configured address with the credentials masked, the addresses it is
  connected to now, its state (`DXFG_V4_MESSAGE_CONNECTOR_STATE_*`: connecting, connected, disconnected), whether it is
  active, its open and closed connection counts, and its statistics (`dxfg_v4_MessageConnector_getStatistics()`: bytes
  and data records read and written, without resetting them). A connector listener (experimental:
  `dxfg_v4_MessageConnectorListener_new()`, `dxfg_v4_DXEndpoint_addMessageConnectorListener()`,
  `dxfg_v4_DXEndpoint_removeMessageConnectorListener()`) is called after the state changes, also of the connectors of
  a later connect, on one thread of the SDK; `dxfg_v4_system_shutdown()` stops that thread.
  `DXFG_V4_FEATURE_MESSAGE_CONNECTORS` reports the family.
* **\[MDAPI-477]\[GRAAL]** The last connection error of a message connector and of an endpoint, and `log.level` that
  really sets the level of logging.
  * The last connection error (experimental, `dxfg_v4_message_connector.h`): why a connect failed or a connection
    dropped. `dxfg_v4_MessageConnector_getLastError()` and `dxfg_v4_DXEndpoint_getLastConnectionError()` return a copy
    (`dxfg_v4_connection_error_t`, released by `dxfg_v4_ConnectionError_release()`) or NULL: the time, the configured
    address with the credentials masked, the category (`DXFG_V4_CONNECTION_ERROR_*`: DNS, connect failed (refused or
    timed out), read timeout, heartbeat timeout, closed by remote, TLS, I/O, other), whether an established connection
    dropped, the exception class name, a bounded message and the error count. The endpoint keeps its newest error
    after a connect to another address; a successful connection clears nothing. The error is recorded before the
    connector listener is called. The SDK captures the errors from QD's log records, without the logging listener and
    at any `log.level`; with another `log.className` nothing is captured, and a level the user sets on QD's connector
    loggers wins. `DXFG_V4_FEATURE_CONNECTION_ERRORS` reports the family.
  * `log.level`, `err.level`, `dxfg_logging_set_log_level()` and `dxfg_logging_set_err_level()` (and their v4
    functions) also set the level of the root logger of `java.util.logging` when `log.className` is
    `com.devexperts.logging.InterceptableLogging`: `DEBUG` and `ALL` now produce the messages below `INFO`, and the
    messages below the level are no longer created. The default `log.level` is `INFO`, which was already the effective
    default.

### For integrators

* Check `DXFG_V4_FEATURE_MESSAGE_CONNECTORS` with `dxfg_v4_system_is_feature_supported()` before binding the connector
  functions. Bind the experimental listener functions only when `dxfg_v4_system_get_advanced_revision()` equals
  `DXFG_V4_ADVANCED_REVISION` of the headers the wrapper is built with.
* `dxfg_v4_message_connector_list_release()` releases the list with all its handles; clone a connector with
  `dxfg_v4_JavaObjectHandler_clone()` to keep it. The handles stay valid after the endpoint is closed. A connect to
  another address replaces the connectors, so read the list anew. The connector handle passed to a listener is valid
  during the call only.
* For a status display, reading the connectors on a timer is enough. The listener is called on one thread of the SDK
  for the whole isolate, one call at a time; fast changes of a connector may come as one call, so the listener reads
  the current state. It should return quickly and may call any function of the SDK, `dxfg_v4_DXEndpoint_connect()`
  included. Remove a listener before releasing it.
* Data arrives when `read_data_records` grows between two reads of the statistics; `read_bytes` grows on heartbeats
  too. The counters are never reset: compute the rates from the differences. `dxfg_v4_message_connector_stats_t`
  keeps its layout; new counters will come with a new function.
* In the explicit shutdown of the application, call `dxfg_v4_system_shutdown()` first (it also stops the listener
  thread), then release the remaining handles.
* The README section "Message connectors" has an example, and `DxfgClient mc` prints the connectors of an endpoint.
* Check `DXFG_V4_FEATURE_CONNECTION_ERRORS` and the advanced revision before binding the connection error functions.
  Treat an unknown category as `DXFG_V4_CONNECTION_ERROR_OTHER`: new categories may come. Release every error with
  `dxfg_v4_ConnectionError_release()`. Read the error in the connector listener or after a state other than
  connected; its time tells how old it is.
* An application that set `log.level` to `DEBUG` or `ALL` with `InterceptableLogging` now gets the debug messages it
  asked for; set `INFO` to keep the output as before.

## v3.13.0

* **\[MDAPI-466]\[GRAAL]** The event queue (`dxfg_v4_event_queue.h`): the events of a subscription received on the
  application's own thread, without callbacks. `dxfg_v4_EventQueue_new()` creates a queue on a subscription (on one
  attached to a feed, before its symbols are added, as for a listener); `dxfg_v4_EventQueue_poll()` waits up to a
  timeout and returns the events as a `dxfg_v4_event_type_list`, or the positive codes `DXFG_EVENT_QUEUE_NO_EVENTS`
  and `DXFG_EVENT_QUEUE_CLOSED`;
  `dxfg_v4_EventQueue_wakeUp()`, `dxfg_v4_EventQueue_close()` and `dxfg_v4_EventQueue_getDroppedEvents()` complete it.
  The SDK retrieves the events only when the queue is polled, on the polling thread, so the lasting events are
  conflated and the buffers of the SDK bound the memory; the lost events are counted. `dxfg_v4_system_shutdown()` and
  `dxfg_v4_NativeListener_deactivateAll()` close the queues, so the polling threads can leave the SDK before
  `graal_tear_down_isolate()`. The executor of a subscription with an event queue cannot be changed:
  `dxfg_v4_DXFeedSubscription_setExecutor()`, `dxfg_v4_SubscriptionController_setExecutor()` and their older functions
  fail with `IllegalStateException`. `dxfg_v4_DXFeed_detachSubscriptionAndClear()` does not wait for a poll of the
  queue of the subscription.
* **\[MDAPI-468]\[GRAAL]** The shared event queue: one thread polls many subscriptions and runs the models.
  `dxfg_v4_EventQueue_create()` creates a queue without a subscription, `dxfg_v4_EventQueue_attach()` attaches a
  subscription with an `int64_t` tag, and `dxfg_v4_EventQueue_pollTagged()` returns a `dxfg_v4_tagged_event_list` (the
  events and a parallel array of their tags, released by `dxfg_v4_tagged_event_list_release()`); a subscription close
  detaches only that subscription. `dxfg_v4_EventQueue_getExecutor()` returns the queue's executor for the models, the
  instrument profile collector, the subscription and controller setters, the endpoint builder and the promise handlers:
  their listeners run inside the polls on the polling thread, and a poll that ran only their work returns
  `DXFG_EVENT_QUEUE_NO_EVENTS` at once. A closed queue calls no listener of the application, also from the work it
  still runs for the SDK. A queue made by `dxfg_v4_EventQueue_new()` stays bound to its subscription and rejects
  `attach` and `getExecutor`. The executor of an attached subscription is refused by every function that accepts an
  executor (`IllegalStateException`); `dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks()` fails with
  `IllegalArgumentException` for an executor it did not create, instead of a `ClassCastException`.
* **\[MDAPI-482]\[GRAAL]** Stable and experimental functions. A stable function changes only by deprecation and is
  removed only in a major release; an experimental one may change or be removed in a minor release, and an incompatible
  change gives it a new name. The experimental functions are marked `DXFG_EXPERIMENTAL` (`dxfg_common.h`, empty unless
  the application defines it first, e.g. as a deprecation attribute) and listed on the "Experimental declarations" page
  of the C API reference: the twins of the experimental Java API (`dxfg_v4_indexed_event_tx_model.h`,
  `dxfg_v4_subscription_controller.h`, `dxfg_v4_DXFeedSubscription_getSubscriptionController()`,
  `dxfg_v4_DXFeedSubscription_getTotalDroppedEvents()`, the `IndexedTxModel`, `TimeSeriesTxModel` and `TxModelListener`
  functions of `dxfg_v4_event_model.h`). These 73 functions came out in 3.12.0 with no tier and are experimental from
  now on; their older twins stay until 4.0.0 as the other deprecated functions do, and their deprecation messages say
  that the replacement is experimental. Later advanced headers with experimental functions only are not included by
  `dxfg_v4_api.h`; the header check of the build enforces the boundary. The Nuam and `OrderImbalance` event types are
  experimental in their descriptions. `dxfg_v4_system_get_version()`, `dxfg_v4_system_get_advanced_revision()`
  (`DXFG_V4_ADVANCED_REVISION`, now 1) and `dxfg_v4_system_is_feature_supported()` (`DXFG_V4_FEATURE_EVENT_QUEUE`,
  `DXFG_V4_FEATURE_SHARED_EVENT_QUEUE`) let a wrapper check the library before it binds the other functions.
* **\[MDAPI-483]\[GRAAL]** The description of the C API for wrapper generators: every archive has `dxfg-api.json` next
  to the library, generated from the headers and the same on every platform. It lists the version, the advanced
  revision, the features, the headers with their tiers and the functions, types and macros with their tiers and the
  versions that added them; a function has its return type, its parameters with `DXFG_OUT`, the functions that
  release its results and its `DXFG_DEPRECATED` replacements. The build checks the headers against the committed
  description of 3.12.0: a stable declaration is not changed or removed, an experimental one changes only with a higher
  `DXFG_V4_ADVANCED_REVISION`, and a new declaration needs `@since`. See "API description" in the README.
* **\[MDAPI-484]\[GRAAL]** The library no longer prints the JDK warning about `sun.misc.Unsafe` ("A terminally
  deprecated method in sun.misc.Unsafe has been called", from `com.devexperts.util.AtomicArraysUnsafe` of QD) to
  stderr of the application: the image is built with `--sun-misc-unsafe-memory-access=allow`.
* **\[MDAPI-485]\[GRAAL]** The ready signal of an event queue: an event loop polls without a reader thread.
  `dxfg_v4_EventQueue_setReadySignal()` (experimental, `dxfg_v4_event_queue.h`) gives a queue an OS object of the host,
  which the SDK makes ready whenever the queue has work (the events of a subscription, the work of a model or the
  collector, a wake-up, the close): a non-blocking file descriptor on Linux, macOS and iOS
  (`DXFG_V4_READY_SIGNAL_FD`, an `eventfd` or the write end of a pipe; a blocking or read-only descriptor is refused
  with `IllegalArgumentException`), an event on Windows (`DXFG_V4_READY_SIGNAL_WINDOWS_EVENT`). The loop waits for the
  object, resets it and polls with the timeout 0; no work is left without a signal, and a signal without work gives
  `DXFG_EVENT_QUEUE_NO_EVENTS`. The host creates, owns and closes the object, after the queue is closed; the SDK never
  closes it and does not write to it after the signal is removed or the queue is closed.
  `DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL` reports the function; `DXFG_V4_ADVANCED_REVISION` stays 1. A loop that
  cannot wait for a foreign object (the asyncio proactor loop and libuv on Windows) keeps a reader thread.

* **\[MDAPI-488]\[GRAAL]** A promise created, completed and failed from C (`dxfg_v4_feed.h`):
  `dxfg_v4_Promise_new()` creates a promise, `dxfg_v4_Promise_complete()` completes it with a Java object handle (or
  NULL) and `dxfg_v4_Promise_getResult()` reads the result of a promise of any kind as a handle.
  `dxfg_v4_Promise_completeExceptionallyWithClassNameAndMessage()` fails a promise and
  `dxfg_v4_Promise_failedWithClassNameAndMessage()` creates a failed one, with an exception of the named class and the
  message. The class comes from a fixed set (`RuntimeException`, `IllegalArgumentException`, `IllegalStateException`,
  `UnsupportedOperationException`, `java.io.IOException`, `CancellationException`, `TimeoutException`, by their fully
  qualified names); any other name gives a `RuntimeException` whose message starts with the name. As in Java, the first
  result wins: a later completion does nothing and returns success. `dxfg_Promise_failed()`,
  `dxfg_v4_Promise_failed()` and the `completeExceptionally` functions that take a `dxfg_exception_t` keep their
  behaviour; their descriptions point to the new functions.
* **\[MDAPI-491]\[GRAAL]** The instrument profile writer (`dxfg_v4_ipf.h`):
  `dxfg_v4_InstrumentProfileWriter_writeToFile()` writes a list of profile structures to a file and
  `dxfg_v4_InstrumentProfileWriter_writeToBytes()` to memory, as a new `dxfg_v4_byte_list` (`dxfg_v4_javac.h`, released by `dxfg_v4_byte_list_release()`, never NULL on success);
  `writeHandlesToFile()` and `writeHandlesToBytes()` do the same for a list of profile handles. As in Java, the name
  selects plain text, `.gz` or `.zip`. The custom fields of the structures are written: the header lists their names.
  A NULL argument or list element fails with `IllegalArgumentException` before anything is written, data over
  `Integer.MAX_VALUE - 8` bytes with `IllegalStateException`. The description of `dxfg_instrument_profile2_t` no
  longer says that the handle of its custom fields always shares them with the profile: a field added through the
  handle may move them to a new storage.

* **\[MDAPI-489]\[GRAAL]** Four members of the Java API that the SDK did not export, and the exceptions of the ORCS
  server with their classes. The new listeners stop on release and on `dxfg_v4_NativeListener_deactivateAll()`.
  * `dxfg_v4_ObservableSubscriptionChangeListener_newWithConfigurationChanged()` (`dxfg_v4_events.h`) creates a
    subscription change listener with the `configurationChanged` callback (called, for example, when the aggregation
    period changes); each of its four callbacks may be NULL.
  * `dxfg_v4_DXFeedTimeSeriesSubscription_getFromTime()` returns the time that
    `dxfg_v4_DXFeedTimeSeriesSubscription_setFromTime()` set.
  * `dxfg_v4_AuthOrderSource_create()` creates an AuthOrderSource from the entries of identifiers and symbols; a NULL
    entry, symbol list or symbol and a duplicate identifier fail the call.
  * `dxfg_v4_OnDemandPropertyChangeListener_new()`, `dxfg_v4_OnDemandService_addPropertyChangeListener()` and
    `dxfg_v4_OnDemandService_removePropertyChangeListener()` report the property changes of an on-demand service
    (`replaySupported`, `replay`, `clear`, `speed`, `time`) with the old and the new values, each with its kind
    (`DXFG_V4_ON_DEMAND_PROPERTY_VALUE_NULL` for null).
  * The `PriceLevelService` functions (also the older `dxfg_PriceLevelService_*`) report the exceptions of the ORCS
    server with their classes: an unsupported order source gave an `UndeclaredThrowableException` over
    `java.lang.Exception` and now gives `IllegalArgumentException`, as in Java. The image registers for serialization
    the classes of the serialization whitelist of ORCS.

* **\[MDAPI-492]\[GRAAL]** The feed filter of QD (`DXFeedFilter`, `dxfg_v4_feed_filter.h`, experimental): a filter
  of named categories, each a QD filter expression (a list, a pattern, a range, an `ipf[...]` file), built with
  `dxfg_v4_DXFeedFilter_newBuilder()`, `_Builder_withCategory()`, `_Builder_withCategories()` and `_Builder_build()`
  for an endpoint. It accepts subscriptions and events (`_accept()`, `_acceptEvent()`) and names their first matching
  category (`_getCategory()`, `_getEventCategory()`, NULL when none matches). A dynamic filter (an expression with an
  update period) has a tracker (`dxfg_v4_DXFeedFilterTracker_*`) with the current version and listeners
  (`dxfg_v4_DXFeedFilterListener_new()`), called on a thread of the SDK; a listener may add, remove or release
  listeners from its call. `DXFG_V4_FEATURE_FEED_FILTER` reports the family.

* **\[MDAPI-490]\[GRAAL]** Day and session filters with user data, and an event peek of the time series model that
  works. `dxfg_v4_DayFilter_newWithUserData()` and `dxfg_v4_SessionFilter_newWithUserData()` (`dxfg_v4_schedule.h`)
  take the conditions of the Java constructors and a function with `void *userData`: a day or a session is accepted
  when it passes the conditions and the function returns 1. A condition of `DXFG_V4_FILTER_ANY` (-1) is not checked;
  a flag other than -1, 0 or 1 or an unknown session type fails with `IllegalArgumentException`. The function gets a
  handle that the SDK releases after the call. The filters are plain Java objects:
  `dxfg_v4_NativeListener_deactivateAll()` does not stop them.
  `dxfg_v4_TimeSeriesTxModel_Builder_withEventPeekFunction()` (experimental, `dxfg_v4_event_model.h`) calls a function
  for each event before the listener gets the list, on the model's executor; NULL removes it, and it is not called
  inside the tasks of a closed event queue or after `deactivateAll`.
  `DXFG_V4_FEATURE_TIME_SERIES_TX_MODEL_EVENT_PEEK` reports it. `dxfg_TimeSeriesTxModel_Builder_withEventPeek()` and
  its v4 twin stay no-ops.

## v3.12.0

* **\[MDAPI-458]\[GRAAL]** The `dxfg_v4_` functions: the operations of the C API in one form. A v4 function returns the
  `int32_t` status only (`DXFG_EXECUTE_SUCCESSFULLY` or `DXFG_EXECUTE_FAIL`), writes its result to the last parameter,
  marked `DXFG_OUT` (a NULL result means "no value", never an error), returns collections as `dxfg_v4_<element>_list`
  with a `_release` function, and its name has no digit suffix: an overload is named by its parameters. Its types are
  `dxfg_v4_` typedefs of the older ones, so the older and the v4 functions can be mixed. The headers are
  `dxfg_v4_<area>.h`, all included by `dxfg_v4_api.h` and `dxfg_api.h`; the rules are in the "The v4 functions" group of
  the C API reference. The prefix names a generation of the C API, not a release of the SDK: later releases, major
  ones included, keep it until these rules change. This release adds the twins of:
  * `dxfg_free()` (`dxfg_common.h`) and the functions of `dxfg_javac.h`, `dxfg_catch_exception.h`, `dxfg_endpoint.h`,
    `dxfg_feed.h`, `dxfg_subscription.h`, `dxfg_publisher.h` and `dxfg_events.h`, e.g.
    `dxfg_v4_get_and_clear_thread_exception()`, `dxfg_v4_JavaObjectHandler_release()`,
    `dxfg_v4_DXEndpoint_createWithRole()`, `dxfg_v4_DXEndpoint_getState()` (writes the enum value),
    `dxfg_v4_Promise_getException()` (no `(dxfg_exception_t *)-1` marker) and `dxfg_v4_promise_list_release()`.
  * The functions of `dxfg_ipf.h`: the instrument profiles are read, iterated and given to a collector as structures
    (`dxfg_v4_instrument_profile_data_t`, the older `dxfg_instrument_profile2_t`) in
    `dxfg_v4_instrument_profile_data_list`, e.g. `dxfg_v4_InstrumentProfileReader_readFromFile()`,
    `dxfg_v4_Iterable_InstrumentProfile_next()`, `dxfg_v4_InstrumentProfileCollector_updateInstrumentProfiles()`. The
    forms with profile handles keep a suffix (`dxfg_v4_InstrumentProfileReader_readFromFileAsHandles()`,
    `dxfg_v4_Iterable_InstrumentProfile_nextHandle()`,
    `dxfg_v4_InstrumentProfileCollector_updateInstrumentProfileHandles()`).
  * The functions of `dxfg_schedule.h`: `dxfg_v4_Schedule_getInstance()` takes an instrument profile structure,
    `dxfg_v4_Schedule_getInstanceByProfileHandle()` a handle and `dxfg_v4_Schedule_getInstanceByDefinition()` a
    definition string; a day or a session that is not found is written as NULL. `dxfg_v4_session_list_release()`
    releases the sessions of a list together with the list, `dxfg_v4_session_list_wrapper_release()` the list only.
  * The functions of `dxfg_glossary.h`: the arrays with a size become lists, `dxfg_v4_double_list` (the price
    increments), `dxfg_v4_string_to_double_map_entry_list` (the map of additional underlyings) and
    `dxfg_v4_cfi_value_list` (the values of a CFI code).
  * The functions of `dxfg_event_model.h`, `dxfg_indexed_event_tx_model.h`, `dxfg_ondemand.h`, `dxfg_orcs.h`,
    `dxfg_candlewebservice.h`, `dxfg_subscription_controller.h`, `dxfg_logging.h`, `dxfg_system.h` and `dxfg_qds.h`,
    e.g. `dxfg_v4_OrderBookModel_getFilter()` (writes the enum value), `dxfg_v4_system_get_property()` (a missing
    property is written as NULL), `dxfg_v4_OnDemandService_getInstanceByEndpoint()`,
    `dxfg_v4_OnDemandService_replayWithSpeed()`, `dxfg_v4_PriceLevelService_getOrdersWithCaller()` and
    `dxfg_v4_symbols_by_order_source_map_entry_list_release()`. A function that already has the v4 form gets a twin with
    the `dxfg_v4_` name and types.
* **\[MDAPI-458]\[GRAAL]** **Deprecated:** `dxfg_free()` and the functions of `dxfg_javac.h`, `dxfg_catch_exception.h`,
  `dxfg_endpoint.h`, `dxfg_feed.h`, `dxfg_subscription.h`, `dxfg_publisher.h`, `dxfg_events.h`, `dxfg_ipf.h`,
  `dxfg_schedule.h`, `dxfg_glossary.h`, `dxfg_event_model.h`, `dxfg_indexed_event_tx_model.h`, `dxfg_ondemand.h`,
  `dxfg_orcs.h`, `dxfg_candlewebservice.h`, `dxfg_subscription_controller.h`, `dxfg_logging.h`, `dxfg_system.h` and
  `dxfg_qds.h`: each one is marked `DXFG_DEPRECATED` and names its v4 twin. The instrument profile forms with an array
  and a size, `dxfg_free_strings()` and `dxfg_free_string_to_double_map_entries()` have no twin: their deprecation names
  the list function or the release function of the list. They work as before and are removed in 4.0.0.
* **\[MDAPI-458]\[GRAAL]** The build checks that the C API headers declare the functions that the library exports, with
  the same numbers of parameters, that every older function is marked `DXFG_DEPRECATED` in favour of declared `dxfg_v4_`
  functions, that `dxfg_v4_api.h` includes every `dxfg_v4_*.h` and that `dxfg_api.h` includes `dxfg_v4_api.h`
  (`-Dapi.headers.check.skip=true` skips it).
* **\[MDAPI-464]\[GRAAL]** `dxfg_v4_NativeListener_deactivateAll()` (`dxfg_v4_javac.h`) deactivates all the
  listeners of the isolate created by the `dxfg_*Listener_new` functions and waits for their callbacks in progress on
  other threads; the listeners created afterwards are inactive from the start. `dxfg_v4_system_shutdown()`
  (`dxfg_v4_system.h`) deactivates them and then closes the endpoints and instrument profile connections, as
  `dxfg_v4_system_close_all_and_await_termination()` does, so that no callback runs while they are closed.

### For integrators

* Code that calls a deprecated function gets a compiler warning (C4996 with MSVC, `-Wdeprecated-declarations` with GCC
  and Clang), an error with `-Werror` or `/WX`. Move the call to the v4 twin, or silence the warning: define the macro
  empty before the headers are included (`#define DXFG_DEPRECATED(message)`, or `-DDXFG_DEPRECATED(message)=` with GCC
  and Clang), or disable C4996 with `/wd4996`.
* To stop the callbacks before the objects of the application are destroyed, call `dxfg_v4_system_shutdown()` first
  in the explicit shutdown of the application instead of releasing or deactivating the listeners one by one; then
  release the remaining handles. Do not call it from an exit handler of a DLL: a callback that waits for the loader
  lock never finishes.

## v3.11.0

* **\[MDAPI-453]\[GRAAL]** New functions for the Java API of QDS 3.326–3.351:
  * `dxfg_DXEndpoint_setRequestedAggregationPeriod[Millis]()`, `dxfg_DXEndpoint_getRequestedAggregationPeriod[Millis]()`:
    the aggregation period that the endpoint requests from the server (`DXEndpoint.setRequestedAggregationPeriod`). In
    millis, -1 means that no period is requested.
  * `dxfg_DXEndpoint_getAggregationPeriodInfo()` and `dxfg_DXFeedSubscription_getAggregationPeriodInfo()`: the minimum
    and maximum period that was applied, in millis, negative while unknown (`TimePeriodInfo`).
  * `dxfg_DXFeedSubscription_getTotalDroppedEvents()` (experimental in Java).
  * `dxfg_Schedule_getDefinition()`.
* **\[GRAAL]** The version info of `DxFeedGraalNativeSdk.dll` has the `FileVersion` string, so .NET
  (`FileVersionInfo`, PowerShell `VersionInfo`) reads the company, the description and the versions: before, it showed
  them empty, while Explorer showed them. The copyright year is the year of the build, and an rc build is marked as a
  pre-release.

## v3.10.0

* **\[GRAAL]** The C API reference of a release is generated by Doxygen 1.18.0, the version that checks the headers in
  the pull request build, instead of Doxygen 1.8.5 of the Linux build image: the code samples (`dxfg_logging.h`,
  `dxfg_glossary.h`, `dxfg_javac.h`) are code blocks now, not text with backticks. The reference of 3.9.0 at
  https://dxfeed.github.io/dxfeed-graal-native-sdk/3.9.0/ is published again; its zip in the GitHub release stays as it
  is. The build `Publish C API Docs to GitHub Pages` can publish the reference generated from a tag
  (`env.C_API_DOCS_SOURCE` = `tag`).
* **\[MDAPI-443]\[GRAAL]** The C API reference is online: https://dxfeed.github.io/dxfeed-graal-native-sdk/latest/ (the
  latest release), every release from 3.9.0 on at `https://dxfeed.github.io/dxfeed-graal-native-sdk/<version>/`. The
  new build `Publish C API Docs to GitHub Pages` of the release chain publishes it after `Publish GitHub Release`.
  Every header of `src/main/c/api` describes every type, field and function: what a function returns on error, who
  releases its result and how, on which thread a listener is called, and the Java method a function calls.
* **\[MDAPI-439]\[GRAAL]** IndexedTxModel and TimeSeriesTxModel are part of the SDK now (`com.dxfeed.sdk.model.tx`,
  copied from `dxfeed-model` 2.3.1 without changes): the build no longer needs the artifact
  `com.dxfeed.api.experimental.model:dxfeed-model`. The functions `dxfg_IndexedTxModel_*`, `dxfg_TimeSeriesTxModel_*`
  and `dxfg_TxModelListener_new()` and their types are the same and are described in `dxfg_event_model.h`; the listener
  frees the copies of the source and the events even if its call fails.
* **\[GRAAL]** The release chain releases the SDK in Jira after `Publish GitHub Release`: the new build `Release in Jira`
  creates the version `graal-native-sdk vX.Y.Z` of MDAPI, sets it on the SDK tickets in Waiting for build / Waiting for
  test, resolves them, sends the release mail and releases the version. An rc release changes nothing in Jira.

## v3.9.0

* **\[GRAAL]** The archives no longer contain `docs/changelog.txt` (a list of the commit messages by tags).
* **\[GRAAL]** New Windows archives with the static library of the SDK:
  `graal-native-sdk-<version>-amd64-windows-static-lib.zip` and `graal-native-sdk-<version>-amd64-windows-debug-static-lib.zip`
  (the GitHub release, the Maven classifiers `amd64-windows-static-lib` and `amd64-windows-debug-static-lib`).
  `DxFeedGraalNativeSdk_static.lib` links the SDK into the executable: no DLL, no Visual C++ Redistributable. The
  builds of the `-static-crt` archives make it from the same image and check it before the deploy with the four C
  runtimes.
* **\[GRAAL]** Migrated to QDS 3.356
  (see the [QD release notes](https://github.com/devexperts/QD/blob/master/ReleaseNotes.txt)) and `orcs-api` /
  `auther-api` 549. The event structures stay the same; the only change of the C API is the executor of the endpoint,
  see the next item.
* **\[GRAAL]** New `dxfg_DXEndpoint_Builder_withExecutor()` sets the executor of the endpoint's notifications (events,
  state changes, etc.) when the endpoint is built. **Deprecated:** `dxfg_DXEndpoint_executor()`: QDS 3.356 deprecated
  `DXEndpoint.executor(Executor)` in favour of `DXEndpoint.Builder.withExecutor(Executor)` (QD-1795). Changing the
  executor of an endpoint that already works is unsafe: called after `dxfg_DXEndpoint_getFeed()` (or another function
  that uses the feed, e.g. `dxfg_OnDemandService_getInstance2()`), it leaves the feed with the default executor, which
  is shut down, and the last events requests (`dxfg_DXFeed_getLastEventPromise()`, etc.) never complete. The builder
  sets the executor before the endpoint exists, so the order cannot be wrong. `dxfg_DXEndpoint_executor()` still works
  as before and stays in the library.
* **\[MDAPI-304]\[GRAAL]** New Windows archives linked with the static C runtime (`/MT`):
  `graal-native-sdk-<version>-amd64-windows-static-crt.zip` and `graal-native-sdk-<version>-amd64-windows-debug-static-crt.zip`
  (the GitHub release, the Maven classifiers `amd64-windows-static-crt` and `amd64-windows-debug-static-crt`). Their
  `DxFeedGraalNativeSdk.dll` does not need the Visual C++ Redistributable (no `VCRUNTIME140.dll`, `api-ms-win-crt-*.dll`);
  the C API and the other files are the same. Each archive is checked before the deploy, also in a Windows container
  without the Redistributable.
* **\[MDAPI-302]\[GRAAL]** The Windows library can be built with the static C runtime: `-DwindowsStaticRuntime=true`
  (the Maven profile `windowsStaticRuntime`). The JDK libraries of GraalVM are built with `/MD`, so
  `scripts/windows-static-runtime/New-CrtShim.ps1` assembles the pointers of their CRT imports to the static CRT, and
  native-image links the image with them and `libcmt`, `libvcruntime`, `libucrt` instead of the dynamic CRT.

### For integrators

* The archives with the static C runtime were named `-static-mt` in 3.9.0-rc1, now `-static-crt`: download
  `graal-native-sdk-<version>-amd64-windows[-debug]-static-crt.zip` (the Maven classifiers
  `amd64-windows-static-crt` and `amd64-windows-debug-static-crt`).
* The `DxFeedGraalNativeSdk.dll` of the `-static-crt` archives fits an application built with `/MT`, `/MTd`, `/MD` or
  `/MDd`: the C runtime is inside the library.
* The static library of the `-static-lib` archives fits an application built with `/MT`, `/MTd`, `/MD` or `/MDd`,
  without `/NODEFAULTLIB`. Link it with the Windows libraries listed in `link-flags.txt`, or use `CMakeLists.txt` of
  the archive (the target `DxFeedGraalNativeSdk`). Only the `dxfg_*` and `graal_*` functions are visible to the
  application, so the JDK inside (zlib, `JNU_*`) does not clash with its symbols, and the executable exports nothing.
* The changes are described in this file (and in the description of the GitHub release), not in `docs/changelog.txt`.
* The compilers warn where `dxfg_DXEndpoint_executor()` is used (`DXFG_DEPRECATED`: C4996 in MSVC,
  `-Wdeprecated-declarations` in GCC and Clang), so a build that treats warnings as errors fails until the call is
  replaced. Its first call in an isolate logs
  `WARNING: DEPRECATED use of executor() method. Set executor in the DXEndpoint.Builder`.
* Create the endpoint with the builder and pass the executor to it. Wrappers (C++, .NET, Swift, Go) that have a method
  to change the executor of an endpoint should move it to their endpoint builder.
  ```c
  // Before
  dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_create(thread);
  dxfg_DXEndpoint_executor(thread, endpoint, executor);

  // After (dxfg_DXEndpoint_Builder_withRole() for a role other than FEED)
  dxfg_endpoint_builder_t *builder = dxfg_DXEndpoint_newBuilder(thread);
  dxfg_DXEndpoint_Builder_withExecutor(thread, builder, executor);
  dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_Builder_build(thread, builder);
  dxfg_JavaObjectHandler_release(thread, &builder->handler);
  ```
* A custom executor is often not needed: the size of the default thread pool is set by the properties
  `dxfeed.threadPoolSize` (the FEED and ON_DEMAND_FEED roles) and `dxpublisher.threadPoolSize` (PUBLISHER), e.g. with
  `dxfg_DXEndpoint_Builder_withProperty()`; the listeners of one subscription can get their own executor with
  `dxfg_DXFeedSubscription_setExecutor()`.
* Until the migration, call `dxfg_DXEndpoint_executor()` right after the endpoint is created, before
  `dxfg_DXEndpoint_getFeed()` and `dxfg_DXEndpoint_getPublisher()`.

## v3.8.0

* New artifact `graal-native-sdk-<version>-xcframework.zip` (the GitHub release, the Maven classifier `xcframework`):
  `DxFeedGraalNativeSdk.xcframework` with static frameworks for iOS and the iOS Simulator and a dynamic framework for
  macOS (arm64 and x86_64), each with the headers and the Clang module `DxFeedGraalNativeSdk`, for Swift Package Manager
  (`binaryTarget`) and Xcode. The other archives do not change.
* The archives for iOS, the iOS Simulator and macOS contain `link-flags.txt`: the linker flags of an application, the
  platforms and the minimum versions, made from the files of the archive. The build checks that the iOS image links
  with these flags only (without autolinking) before the archive is published.
* iOS and the iOS Simulator: `libDxFeedGraalNativeSdk.o` declares the platform of each architecture
  (`LC_BUILD_VERSION`): `ios` 12.0 on arm64 in `aarch64-ios`, `iossimulator` 14.0 on arm64 and 12.0 on x86_64 in
  `ios-simulator`, so the linker reports an object linked for the wrong platform. native-image declared none (the arm64
  object of the simulator is the object of the device). The size of the object does not change. The build checks
  that the object links with the libraries of its archive before the archive is published.
* The archives for iOS and the iOS Simulator do not contain `CMakeLists.txt` any more: it imported a dynamic library
  that they do not have. The `docs` of the simulator archive do not contain the reports of the build for iOS any more.
* `graal_isolate.h` and `graal_isolate_dynamic.h` are documented (the C API documentation and the headers in the
  archives): the isolate threads, the shutdown and the isolate arguments of `graal_create_isolate_params_t.argv`
  (`-D<name>=<value>`, `-Xmx`, `-XX:` options, `ignore_unrecognized_args`). The documentation described the parameters
  of GraalVM 22 (version 1, without `argc` and `argv`); the build fails if the declarations of the headers differ from
  the ones that native-image generates (the GraalVM of the x64 libraries declares version 4 of the parameters, with
  the same fields).

### For integrators

* iOS and the iOS Simulator: an object of the SDK linked for the wrong platform now fails the link (it was a warning:
  "no platform load command found"). The size of the object and of the linked application does not change.
* `link-flags.txt` of the iOS, iOS Simulator and macOS archives lists the files and the system libraries to link
  (`-framework Foundation -framework CFNetwork -lz`); with autolinking (the default of clang and Xcode) the files and
  `-framework Foundation` are enough, `-lc++` is not needed.
* The iOS archives do not contain `CMakeLists.txt`, the archives contain the documented `graal_isolate.h` (version 5 of
  `graal_create_isolate_params_t`).
* `graal-native-sdk-<version>-xcframework.zip` contains `DxFeedGraalNativeSdk.xcframework`: link it on all the platforms
  instead of the files of the iOS and iOS Simulator archives and of the macOS libraries (Xcode takes the slice of the
  platform); embed it on macOS only (the frameworks for iOS and the simulator are static). Unpack the zip with `ditto`
  or `unzip`: Gradle's `zipTree` turns the links of the macOS framework (`Versions/Current`, ...) into files. A Clang
  module map of the application cannot name the headers inside the framework (they belong to its module
  `DxFeedGraalNativeSdk`): import the module, or copy the headers out. A test bundle that imports the module links a
  second copy of the static SDK, unless autolinking is disabled for it
  (`-Xfrontend -disable-autolink-framework -Xfrontend DxFeedGraalNativeSdk`).

## v3.7.0

* Fixed on macOS x64: the jvmstat support called the 32-bit inode `fdopendir` and `readdir` of the system (GraalVM
  jdk-25.0.1 for macOS x64 declares them without `$INODE64`) but read the 64-bit inode layout of `dirent` when it
  cleaned up the `hsperfdata` directory at the start: it read the file names at a wrong offset and passed them to
  `unlinkat` (the stale files were not removed), and an application with AddressSanitizer aborted at the creation of
  the first isolate. The performance data now stays in the memory of the process on macOS x64 (tools such as `jstat` do
  not see the process there), and the build fails if the library imports a 32-bit inode function.
* Fixed: the macOS library declared the macOS version of the build machine as its minimum version (macOS 26.0 for
  3.6.0, 3.7.0-rc1 and 3.7.0-rc2), so the linker of an application for an older macOS warned that the library was
  built for a newer version. It now declares macOS 14.0 on arm64 and macOS 11.0 on x64, the minimum versions of the
  GraalVM libraries that it is built with, and the build fails if the library declares another version.
* iOS and the iOS Simulator: the JDK library names the system libraries that it needs for the linker
  (`LC_LINKER_OPTION`): `-lz` and `-framework CFNetwork`, so an application links them without adding them. With
  GraalVM 25 (since 3.6.0) the SDK uses the zlib functions of `java.util.zip` (`Inflater`, `Deflater`), and an
  application without `-lz` did not link; an application in C or Objective-C also needed `-framework CFNetwork`
  (Swift adds it with Foundation). The libraries do not embed their own zlib, so there is no conflict with the zlib
  of the application.
* Fixed: on Linux and macOS, the process could be killed by `SIGPIPE` when an endpoint was disconnected or closed
  while its connector wrote to the socket (a QD writer thread writing to a socket already shut down by the stopping
  thread). The SDK now ignores `SIGPIPE` and `SIGXFSZ` as the JVM does: when an isolate is created and the action of
  the signal is the default one, a no-op handler is installed, so such a write fails with `EPIPE` (handled by QD) and
  a handler or `SIG_IGN` set by the application is kept. A no-op handler, unlike `SIG_IGN`, is not inherited by the
  programs that the application executes. It can be turned off with the isolate argument `-XX:-EnableSignalHandling`.
* Fixed the archives for iOS and the iOS Simulator (`aarch64-ios`, `ios-simulator`): an application did not link with
  them since 3.6.0 (undefined symbols such as `Java_java_io_FileInputStream_isRegularFile0` and `svm_get_locale`), as
  their JDK and Substrate VM libraries were built for GraalVM 22. The libraries are now built from the sources of the
  GraalVM versions that build the SDK (see `jre-ios/README.MD`).
* The archives contain only the library of the SDK: they do not contain `management_ext` (`management_ext.dll`,
  `libmanagement_ext.so`, `libmanagement_ext.dylib`) and `libosxkrb5.dylib` any more. native-image copied these JDK
  libraries next to the SDK library: `management_ext` could not even be loaded without the `java` and `jvm` libraries
  of a JDK (the SDK already contains the native code of `OperatingSystemMXBean` that it uses), `libosxkrb5.dylib` is
  needed only for the Kerberos authentication on macOS. The NuGet package never contained them.
* Fixed memory leaks: the release of a `NuamOrder` (`dxfg_EventType_release`, the lists of events, the events of
  the listeners) did not free its strings `client_order_id`, `customer_account`, `customer_info` and `exchange_info`,
  and `dxfg_Exception_release` did not free the method names of the stack trace.
* **\[MDAPI-169\]** Added `dxfg_DXFeed_getLastEvent2` and `dxfg_DXFeed_getLastEvents2`. They only read the given
  events (the caller allocates and frees them with any allocator, as for `dxfg_DXPublisher_publishEvents`) and write
  new events into the output parameter: the last events, or copies of the given events when the last events are not
  available, as `DXFeed.getLastEvent` in Java. They return 0 or -1 on error (the output is `NULL` then). Free the
  results with `dxfg_EventType_release` and `dxfg_CList_EventType_release`.
* **Deprecated:** `dxfg_DXFeed_getLastEvent` and `dxfg_DXFeed_getLastEvents`: they write into the given events and free
  their strings with the SDK's allocator, so an event allocated by the caller leaks the SDK's strings or corrupts
  memory. They still work. The compilers warn where the deprecated functions are used (`DXFG_DEPRECATED`), including
  `dxfg_InstrumentProfileCustomFields_addNonEmptyFieldNames` (deprecated before).
* Fixed the declaration of `dxfg_DXFeedSubscription_getSubscriptionController`: its output parameter is
  `dxfg_subscription_controller_t **`, as the function writes the pointer to the handle there (it was declared as
  `dxfg_subscription_controller_t *`).

### For integrators

* iOS and the iOS Simulator: an application links `libDxFeedGraalNativeSdk.o` and the two `.a` libraries of the
  archive. They name `-lz` and `-framework CFNetwork` for the linker themselves, so they are not needed in the
  linker flags any more (they do no harm there); the SDK checks that the libraries link with `-framework Foundation`
  only.
* Minimum OS versions: macOS 14.0 on arm64 and macOS 11.0 on x64 (before: the version of the build machine, macOS 26.0
  in 3.6.0); iOS 12.0, the iOS Simulator 14.0 on arm64 and 12.0 on x86_64 (the `.a` libraries;
  `libDxFeedGraalNativeSdk.o` declares no platform).
* The archives of Windows, Linux and macOS do not contain `management_ext` and `libosxkrb5.dylib` any more.
* Linux and macOS: an isolate installs no-op handlers of `SIGPIPE` and `SIGXFSZ` when their action is the default one
  (`-XX:-EnableSignalHandling` turns it off).
* macOS x64: tools such as `jstat` do not see the process.
* The compilers warn where `dxfg_DXFeed_getLastEvent` and `dxfg_DXFeed_getLastEvents` are used (use
  `dxfg_DXFeed_getLastEvent2` and `dxfg_DXFeed_getLastEvents2`); a call of
  `dxfg_DXFeedSubscription_getSubscriptionController` that casts its output parameter to
  `dxfg_subscription_controller_t *` may need a change.

## v3.6.0

* **Changed:** the `attachment` of `Message` and `Configuration` (`dxfg_message_t`, `dxfg_configuration_t`) is a UTF-8
  string or `NULL`. Java -> C: a `String` attachment is passed as is (it was JSON before: `"text"` with the quotes),
  another object as its `toString()`, no attachment as `NULL` (it was the string `null`). C -> Java: the attachment
  becomes a `String` (`NULL`: no attachment), it was lost before. So a non-string attachment loses its type when the
  event is published back from C. An attachment that cannot be deserialized is `NULL` with a warning in the log (it
  failed the whole listener call before, e.g. for a `Configuration` without an attachment read from a QD text tape).
  See `dxfg_message_t` for the memory ownership. Arbitrary Java attachments are not supported in C; for a string payload
  prefer `TextMessage` and `TextConfiguration`.
* The SDK library does not use Jackson any more: `jackson-databind` was used only for the attachments, it is a test
  dependency now.
* Pre-releases (e.g. `3.6.0-rc1`): the file version of `DxFeedGraalNativeSdk.dll` has the number of the pre-release
  in the fourth field (`3.6.0-rc2` -> `3.6.0.2`, 0 for a release); the release notes of a version include the changes
  of its pre-releases; the GitHub release of a pre-release is marked as a pre-release. See
  [Pre-releases](README.md#pre-releases).

## v3.5.0

* Fixed the configuration of a project that adds the SDK from a Linux debug build (`*-debug.zip`) without defining
  `INSTALL_SHARED` (`install FILES given no DESTINATION!`): the debug info is installed next to the library then.
* **Changed:** `dxfg_Promise_awaitWithoutException` returns `DXFG_PROMISE_AWAIT_TIMED_OUT` (1) when the wait times out
  (the promise is cancelled then), it returned 0 ("completed") before. 0 still means "completed", -1 means an error.
* **Changed:** the listeners created by the `dxfg_*Listener_new` functions stop calling their functions when their last
  handle is released (`dxfg_JavaObjectHandler_release`), which waits for the calls in progress, so the user data can be
  freed after that. Before, a released listener kept calling its function with the old user data.
  The new `dxfg_NativeListener_deactivate` does the same explicitly. See [Listeners](README.md#listeners).
* **Changed:** the QD `TimeSyncTracker` (UDP multicast to `239.192.51.45:5145` from every process) is disabled by default,
  set the system property `com.dxfeed.sdk.TimeSyncTracker.enable=true` to enable it.
  See [Network activity](README.md#network-activity).
* Added `dxfg_system_close_all_and_await_termination`: closes all the endpoints and instrument profile connections
  created through the SDK, so that `graal_tear_down_isolate` does not wait for their threads forever.
  See [Shutdown](README.md#shutdown). The debug builds print the threads that block the isolate tear-down
  for more than 10 seconds (`-XX:TearDownWarningSeconds`).
* Fixed `MissingReflectionRegistrationError` for the connector properties in addresses (e.g. `:7700[bindAddr=127.0.0.1]`):
  all the properties of the QD connectors, codecs (`ssl`, `tls`, `shaped`, `delayed`) and file/tape parameters
  are registered for reflection.
* `DXFG_EXECUTE_SUCCESSFULLY` and `DXFG_EXECUTE_FAIL` compile in C (they used the C++ cast syntax).
* C API documentation: the endpoint creation functions (`dxfg_DXEndpoint_getInstance2` creates a new endpoint on every
  call), closing, executor, state and event listeners, promises.

## v3.3.1

* Linux debug builds (`*-debug.zip`) include the separate debug info `libDxFeedGraalNativeSdk.so.debug`
  and `gdb-debughelpers.py`; the SDK code keeps frame pointers, so LSan/ASan and gdb unwind the stack through the SDK.
  See [Debug builds](README.md#debug-builds).

## v3.3.0

* **\[MDAPI-420]\[GRAAL]** Migrated to GraalVM 25.4.4.1.1 (JDK 25.0.4); Oracle GraalVM is supported as well.
* Migrated to QDS 3.355 and MDD 548.
    * All QDS modules have the same version now (the previous releases mixed QDS 3.352 and 3.353 modules).
    * Supported the new connector JMX attributes `Role`, `DisplayFilter` and `DisplayChannels`.
    * The new `NEO` and `neo` order sources are available.
* The C API documentation (`c-api-docs-html.zip`) is published with every release to the Maven repositories and to the GitHub release.
* Publishing releases to the public Maven repository https://repo.devexperts.com/repository/mdd-maven/ is prepared but temporarily disabled.
* JFrog credentials and references removed from TeamCity settings and scripts.
* Samples: fixed the crash of the `DxfgClient` `HistoryEndpointCase` on a server error.

## v3.2.999

## v3.2.99

## v3.2.13

## v3.2.12

## v3.2.11

## v3.2.10

* **\[MDAPI-415]\[GRAAL]** Fixed Graal SDK build and deployment.

## v3.2.0

* **\[MDAPI-315]\[GRAAL]** Added `dxfg_CList_IndexedEventSource_release` function.
* Improved the reflection and serialization.

## v3.1.0

* **\[MDAPI-71]\[GRAAL]** Added new alternatives to the `getAggregationPeriod` and `setAggregationPeriod` methods for subscription.
  * Added `int32_t dxfg_DXFeedSubscription_getAggregationPeriodMillis(graal_isolatethread_t *thread, dxfg_subscription_t *sub, DXFG_OUT int64_t *aggregationPeriod)` function.
  * Added `int32_t dxfg_DXFeedSubscription_setAggregationPeriodMillis(graal_isolatethread_t *thread, dxfg_subscription_t *sub, int64_t aggregationPeriod)` function.

## v3.0.0

* **\[MDAPI-243]\[GRAAL]** Added `NuamOrder`, `NuamTimeAndSale`, `NuamTrade`, and `OrderImbalance` events.
  * Added all necessary structs, mappers, wrappers.
  * Impoved the `DxfgClient` tool.
  * Added reflection info for the `NuamOrder`, `NuamTimeAndSale`, `NuamTrade`, and `OrderImbalance` events.
  * Added `dxfg_IndexedEventSource_new2(graal_isolatethread_t *thread, int32_t sourceId, DXFG_OUT dxfg_indexed_event_source_t** source)` function.
* **\[MDAPI-296]\[GRAAL]** TimeAndSale, Trade and TradeETH events extended with new field tradeId.
* Migrated to QDS 3.347 and MDD 525
* **\[MDAPI-298]** dxFeed Graal Native SDK + ORCS API.
  * Added `dxfg_orcs.h` header
  * Added `PriceLevelServiceHolder` class.
  * Added all mappers, wrappers, headers, and handlers for ORCS API.
* **\[MDAPI-282]\[GRAAL]** Fixed build with new CMake.
* **\[MDAPI-244]\[GRAAL]** Added IndexedEventTxModel.
  * `DxfgExecutor` renamed to `DxfgExecutorHandle`. Added `DxfgExecutorHandlePointer`.
  * `DxfgTimePeriod` renamed to `DxfgTimePeriodHandle`. Added `DxfgTimePeriodHandlePointer`.
  * Added `dxfg_subscription_controller.h` header.
  * Added `DxfgSubscriptionControllerHandle` (`dxfg_subscription_controller_t` struct), `DxfgSubscriptionControllerHandlePointer`, mapper and native wrappers.
  * Added functions to `SubscriptionControllerNative` (`dxfg_subscription_controller.h`):
    * `dxfg_SubscriptionController_*`.
  * Added functions to `SubscriptionNative` (`dxfg_subscription.h`):
    * `dxfg_DXFeedSubscription_getSubscriptionController`
  * Added `dxfg_indexed_event_tx_model.h` header.
  * Added `DxfgIndexedEventTxModelHandle` (`dxfg_indexed_event_tx_model_t` struct), `DxfgIndexedEventTxModelHandlePointer`, mapper and native wrappers.
  * Added `DxfgIndexedEventTxModelListenerHandle` (`dxfg_indexed_event_tx_model_listener_t` struct), `DxfgIndexedEventTxModelListenerHandlePointer`, mapper and native wrappers.
  * Added `DxfgIndexedEventTxModelBuilderHandle` (`dxfg_indexed_event_tx_model_builder_t` struct), `DxfgIndexedEventTxModelBuilderHandlePointer`, mapper and native wrappers.
  * Added `DxfgIndexedEventTxModelListenerEventsReceivedFunctionPointer` (`dxfg_IndexedEventTxModel_Listener_eventsReceived_f` define).
  * Added functions to `IndexedEventTxModelNative` (`dxfg_indexed_event_tx_model.h`):
    *  `dxfg_IndexedEventTxModel_*`
    *  `dxfg_IndexedEventTxModel_Listener_new`
    *  `dxfg_IndexedEventTxModel_Builder_*`
  * Added `DxfgClient`'s cases:
      * `IndexedEventTxModelCase`.

## v2.6.0

* **\[MDAPI-260]\[GRAAL]** Improved the export of `HistoryEndpoint.Builder` methods:
    * Removed an unnecessary `newBuilder` parameter from exported functions `dxfg_HistoryEndpoint_Builder_withCompression` and `dxfg_HistoryEndpoint_Builder_withFormat`.
    * Removed all error messages "The `newBuilder` pointer is null"
* Migrated to QDS 3.342 and MDD 510

## v2.5.0

* **\[MDAPI-242]\[GRAAL]** Migrated to QDS 3.340.
    * Migrated to QDS 3.340 and MDD 507
    * Added `DxfgTextConfiguration` interface and `dxfg_text_configuration_t` struct.
    * Added `DXFG_EVENT_TEXT_CONFIGURATION` to `DxfgEventClazz` enum and `DXFG_EVENT_TEXT_CONFIGURATION`
      to `dxfg_event_clazz_t` C-enum.
    * `com.dxfeed.event.misc.TextConfiguration` info added to the `reflect-config.json`.
    * The mappers are moved according to the packages in QD.
    * Added `getEventClazz` method to the `EventMapper` interface. Added implementation to all inheritors.
* **\[MDAPI-241]\[GRAAL]** Implemented HistoryEndpoint.
    * Added `HistoryEndpoint`.
    * Added `DxfgHistoryEndpointHandle` interface (`dxfg_history_endpoint_t` struct), mappers, etc.
    * Added `DxfgHistoryEndpointBuilderHandle` (`dxfg_history_endpoint_builder_t`), mappers, etc.
    * Added `DxfgHistoryEnpointCompression` enum (`dxfg_history_endpoint_compression_t`).
    * Added `DxfgHistoryEnpointFormat` enum (`dxfg_history_endpoint_compression_t`).
    * Added `dxfg_candlewebservice.h` header.
    * Added functions:
        * `dxfg_HistoryEndpoint_newBuilder`.
        * `dxfg_HistoryEndpoint_getTimeSeries`.
        * `dxfg_HistoryEndpoint_Builder_withAddress`.
        * `dxfg_HistoryEndpoint_Builder_withUserName`.
        * `dxfg_HistoryEndpoint_Builder_withPassword`.
        * `dxfg_HistoryEndpoint_Builder_withAuthToken`.
        * `dxfg_HistoryEndpoint_Builder_withCompression`.
        * `dxfg_HistoryEndpoint_Builder_withFormat`.
        * `dxfg_HistoryEndpoint_Builder_build`.
* **\[MDAPI-233]\[GRAAL]** Fixed an issue with setting the buffer size for the collector.
* **\[MDAPI-237]\[GRAAL]** The deprecated cached mappers and unused StringMapperCacheStore were removed.

## v2.4.0

* **\[MDAPI-230]\[GRAAL]** Improved InstrumentProfileCustomFields.
    * Added `dxfg_InstrumentProfileCustomFields_getNonEmptyNames` function.
    * Deprecated the `dxfg_InstrumentProfileCustomFields_addNonEmptyFieldNames` function.

## v2.3.0

* **\[MDAPI-223]\[GRAAL]** Improved InstrumentProfile
    * Migrated to QDS 3.339 and MDD 504.
    * Added structs:
        * `dxfg_instrument_profile2_t` struct.
        * `dxfg_instrument_profile_custom_fields_t` handle struct.
        * `dxfg_instrument_profile2_list_t` struct.
        * `dxfg_java_object_t` struct.
    * Added functions:
        * `dxfg_free_strings`.
        * `dxfg_Object_new`.
        * `dxfg_InstrumentProfileField_formatNumber`.
        * `dxfg_InstrumentProfileField_parseNumber`.
        * `dxfg_InstrumentProfileField_formatDate`.
        * `dxfg_InstrumentProfileField_parseDate`.
        * `dxfg_InstrumentProfileCustomFields_new`.
        * `dxfg_InstrumentProfileCustomFields_new2`.
        * `dxfg_InstrumentProfileCustomFields_new3`.
        * `dxfg_InstrumentProfileCustomFields_new4`.
        * `dxfg_InstrumentProfileCustomFields_getField`.
        * `dxfg_InstrumentProfileCustomFields_setField`.
        * `dxfg_InstrumentProfileCustomFields_getNumericField`.
        * `dxfg_InstrumentProfileCustomFields_setNumericField`.
        * `dxfg_InstrumentProfileCustomFields_getDateField`.
        * `dxfg_InstrumentProfileCustomFields_setDateField`.
        * `dxfg_InstrumentProfileCustomFields_addNonEmptyFieldNames`.
        * `dxfg_InstrumentProfileCollector_updateInstrumentProfile2`.
        * `dxfg_InstrumentProfileCollector_updateInstrumentProfile2_cached`.
        * `dxfg_InstrumentProfileCollector_updateInstrumentProfiles2`.
        * `dxfg_InstrumentProfileCollector_updateInstrumentProfiles2_cached`.
        * `dxfg_InstrumentProfileCollector_updateInstrumentProfiles3`.
        * `dxfg_InstrumentProfileCollector_updateInstrumentProfiles3_cached`.
        * `dxfg_Iterable_InstrumentProfile_next2`.
        * `dxfg_Iterable_InstrumentProfile_next2_cached`.
        * `dxfg_InstrumentProfileReader_readFromFile4`.
        * `dxfg_InstrumentProfileReader_readFromFile4_cached`.
        * `dxfg_InstrumentProfileReader_readFromFile5`.
        * `dxfg_InstrumentProfileReader_readFromFile5_cached`.
        * `dxfg_InstrumentProfileReader_readFromFile6`.
        * `dxfg_InstrumentProfileReader_readFromFile6_cached`.
        * `dxfg_InstrumentProfileReader_readFromFile7`.
        * `dxfg_InstrumentProfileReader_readFromFile7_cached`.
        * `dxfg_InstrumentProfileReader_readFromFile8`.
        * `dxfg_InstrumentProfileReader_readFromFile8_cached`.
        * `dxfg_InstrumentProfileReader_readFromFile9`.
        * `dxfg_InstrumentProfileReader_readFromFile9_cached`.
        * `dxfg_InstrumentProfileReader_read3`.
        * `dxfg_InstrumentProfileReader_read3_cached`.
        * `dxfg_InstrumentProfileReader_read4`.
        * `dxfg_InstrumentProfileReader_read4_cached`.
        * `dxfg_InstrumentProfileReader_read5`.
        * `dxfg_InstrumentProfileReader_read5_cached`.
        * `dxfg_InstrumentProfileReader_read6`.
        * `dxfg_InstrumentProfileReader_read6_cached`.
        * `dxfg_InstrumentProfileReader_readCompressed2`.
        * `dxfg_InstrumentProfileReader_readCompressed2_cached`.
        * `dxfg_InstrumentProfileReader_readCompressed3`.
        * `dxfg_InstrumentProfileReader_readCompressed3_cached`.
        * `dxfg_instrument_profile_free`.
        * `dxfg_instrument_profile_free_cached`.
        * `dxfg_instrument_profiles_array_free`.
        * `dxfg_instrument_profiles_array_free_cached`.
        * `dxfg_instrument_profile2_list_free`.
        * `dxfg_instrument_profile2_list_free_cached`.
        * `dxfg_Schedule_getInstance4`.
        * `dxfg_Schedule_getInstance4_cached`.
        * `dxfg_Schedule_getInstance5`.
        * `dxfg_Schedule_getInstance5_cached`.
        * `dxfg_Schedule_getTradingVenues2`.
        * `dxfg_Schedule_getTradingVenues2_cached`.
    * Fixed functions:
        * `dxfg_Object_equals`.
        * `dxfg_Object_hashCode`.
        * `dxfg_Comparable_compareTo`.
    * Added `DxfgClient`'s cases:
        * `InstrumentProfileCustomFieldsCase`.
        * `InstrumentProfileFieldCase`.
        * `InstrumentProfileReaderBench` to run ipf loading benchmarks.
    * All of these cached functions use an internal cache for strings when working with tool
      profiles. You should use cached memory release functions if you use cached functions to create
      instrument profiles. This reduces memory usage slightly when you have a large number of profiles.
    * Please see generated docs.
* **\[MDAPI-130]\[GRAAL]** Added MarketMaker event
    * Added `DxfgMarketMaker` interface (`dxfg_market_maker_t` struct), mappers, etc.
    * Added `DXFG_EVENT_MARKET_MAKER` enum value.
    * Improved the `DxfgClient` tool.
    * Added reflection info for the `MarketMaker` event for dxLink.
    * Migrated to QDS 3.338 and MDD 503

## v2.2.0

* **\[MDAPI-222]\[Graal]** Implement TextMessage event
    * Added `DxfgTextMessage` interface (`dxfg_text_message_t` struct), mappers, etc.
    * Added `DXFG_EVENT_TEXT_MESSAGE` enum value.
    * Improved the `DxfgClient` tool.

## v2.1.0

* The TeamCity settings were prepared to support build on linux-aarch64.
* Fixed the `dxfg_DXEndpoint_getEventTypes` function for unknown event types.
* Added `BuildForLinuxAarch64` build step to allow to check project build on `linux-aarch64`.
* Added `BuildAndPushDockerImageForLinuxAarch64` build step to allow to create `linux-aarch64` docker images.
* Added an ability to intercept logged data, configure the logging level, and set it on the fly
    * Added `InterceptableLogging` class and `InterceptableLoggingListener` functional interface.
      Use this code to enable it:
  ```c
  dxfg_system_set_property(isolate_thread, "log.className", "com.devexperts.logging.InterceptableLogging")
  ```
    * Added `log.level` system property that allows to set the logging level to console or to
      listener or to log file (default = `ALL`).
  ```c
  // Disable logging
  dxfg_system_set_property(isolate_thread, "log.level", "OFF");
  ```
    * Added `err.level` system property that allows to set the logging level to an err file (
      default = `WARN`).
    * Added `DxfgLoggingLevel` enum (`dxfg_logging_level_t`). See the `dxfg_logging.h` header.
    * Added `dxfg_logging_listener_t` handle of a wrapper class for a listener that allows to
      intercept logged messages.
    * Added `dxfg_logging_listener_function_t` callback type for the logging listener.
    * Added `dxfg_logging_listener_new` function that creates a new listener.
    * Added `dxfg_logging_set_listener` function that sets the logging listener. This function will
      toggle logging. Messages will not be logged to console and files.
    * Added `dxfg_logging_set_log_level` function that sets the logging level for the listener or
      console or log file.
    * Added `dxfg_logging_set_err_level` function that sets the logging level for the err file.

## v2.0.0

* **\[MDAPI-172]\[GRAAL]** Added com.dxfeed.glossary package to SDK
    * Fixed URLs.
    * Optimized antrun plugin's executions.
    * Version updates:
        * QD -> 3.337
        * jackson-databind -> 2.13.4.2
        * Plugins:
            * build-helper-maven-plugin -> 3.5.0
            * exec-maven-plugin -> 3.3.0
            * maven-compiler-plugin -> 3.13.0
            * maven-antrun-plugin -> 3.1.0
            * git-changelog-maven-plugin -> 1.101.0
    * Added a `Glossary` package.
    * Added `dxfg_glossary.h` C-header.
    * The `maper` package renamed to `mappers`
    * Added `DxfgRoundingMode` (`math.RoundingMode`) enum. Added `dxfg_rounding_mode_t` enum to `dxfg_javac.h`
    * Added `dxfg_additional_underlyings_t` (`DxfgAdditionalUnderlyingsHandle`, `AdditionalUnderlyings`) struct.
    * Added `dxfg_cfi_t` (`DxfgCFI`, `CFI`) struct.
    * Added `dxfg_cfi_attribute_t` (`DxfgCFIAttributeHandle`, `CFI.Attribute`) struct.
    * Added `dxfg_cfi_value_t` (`DxfgCFIValueHandle`, `CFI.Value`) struct.
    * Added `dxfg_price_increments_t` (`DxfgPriceIncrementsHandle`, `PriceIncrements`) struct.
    * Added `dxfg_common.h` header. All headers now have Doxygen file briefs.
    * Added `DxfgAdditionalUnderlyingsHandlePointer` (`dxfg_additional_underlyings_t**`)
    * Added `AdditionalUnderlyingsMapper`.
    * Added `dxfg_AdditionalUnderlyings_*` functions with correct Doxygen documentation and standard way of declaring parameters and result. See: `dxfg_glossary.h`
    * Fixed `DxfgClient` build on Windows.
    * Added `DxfgOut` annotation for output parameters.
    * Added `DxfgStringToDoubleMapEntryPointer` (`dxfg_string_to_double_map_entry_t`) and `DxfgStringToDoubleMapEntryPointerPointer` interfaces.
    * Added `dxfg_CFI_*` functions.
    * Added `dxfg_CFI_Value_*` functions.
    * Added `dxfg_CFI_Attribute_*` functions.
    * Added `dxfg_PriceIncrements_*` functions.
    * Added `dxfg_JavaObjectHandler_clone` function.
    * Added `dxfg_JavaObjectHandler_array_free` function.
    * Added `dxfg_free` function.
    * Added `CInt16Pointer`, `CInt32Pointer`, `CDoublePointerPointer` interfaces.
    * Added `CommonNative` class.
    * Added `dxfg_free_string_to_double_map_entries` function to `dxfg_common.h`.

