package com.dxfeed.sdk.maper;

import com.dxfeed.sdk.exception.DxfgException;
import com.dxfeed.sdk.exception.DxfgStackTraceElement;
import com.dxfeed.sdk.exception.DxfgStackTraceElementList;
import com.dxfeed.sdk.exception.DxfgStackTraceElementPointer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class ExceptionMapper extends Mapper<Throwable, DxfgException> {

  protected final Mapper<String, CCharPointer> stringMapper;
  protected final ListMapper<StackTraceElement, DxfgStackTraceElement, DxfgStackTraceElementPointer, DxfgStackTraceElementList> stackTraceElementListMapper;

  public ExceptionMapper(
      final Mapper<String, CCharPointer> stringMapper,
      final ListMapper<StackTraceElement, DxfgStackTraceElement, DxfgStackTraceElementPointer, DxfgStackTraceElementList> stackTraceElementListMapper
  ) {
    this.stringMapper = stringMapper;
    this.stackTraceElementListMapper = stackTraceElementListMapper;
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
    final StackTraceElement[] stackTrace = jObject.getStackTrace();
    final ArrayList<StackTraceElement> stackTraceElements = new ArrayList<>(stackTrace.length);
    Collections.addAll(stackTraceElements, stackTrace);
    nObject.setStackTrace(this.stackTraceElementListMapper.toNativeList(stackTraceElements));
    nObject.setCause(toNative(jObject.getCause()));
    final StringWriter sw = new StringWriter();
    jObject.printStackTrace(new PrintWriter(sw));
    nObject.setPrintStackTrace(this.stringMapper.toNative(
        sw.toString().replaceFirst("^.*" + System.lineSeparator(), ""))
    );
  }

  @Override
  public final void cleanNative(final DxfgException nObject) {
    this.stringMapper.release(nObject.getClassName());
    this.stringMapper.release(nObject.getMessage());
    this.stackTraceElementListMapper.release(nObject.getStackTrace());
    release(nObject.getCause());
    this.stringMapper.release(nObject.getPrintStackTrace());
  }

  @Override
  protected Throwable doToJava(final DxfgException nObject) {
    throw new IllegalStateException();
  }

  @Override
  public void fillJava(final DxfgException nObject, final Throwable jObject) {
    throw new IllegalStateException("The Java object does not support setters.");
  }
}
