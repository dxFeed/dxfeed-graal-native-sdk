package com.dxfeed.event.market;

import com.dxfeed.api.events.DxfgCandleAlignment;
import com.dxfeed.api.events.DxfgCandlePrice;
import com.dxfeed.api.events.DxfgCandleSession;
import com.dxfeed.api.events.DxfgCandleSymbol;
import com.dxfeed.event.candle.CandleSymbol;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;

public class CandleSymbolMapper extends Mapper<CandleSymbol, DxfgCandleSymbol> {

  protected final Mapper<String, CCharPointer> stringMapper;
  protected final CandlePeriodMapper candlePeriodMapper;
  protected final CandleExchangeMapper candleExchangeMapper;
  protected final CandlePriceLevelMapper candlePriceLevelMapper;

  public CandleSymbolMapper(
      final Mapper<String, CCharPointer> stringMapper,
      final CandlePeriodMapper candlePeriodMapper,
      final CandleExchangeMapper candleExchangeMapper,
      final CandlePriceLevelMapper candlePriceLevelMapper
  ) {
    this.stringMapper = stringMapper;
    this.candlePeriodMapper = candlePeriodMapper;
    this.candleExchangeMapper = candleExchangeMapper;
    this.candlePriceLevelMapper = candlePriceLevelMapper;
  }

  @Override
  protected int size() {
    return SizeOf.get(DxfgCandleSymbol.class);
  }

  @Override
  protected void fillNativeObject(final DxfgCandleSymbol nObject, final CandleSymbol jObject) {
    nObject.setSymbol(stringMapper.nativeObject(jObject.toString()));
    nObject.setBaseSymbol(stringMapper.nativeObject(jObject.getBaseSymbol()));
    nObject.setExchange(candleExchangeMapper.nativeObject(jObject.getExchange()));
    nObject.setPrice(DxfgCandlePrice.valueOf(jObject.getPrice().name()).getCValue());
    nObject.setSession(DxfgCandleSession.valueOf(jObject.getSession().name()).getCValue());
    nObject.setPeriod(candlePeriodMapper.nativeObject(jObject.getPeriod()));
    nObject.setAlignment(DxfgCandleAlignment.valueOf(jObject.getAlignment().name()).getCValue());
    nObject.setPriceLevel(candlePriceLevelMapper.nativeObject(jObject.getPriceLevel()));
  }

  @Override
  protected void cleanNativeObject(final DxfgCandleSymbol nObject) {
    stringMapper.delete(nObject.getSymbol());
    stringMapper.delete(nObject.getBaseSymbol());
    candleExchangeMapper.delete(nObject.getExchange());
    candlePeriodMapper.delete(nObject.getPeriod());
    candlePriceLevelMapper.delete(nObject.getPriceLevel());
  }
}
