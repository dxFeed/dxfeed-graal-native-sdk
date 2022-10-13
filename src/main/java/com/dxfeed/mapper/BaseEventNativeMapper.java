// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.mapper;

import com.dxfeed.api.UtilsNative;
import com.dxfeed.event.market.MarketEvent;
import org.graalvm.nativeimage.UnmanagedMemory;

import static com.dxfeed.api.events.EventNative.BaseEventNative;

public abstract class BaseEventNativeMapper
        <V extends MarketEvent, T extends BaseEventNative>
        implements NativeMapper<V, T> {
    @Override
    public final T nativeObject(final V qdEvent) {
        final T nativeEvent = createNativeEvent(qdEvent);
        nativeEvent.setSymbolName(UtilsNative.createCString(qdEvent.getEventSymbol()));
        return nativeEvent;
    }

    @Override
    public void delete(final T nativeEvent) {
        UnmanagedMemory.free(nativeEvent.getSymbolName());
        doDelete(nativeEvent);
    }

    protected abstract T createNativeEvent(final V qdEvent);

    protected abstract void doDelete(final T nativeEvent);
}
