// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
@CStruct("dxfg_text_configuration_t")
public interface DxfgTextConfiguration extends DxfgEventType {

    @CField("event_symbol")
    CCharPointer getEventSymbol();

    @CField("event_symbol")
    void setEventSymbol(CCharPointer eventSymbol);

    @CField("event_time")
    long getEventTime();

    @CField("event_time")
    void setEventTime(long eventTime);

    @CField("time_sequence")
    long getTimeSequence();

    @CField("time_sequence")
    void setTimeSequence(long value);

    @CField("version")
    int getVersion();

    @CField("version")
    void setVersion(int eventTime);

    @CField("text")
    CCharPointer getText();

    @CField("text")
    void setText(CCharPointer value);
}
