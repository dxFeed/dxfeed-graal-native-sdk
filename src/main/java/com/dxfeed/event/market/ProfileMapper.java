package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgProfile;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class ProfileMapper extends Mapper<Profile, DxfgProfile> {

  private final MarketEventMapper marketEventMapper;
  private final Mapper<String, CCharPointer> stringMapper;

  public ProfileMapper(
      final MarketEventMapper marketEventMapper,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    this.marketEventMapper = marketEventMapper;
    this.stringMapper = stringMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgProfile.class);
  }

  @Override
  protected void fillNativeObject(final DxfgProfile nObject, final Profile jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_PROFILE.getCValue());
    this.marketEventMapper.fillNativeObject(nObject, jObject);
    nObject.setDescription(this.stringMapper.nativeObject(jObject.getDescription()));
    nObject.setStatusReason(this.stringMapper.nativeObject(jObject.getStatusReason()));
    nObject.setHaltStartTime(jObject.getHaltStartTime());
    nObject.setHaltEndTime(jObject.getHaltEndTime());
    nObject.setHighLimitPrice(jObject.getHighLimitPrice());
    nObject.setLowLimitPrice(jObject.getLowLimitPrice());
    nObject.setHigh52WeekPrice(jObject.getHigh52WeekPrice());
    nObject.setLow52WeekPrice(jObject.getLow52WeekPrice());
    nObject.setFlags(jObject.getFlags());
  }

  @Override
  protected void cleanNativeObject(final DxfgProfile nObject) {
    this.marketEventMapper.cleanNativeObject(nObject);
    this.stringMapper.delete(nObject.getDescription());
    this.stringMapper.delete(nObject.getStatusReason());
  }
}
