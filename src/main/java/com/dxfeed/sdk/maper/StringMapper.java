package com.dxfeed.sdk.maper;

import com.oracle.svm.core.SubstrateUtil;
import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.WordFactory;

public class StringMapper extends Mapper<String, CCharPointer> {

  @Override
  public CCharPointer toNative(final String jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    // ToDo: Avoid creating a byte array, use the previously allocated one instead.
    final byte[] bytes = jObject.getBytes(StandardCharsets.UTF_8);
    // Alloc +1 byte for null-terminate.
    final CCharPointer nObject = UnmanagedMemory.malloc(bytes.length + 1);
    for (int i = 0; i < bytes.length; ++i) {
      nObject.write(i, bytes[i]);
    }
    // Writes null-terminate.
    nObject.write(bytes.length, (byte) 0);
    return nObject;
  }

  @Override
  public void fillNative(final String jObject, final CCharPointer nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void cleanNative(final CCharPointer nObject) {
    // nothing
  }

  @Override
  protected String doToJava(final CCharPointer nObject) {
    return CTypeConversion.toJavaString(
        nObject,
        SubstrateUtil.strlen(nObject),
        StandardCharsets.UTF_8
    );
  }

  @Override
  public void fillJava(final CCharPointer nObject, final String jObject) {
    throw new IllegalStateException("The Java object does not support setters.");
  }
}
