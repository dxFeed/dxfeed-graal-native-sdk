package com.dxfeed.event.market;

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
  public DxfgException toNativeObject(final Throwable jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgException nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgException.class));
    fillNativeObject(jObject, nObject);
    return nObject;
  }

  @Override
  public final void fillNativeObject(final Throwable jObject, final DxfgException nObject) {
    cleanNativeObject(nObject);
    nObject.setClassName(this.stringMapper.toNativeObject(jObject.getClass().getCanonicalName()));
    nObject.setMessage(this.stringMapper.toNativeObject(jObject.getMessage()));
    final StringWriter sw = new StringWriter();
    jObject.printStackTrace(new PrintWriter(sw));
    nObject.setStackTrace(this.stringMapper.toNativeObject(sw.toString()));
  }

  @Override
  protected final void cleanNativeObject(final DxfgException nObject) {
    stringMapper.release(nObject.getClassName());
    stringMapper.release(nObject.getMessage());
    stringMapper.release(nObject.getStackTrace());
  }

  @Override
  public Throwable toJavaObject(final DxfgException nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void fillJavaObject(final DxfgException nObject, final Throwable jObject) {
    throw new IllegalStateException();
  }
}
