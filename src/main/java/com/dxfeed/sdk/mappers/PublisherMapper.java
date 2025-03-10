// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import com.dxfeed.api.DXPublisher;
import com.dxfeed.sdk.publisher.DxfgPublisher;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class PublisherMapper extends JavaObjectHandlerMapper<DXPublisher, DxfgPublisher> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgPublisher.class);
    }
}
