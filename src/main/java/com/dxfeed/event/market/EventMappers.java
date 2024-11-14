package com.dxfeed.event.market;

import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_ANALYTIC_ORDER;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_CANDLE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_CONFIGURATION;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_DAILY_CANDLE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_GREEKS;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_MESSAGE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_OPTION_SALE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_ORDER;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_ORDER_BASE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_OTC_MARKETS_ORDER;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_PROFILE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_QUOTE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_SERIES;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_SPREAD_ORDER;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_SUMMARY;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_THEO_PRICE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_TIME_AND_SALE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_TRADE;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_TRADE_ETH;
import static com.dxfeed.sdk.events.DxfgEventClazz.DXFG_EVENT_UNDERLYING;

import com.dxfeed.event.EventType;
import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.DailyCandle;
import com.dxfeed.event.misc.Configuration;
import com.dxfeed.event.misc.Message;
import com.dxfeed.event.option.Greeks;
import com.dxfeed.event.option.Series;
import com.dxfeed.event.option.TheoPrice;
import com.dxfeed.event.option.Underlying;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.mappers.Mapper;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.WordFactory;

public class EventMappers extends Mapper<EventType<?>, DxfgEventType> {

  protected final Map<Class<? extends EventType<?>>, EventMapper<?, ?>> mapperByClass;
  protected final Map<DxfgEventClazz, EventMapper<?, ?>> mapperByDxfgEventType;
  protected final Mapper<String, CCharPointer> stringMapperForMarketEvent;

  public EventMappers(
      final Mapper<String, CCharPointer> stringMapperForMarketEvent,
      final QuoteMapper quoteMapper,
      final SeriesMapper seriesMapper,
      final OptionSaleMapper optionSaleMapper,
      final TimeAndSaleMapper timeAndSaleMapper,
      final SpreadOrderMapper spreadOrderMapper,
      final OrderMapper orderMapper,
      final AnalyticOrderMapper analyticOrderMapper,
      final OtcMarketsOrderMapper otcMarketsOrderMapper,
      final MessageMapper messageMapper,
      final OrderBaseMapper orderBaseMapper,
      final ConfigurationMapper configurationMapper,
      final TradeMapper tradeMapper,
      final TradeETHMapper tradeETHMapper,
      final TheoPriceMapper theoPriceMapper,
      final UnderlyingMapper underlyingMapper,
      final GreeksMapper greeksMapper,
      final SummaryMapper summaryMapper,
      final ProfileMapper profileMapper,
      final DailyCandleMapper dailyCandleMapper,
      final CandleMapper candleMapper
  ) {
    this.stringMapperForMarketEvent = stringMapperForMarketEvent;
    this.mapperByClass = new HashMap<>();
    this.mapperByClass.put(Quote.class, quoteMapper);
    this.mapperByClass.put(Series.class, seriesMapper);
    this.mapperByClass.put(OptionSale.class, optionSaleMapper);
    this.mapperByClass.put(TimeAndSale.class, timeAndSaleMapper);
    this.mapperByClass.put(SpreadOrder.class, spreadOrderMapper);
    this.mapperByClass.put(AnalyticOrder.class, analyticOrderMapper);
    this.mapperByClass.put(OtcMarketsOrder.class, otcMarketsOrderMapper);
    this.mapperByClass.put(Message.class, messageMapper);
    this.mapperByClass.put(Order.class, orderMapper);
    this.mapperByClass.put(OrderBase.class, orderBaseMapper);
    this.mapperByClass.put(Configuration.class, configurationMapper);
    this.mapperByClass.put(Trade.class, tradeMapper);
    this.mapperByClass.put(TradeETH.class, tradeETHMapper);
    this.mapperByClass.put(TheoPrice.class, theoPriceMapper);
    this.mapperByClass.put(Underlying.class, underlyingMapper);
    this.mapperByClass.put(Greeks.class, greeksMapper);
    this.mapperByClass.put(Summary.class, summaryMapper);
    this.mapperByClass.put(Profile.class, profileMapper);
    this.mapperByClass.put(DailyCandle.class, dailyCandleMapper);
    this.mapperByClass.put(Candle.class, candleMapper);
    this.mapperByDxfgEventType = new EnumMap<>(DxfgEventClazz.class);
    this.mapperByDxfgEventType.put(DXFG_EVENT_QUOTE, quoteMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_SERIES, seriesMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_OPTION_SALE, optionSaleMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_TIME_AND_SALE, timeAndSaleMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_SPREAD_ORDER, spreadOrderMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_ORDER, orderMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_ANALYTIC_ORDER, analyticOrderMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_OTC_MARKETS_ORDER, otcMarketsOrderMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_MESSAGE, messageMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_ORDER_BASE, orderBaseMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_CONFIGURATION, configurationMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_TRADE, tradeMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_TRADE_ETH, tradeETHMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_THEO_PRICE, theoPriceMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_UNDERLYING, underlyingMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_GREEKS, greeksMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_SUMMARY, summaryMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_PROFILE, profileMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_DAILY_CANDLE, dailyCandleMapper);
    this.mapperByDxfgEventType.put(DXFG_EVENT_CANDLE, candleMapper);
  }

  @Override
  public DxfgEventType toNative(final EventType<?> jEvent) {
    if (jEvent == null) {
      return WordFactory.nullPointer();
    }
    return this.mapperByClass.get(jEvent.getClass()).toNativeObjectWithCast(jEvent);
  }

  @Override
  public void release(final DxfgEventType nObject) {
    if (nObject.isNull()) {
      return;
    }
    final DxfgEventClazz key = DxfgEventClazz.fromCValue(nObject.getClazz());
    this.mapperByDxfgEventType.get(key).releaseWithCast(nObject);
  }

  @Override
  public void fillNative(final EventType<?> jObject, final DxfgEventType nObject, boolean clean) {
    this.mapperByClass.get(jObject.getClass()).fillNativeObjectWithCast(jObject, nObject);
  }

  @Override
  public void cleanNative(final DxfgEventType nObject) {
    if (nObject.isNull()) {
      return;
    }
    final DxfgEventClazz key = DxfgEventClazz.fromCValue(nObject.getClazz());
    this.mapperByDxfgEventType.get(key).cleanNativeObjectWithCast(nObject);
  }

  @Override
  protected EventType<?> doToJava(final DxfgEventType nObject) {
    final DxfgEventClazz key = DxfgEventClazz.fromCValue(nObject.getClazz());
    return this.mapperByDxfgEventType.get(key).toJavaObjectWithCast(nObject);
  }

  @Override
  public void fillJava(final DxfgEventType nObject, final EventType<?> jObject) {
    final DxfgEventClazz key = DxfgEventClazz.fromCValue(nObject.getClazz());
    this.mapperByDxfgEventType.get(key).fillJavaObjectWithCast(nObject, jObject);
  }

  public DxfgEventType createNativeEvent(
      final DxfgEventClazz dxfgClazz,
      final CCharPointer symbol
  ) {
    return this.mapperByDxfgEventType.get(dxfgClazz)
        .createNativeObject(this.stringMapperForMarketEvent.toJava(symbol));
  }
}
