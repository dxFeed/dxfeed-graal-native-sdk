// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.ondemand;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;

/**
 * The header of the types that only the v4 functions of the on-demand service use (the property change listener).
 */
class OnDemandV4Directives implements CContext.Directives {

    @Override
    public List<String> getHeaderFiles() {
        return Collections.singletonList(
                "\"" + Path.of(System.getProperty("project.path"), "/src/main/c/api/dxfg_v4_ondemand.h")
                        .toAbsolutePath() + "\"");
    }
}
