// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.api.events;

import com.oracle.svm.core.c.ProjectHeaderFile;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;

import java.util.*;

@CContext(EventNative.NativeDirectives.class)
public class EventNative {
    static class NativeDirectives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            return Collections.singletonList(ProjectHeaderFile.resolve(
                    "com.dxfeed",
                    "src/main/c/dxf_graal_events.h"));
        }
    }

    @CStruct("dxf_graal_event_t")
    public interface BaseEventNative extends PointerBase {
        @CField("event_type")
        int getEventType();

        @CField("event_type")
        void setEventType(int eventType);

        @CField("symbol_name")
        CCharPointer getSymbolName();

        @CField("symbol_name")
        void setSymbolName(CCharPointer symbolName);
    }

    @CPointerTo(BaseEventNative.class)
    public interface BaseEventNativePtr extends PointerBase {
        BaseEventNativePtr addressOf(int index);

        BaseEventNative read();

        void write(BaseEventNative data);
    }
}
