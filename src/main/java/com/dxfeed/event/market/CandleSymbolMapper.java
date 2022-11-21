package com.dxfeed.event.market;

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
  public DxfgCandleSymbol toNativeObject(final CandleSymbol jObject) {
    if (jObject == null) {
      return WordFactory.nullPointer();
    }
    final DxfgCandleSymbol nObject = UnmanagedMemory.calloc(SizeOf.get(DxfgCandleSymbol.class));
    fillNativeObject(jObject, nObject);
    return nObject;
  }

  @Override
  public void fillNativeObject(final CandleSymbol jObject, final DxfgCandleSymbol nObject) {
    cleanNativeObject(nObject);
    nObject.setSymbol(this.stringMapper.toNativeObject(jObject.toString()));
    nObject.setBaseSymbol(this.stringMapper.toNativeObject(jObject.getBaseSymbol()));
    nObject.setExchange(this.candleExchangeMapper.toNativeObject(jObject.getExchange()));
    nObject.setPrice(DxfgCandlePrice.valueOf(jObject.getPrice().name()).getCValue());
    nObject.setSession(DxfgCandleSession.valueOf(jObject.getSession().name()).getCValue());
    nObject.setPeriod(this.candlePeriodMapper.toNativeObject(jObject.getPeriod()));
    nObject.setAlignment(DxfgCandleAlignment.valueOf(jObject.getAlignment().name()).getCValue());
    nObject.setPriceLevel(this.candlePriceLevelMapper.toNativeObject(jObject.getPriceLevel()));
  }

  @Override
  protected void cleanNativeObject(final DxfgCandleSymbol nObject) {
    this.stringMapper.release(nObject.getSymbol());
    this.stringMapper.release(nObject.getBaseSymbol());
    this.candleExchangeMapper.release(nObject.getExchange());
    this.candlePeriodMapper.release(nObject.getPeriod());
    this.candlePriceLevelMapper.release(nObject.getPriceLevel());
  }

  @Override
  public CandleSymbol toJavaObject(final DxfgCandleSymbol nObject) {
    return CandleSymbol.valueOf(this.stringMapper.toJavaObject(nObject.getSymbol()));
  }

  @Override
  public void fillJavaObject(final DxfgCandleSymbol nObject, final CandleSymbol jObject) {
    throw new IllegalStateException();
  }
}
