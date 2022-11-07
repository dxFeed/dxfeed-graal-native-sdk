package com.dxfeed.api.subscription;

import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_subscription_event_listener_t")
public interface DxfgEventListener extends PointerBase {

  @CField("f_java_object_handle")
  ObjectHandle getJavaObjectHandler();

  @CField("f_java_object_handle")
  void setJavaObjectHandler(ObjectHandle value);

  @CField("dxfg_subscription_event_listener")
  DxfgEventListenerFunction getEventListener();

  @CField("dxfg_subscription_event_listener")
  void setEventListener(DxfgEventListenerFunction printFunction);
}
