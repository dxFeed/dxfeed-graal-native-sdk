package com.dxfeed.event.market;

import com.dxfeed.api.Mapper;
import com.dxfeed.api.events.DxfgCandleAlignment;
import com.dxfeed.api.events.DxfgCandlePrice;
import com.dxfeed.api.events.DxfgCandleSession;
import com.dxfeed.api.events.DxfgCandleSymbol;
import com.dxfeed.event.candle.CandleSymbol;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

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
  public DxfgCandleSymbol toNative(final CandleSymbol jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgCandleSymbol nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandleSymbol.class));
    fillNative(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNative(final CandleSymbol jObject, final DxfgCandleSymbol nObject) {
    cleanNative(nObject);
    nObject.setSymbol(this.stringMapper.toNative(jObject.toString()));
    nObject.setBaseSymbol(this.stringMapper.toNative(jObject.getBaseSymbol()));
    nObject.setExchange(this.candleExchangeMapper.toNative(jObject.getExchange()));
    nObject.setPrice(DxfgCandlePrice.valueOf(jObject.getPrice().name()).getCValue());
    nObject.setSession(DxfgCandleSession.valueOf(jObject.getSession().name()).getCValue());
    nObject.setPeriod(this.candlePeriodMapper.toNative(jObject.getPeriod()));
    nObject.setAlignment(DxfgCandleAlignment.valueOf(jObject.getAlignment().name()).getCValue());
    nObject.setPriceLevel(this.candlePriceLevelMapper.toNative(jObject.getPriceLevel()));
  }

  @Override
  public void cleanNative(final DxfgCandleSymbol nObject) {
    this.stringMapper.release(nObject.getSymbol());
    this.stringMapper.release(nObject.getBaseSymbol());
    this.candleExchangeMapper.release(nObject.getExchange());
    this.candlePeriodMapper.release(nObject.getPeriod());
    this.candlePriceLevelMapper.release(nObject.getPriceLevel());
  }

  @Override
  public CandleSymbol toJava(final DxfgCandleSymbol nObject) {
    return CandleSymbol.valueOf(this.stringMapper.toJava(nObject.getSymbol()));
  }

  @Override
  public void fillJava(final DxfgCandleSymbol nObject, final CandleSymbol jObject) {
    throw new IllegalStateException();
  }
}
