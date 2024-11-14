package com.dxfeed.event.market;

import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgOtcMarketsOrder;
import com.dxfeed.sdk.mappers.Mapper;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class OtcMarketsOrderMapper extends OrderMapper<OtcMarketsOrder, DxfgOtcMarketsOrder> {

  public OtcMarketsOrderMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<String, CCharPointer> stringMapper
  ) {
    super(stringMapperForMarketEvent, stringMapper);
  }

  @Override
  public DxfgOtcMarketsOrder createNativeObject() {
    final DxfgOtcMarketsOrder nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgOtcMarketsOrder.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_OTC_MARKETS_ORDER.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final OtcMarketsOrder jObject, final DxfgOtcMarketsOrder nObject, boolean clean) {
    super.fillNative(jObject, nObject, clean);
    nObject.setQuoteAccessPayment(jObject.getQuoteAccessPayment());
    nObject.setOtcMarketsFlags(jObject.getOtcMarketsFlags());
  }

  @Override
  protected OtcMarketsOrder doToJava(final DxfgOtcMarketsOrder nObject) {
    final OtcMarketsOrder jObject = new OtcMarketsOrder();
    this.fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgOtcMarketsOrder nObject, final OtcMarketsOrder jObject) {
    super.fillJava(nObject, jObject);
    jObject.setQuoteAccessPayment(nObject.getQuoteAccessPayment());
    jObject.setOtcMarketsFlags(nObject.getOtcMarketsFlags());
  }
}
