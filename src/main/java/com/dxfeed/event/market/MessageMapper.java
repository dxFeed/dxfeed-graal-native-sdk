package com.dxfeed.event.market;

import com.dxfeed.event.misc.Message;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgMessage;
import com.dxfeed.sdk.maper.Mapper;
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
  public final void fillNative(final Message jObject, final DxfgMessage nObject) {
    cleanNative(nObject);
    nObject.setEventSymbol(this.stringMapper.toNative(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
    try {
      nObject.setAttachment(
          this.stringMapper.toNative(OBJECT_MAPPER.writeValueAsString(jObject.getAttachment()))
      );
    } catch (final JsonProcessingException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

  @Override
  public DxfgMessage createNativeObject() {
    final DxfgMessage nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgMessage.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_MESSAGE.getCValue());
    return nObject;
  }

  @Override
  public final void cleanNative(final DxfgMessage nObject) {
    this.stringMapper.release(nObject.getEventSymbol());
    this.stringMapper.release(nObject.getAttachment());
  }

  @Override
  protected Message doToJava(final DxfgMessage nObject) {
    final Message jObject = new Message();
    fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgMessage nObject, final Message jObject) {
    jObject.setEventSymbol(this.stringMapper.toJava(nObject.getEventSymbol()));
    jObject.setEventTime(nObject.getEventTime());
    final String content = this.stringMapper.toJava(nObject.getAttachment());
    if (content == null) {
      jObject.setAttachment(null);
    } else {
      final Object attachment = jObject.getAttachment();
      if (attachment != null) {
        try {
          jObject.setAttachment(OBJECT_MAPPER.readValue(content, attachment.getClass()));
        } catch (final JsonProcessingException e) {
          logger.log(Level.WARNING, e.getMessage(), e);
        }
      }
    }
  }

  @Override
  public DxfgMessage createNativeObject(final String symbol) {
    final DxfgMessage nObject = createNativeObject();
    nObject.setEventSymbol(this.stringMapper.toNative(symbol));
    return nObject;
  }
}
