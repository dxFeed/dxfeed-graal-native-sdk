package com.dxfeed.api;

import com.oracle.svm.core.SubstrateUtil;
import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

public interface NativeUtils {

  static ObjectHandle createHandler(final Object object) {
    return ObjectHandles.getGlobal().create(object);
  }

  static <T> T extractHandler(final ObjectHandle objectHandle) {
    return ObjectHandles.getGlobal().get(objectHandle);
  }

  static void destroyHandler(final ObjectHandle objectHandle) {
    ObjectHandles.getGlobal().destroy(objectHandle);
  }

  static String toJavaString(final CCharPointer cCharPointer) {
    return CTypeConversion.toJavaString(
        cCharPointer,
        SubstrateUtil.strlen(cCharPointer),
        StandardCharsets.UTF_8
    );
  }
}
