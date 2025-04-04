// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.event.EventMapper;
import com.dxfeed.sdk.events.DxfgMarketEvent;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.c.type.CCharPointer;

public abstract class MarketEventMapper<T extends MarketEvent, V extends DxfgMarketEvent>
        extends EventMapper<T, V> {

    protected final Mapper<String, CCharPointer> stringMapper;

    public MarketEventMapper(final Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    public void fillNative(final T javaObject, final V nativeObject, boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setEventSymbol(this.stringMapper.toNative(javaObject.getEventSymbol()));
        nativeObject.setEventTime(javaObject.getEventTime());
    }

    @Override
    public void cleanNative(final V nativeObject) {
        this.stringMapper.release(nativeObject.getEventSymbol());
    }

    @Override
    public void fillJava(final V nativeObject, final T javaObject) {
        javaObject.setEventSymbol(this.stringMapper.toJava(nativeObject.getEventSymbol()));
        javaObject.setEventTime(nativeObject.getEventTime());
    }

    @Override
    public V createNativeObject(final String symbol) {
        final V nativeObject = createNativeObject();

        nativeObject.setEventSymbol(this.stringMapper.toNative(symbol));

        return nativeObject;
    }
}
