// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;

/**
 * The header of the types that only the v4 functions of the events use (the configurationChanged callback of a
 * subscription change listener).
 */
class EventsV4Directives implements CContext.Directives {

    @Override
    public List<String> getHeaderFiles() {
        return Collections.singletonList(
                "\"" + Path.of(System.getProperty("project.path"), "/src/main/c/api/dxfg_v4_events.h")
                        .toAbsolutePath() + "\"");
    }
}
