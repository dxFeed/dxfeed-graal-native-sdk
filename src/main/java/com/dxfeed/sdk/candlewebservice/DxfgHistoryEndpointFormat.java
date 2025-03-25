// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.candlewebservice;

import static com.dxfeed.sdk.candlewebservice.HistoryEndpoint.Format.BINARY;
import static com.dxfeed.sdk.candlewebservice.HistoryEndpoint.Format.CSV;
import static com.dxfeed.sdk.candlewebservice.HistoryEndpoint.Format.TEXT;

import java.util.EnumMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_history_endpoint_format_t")
public enum DxfgHistoryEndpointFormat {
    DXFG_HISTORY_ENDPOINT_FORMAT_TEXT(TEXT),
    DXFG_HISTORY_ENDPOINT_FORMAT_CSV(CSV),
    DXFG_HISTORY_ENDPOINT_FORMAT_BINARY(BINARY);

    private static final Map<HistoryEndpoint.Format, DxfgHistoryEndpointFormat> MAP = new EnumMap<>(
            HistoryEndpoint.Format.class);

    static {
        for (DxfgHistoryEndpointFormat value : values()) {
            MAP.put(value.qdFormat, value);
        }
    }

    public final HistoryEndpoint.Format qdFormat;

    DxfgHistoryEndpointFormat(final HistoryEndpoint.Format qdFormat) {
        this.qdFormat = qdFormat;
    }

    public static DxfgHistoryEndpointFormat of(final HistoryEndpoint.Format format) {
        return MAP.get(format);
    }

    @CEnumLookup
    public static native DxfgHistoryEndpointFormat fromCValue(int value);

    @CEnumValue
    public native int getCValue();
}

