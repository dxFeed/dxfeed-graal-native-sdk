package com.dxfeed.sdk.javac;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.word.PointerBase;

@CContext(Directives.class)
@CStruct("dxfg_list")
public interface CList<T extends CPointerOnPointer<?>> extends PointerBase {

  @CField("elements")
  T getElements();

  @CField("elements")
  void setElements(T value);

  @CField("size")
  int getSize();

  @CField("size")
  void setSize(int value);
}
