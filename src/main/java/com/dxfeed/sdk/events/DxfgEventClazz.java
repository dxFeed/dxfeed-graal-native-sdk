package com.dxfeed.sdk.events;

import com.dxfeed.event.EventType;
import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.DailyCandle;
import com.dxfeed.event.market.AnalyticOrder;
import com.dxfeed.event.market.OptionSale;
import com.dxfeed.event.market.Order;
import com.dxfeed.event.market.OrderBase;
import com.dxfeed.event.market.OtcMarketsOrder;
import com.dxfeed.event.market.Profile;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.SpreadOrder;
import com.dxfeed.event.market.Summary;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import com.dxfeed.event.market.TradeETH;
import com.dxfeed.event.misc.Configuration;
import com.dxfeed.event.misc.Message;
import com.dxfeed.event.option.Greeks;
import com.dxfeed.event.option.Series;
import com.dxfeed.event.option.TheoPrice;
import com.dxfeed.event.option.Underlying;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_event_clazz_t")
public enum DxfgEventClazz {
  DXFG_EVENT_QUOTE(Quote.class),                 // LASTING
  DXFG_EVENT_PROFILE(Profile.class),             // LASTING
  DXFG_EVENT_SUMMARY(Summary.class),             // LASTING
  DXFG_EVENT_GREEKS(Greeks.class),               // LASTING + INDEXED -> TIME_SERIES
  DXFG_EVENT_CANDLE(Candle.class),               // LASTING + INDEXED -> TIME_SERIES
  DXFG_EVENT_DAILY_CANDLE(DailyCandle.class),    // LASTING + INDEXED -> TIME_SERIES -> CANDLE
  DXFG_EVENT_UNDERLYING(Underlying.class),       // LASTING + INDEXED -> TIME_SERIES
  DXFG_EVENT_THEO_PRICE(TheoPrice.class),        // LASTING + INDEXED -> TIME_SERIES
  //abstract DXFG_EVENT_TRADE_BASE,              // LASTING
  DXFG_EVENT_TRADE(Trade.class),                 // LASTING -> TRADE_BASE
  DXFG_EVENT_TRADE_ETH(TradeETH.class),          // LASTING -> TRADE_BASE
  DXFG_EVENT_CONFIGURATION(Configuration.class), // LASTING
  DXFG_EVENT_MESSAGE(Message.class),             //
  DXFG_EVENT_TIME_AND_SALE(TimeAndSale.class),   // INDEXED -> TIME_SERIES
  DXFG_EVENT_ORDER_BASE(OrderBase.class),        // INDEXED
  DXFG_EVENT_ORDER(Order.class),                 // INDEXED -> ORDER_BASE
  DXFG_EVENT_ANALYTIC_ORDER(AnalyticOrder.class),// INDEXED -> ORDER_BASE -> ORDER
  DXFG_EVENT_OTC_MARKETS_ORDER(OtcMarketsOrder.class),// INDEXED -> ORDER_BASE -> ORDER
  DXFG_EVENT_SPREAD_ORDER(SpreadOrder.class),    // INDEXED -> ORDER_BASE
  DXFG_EVENT_SERIES(Series.class),               // INDEXED
  DXFG_EVENT_OPTION_SALE(OptionSale.class),      // INDEXED
  ;
  private static final Map<Class<? extends EventType<?>>, DxfgEventClazz> map = new HashMap<>();

  static {
    map.put(Quote.class, DXFG_EVENT_QUOTE);
    map.put(Profile.class, DXFG_EVENT_PROFILE);
    map.put(Summary.class, DXFG_EVENT_SUMMARY);
    map.put(Greeks.class, DXFG_EVENT_GREEKS);
    map.put(Candle.class, DXFG_EVENT_CANDLE);
    map.put(DailyCandle.class, DXFG_EVENT_DAILY_CANDLE);
    map.put(Underlying.class, DXFG_EVENT_UNDERLYING);
    map.put(TheoPrice.class, DXFG_EVENT_THEO_PRICE);
    // map.put(TradeBase.class, DXFG_EVENT_TRADE_BASE);
    map.put(Trade.class, DXFG_EVENT_TRADE);
    map.put(TradeETH.class, DXFG_EVENT_TRADE_ETH);
    map.put(Configuration.class, DXFG_EVENT_CONFIGURATION);
    map.put(Message.class, DXFG_EVENT_MESSAGE);
    map.put(TimeAndSale.class, DXFG_EVENT_TIME_AND_SALE);
    map.put(OrderBase.class, DXFG_EVENT_ORDER_BASE);
    map.put(Order.class, DXFG_EVENT_ORDER);
    map.put(AnalyticOrder.class, DXFG_EVENT_ANALYTIC_ORDER);
    map.put(OtcMarketsOrder.class, DXFG_EVENT_OTC_MARKETS_ORDER);
    map.put(SpreadOrder.class, DXFG_EVENT_SPREAD_ORDER);
    map.put(Series.class, DXFG_EVENT_SERIES);
    map.put(OptionSale.class, DXFG_EVENT_OPTION_SALE);
  }

  public final Class<? extends EventType<?>> clazz;

  DxfgEventClazz(final Class<? extends EventType<?>> clazz) {
    this.clazz = clazz;
  }

  public static DxfgEventClazz of(final Class<? extends EventType<?>> clazz) {
    return map.get(clazz);
  }

  @CEnumLookup
  public static native DxfgEventClazz fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
