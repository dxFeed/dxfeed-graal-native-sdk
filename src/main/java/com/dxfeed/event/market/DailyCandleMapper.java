package com.dxfeed.event.market;

import com.dxfeed.event.candle.DailyCandle;
import com.dxfeed.sdk.events.DxfgDailyCandle;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.maper.Mapper;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class DailyCandleMapper extends CandleMapper<DailyCandle, DxfgDailyCandle> {

  public DailyCandleMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<Object, DxfgSymbol> symbolMapper
  ) {
    super(stringMapperForMarketEvent, symbolMapper);
  }

  @Override
  public DxfgDailyCandle createNativeObject() {
    final DxfgDailyCandle nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgDailyCandle.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_DAILY_CANDLE.getCValue());
    return nObject;
  }

  @Override
  protected DailyCandle doToJava(final DxfgDailyCandle nObject) {
    final DailyCandle jObject = new DailyCandle();
    fillJava(nObject, jObject);
    return jObject;
  }
}
