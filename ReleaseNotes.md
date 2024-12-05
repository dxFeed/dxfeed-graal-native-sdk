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

