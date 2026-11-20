// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import com.devexperts.rmi.RMIEndpoint;
import com.dxfeed.event.candle.CandleSymbol;
import com.dxfeed.event.market.Order;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.event.market.Quote;
import com.dxfeed.orcs.api.AuthOrderSource;
import com.dxfeed.orcs.api.PriceLevelService;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * The PriceLevelServiceHolder class serves as a client-side wrapper to access the PriceLevelService.
 * It establishes and manages a remote connection to the corresponding service using RMIEndpoint.
 * The class provides functionality to retrieve market orders and quotes within a specified time frame.
 */
public class PriceLevelServiceHolder implements Closeable {

    RMIEndpoint endpoint;
    PriceLevelService service;

    public PriceLevelServiceHolder(String address) {
        endpoint = RMIEndpoint.createEndpoint(RMIEndpoint.Side.CLIENT);
        endpoint.connect(address);
        service = endpoint.getClient().getProxy(PriceLevelService.class);
    }

    public List<Order> getOrders(CandleSymbol candleSymbol, OrderSource source, long from, long to, String caller) {
        return service.getOrders(candleSymbol, source.id(), from, to, caller);
    }

    public List<Order> getOrders(CandleSymbol candleSymbol, OrderSource source, long from, long to) {
        return getOrders(candleSymbol, source, from, to, "dxfg");
    }

    public AuthOrderSource getAuthOrderSource() {
        return service.getAuthOrderSource();
    }

    public List<Quote> getQuotes(CandleSymbol candleSymbol, long from, long to, String caller) {
        return service.getQuotes(candleSymbol, from, to, caller);
    }

    public List<Quote> getQuotes(CandleSymbol candleSymbol, long from, long to) {
        return getQuotes(candleSymbol, from, to, "dxfg");
    }

    @Override
    public void close() throws IOException {
        endpoint.close();
    }
}
