// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.source;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_indexed_event_source_type_t")
public enum DxfgIndexedEventSourceType {
    INDEXED_EVENT_SOURCE,
    ORDER_SOURCE,
    ;

    @CEnumLookup
    public static native DxfgIndexedEventSourceType fromCValue(int value);

    @CEnumValue
    public native int getCValue();
}
