// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.market;

import com.dxfeed.event.misc.Configuration;
import com.dxfeed.sdk.events.DxfgConfiguration;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.mappers.Mapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class ConfigurationMapper extends EventMapper<Configuration, DxfgConfiguration> {

    private final static Logger logger = Logger.getLogger(
            ConfigurationMapper.class.getCanonicalName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);
    }

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
        try {
            nativeObject.setAttachment(
                    this.stringMapper.toNative(OBJECT_MAPPER.writeValueAsString(javaObject.getAttachment()))
            );
        } catch (final JsonProcessingException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
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
        final String content = this.stringMapper.toJava(nativeObject.getAttachment());
        if (content == null) {
            javaObject.setAttachment(null);
        } else {
            final Object attachment = javaObject.getAttachment();
            if (attachment != null) {
                try {
                    javaObject.setAttachment(OBJECT_MAPPER.readValue(content, attachment.getClass()));
                } catch (final JsonProcessingException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public DxfgConfiguration createNativeObject(final String symbol) {
        final DxfgConfiguration nativeObject = createNativeObject();
        nativeObject.setEventSymbol(this.stringMapper.toNative(symbol));
        return nativeObject;
    }
}
