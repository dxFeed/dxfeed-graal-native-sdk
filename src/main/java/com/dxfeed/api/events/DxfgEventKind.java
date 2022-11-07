package com.dxfeed.api.events;

import com.dxfeed.event.EventType;
import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.DailyCandle;
import com.dxfeed.event.market.AnalyticOrder;
import com.dxfeed.event.market.OrderBase;
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
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumConstant;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_event_kind_t")
public enum DxfgEventKind {
  DXFG_EVENT_TYPE_QUOTE,          // LASTING
  DXFG_EVENT_TYPE_PROFILE,        // LASTING
  DXFG_EVENT_TYPE_SUMMARY,        // LASTING
  DXFG_EVENT_TYPE_GREEKS,         // LASTING + INDEXED -> TIME_SERIES
  DXFG_EVENT_TYPE_CANDLE,         // LASTING + INDEXED -> TIME_SERIES
  DXFG_EVENT_TYPE_DAILY_CANDLE,   // LASTING + INDEXED -> TIME_SERIES -> CANDLE
  DXFG_EVENT_TYPE_UNDERLYING,     // LASTING + INDEXED -> TIME_SERIES
  DXFG_EVENT_TYPE_THEO_PRICE,     // LASTING + INDEXED -> TIME_SERIES
  //abstract DXFG_EVENT_TYPE_TRADE_BASE,     // LASTING
  DXFG_EVENT_TYPE_TRADE,          // LASTING -> TRADE_BASE
  DXFG_EVENT_TYPE_TRADE_ETH,      // LASTING -> TRADE_BASE
  DXFG_EVENT_TYPE_CONFIGURATION,  // LASTING
  DXFG_EVENT_TYPE_MESSAGE,        //
  DXFG_EVENT_TYPE_TIME_AND_SALE,  // INDEXED -> TIME_SERIES
  DXFG_EVENT_TYPE_ORDER_BASE,     // INDEXED
  DXFG_EVENT_TYPE_ORDER,          // INDEXED -> ORDER_BASE
  DXFG_EVENT_TYPE_ANALYTIC_ORDER, // INDEXED -> ORDER_BASE -> ORDER
  DXFG_EVENT_TYPE_SPREAD_ORDER,   // INDEXED -> ORDER_BASE
  DXFG_EVENT_TYPE_SERIES,         // INDEXED
  ;

  public static Class<? extends EventType<?>> toEventType(final int value) {
    switch (fromCValue(value)) {
      case DXFG_EVENT_TYPE_QUOTE:
        return Quote.class;
      case DXFG_EVENT_TYPE_PROFILE:
        return Profile.class;
      case DXFG_EVENT_TYPE_SUMMARY:
        return Summary.class;
      case DXFG_EVENT_TYPE_GREEKS:
        return Greeks.class;
      case DXFG_EVENT_TYPE_CANDLE:
        return Candle.class;
      case DXFG_EVENT_TYPE_DAILY_CANDLE:
        return DailyCandle.class;
      case DXFG_EVENT_TYPE_UNDERLYING:
        return Underlying.class;
      case DXFG_EVENT_TYPE_THEO_PRICE:
        return TheoPrice.class;
      case DXFG_EVENT_TYPE_TRADE:
        return Trade.class;
      case DXFG_EVENT_TYPE_TRADE_ETH:
        return TradeETH.class;
      case DXFG_EVENT_TYPE_CONFIGURATION:
        return Configuration.class;
      case DXFG_EVENT_TYPE_MESSAGE:
        return Message.class;
      case DXFG_EVENT_TYPE_TIME_AND_SALE:
        return TimeAndSale.class;
      case DXFG_EVENT_TYPE_ORDER_BASE:
        return OrderBase.class;
      case DXFG_EVENT_TYPE_ORDER:
        return OrderBase.class;
      case DXFG_EVENT_TYPE_ANALYTIC_ORDER:
        return AnalyticOrder.class;
      case DXFG_EVENT_TYPE_SPREAD_ORDER:
        return SpreadOrder.class;
      case DXFG_EVENT_TYPE_SERIES:
        return Series.class;
      default:
        throw new IllegalArgumentException();
    }
  }

  @CEnumLookup
  public static native DxfgEventKind fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
