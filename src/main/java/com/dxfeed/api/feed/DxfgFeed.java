package com.dxfeed.api.feed;

import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_feed_t")
public interface DxfgFeed extends PointerBase {

  @CField("f_java_object_handle")
  ObjectHandle getJavaObjectHandler();

  @CField("f_java_object_handle")
  void setJavaObjectHandler(ObjectHandle value);
}
