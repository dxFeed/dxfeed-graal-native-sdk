package com.dxfeed.sdk.model;

import com.dxfeed.model.market.OrderBookModelFilter;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_order_book_model_filter_t")
public enum DxfgOrderBookModelFilter {
  COMPOSITE(OrderBookModelFilter.COMPOSITE),
  REGIONAL(OrderBookModelFilter.REGIONAL),
  AGGREGATE(OrderBookModelFilter.AGGREGATE),
  ORDER(OrderBookModelFilter.ORDER),
  COMPOSITE_REGIONAL(OrderBookModelFilter.COMPOSITE_REGIONAL),
  COMPOSITE_REGIONAL_AGGREGATE(OrderBookModelFilter.COMPOSITE_REGIONAL_AGGREGATE),
  ALL(OrderBookModelFilter.ALL),
  ;

  public final OrderBookModelFilter filter;

  DxfgOrderBookModelFilter(final OrderBookModelFilter filter) {
    this.filter = filter;
  }

  @CEnumLookup
  public static native DxfgOrderBookModelFilter fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
