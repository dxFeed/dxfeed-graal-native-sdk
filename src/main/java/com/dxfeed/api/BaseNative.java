package com.dxfeed.api;

import com.dxfeed.event.EventType;
import com.oracle.svm.core.SubstrateUtil;
import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.WordFactory;

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

  protected static DXFeedSubscription<EventType<?>> getDxFeedSubscription(
      final ObjectHandle objectHandle) {
    return getJavaObject(objectHandle);
  }

  protected static DXFeedEventListener<EventType<?>> getDxFeedEventListener(
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

  protected static CCharPointer toCString(final String string) {
    if (string == null) {
      return WordFactory.nullPointer();
    }
    // ToDo: Avoid creating a byte array, use the previously allocated one instead.
    final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
    // Alloc +1 byte for null-terminate.
    final CCharPointer pointer = UnmanagedMemory.malloc(bytes.length + 1);
    for (int i = 0; i < bytes.length; ++i) {
      pointer.write(i, bytes[i]);
    }
    // Writes null-terminate.
    pointer.write(bytes.length, (byte) 0);
    return pointer;
  }
}
