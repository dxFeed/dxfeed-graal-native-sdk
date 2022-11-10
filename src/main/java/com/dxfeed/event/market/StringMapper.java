package com.dxfeed.event.market;

import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class StringMapper extends Mapper<String, CCharPointer> {

  @Override
  public CCharPointer nativeObject(final String jObject) {
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
  protected int size() {
    throw new IllegalStateException(
        "The size of the string depends on the content. It is an array."
    );
  }

  @Override
  protected void fillNativeObject(final CCharPointer nObject, final String jObject) {
    // nothing
  }

  @Override
  protected void cleanNativeObject(final CCharPointer nObject) {
    // nothing
  }
}
