package com.dxfeed.sdk.exception;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_exception_t")
public interface DxfgException extends PointerBase {

  @CField("class_name")
  CCharPointer getClassName();

  @CField("class_name")
  void setClassName(CCharPointer className);

  @CField("message")
  CCharPointer getMessage();

  @CField("message")
  void setMessage(CCharPointer message);

  @CField("print_stack_trace")
  CCharPointer getPrintStackTrace();

  @CField("print_stack_trace")
  void setPrintStackTrace(CCharPointer stackTrace);

  @CField("stack_trace")
  DxfgStackTraceElementList getStackTrace();

  @CField("stack_trace")
  void setStackTrace(DxfgStackTraceElementList stackTrace);

  @CField("cause")
  DxfgException getCause();

  @CField("cause")
  void setCause(DxfgException cause);
}
