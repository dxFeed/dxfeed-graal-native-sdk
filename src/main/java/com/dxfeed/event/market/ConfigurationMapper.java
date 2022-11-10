package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgConfiguration;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.event.misc.Configuration;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ConfigurationMapper extends Mapper<Configuration, DxfgConfiguration> {

  protected final StringMapper stringMapper;

  public ConfigurationMapper(final StringMapper stringMapper) {
    this.stringMapper = stringMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgConfiguration.class);
  }

  @Override
  protected final void fillNativeObject(
      final DxfgConfiguration nObject,
      final Configuration jObject
  ) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_CONFIGURATION.getCValue());
    nObject.setEventSymbol(stringMapper.nativeObject(jObject.getEventSymbol()));
    nObject.setEventTime(jObject.getEventTime());
    nObject.setVersion(jObject.getVersion());
    nObject.setAttachment(stringMapper.nativeObject(jObject.getAttachment().toString()));
  }

  @Override
  protected final void cleanNativeObject(final DxfgConfiguration nObject) {
    stringMapper.delete(nObject.getEventSymbol());
    stringMapper.delete(nObject.getAttachment());
  }
}
