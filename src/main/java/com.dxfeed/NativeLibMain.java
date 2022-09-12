package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class NativeLibMain {
    /**
     * The function name must be converted to run_main in native library.
     */
    public static void main(final String[] args) throws InterruptedException, IOException {
        final String token = System.getProperty("token");
        final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
                .withRole(DXEndpoint.Role.FEED)
                .withProperties(System.getProperties())
                .build();
        final DXFeedSubscription<MarketEvent> subscription = dxEndpoint.getFeed().createSubscription(Quote.class, TimeAndSale.class);
        dxEndpoint.connect("lessona.dxfeed.com:7905[login=entitle:" + token + "]");
        subscription.addEventListener(System.out::println);
        subscription.addSymbols(Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD", "BTC/USDT:CXBINA"));
        Thread.sleep(1000);
        dxEndpoint.close();
    }

}