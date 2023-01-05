package com.dxfeed.api.model;

import com.oracle.svm.core.c.ProjectHeaderFile;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;

class Directives implements CContext.Directives {

  @Override
  public List<String> getHeaderFiles() {
    return Collections.singletonList(ProjectHeaderFile.resolve(
        "com.dxfeed",
        "src/main/c/api/dxfg_order_book_model.h"));
  }
}
