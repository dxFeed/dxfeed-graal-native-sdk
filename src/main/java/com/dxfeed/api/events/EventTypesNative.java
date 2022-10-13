// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api.events;

import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;
import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

import java.util.Collections;
import java.util.List;

@CContext(EventTypesNative.NativeDirectives.class)
@CEnum("dxf_graal_event_type_t")
public enum EventTypesNative {
    DXF_GRAAL_EVENT_TYPE_QUOTE,
    DXF_GRAAL_EVENT_TYPE_TIME_AND_SALE;

    static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/dxf_graal_events.h"));
        }
    }

    public static Class<?> toEventType(int value) {
        switch (fromCValue(value)) {
            case DXF_GRAAL_EVENT_TYPE_QUOTE:
                return Quote.class;
            case DXF_GRAAL_EVENT_TYPE_TIME_AND_SALE:
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
