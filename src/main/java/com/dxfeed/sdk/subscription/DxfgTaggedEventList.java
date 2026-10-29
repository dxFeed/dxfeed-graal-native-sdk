// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CLongPointer;

/**
 * {@code dxfg_v4_tagged_event_list}: a list of events with a tag for each. Its first two fields are those of
 * {@code dxfg_v4_event_type_list}, so the list mapper of the events releases it after its tags are freed.
 */
@CContext(EventQueueDirectives.class)
@CStruct("dxfg_v4_tagged_event_list")
public interface DxfgTaggedEventList extends DxfgEventTypeListPointer {

    @CField("tags")
    CLongPointer getTags();

    @CField("tags")
    void setTags(CLongPointer value);
}
