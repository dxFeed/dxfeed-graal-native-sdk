package com.dxfeed.api.events;

import com.oracle.svm.core.c.ProjectHeaderFile;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

@CContext(BaseSymbol.NativeDirectives.class)
@CStruct("dxfg_symbol_t")
public interface BaseSymbol extends PointerBase {
  class NativeDirectives implements CContext.Directives {
    @Override
    public List<String> getHeaderFiles() {
      return Collections.singletonList(ProjectHeaderFile.resolve(
          "com.dxfeed",
          "src/main/c/dxfg_events.h"));
    }
  }

  @CField("symbol_name")
  CCharPointer getSymbolName();

  @CField("symbol_name")
  void setSymbolName(CCharPointer symbolName);
}
