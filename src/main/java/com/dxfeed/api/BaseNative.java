package com.dxfeed.api;

import com.dxfeed.event.EventType;
import com.oracle.svm.core.SubstrateUtil;
import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

public abstract class BaseNative {

  protected static ObjectHandle createJavaObjectHandler(final Object object) {
    return ObjectHandles.getGlobal().create(object);
  }

  protected static <T> T getJavaObject(final ObjectHandle objectHandle) {
    return ObjectHandles.getGlobal().get(objectHandle);
  }

  protected static DXEndpoint getDxEndpoint(final ObjectHandle objectHandle) {
    return getJavaObject(objectHandle);
  }

  protected static DXEndpoint.Builder getDxEndpointBuilder(final ObjectHandle objectHandle) {
    return getJavaObject(objectHandle);
  }

  protected static DXFeedSubscription<EventType<?>> getDxFeedSubscription(
      final ObjectHandle objectHandle
  ) {
    return getJavaObject(objectHandle);
  }

  protected static void destroyJavaObjectHandler(final ObjectHandle objectHandle) {
    ObjectHandles.getGlobal().destroy(objectHandle);
  }

  protected static String toJavaString(final CCharPointer cCharPointer) {
    return CTypeConversion.toJavaString(
        cCharPointer,
        SubstrateUtil.strlen(cCharPointer),
        StandardCharsets.UTF_8
    );
  }
}
