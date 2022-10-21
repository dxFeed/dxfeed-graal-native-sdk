// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api.events;

import com.dxfeed.event.EventType;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;
import com.oracle.svm.core.c.ProjectHeaderFile;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(EventTypesNative.NativeDirectives.class)
@CEnum("dxfg_event_type_t")
public enum EventTypesNative {
    DXFG_EVENT_TYPE_QUOTE,
    DXFG_EVENT_TYPE_TIME_AND_SALE;

    static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/api/dxfg_events.h"));
        }
    }

    public static Class<? extends EventType<?>> toEventType(int value) {
        switch (fromCValue(value)) {
            case DXFG_EVENT_TYPE_QUOTE:
                return Quote.class;
            case DXFG_EVENT_TYPE_TIME_AND_SALE:
                return TimeAndSale.class;
            default:
                return null;
        }
    }

    @CEnumValue
    public native int getCValue();

    @CEnumLookup
    public static native EventTypesNative fromCValue(int value);
}
