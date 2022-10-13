// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.mapper;

import com.dxfeed.api.events.EventNative.BaseEventNativePtr;
import com.dxfeed.api.events.EventTypesNative;
import com.dxfeed.api.events.QuoteNative;
import com.dxfeed.event.EventType;
import com.dxfeed.event.market.Quote;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

import java.util.List;

import static com.dxfeed.api.events.EventNative.*;

public class ListEventMapper implements ListNativeMapper<EventType<?>, BaseEventNativePtr> {
    private static final QuoteNativeMapper QUOTE_NATIVE_MAPPER = new QuoteNativeMapper();

    @Override
    public BaseEventNativePtr nativeObject(List<EventType<?>> jObjects) {
        BaseEventNativePtr events = UnmanagedMemory.calloc(SizeOf.get(BaseEventNativePtr.class) * jObjects.size());
        for (int i = 0; i < jObjects.size(); ++i) {
            var eventType = jObjects.get(i);
            BaseEventNative nativeObject;
            if (eventType instanceof Quote) {
                nativeObject = QUOTE_NATIVE_MAPPER.nativeObject((Quote) eventType);
                events.addressOf(i).write(nativeObject);
            }
        }
        return events;
    }

    @Override
    public void delete(BaseEventNativePtr cObject, int size) {
        for (int i = 0; i < size; ++i) {
            var nativeObject = cObject.addressOf(i).read();
            var eventType = nativeObject.getEventType();
            if (EventTypesNative.fromCValue(eventType) == EventTypesNative.DXF_GRAAL_EVENT_TYPE_QUOTE) {
                QUOTE_NATIVE_MAPPER.delete((QuoteNative) nativeObject);
            }
        }
        UnmanagedMemory.free(cObject);
    }
}
