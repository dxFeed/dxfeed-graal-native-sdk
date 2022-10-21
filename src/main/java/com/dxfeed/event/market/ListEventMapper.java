// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.api.events.EventNative.BaseEventNativePtr;
import com.dxfeed.api.events.EventTypesNative;
import com.dxfeed.api.events.QuoteNative;
import com.dxfeed.api.events.TimeAndSaleNative;
import com.dxfeed.event.EventType;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.WordFactory;

import java.util.*;

import static com.dxfeed.api.events.EventNative.*;

public class ListEventMapper implements ListNativeMapper<EventType<?>, BaseEventNativePtr> {
    private static final QuoteNativeMapper QUOTE_NATIVE_MAPPER = new QuoteNativeMapper();
    private static final TimeAndSaleNativeMapper TIME_AND_SALE_NATIVE_MAPPER = new TimeAndSaleNativeMapper();

    @Override
    public BaseEventNativePtr nativeObject(List<EventType<?>> events) {
        BaseEventNativePtr nativeEvents =
                UnmanagedMemory.calloc(SizeOf.get(BaseEventNativePtr.class) * events.size());
        for (int i = 0; i < events.size(); ++i) {
            var eventType = events.get(i);
            BaseEventNative nativeEvent;
            if (eventType instanceof Quote) {
                nativeEvent = QUOTE_NATIVE_MAPPER.nativeObject((Quote) eventType);
            } else if (eventType instanceof TimeAndSale) {
                nativeEvent = TIME_AND_SALE_NATIVE_MAPPER.nativeObject((TimeAndSale) eventType);
            } else {
                nativeEvent = WordFactory.nullPointer();
            }
            nativeEvents.addressOf(i).write(nativeEvent);
        }
        return nativeEvents;
    }

    @Override
    public void delete(BaseEventNativePtr nativeEvents, int size) {
        for (int i = 0; i < size; ++i) {
            var nativeEvent = nativeEvents.addressOf(i).read();
            if (nativeEvent.isNonNull()) {
                var eventType = nativeEvent.getEventType();
                if (EventTypesNative.fromCValue(eventType) == EventTypesNative.DXFG_EVENT_TYPE_QUOTE) {
                    QUOTE_NATIVE_MAPPER.delete((QuoteNative) nativeEvent);
                } else if (EventTypesNative.fromCValue(eventType) == EventTypesNative.DXFG_EVENT_TYPE_TIME_AND_SALE) {
                    TIME_AND_SALE_NATIVE_MAPPER.delete((TimeAndSaleNative) nativeEvent);
                }
            }
        }
        UnmanagedMemory.free(nativeEvents);
    }
}
