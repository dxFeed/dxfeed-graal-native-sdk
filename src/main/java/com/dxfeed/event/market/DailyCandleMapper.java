package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgDailyCandle;
import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.event.candle.CandleSymbol;
import com.dxfeed.event.candle.DailyCandle;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class DailyCandleMapper extends EventMapper<DailyCandle, DxfgDailyCandle> {

  protected final CandleSymbolMapper candleSymbolMapper;

  public DailyCandleMapper(final CandleSymbolMapper candleSymbolMapper) {
    this.candleSymbolMapper = candleSymbolMapper;
  }

  @Override
  public DxfgDailyCandle createNativeObject() {
    final DxfgDailyCandle nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgDailyCandle.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_DAILY_CANDLE.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final DailyCandle jObject, final DxfgDailyCandle nObject) {
    cleanNative(nObject);
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setEventTime(jObject.getEventTime());
    nObject.setIndex(jObject.getIndex());
    nObject.setCount(jObject.getCount());
    nObject.setOpen(jObject.getOpen());
    nObject.setHigh(jObject.getHigh());
    nObject.setLow(jObject.getLow());
    nObject.setClose(jObject.getClose());
    nObject.setVolume(jObject.getVolumeAsDouble());
    nObject.setVwap(jObject.getVWAP());
    nObject.setBidVolume(jObject.getBidVolumeAsDouble());
    nObject.setAskVolume(jObject.getAskVolumeAsDouble());
    nObject.setImpVolatility(jObject.getImpVolatility());
    nObject.setOpenInterest(jObject.getOpenInterestAsDouble());
    nObject.setCandleSymbol(this.candleSymbolMapper.toNative(jObject.getEventSymbol()));
  }

  @Override
  public void cleanNative(final DxfgDailyCandle nObject) {
    this.candleSymbolMapper.release(nObject.getCandleSymbol());
  }

  @Override
  public DailyCandle toJava(final DxfgDailyCandle nObject) {
    final DailyCandle jObject = new DailyCandle();
    fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final DxfgDailyCandle nObject, final DailyCandle jObject) {
    jObject.setEventFlags(nObject.getEventFlags());
    jObject.setEventTime(nObject.getEventTime());
    jObject.setIndex(nObject.getIndex());
    jObject.setCount(nObject.getCount());
    jObject.setOpen(nObject.getOpen());
    jObject.setHigh(nObject.getHigh());
    jObject.setLow(nObject.getLow());
    jObject.setClose(nObject.getClose());
    jObject.setVolumeAsDouble(nObject.getVolume());
    jObject.setVWAP(nObject.getVwap());
    jObject.setBidVolumeAsDouble(nObject.getBidVolume());
    jObject.setAskVolumeAsDouble(nObject.getAskVolume());
    jObject.setImpVolatility(nObject.getImpVolatility());
    jObject.setOpenInterestAsDouble(nObject.getOpenInterest());
    jObject.setEventSymbol(this.candleSymbolMapper.toJava(nObject.getCandleSymbol()));
  }

  @Override
  public DxfgDailyCandle createNativeObject(final String symbol) {
    final DxfgDailyCandle nObject = createNativeObject();
    nObject.setCandleSymbol(this.candleSymbolMapper.toNative(CandleSymbol.valueOf(symbol)));
    return nObject;
  }
}
