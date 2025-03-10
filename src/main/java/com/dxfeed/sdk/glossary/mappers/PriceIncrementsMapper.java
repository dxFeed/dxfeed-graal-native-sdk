// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.glossary.mappers;

import com.dxfeed.glossary.PriceIncrements;
import com.dxfeed.sdk.glossary.DxfgPriceIncrementsHandle;
import com.dxfeed.sdk.mappers.JavaObjectHandlerMapper;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class PriceIncrementsMapper extends
        JavaObjectHandlerMapper<PriceIncrements, DxfgPriceIncrementsHandle> {

    @Override
    protected int getSizeJavaObjectHandler() {
        return SizeOf.get(DxfgPriceIncrementsHandle.class);
    }
}
