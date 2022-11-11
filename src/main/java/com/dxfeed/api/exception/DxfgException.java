package com.dxfeed.api.exception;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_exception_t")
public interface DxfgException extends PointerBase {

  @CField("className")
  CCharPointer getClassName();

  @CField("className")
  void setClassName(CCharPointer className);

  @CField("message")
  CCharPointer getMessage();

  @CField("message")
  void setMessage(CCharPointer message);

  @CField("stackTrace")
  CCharPointer getStackTrace();

  @CField("stackTrace")
  void setStackTrace(CCharPointer stackTrace);
}
