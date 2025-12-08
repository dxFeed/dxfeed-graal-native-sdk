// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs.mappers;

import com.dxfeed.orcs.api.AuthOrderSource;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import com.dxfeed.sdk.orcs.DxfgAuthOrderSourceHandle;
import org.graalvm.nativeimage.c.struct.SizeOf;
public class AuthOrderSourceMapper extends
        JavaObjectHandlerMapper<AuthOrderSource, DxfgAuthOrderSourceHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgAuthOrderSourceHandle.class);
    }
}
