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
    final DxfgConfiguration nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgConfiguration.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_CONFIGURATION.getCValue());
    return nObject;
  }

  @Override
  public final void fillNative(
      final Configuration jObject, final DxfgConfiguration nObject, boolean clean
  ) {
    if (clean) {
      cleanNative(nObject);
    }
    
    nObject.setEventSymbol(stringMapper.toNative(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
    nObject.setVersion(jObject.getVersion());
    try {
      nObject.setAttachment(
          this.stringMapper.toNative(OBJECT_MAPPER.writeValueAsString(jObject.getAttachment()))
      );
    } catch (final JsonProcessingException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

  @Override
  public final void cleanNative(final DxfgConfiguration nObject) {
    stringMapper.release(nObject.getEventSymbol());
    stringMapper.release(nObject.getAttachment());
  }

  @Override
  protected Configuration doToJava(final DxfgConfiguration nObject) {
    final Configuration jObject = new Configuration();
    fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgConfiguration nObject, final Configuration jObject) {
    jObject.setEventSymbol(stringMapper.toJava(nObject.getEventSymbol()));
    jObject.setEventTime(nObject.getEventTime());
    jObject.setVersion(nObject.getVersion());
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
  public DxfgConfiguration createNativeObject(final String symbol) {
    final DxfgConfiguration nObject = createNativeObject();
    nObject.setEventSymbol(this.stringMapper.toNative(symbol));
    return nObject;
  }
}
