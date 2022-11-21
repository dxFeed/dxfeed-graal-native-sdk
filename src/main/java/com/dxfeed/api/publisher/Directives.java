package com.dxfeed.api.publisher;

import com.oracle.svm.core.c.ProjectHeaderFile;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;

class Directives implements CContext.Directives {

  @Override
  public List<String> getHeaderFiles() {
    return Collections.singletonList(ProjectHeaderFile.resolve(
        "com.dxfeed",
        "src/main/c/api/dxfg_publisher.h"));
  }
}
