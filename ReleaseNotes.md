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

