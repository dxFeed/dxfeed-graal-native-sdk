package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgProfile;
import com.dxfeed.sdk.maper.Mapper;
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
  public void fillNative(final Profile jObject, final DxfgProfile nObject) {
    super.fillNative(jObject, nObject);
    nObject.setDescription(this.stringMapper.toNative(jObject.getDescription()));
    nObject.setStatusReason(this.stringMapper.toNative(jObject.getStatusReason()));
    nObject.setHaltStartTime(jObject.getHaltStartTime());
    nObject.setHaltEndTime(jObject.getHaltEndTime());
    nObject.setHighLimitPrice(jObject.getHighLimitPrice());
    nObject.setLowLimitPrice(jObject.getLowLimitPrice());
    nObject.setHigh52WeekPrice(jObject.getHigh52WeekPrice());
    nObject.setLow52WeekPrice(jObject.getLow52WeekPrice());
    nObject.setBeta(jObject.getBeta());
    nObject.setEarningsPerShare(jObject.getEarningsPerShare());
    nObject.setDividendFrequency(jObject.getDividendFrequency());
    nObject.setExDividendAmount(jObject.getExDividendAmount());
    nObject.setExDividendDayId(jObject.getExDividendDayId());
    nObject.setShares(jObject.getShares());
    nObject.setFreeFloat(jObject.getFreeFloat());
    nObject.setFlags(jObject.getFlags());
  }

  @Override
  public void cleanNative(final DxfgProfile nObject) {
    super.cleanNative(nObject);
    this.stringMapper.release(nObject.getDescription());
    this.stringMapper.release(nObject.getStatusReason());
  }

  @Override
  protected Profile doToJava(final DxfgProfile nObject) {
    final Profile jObject = new Profile();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgProfile nObject, final Profile jObject) {
    super.fillJava(nObject, jObject);
    jObject.setDescription(this.stringMapper.toJava(nObject.getDescription()));
    jObject.setStatusReason(this.stringMapper.toJava(nObject.getStatusReason()));
    jObject.setHaltStartTime(nObject.getHaltStartTime());
    jObject.setHaltEndTime(nObject.getHaltEndTime());
    jObject.setHighLimitPrice(nObject.getHighLimitPrice());
    jObject.setLowLimitPrice(nObject.getLowLimitPrice());
    jObject.setHigh52WeekPrice(nObject.getHigh52WeekPrice());
    jObject.setLow52WeekPrice(nObject.getLow52WeekPrice());
    jObject.setBeta(nObject.getBeta());
    jObject.setEarningsPerShare(nObject.getEarningsPerShare());
    jObject.setDividendFrequency(nObject.getDividendFrequency());
    jObject.setExDividendAmount(nObject.getExDividendAmount());
    jObject.setExDividendDayId(nObject.getExDividendDayId());
    jObject.setShares(nObject.getShares());
    jObject.setFreeFloat(nObject.getFreeFloat());
    jObject.setFlags(nObject.getFlags());
  }
}
