package com.dxfeed.event.market;

import com.oracle.svm.core.SubstrateUtil;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.WordFactory;

public class StringMapperUnlimitedStore extends Mapper<String, CCharPointer> {

  protected final Map<String, PinnedObject> cache = new ConcurrentHashMap<>();

  @Override
  public CCharPointer toNativeObject(final String jObject) {
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
  public void release(final CCharPointer nObject) {
    // cache.remove(toJavaObject(nObject)).close();
  }

  @Override
  public void fillNativeObject(final String jObject, final CCharPointer nObject) {
    // nothing
  }

  @Override
  protected void cleanNativeObject(final CCharPointer nObject) {
    // nothing
  }

  @Override
  public String toJavaObject(final CCharPointer nObject) {
    if (nObject.isNull()) {
      return null;
    }
    return CTypeConversion.toJavaString(
        nObject,
        SubstrateUtil.strlen(nObject),
        StandardCharsets.UTF_8
    );
  }

  @Override
  public void fillJavaObject(final CCharPointer nObject, final String jObject) {
    throw new IllegalStateException();
  }
}
