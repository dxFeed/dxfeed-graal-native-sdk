// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.candlewebservice;

import static com.dxfeed.sdk.candlewebservice.HistoryEndpoint.Compression.GZIP;
import static com.dxfeed.sdk.candlewebservice.HistoryEndpoint.Compression.NONE;
import static com.dxfeed.sdk.candlewebservice.HistoryEndpoint.Compression.ZIP;

import java.util.EnumMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_history_endpoint_compression_t")
public enum DxfgHistoryEndpointCompression {
    DXFG_HISTORY_ENDPOINT_COMPRESSION_NONE(NONE),
    DXFG_HISTORY_ENDPOINT_COMPRESSION_ZIP(ZIP),
    DXFG_HISTORY_ENDPOINT_COMPRESSION_GZIP(GZIP),
    ;

    private static final Map<HistoryEndpoint.Compression, DxfgHistoryEndpointCompression> MAP = new EnumMap<>(
            HistoryEndpoint.Compression.class);

    static {
        for (DxfgHistoryEndpointCompression value : values()) {
            MAP.put(value.qdCompression, value);
        }
    }

    public final HistoryEndpoint.Compression qdCompression;

    DxfgHistoryEndpointCompression(final HistoryEndpoint.Compression qdCompression) {
        this.qdCompression = qdCompression;
    }

    public static DxfgHistoryEndpointCompression of(final HistoryEndpoint.Compression compression) {
        return MAP.get(compression);
    }

    @CEnumLookup
    public static native DxfgHistoryEndpointCompression fromCValue(int value);

    @CEnumValue
    public native int getCValue();
}
