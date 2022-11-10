package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgProfile;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class ProfileMapper extends MarketEventMapper<Profile, DxfgProfile> {

  public ProfileMapper(final StringMapper stringMapper) {
    super(stringMapper);
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgProfile.class);
  }

  @Override
  protected void doFillNativeObject(final DxfgProfile nObject, final Profile jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_PROFILE.getCValue());
    nObject.setDescription(super.stringMapper.nativeObject(jObject.getDescription()));
    nObject.setStatusReason(super.stringMapper.nativeObject(jObject.getStatusReason()));
    nObject.setHaltStartTime(jObject.getHaltStartTime());
    nObject.setHaltEndTime(jObject.getHaltEndTime());
    nObject.setHighLimitPrice(jObject.getHighLimitPrice());
    nObject.setLowLimitPrice(jObject.getLowLimitPrice());
    nObject.setHigh52WeekPrice(jObject.getHigh52WeekPrice());
    nObject.setLow52WeekPrice(jObject.getLow52WeekPrice());
    nObject.setFlags(jObject.getFlags());
  }

  @Override
  protected void doCleanNativeObject(final DxfgProfile nObject) {
    super.stringMapper.delete(nObject.getDescription());
    super.stringMapper.delete(nObject.getStatusReason());
  }
}
