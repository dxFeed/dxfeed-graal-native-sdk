// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.misc;

import com.dxfeed.event.EventMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgTextMessage;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TextMessageMapper extends EventMapper<TextMessage, DxfgTextMessage> {

    protected final Mapper<String, CCharPointer> stringMapper;

    public TextMessageMapper(final Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    protected DxfgTextMessage createNativeObject() {
        final DxfgTextMessage nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTextMessage.class));

        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_TEXT_MESSAGE.getCValue());

        return nativeObject;
    }

    @Override
    public DxfgTextMessage createNativeObject(final String symbol) {
        final DxfgTextMessage nativeObject = createNativeObject();

        nativeObject.setEventSymbol(this.stringMapper.toNative(symbol));

        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_TEXT_MESSAGE;
    }

    @Override
    public void fillNative(final TextMessage javaObject, final DxfgTextMessage nativeObject,
            final boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setEventSymbol(this.stringMapper.toNative(javaObject.getEventSymbol()));
        nativeObject.setEventTime(javaObject.getEventTime());
        nativeObject.setTimeSequence(javaObject.getTimeSequence());
        nativeObject.setText(this.stringMapper.toNative(javaObject.getText()));
    }

    @Override
    public void cleanNative(final DxfgTextMessage nativeObject) {
        this.stringMapper.release(nativeObject.getEventSymbol());
        this.stringMapper.release(nativeObject.getText());
    }

    @Override
    protected TextMessage doToJava(final DxfgTextMessage nativeObject) {
        final TextMessage javaObject = new TextMessage();

        fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @Override
    public void fillJava(final DxfgTextMessage nativeObject, final TextMessage javaObject) {
        javaObject.setEventSymbol(this.stringMapper.toJava(nativeObject.getEventSymbol()));
        javaObject.setEventTime(nativeObject.getEventTime());
        javaObject.setTimeSequence(nativeObject.getTimeSequence());
        javaObject.setText(this.stringMapper.toJava(nativeObject.getText()));
    }
}
