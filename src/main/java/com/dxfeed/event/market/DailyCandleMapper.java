package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgDailyCandle;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.event.candle.DailyCandle;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class DailyCandleMapper extends Mapper<DailyCandle, DxfgDailyCandle> {

  protected final CandleSymbolMapper candleSymbolMapper;

  public DailyCandleMapper(final CandleSymbolMapper candleSymbolMapper) {
    this.candleSymbolMapper = candleSymbolMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgDailyCandle.class);
  }

  @Override
  protected void fillNativeObject(final DxfgDailyCandle nObject, final DailyCandle jObject) {
    nObject.setKind(DxfgEventKind.DXFG_EVENT_TYPE_DAILY_CANDLE.getCValue());
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
  protected void cleanNativeObject(final DxfgDailyCandle nObject) {
    candleSymbolMapper.delete(nObject.getCandleSymbol());
  }
}
