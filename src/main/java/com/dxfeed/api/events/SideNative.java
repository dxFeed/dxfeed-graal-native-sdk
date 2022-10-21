// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api.events;

import com.dxfeed.event.market.Side;
import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

import java.util.Collections;
import java.util.List;

@CContext(SideNative.NativeDirectives.class)
@CEnum("dxfg_event_side")
public enum SideNative {
    DXFG_EVENT_SIDE_UNDEFINED,
    DXFG_EVENT_SIDE_BUY,
    DXFG_EVENT_SIDE_SELL;

    static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/dxfg_events.h"));
        }
    }

    public static SideNative fromSide(Side side) {
        switch (side) {
            case BUY:
                return DXFG_EVENT_SIDE_BUY;
            case SELL:
                return DXFG_EVENT_SIDE_SELL;
            default:
                return DXFG_EVENT_SIDE_UNDEFINED;
        }
    }

    @CEnumValue
    public native int getCValue();

    @CEnumLookup
    public static native SideNative fromCValue(int value);
}
