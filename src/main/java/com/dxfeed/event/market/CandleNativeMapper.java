package com.dxfeed.event.market;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.events.DxfgCandle;
import com.dxfeed.api.events.DxfgCandleAlignment;
import com.dxfeed.api.events.DxfgCandlePrice;
import com.dxfeed.api.events.DxfgCandleSession;
import com.dxfeed.api.events.DxfgCandleSymbol;
import com.dxfeed.api.events.DxfgCandleType;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.CandleSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;

public class CandleNativeMapper extends BaseNative implements NativeMapper<Candle, DxfgCandle> {

  @Override
  public DxfgCandle nativeObject(final Candle candle) {
    final DxfgCandle dxfgCandle = UnmanagedMemory.calloc(SizeOf.get(DxfgCandle.class));
    dxfgCandle.setKind(DxfgEventKind.DXFG_EVENT_TYPE_CANDLE.getCValue());
    dxfgCandle.setEventFlags(candle.getEventFlags());
    dxfgCandle.setEventTime(candle.getEventTime());
    dxfgCandle.setIndex(candle.getIndex());
    dxfgCandle.setCount(candle.getCount());
    dxfgCandle.setOpen(candle.getOpen());
    dxfgCandle.setHigh(candle.getHigh());
    dxfgCandle.setLow(candle.getLow());
    dxfgCandle.setClose(candle.getClose());
    dxfgCandle.setVolume(candle.getVolume());
    dxfgCandle.setVwap(candle.getVWAP());
    dxfgCandle.setBidVolume(candle.getBidVolume());
    dxfgCandle.setAskVolume(candle.getAskVolume());
    dxfgCandle.setImpVolatility(candle.getImpVolatility());
    dxfgCandle.setOpenInterest(candle.getOpenInterest());

//    final CandleSymbol symbol = candle.getEventSymbol();
//    dxfgCandle.setSymbol(toCString(symbol.toString()));
//    dxfgCandle.setBaseSymbol(toCString(symbol.getBaseSymbol()));
//    dxfgCandle.setExchangeCode(symbol.getExchange().getExchangeCode());
//    dxfgCandle.setPrice(DxfgCandlePrice.valueOf(symbol.getPrice().name()).getCValue());
//    dxfgCandle.setSession(DxfgCandleSession.valueOf(symbol.getSession().name()).getCValue());
//    dxfgCandle.setPeriodIntervalMillis(symbol.getPeriod().getPeriodIntervalMillis());
//    dxfgCandle.setValueCandlePeriod(symbol.getPeriod().getValue());
//    dxfgCandle.setType(DxfgCandleType.valueOf(symbol.getPeriod().getType().name()).getCValue());
//    dxfgCandle.setString(toCString(symbol.getPeriod().toString()));
//    dxfgCandle.setAlignment(DxfgCandleAlignment.valueOf(symbol.getAlignment().name()).getCValue());
//    dxfgCandle.setValuePriceLevel(symbol.getPriceLevel().getValue());

    return dxfgCandle;
  }

  @Override
  public void delete(final DxfgCandle dxfgCandle) {
//    final DxfgCandleSymbol dxfgCandleSymbol = dxfgCandle.dxfgCandleSymbol();
//    UnmanagedMemory.free(dxfgCandleSymbol.getSymbol());
//    UnmanagedMemory.free(dxfgCandleSymbol.getBaseSymbol());
//    UnmanagedMemory.free(dxfgCandleSymbol.getPeriod().getString());
    UnmanagedMemory.free(dxfgCandle);
  }
}
