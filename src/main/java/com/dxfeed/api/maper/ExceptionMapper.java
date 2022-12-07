package com.dxfeed.api.maper;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.exception.DxfgException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class ExceptionMapper extends Mapper<Throwable, DxfgException> {

  protected final Mapper<String, CCharPointer> stringMapper;

  public ExceptionMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgException toNative(final Throwable jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgException nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgException.class));
    fillNative(jObject, nObject);
    return nObject;
  }

  @Override
  public final void fillNative(final Throwable jObject, final DxfgException nObject) {
    cleanNative(nObject);
    nObject.setClassName(this.stringMapper.toNative(jObject.getClass().getCanonicalName()));
    nObject.setMessage(this.stringMapper.toNative(jObject.getMessage()));
    final StringWriter sw = new StringWriter();
    jObject.printStackTrace(new PrintWriter(sw));
    nObject.setStackTrace(this.stringMapper.toNative(sw.toString()));
  }

  @Override
  public final void cleanNative(final DxfgException nObject) {
    stringMapper.release(nObject.getClassName());
    stringMapper.release(nObject.getMessage());
    stringMapper.release(nObject.getStackTrace());
  }

  @Override
  public Throwable toJava(final DxfgException nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void fillJava(final DxfgException nObject, final Throwable jObject) {
    throw new IllegalStateException("The Java object does not support setters.");
  }
}
