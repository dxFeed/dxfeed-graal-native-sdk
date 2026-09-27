// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.misc;

import com.dxfeed.event.EventMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgMessage;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class MessageMapper extends EventMapper<Message, DxfgMessage> {

    protected final Mapper<String, CCharPointer> stringMapper;

    public MessageMapper(final Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    public final void fillNative(final Message javaObject, final DxfgMessage nativeObject, boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setEventSymbol(this.stringMapper.toNative(javaObject.getEventSymbol()));
        nativeObject.setEventTime(javaObject.getEventTime());
        nativeObject.setAttachment(this.stringMapper.toNative(
                NativeAttachment.toNative(javaObject::getAttachment, "Message", javaObject.getEventSymbol())));
    }

    @Override
    public DxfgMessage createNativeObject() {
        final DxfgMessage nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgMessage.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_MESSAGE.getCValue());
        return nativeObject;
    }

    @Override
    public final void cleanNative(final DxfgMessage nativeObject) {
        this.stringMapper.release(nativeObject.getEventSymbol());
        this.stringMapper.release(nativeObject.getAttachment());
    }

    @Override
    protected Message doToJava(final DxfgMessage nativeObject) {
        final Message javaObject = new Message();
        fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgMessage nativeObject, final Message javaObject) {
        javaObject.setEventSymbol(this.stringMapper.toJava(nativeObject.getEventSymbol()));
        javaObject.setEventTime(nativeObject.getEventTime());
        // The attachment is a string on the native side (see dxfg_message_t.attachment), NULL is no attachment.
        // A new event has no attachment: setAttachment(null) would set an empty one, which the QD text tapes write
        // as "null " instead of \NULL and cannot read back.
        final String attachment = this.stringMapper.toJava(nativeObject.getAttachment());

        if (attachment != null) {
            javaObject.setAttachment(attachment);
        }
    }

    @Override
    public DxfgMessage createNativeObject(final String symbol) {
        final DxfgMessage nativeObject = createNativeObject();
        nativeObject.setEventSymbol(this.stringMapper.toNative(symbol));
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_MESSAGE;
    }
}
