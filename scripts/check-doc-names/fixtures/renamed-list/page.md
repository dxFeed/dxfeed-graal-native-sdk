# Migration

<!-- dxfg-deprecated:begin -->
Without the digit suffix, dxfg_Schedule_getInstance4() is replaced by dxfg_v4_Schedule_getInstance(). These are all:
<!-- dxfg-deprecated:end -->

<!-- dxfg-renamed:begin -->
- dxfg_Schedule_getInstance() ->
  dxfg_v4_Schedule_getInstanceByProfileHandle();
  dxfg_v4_Schedule_getInstance() replaces dxfg_Schedule_getInstance4().
<!-- dxfg-renamed:end -->

The alternatives of another shape:

<!-- dxfg-alternatives:begin -->
- dxfg_DXEndpoint_executor() -> dxfg_v4_DXEndpoint_Builder_withExecutor(): set on the builder.
- dxfg_free_strings() -> dxfg_v4_string_list_release().
<!-- dxfg-alternatives:end -->
