// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.api.UtilsNative;
import com.dxfeed.api.events.BaseSymbol;
import com.dxfeed.api.events.QuoteNative;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

import static com.dxfeed.api.events.EventNative.BaseEventNative;

public abstract class BaseEventNativeMapper
        <V extends MarketEvent, T extends BaseEventNative>
        implements NativeMapper<V, T> {
    @Override
    public final T nativeObject(final V qdEvent) {
        final T nativeEvent = createNativeEvent(qdEvent);
        var baseSymbol = (BaseSymbol) UnmanagedMemory.calloc(SizeOf.get(BaseSymbol.class));
        baseSymbol.setSymbolName(UtilsNative.createCString(qdEvent.getEventSymbol()));
        nativeEvent.setSymbol(baseSymbol);
        return nativeEvent;
    }

    @Override
    public void delete(final T nativeEvent) {
        UnmanagedMemory.free(nativeEvent.getSymbol().getSymbolName());
        UnmanagedMemory.free(nativeEvent.getSymbol());
        doDelete(nativeEvent);
    }

    protected abstract T createNativeEvent(final V qdEvent);

    protected abstract void doDelete(final T nativeEvent);
}
