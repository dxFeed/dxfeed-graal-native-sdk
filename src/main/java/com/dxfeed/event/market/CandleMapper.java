package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgCandle;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.event.candle.Candle;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class CandleMapper extends Mapper<Candle, DxfgCandle> {

  protected final CandleSymbolMapper candleSymbolMapper;

  public CandleMapper(final CandleSymbolMapper candleSymbolMapper) {
    this.candleSymbolMapper = candleSymbolMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgCandle.class);
  }

  @Override
  protected void fillNativeObject(final DxfgCandle nObject, final Candle jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_CANDLE.getCValue());
    nObject.setEventFlags(jObject.getEventFlags());
    nObject.setEventTime(jObject.getEventTime());
    nObject.setIndex(jObject.getIndex());
    nObject.setCount(jObject.getCount());
    nObject.setOpen(jObject.getOpen());
    nObject.setHigh(jObject.getHigh());
    nObject.setLow(jObject.getLow());
    nObject.setClose(jObject.getClose());
    nObject.setVolume(jObject.getVolume());
    nObject.setVwap(jObject.getVWAP());
    nObject.setBidVolume(jObject.getBidVolume());
    nObject.setAskVolume(jObject.getAskVolume());
    nObject.setImpVolatility(jObject.getImpVolatility());
    nObject.setOpenInterest(jObject.getOpenInterest());
    nObject.setCandleSymbol(candleSymbolMapper.nativeObject(jObject.getEventSymbol()));
  }

  @Override
  protected void cleanNativeObject(final DxfgCandle nObject) {
    candleSymbolMapper.delete(nObject.getCandleSymbol());
  }
}
