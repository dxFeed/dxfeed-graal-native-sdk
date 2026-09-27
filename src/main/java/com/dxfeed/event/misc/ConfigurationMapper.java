// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.misc;

import com.dxfeed.event.EventMapper;
import com.dxfeed.sdk.events.DxfgConfiguration;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class ConfigurationMapper extends EventMapper<Configuration, DxfgConfiguration> {

    protected final Mapper<String, CCharPointer> stringMapper;

    public ConfigurationMapper(final Mapper<String, CCharPointer> stringMapper) {
        this.stringMapper = stringMapper;
    }

    @Override
    public DxfgConfiguration createNativeObject() {
        final DxfgConfiguration nativeObject = UnmanagedMemory.calloc(SizeOf.get(DxfgConfiguration.class));
        nativeObject.setClazz(DxfgEventClazz.DXFG_EVENT_CONFIGURATION.getCValue());
        return nativeObject;
    }

    @Override
    public final void fillNative(
            final Configuration javaObject, final DxfgConfiguration nativeObject, boolean clean
    ) {
        if (clean) {
            cleanNative(nativeObject);
        }

        nativeObject.setEventSymbol(stringMapper.toNative(javaObject.getEventSymbol()));
        nativeObject.setEventTime(javaObject.getEventTime());
        nativeObject.setVersion(javaObject.getVersion());
        nativeObject.setAttachment(this.stringMapper.toNative(
                NativeAttachment.toNative(javaObject::getAttachment, "Configuration", javaObject.getEventSymbol())));
    }

    @Override
    public final void cleanNative(final DxfgConfiguration nativeObject) {
        stringMapper.release(nativeObject.getEventSymbol());
        stringMapper.release(nativeObject.getAttachment());
    }

    @Override
    protected Configuration doToJava(final DxfgConfiguration nativeObject) {
        final Configuration javaObject = new Configuration();
        fillJava(nativeObject, javaObject);
        return javaObject;
    }

    @Override
    public void fillJava(final DxfgConfiguration nativeObject, final Configuration javaObject) {
        javaObject.setEventSymbol(stringMapper.toJava(nativeObject.getEventSymbol()));
        javaObject.setEventTime(nativeObject.getEventTime());
        javaObject.setVersion(nativeObject.getVersion());
        // The attachment is a string on the native side (see dxfg_configuration_t.attachment), NULL is no attachment.
        // A new event has no attachment: setAttachment(null) would set an empty one, which the QD text tapes write
        // as "null " instead of \NULL and cannot read back.
        final String attachment = this.stringMapper.toJava(nativeObject.getAttachment());

        if (attachment != null) {
            javaObject.setAttachment(attachment);
        }
    }

    @Override
    public DxfgConfiguration createNativeObject(final String symbol) {
        final DxfgConfiguration nativeObject = createNativeObject();
        nativeObject.setEventSymbol(this.stringMapper.toNative(symbol));
        return nativeObject;
    }

    @Override
    public DxfgEventClazz getEventClazz() {
        return DxfgEventClazz.DXFG_EVENT_CONFIGURATION;
    }
}
