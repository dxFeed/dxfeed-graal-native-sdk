package com.dxfeed.api.endpoint;

import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_endpoint_state_change_listener_t")
interface DxfgStateChangeListener extends PointerBase {

  @CField("java_object_handle")
  ObjectHandle getJavaObjectHandler();

  @CField("java_object_handle")
  void setJavaObjectHandler(ObjectHandle value);

  @CField("dxfg_endpoint_state_change_listener")
  DxfgStateChangeListenerFunction getStateChangeListener();

  @CField("dxfg_endpoint_state_change_listener")
  void setStateChangeListener(DxfgStateChangeListenerFunction printFunction);
}
