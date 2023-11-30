package com.dxfeed.sdk.exception;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_stack_trace_element_t")
public interface DxfgStackTraceElement extends PointerBase {

  @CField("class_name")
  CCharPointer getClassName();

  @CField("class_name")
  void setClassName(CCharPointer value);

  @CField("class_loader_name")
  CCharPointer getClassLoaderName();

  @CField("class_loader_name")
  void setClassLoaderName(CCharPointer value);

  @CField("file_name")
  CCharPointer getFileName();

  @CField("file_name")
  void setFileName(CCharPointer value);

  @CField("method_name")
  CCharPointer getMethodName();

  @CField("method_name")
  void setMethodName(CCharPointer value);

  @CField("line_number")
  int getLineNumber();

  @CField("line_number")
  void setLineNumber(int value);

  @CField("is_native_method")
  int getIsNativeMethod();

  @CField("is_native_method")
  void setIsNativeMethod(int value);

  @CField("module_name")
  CCharPointer getModuleName();

  @CField("module_name")
  void setModuleName(CCharPointer value);

  @CField("module_version")
  CCharPointer getModuleVersion();

  @CField("module_version")
  void setModuleVersion(CCharPointer value);
}
