package com.dxfeed.sdk.events;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CStruct;

@CContext(Directives.class)
@CStruct("dxfg_trade_eth_t")
public interface DxfgTradeETH extends DxfgTradeBase {

}
