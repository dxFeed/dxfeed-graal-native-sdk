// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.event.misc;

import com.dxfeed.event.EventMapper;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgMessage;
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

public class MessageMapper extends EventMapper<Message, DxfgMessage> {

    private final static Logger logger = Logger.getLogger(MessageMapper.class.getCanonicalName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);
    }

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
        try {
            nativeObject.setAttachment(
                    this.stringMapper.toNative(OBJECT_MAPPER.writeValueAsString(javaObject.getAttachment()))
            );
        } catch (final JsonProcessingException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
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
