// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;

/**
 * The header of the v4 system functions (dxfg_v4_system.h), which declares the structs of the group.
 */
class SystemV4Directives implements CContext.Directives {

    @Override
    public List<String> getHeaderFiles() {
        return Collections.singletonList(
                "\"" + Path.of(System.getProperty("project.path"), "/src/main/c/api/dxfg_v4_system.h").toAbsolutePath()
                        + "\"");
    }
}
