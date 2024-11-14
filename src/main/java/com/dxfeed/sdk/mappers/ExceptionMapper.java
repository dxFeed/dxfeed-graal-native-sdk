package com.dxfeed.sdk.mappers;

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

  public ExceptionMapper(final Mapper<String, CCharPointer> stringMapper,
      final ListMapper<StackTraceElement, DxfgStackTraceElement, DxfgStackTraceElementPointer, DxfgStackTraceElementList> stackTraceElementListMapper) {
    this.stringMapper = stringMapper;
    this.stackTraceElementListMapper = stackTraceElementListMapper;
  }

  @Override
  public DxfgException toNative(final Throwable javaObject) {
    if (javaObject == null) {
      return WordFactory.nullPointer();
    }

    final DxfgException nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgException.class));

    fillNative(javaObject, nativeObject,
        false); // //There is no need to clean since the memory has just been allocated and zeroed.

    return nativeObject;
  }

  @Override
  public final void fillNative(final Throwable javaObject, final DxfgException nativeObject,
      boolean clean) {
    if (clean) {
      cleanNative(nativeObject);
    }

    nativeObject.setClassName(this.stringMapper.toNative(javaObject.getClass().getCanonicalName()));
    nativeObject.setMessage(this.stringMapper.toNative(javaObject.getMessage()));
    final StackTraceElement[] stackTrace = javaObject.getStackTrace();
    final ArrayList<StackTraceElement> stackTraceElements = new ArrayList<>(stackTrace.length);
    Collections.addAll(stackTraceElements, stackTrace);
    nativeObject.setStackTrace(this.stackTraceElementListMapper.toNativeList(stackTraceElements));
    nativeObject.setCause(toNative(javaObject.getCause()));
    final StringWriter sw = new StringWriter();
    javaObject.printStackTrace(new PrintWriter(sw));
    nativeObject.setPrintStackTrace(
        this.stringMapper.toNative(sw.toString().replaceFirst("^.*" + System.lineSeparator(), "")));
  }

  @Override
  public final void cleanNative(final DxfgException nativeObject) {
    this.stringMapper.release(nativeObject.getClassName());
    this.stringMapper.release(nativeObject.getMessage());
    this.stackTraceElementListMapper.release(nativeObject.getStackTrace());
    release(nativeObject.getCause());
    this.stringMapper.release(nativeObject.getPrintStackTrace());
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
