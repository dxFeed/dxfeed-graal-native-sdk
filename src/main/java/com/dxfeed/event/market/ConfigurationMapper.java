package com.dxfeed.event.market;

import com.dxfeed.api.maper.Mapper;
import com.dxfeed.api.events.DxfgConfiguration;
import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.event.misc.Configuration;
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
    final DxfgConfiguration nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgConfiguration.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_CONFIGURATION.getCValue());
    return nObject;
  }

  @Override
  public final void fillNative(
      final Configuration jObject, final DxfgConfiguration nObject
  ) {
    cleanNative(nObject);
    nObject.setEventSymbol(stringMapper.toNative(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
    nObject.setVersion(jObject.getVersion());
    nObject.setAttachment(stringMapper.toNative(jObject.getAttachment().toString()));
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
    jObject.setAttachment(stringMapper.toJava(nObject.getAttachment())); //TODO
  }

  @Override
  public DxfgConfiguration createNativeObject(final String symbol) {
    final DxfgConfiguration nObject = createNativeObject();
    nObject.setEventSymbol(this.stringMapper.toNative(symbol));
    return nObject;
  }
}
