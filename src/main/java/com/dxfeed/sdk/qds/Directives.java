package com.dxfeed.sdk.qds;

import org.graalvm.nativeimage.c.CContext;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

class Directives implements CContext.Directives {

  @Override
  public List<String> getHeaderFiles() {
    return Collections.singletonList("\"" + Path.of(System.getProperty("project.path"),"/src/main/c/api/dxfg_qds.h").toAbsolutePath() + "\"");
  }
}
