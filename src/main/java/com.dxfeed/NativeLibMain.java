package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.TimeAndSale;

import java.util.Arrays;

public class NativeLibMain {
    /**
     * The function name must be converted to run_main in native library.
     */
    public static void main(final String[] args) throws InterruptedException {
        final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
                .withRole(DXEndpoint.Role.FEED)
                .build()
                .user("demo")
                .password("demo");
        final DXFeedSubscription<MarketEvent> subscription = dxEndpoint.getFeed().createSubscription(TimeAndSale.class);
        dxEndpoint.connect("demo.dxfeed.com:7300");
        subscription.addEventListener(System.out::println);
        subscription.addSymbols(Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD"));
        Thread.sleep(1000);
        dxEndpoint.close();
    }
}