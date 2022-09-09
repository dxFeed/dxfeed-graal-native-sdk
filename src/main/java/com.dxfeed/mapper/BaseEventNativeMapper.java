package com.dxfeed.mapper;

import com.dxfeed.NativeLibApi;
import com.dxfeed.event.market.MarketEvent;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.type.CCharPointer;

import java.nio.charset.StandardCharsets;

public abstract class BaseEventNativeMapper<V extends MarketEvent, T extends NativeLibApi.EventNative> implements NativeMapper<V, T> {

    protected static CCharPointer allocCString(final String string) {
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        final CCharPointer pointer = UnmanagedMemory.calloc(bytes.length + 1);
        for (int i = 0; i < bytes.length; ++i) {
            pointer.addressOf(i).write(bytes[i]);
        }
        return pointer;
    }

    @Override
    public final T nativeObject(final V qdEvent) {
        final T nativeEvent = createNativeEvent(qdEvent);
        nativeEvent.setSymbolName(allocCString(qdEvent.getEventSymbol()));
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
