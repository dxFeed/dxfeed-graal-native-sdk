// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.misc;

import com.dxfeed.event.EventMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgTextConfiguration;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class TextConfigurationMapper extends EventMapper<TextConfiguration, DxfgTextConfiguration> {

    protected final Mapper<String, CCharPointer> stringMapper;

    public TextConfigurationMapper(final Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    protected DxfgTextConfiguration createNativeObject() {
        final DxfgTextConfiguration nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgTextConfiguration.class));

        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_TEXT_CONFIGURATION.getCValue());

        return nativeObject;
    }

    @Override
    public DxfgTextConfiguration createNativeObject(final String symbol) {
        final DxfgTextConfiguration nativeObject = createNativeObject();

        nativeObject.setEventSymbol(this.stringMapper.toNative(symbol));

        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_TEXT_CONFIGURATION;
    }

    @Override
    public void fillNative(final TextConfiguration javaObject, final DxfgTextConfiguration nativeObject,
            final boolean clean) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setEventSymbol(this.stringMapper.toNative(javaObject.getEventSymbol()));
        nativeObject.setEventTime(javaObject.getEventTime());
        nativeObject.setTimeSequence(javaObject.getTimeSequence());
        nativeObject.setVersion(javaObject.getVersion());
        nativeObject.setText(this.stringMapper.toNative(javaObject.getText()));
    }

    @Override
    public void cleanNative(final DxfgTextConfiguration nativeObject) {
        this.stringMapper.release(nativeObject.getEventSymbol());
        this.stringMapper.release(nativeObject.getText());
    }

    @Override
    protected TextConfiguration doToJava(final DxfgTextConfiguration nativeObject) {
        final TextConfiguration javaObject = new TextConfiguration();

        fillJava(nativeObject, javaObject);

        return javaObject;
    }

    @Override
    public void fillJava(final DxfgTextConfiguration nativeObject, final TextConfiguration javaObject) {
        javaObject.setEventSymbol(this.stringMapper.toJava(nativeObject.getEventSymbol()));
        javaObject.setEventTime(nativeObject.getEventTime());
        javaObject.setTimeSequence(nativeObject.getTimeSequence());
        javaObject.setVersion(nativeObject.getVersion());
        javaObject.setText(this.stringMapper.toJava(nativeObject.getText()));
    }
}
