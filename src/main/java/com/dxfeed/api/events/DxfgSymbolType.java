package com.dxfeed.api.events;

import com.dxfeed.api.osub.WildcardSymbol;
import com.dxfeed.event.candle.CandleSymbol;
import java.util.function.Function;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;

@CContext(Directives.class)
@CEnum("dxfg_symbol_type_t")
public enum DxfgSymbolType {
  STRING(string -> string),
  CANDLE(CandleSymbol::valueOf),
  WILDCARD(string -> WildcardSymbol.ALL);

  public final Function<String, Object> createSymbolFunction;

  DxfgSymbolType(final Function<String, Object> createSymbolFunction) {
    this.createSymbolFunction = createSymbolFunction;
  }

  @CEnumLookup
  public static native DxfgSymbolType fromCValue(int value);

  @CEnumValue
  public native int getCValue();
}
