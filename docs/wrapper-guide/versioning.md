# Tiers and versioning {#wrapper_versioning}

A wrapper is built against the headers of one release and runs against the library that the application installs,
which may be newer. This page states what it may rely on and how it checks the library at start-up. The tiers are
defined in @ref V4_Conventions, the runtime queries in @ref V4_System.

## Tiers {#wrapper_versioning_tiers}

- A **stable** function keeps its name, parameters and behaviour. It changes only by deprecation, and is removed only
  in a major release. A wrapper binds the stable functions unconditionally. A stable function may call a Java member
  that is `@Experimental` in QD (`IndexedEventTxModel`, `SubscriptionController`, the Nuam and `OrderImbalance`
  events): a change of the member does not reach the C function. If the member is removed without a replacement, the
  function stays, its body is replaced by one that fails with `UnsupportedOperationException`, and the function is
  deprecated and removed only in a major release. The layout of a stable event structure does not change. A function
  promoted from experimental to stable keeps its name: a wrapper that still binds it under the revision check keeps
  working until the revision changes, so it moves the function out of the check.
- An **experimental** function is marked `DXFG_EXPERIMENTAL` and listed on the page @ref experimental. It may change or
  be removed in any minor release. An incompatible change gives it a new name and increments the advanced revision
  (#DXFG_V4_ADVANCED_REVISION), so a wrapper bound by name misses the old symbol instead of calling it with a changed
  signature.
- A **deprecated** function is marked `DXFG_DEPRECATED`, and its warning names its replacement. The deprecated
  functions are removed in 4.0.0: a wrapper binds the replacements (@ref wrapper_versioning_migration).

## The start-up check {#wrapper_versioning_check}

Before it binds anything else, a wrapper checks the library with three functions (3.13.0 and newer; a library without
them is older than the wrapper supports):

1. dxfg_v4_system_get_version() gives the release version, to log and to compare with the minimum the wrapper needs.
2. dxfg_v4_system_get_advanced_revision() gives the revision of the experimental functions. The wrapper binds them only
   when it equals #DXFG_V4_ADVANCED_REVISION of the headers it was built with; otherwise it treats the experimental
   part as missing, and its own experimental API reports "not available" instead of calling the library.
3. dxfg_v4_system_is_feature_supported() tells whether a feature is there (#DXFG_V4_FEATURE_EVENT_QUEUE,
   #DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL, ...). A feature that the library does not know is not supported, not an
   error, so a wrapper may ask for a feature newer than the library.

A wrapper that links to the library instead of loading it at run time makes the same check at start-up, and stops
before it calls an experimental function of another revision: such a call may crash.

## The API description {#wrapper_versioning_description}

Every archive has `dxfg-api.json` next to the library: the C API in a form a wrapper generator reads instead of parsing
the headers. It lists every function, type and macro with its header, its tier, `since`, its parameters and their
`out` flags, the `release` function of what it returns, and `deprecated` with the replacements. Its `sdk_version`,
`advanced_revision` and `features` are what the three functions above report at run time. A generator that binds only
what the description lists, and only the stable tier unless the revisions match, follows this page by construction.

The headers and the description of a release keep every stable declaration of the previous one: the build checks the
headers against the description of the last release, so a wrapper built for an older release keeps working with a
newer library of the same major version.

## Migrating to the v4 functions {#wrapper_versioning_migration}

Migrate a call of a deprecated function to the function that its deprecation message names, which is also the
`replacements` of the function in `dxfg-api.json`; never by the name. A `dxfg_v4_` function has no digit suffix, since
an overload is named by what it takes (@ref V4_Conventions), so the same name may stand for another overload in the
two generations. A migration by the name then compiles, or binds, and calls another function:

<!-- dxfg-deprecated:begin -->
- Without the digit suffix, the `dxfg_v4_` name of an older function may be its replacement
  (dxfg_v4_AdditionalUnderlyings_getSPC() replaces dxfg_AdditionalUnderlyings_getSPC2()) or another function:
  dxfg_DXEndpoint_getInstance2() is replaced by dxfg_v4_DXEndpoint_createWithRole(), while
  dxfg_v4_DXEndpoint_getInstance() replaces dxfg_DXEndpoint_getInstance().
- An older function without a digit suffix may have a `dxfg_v4_` function of the same name that is another overload.
  These are all of them, each with its replacement and the older function that the `dxfg_v4_` function of the same
  name replaces (the build checks the list against `dxfg-api.json`):
<!-- dxfg-deprecated:end -->

<!-- dxfg-renamed:begin -->
- dxfg_AdditionalUnderlyings_getSPC() -> dxfg_v4_AdditionalUnderlyings_getSPCFromText();
  dxfg_v4_AdditionalUnderlyings_getSPC() replaces dxfg_AdditionalUnderlyings_getSPC2().
- dxfg_InstrumentProfileCollector_updateInstrumentProfile() ->
  dxfg_v4_InstrumentProfileCollector_updateInstrumentProfileHandle();
  dxfg_v4_InstrumentProfileCollector_updateInstrumentProfile() replaces
  dxfg_InstrumentProfileCollector_updateInstrumentProfile2().
- dxfg_InstrumentProfileCollector_updateInstrumentProfiles() ->
  dxfg_v4_InstrumentProfileCollector_updateInstrumentProfileHandles();
  dxfg_v4_InstrumentProfileCollector_updateInstrumentProfiles() replaces
  dxfg_InstrumentProfileCollector_updateInstrumentProfiles2() and
  dxfg_InstrumentProfileCollector_updateInstrumentProfiles3().
- dxfg_InstrumentProfileReader_read() -> dxfg_v4_InstrumentProfileReader_readAsHandles();
  dxfg_v4_InstrumentProfileReader_read() replaces dxfg_InstrumentProfileReader_read3() and
  dxfg_InstrumentProfileReader_read5().
- dxfg_InstrumentProfileReader_readCompressed() -> dxfg_v4_InstrumentProfileReader_readCompressedAsHandles();
  dxfg_v4_InstrumentProfileReader_readCompressed() replaces dxfg_InstrumentProfileReader_readCompressed2() and
  dxfg_InstrumentProfileReader_readCompressed3().
- dxfg_InstrumentProfileReader_readFromFile() -> dxfg_v4_InstrumentProfileReader_readFromFileAsHandles();
  dxfg_v4_InstrumentProfileReader_readFromFile() replaces dxfg_InstrumentProfileReader_readFromFile4() and
  dxfg_InstrumentProfileReader_readFromFile7().
- dxfg_Iterable_InstrumentProfile_next() -> dxfg_v4_Iterable_InstrumentProfile_nextHandle();
  dxfg_v4_Iterable_InstrumentProfile_next() replaces dxfg_Iterable_InstrumentProfile_next2().
- dxfg_PriceLevelService_getOrders() -> dxfg_v4_PriceLevelService_getOrdersWithCaller();
  dxfg_v4_PriceLevelService_getOrders() replaces dxfg_PriceLevelService_getOrders2().
- dxfg_PriceLevelService_getQuotes() -> dxfg_v4_PriceLevelService_getQuotesWithCaller();
  dxfg_v4_PriceLevelService_getQuotes() replaces dxfg_PriceLevelService_getQuotes2().
- dxfg_Schedule_getInstance() -> dxfg_v4_Schedule_getInstanceByProfileHandle();
  dxfg_v4_Schedule_getInstance() replaces dxfg_Schedule_getInstance4().
- dxfg_Schedule_getTradingVenues() -> dxfg_v4_Schedule_getTradingVenuesByProfileHandle();
  dxfg_v4_Schedule_getTradingVenues() replaces dxfg_Schedule_getTradingVenues2().
<!-- dxfg-renamed:end -->

The replacements of a few older functions are alternatives of another shape rather than twins, so no `dxfg_v4_`
function has their names:

<!-- dxfg-alternatives:begin -->
- dxfg_DXEndpoint_executor() -> dxfg_v4_DXEndpoint_Builder_withExecutor(): the executor is set on the builder,
  before the endpoint is built, not on an endpoint.
- dxfg_InstrumentProfileCustomFields_addNonEmptyFieldNames() ->
  dxfg_v4_InstrumentProfileCustomFields_getNonEmptyFieldNames(): the same list, without the `updated` flag.
- dxfg_free_strings() -> dxfg_v4_string_list_release(): the v4 functions return lists instead of arrays and sizes.
- dxfg_free_string_to_double_map_entries() -> dxfg_v4_string_to_double_map_entry_list_release(): the same, for the
  entries of a map.
<!-- dxfg-alternatives:end -->

A wrapper generator that reads `dxfg-api.json` follows the rule by construction when it maps every deprecated function
to its `replacements` and never derives a `dxfg_v4_` name from an older one.
