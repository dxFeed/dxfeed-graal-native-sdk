// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.common;

import org.graalvm.nativeimage.c.CContext;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

class Directives implements CContext.Directives {

  @Override
  public List<String> getHeaderFiles() {
    return Collections.singletonList(
        "\"" + Path.of(System.getProperty("project.path"), "/src/main/c/api/dxfg_common.h")
            .toAbsolutePath() + "\"");
  }
}
