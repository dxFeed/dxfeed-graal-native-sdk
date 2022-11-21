package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgCandle;
import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.CandleSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class CandleMapper extends EventMapper<Candle, DxfgCandle> {

  protected final CandleSymbolMapper candleSymbolMapper;

  public CandleMapper(final CandleSymbolMapper candleSymbolMapper) {
    this.candleSymbolMapper = candleSymbolMapper;
  }

  @Override
  public DxfgCandle createNativeObject() {
    final DxfgCandle nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandle.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_CANDLE.getCValue());
    return nObject;
  }

  @Override
  public void fillNativeObject(final Candle jObject, final DxfgCandle nObject) {
    cleanNativeObject(nObject);
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
    nObject.setCandleSymbol(this.candleSymbolMapper.toNativeObject(jObject.getEventSymbol()));
  }

  @Override
  protected void cleanNativeObject(final DxfgCandle nObject) {
    this.candleSymbolMapper.release(nObject.getCandleSymbol());
  }

  @Override
  public Candle toJavaObject(final DxfgCandle nObject) {
    final Candle jObject = new Candle();
    fillJavaObject(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJavaObject(final DxfgCandle nObject, final Candle jObject) {
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
    jObject.setEventSymbol(candleSymbolMapper.toJavaObject(nObject.getCandleSymbol()));
  }

  @Override
  public DxfgCandle createNativeObject(final String symbol) {
    final DxfgCandle nObject = createNativeObject();
    nObject.setCandleSymbol(this.candleSymbolMapper.toNativeObject(CandleSymbol.valueOf(symbol)));
    return nObject;
  }
}
