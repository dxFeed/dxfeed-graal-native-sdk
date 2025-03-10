// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.devexperts.auth.AuthToken;
import com.dxfeed.sdk.javac.DxfgAuthToken;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class AuthTokenMapper extends JavaObjectHandlerMapper<AuthToken, DxfgAuthToken> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgAuthToken.class);
    }
}
