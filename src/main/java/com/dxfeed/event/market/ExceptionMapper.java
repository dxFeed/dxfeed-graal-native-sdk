package com.dxfeed.event.market;

import com.dxfeed.api.exception.DxfgException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class ExceptionMapper extends Mapper<Throwable, DxfgException> {

  protected final Mapper<String, CCharPointer> stringMapper;

  public ExceptionMapper(final Mapper<String, CCharPointer> stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgException.class);
  }

  @Override
  protected final void fillNativeObject(final DxfgException nObject, final Throwable jObject) {
    nObject.setClassName(this.stringMapper.nativeObject(jObject.getClass().getCanonicalName()));
    nObject.setMessage(this.stringMapper.nativeObject(jObject.getMessage()));
    final StringWriter sw = new StringWriter();
    jObject.printStackTrace(new PrintWriter(sw));
    nObject.setStackTrace(this.stringMapper.nativeObject(sw.toString()));
  }

  @Override
  protected final void cleanNativeObject(final DxfgException nObject) {
    stringMapper.delete(nObject.getClassName());
    stringMapper.delete(nObject.getMessage());
    stringMapper.delete(nObject.getStackTrace());
  }
}
