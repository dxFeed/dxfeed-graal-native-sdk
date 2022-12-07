package com.dxfeed.event.market;

import com.dxfeed.api.maper.Mapper;
import com.dxfeed.api.events.DxfgDailyCandle;
import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.symbol.DxfgSymbol;
import com.dxfeed.event.candle.DailyCandle;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class DailyCandleMapper extends CandleMapper<DailyCandle, DxfgDailyCandle> {

  public DailyCandleMapper(final Mapper<Object, DxfgSymbol> symbolMapper) {
    super(symbolMapper);
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
