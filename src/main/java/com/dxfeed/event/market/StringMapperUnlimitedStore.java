package com.dxfeed.event.market;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class StringMapperUnlimitedStore extends Mapper<String, CCharPointer> {

  protected final Map<String, PinnedObject> cache = new ConcurrentHashMap<>();

  @Override
  public CCharPointer nativeObject(final String jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    return cache.computeIfAbsent(
        jObject,
        string -> {
          byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
          bytes = Arrays.copyOf(bytes, bytes.length + 1);
          return PinnedObject.create(bytes);
        }
    ).addressOfArrayElement(0);
  }

  @Override
  public void delete(final CCharPointer nObject) {
    // cache.remove(
    //     CTypeConversion.toJavaString(
    //        nObject,
    //        SubstrateUtil.strlen(nObject),
    //        StandardCharsets.UTF_8
    //    )
    // ).close();
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
