package com.dxfeed.sdk.mappers;

import com.dxfeed.sdk.exception.DxfgStackTraceElement;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class DxfgStackTraceElementMapper extends Mapper<StackTraceElement, DxfgStackTraceElement> {

  protected final Mapper<String, CCharPointer> stringMapper;

  public DxfgStackTraceElementMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgStackTraceElement toNative(final StackTraceElement jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgStackTraceElement nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgStackTraceElement.class));
    fillNative(jObject, nObject, false);
    return nObject;
  }

  @Override
  public final void fillNative(final StackTraceElement jObject, final DxfgStackTraceElement nObject, boolean clean) {
    if (clean) {
      cleanNative(nObject);
    }

    nObject.setClassName(this.stringMapper.toNative(jObject.getClassName()));
    nObject.setClassLoaderName(this.stringMapper.toNative(jObject.getClassLoaderName()));
    nObject.setFileName(this.stringMapper.toNative(jObject.getFileName()));
    nObject.setMethodName(this.stringMapper.toNative(jObject.getMethodName()));
    nObject.setLineNumber(jObject.getLineNumber());
    nObject.setIsNativeMethod(jObject.isNativeMethod() ? 1 : 0);
    nObject.setModuleName(this.stringMapper.toNative(jObject.getModuleName()));
    nObject.setModuleVersion(this.stringMapper.toNative(jObject.getModuleVersion()));
  }

  @Override
  public final void cleanNative(final DxfgStackTraceElement nObject) {
    stringMapper.release(nObject.getClassName());
    stringMapper.release(nObject.getClassLoaderName());
    stringMapper.release(nObject.getFileName());
    stringMapper.release(nObject.getModuleName());
    stringMapper.release(nObject.getModuleVersion());
  }

  @Override
  protected StackTraceElement doToJava(final DxfgStackTraceElement nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void fillJava(final DxfgStackTraceElement nObject, final StackTraceElement jObject) {
    throw new IllegalStateException("The Java object does not support setters.");
  }
}
