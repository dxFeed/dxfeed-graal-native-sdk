package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgProfile;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class ProfileMapper extends MarketEventMapper<Profile, DxfgProfile> {

  private final Mapper<String, CCharPointer> stringMapper;

  public ProfileMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapperForMarketEvent);
    this.stringMapper = stringMapper;
  }

  @Override
  public DxfgProfile createNativeObject() {
    final DxfgProfile nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgProfile.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_PROFILE.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final Profile jObject, final DxfgProfile nObject) {
    super.fillNativeObject(jObject, nObject);
    nObject.setDescription(this.stringMapper.toNativeObject(jObject.getDescription()));
    nObject.setStatusReason(this.stringMapper.toNativeObject(jObject.getStatusReason()));
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
    super.cleanNativeObject(nObject);
    this.stringMapper.release(nObject.getDescription());
    this.stringMapper.release(nObject.getStatusReason());
  }

  @Override
  public Profile toJavaObject(final DxfgProfile nObject) {
    final Profile jObject = new Profile();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgProfile nObject, final Profile jObject) {
    super.fillJavaObject(nObject, jObject);
    jObject.setDescription(this.stringMapper.toJavaObject(nObject.getDescription()));
    jObject.setStatusReason(this.stringMapper.toJavaObject(nObject.getStatusReason()));
    jObject.setHaltStartTime(nObject.getHaltStartTime());
    jObject.setHaltEndTime(nObject.getHaltEndTime());
    jObject.setHighLimitPrice(nObject.getHighLimitPrice());
    jObject.setLowLimitPrice(nObject.getLowLimitPrice());
    jObject.setHigh52WeekPrice(nObject.getHigh52WeekPrice());
    jObject.setLow52WeekPrice(nObject.getLow52WeekPrice());
    jObject.setFlags(nObject.getFlags());
  }
}
