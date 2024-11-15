package com.dxfeed.event.market;

import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.CandleSymbol;
import com.dxfeed.sdk.events.DxfgCandle;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.mappers.Mapper;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class CandleMapper<T extends Candle, V extends DxfgCandle> extends EventMapper<T, V> {

  protected final Mapper<String, CCharPointer> stringMapper;
  protected final Mapper<Object, DxfgSymbol> symbolMapper;

  public CandleMapper(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final Mapper<Object, DxfgSymbol> symbolMapper
  ) {
    this.symbolMapper = symbolMapper;
    this.stringMapper = stringMapperForMarketEvent;
  }

  @Override
  public V createNativeObject() {
    final V nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandle.class));
    nObject.setClazz(DxfgEventClazz.DXFG_EVENT_CANDLE.getCValue());
    return nObject;
  }

  @Override
  public void fillNative(final T jObject, final V nObject, boolean clean) {
    if (clean) {
      cleanNative(nObject);
    }

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
    nObject.setSymbol(this.stringMapper.toNative(jObject.getEventSymbol().toString()));
  }

  @Override
  public void cleanNative(final V nObject) {
    this.stringMapper.release(nObject.getSymbol());
  }

  @Override
  protected T doToJava(final V nObject) {
    final T jObject = (T) new Candle();
    fillJava(nObject, jObject);
    return jObject;
  }

  @Override
  public void fillJava(final V nObject, final T jObject) {
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
    jObject.setEventSymbol(CandleSymbol.valueOf(this.stringMapper.toJava(nObject.getSymbol())));
  }

  @Override
  public V createNativeObject(final String symbol) {
    final V nObject = createNativeObject();
    nObject.setSymbol(this.stringMapper.toNative(symbol));
    return nObject;
  }
}
