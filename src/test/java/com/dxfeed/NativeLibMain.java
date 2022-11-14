// SPDX-License-Identifier: MPL-2.0
package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;

import java.util.Arrays;

public class NativeLibMain {

  /**
   * The function name must be converted to run_main in native library.
   */
  public static void main(final String[] args) throws InterruptedException {
    {
      final String token = System.getProperty("token");
      final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
          .withRole(DXEndpoint.Role.FEED)
          .withProperties(System.getProperties())
          .build().user("demo").password("demo");
      final DXFeedSubscription<MarketEvent> subscription = dxEndpoint.getFeed()
          .createSubscription(Quote.class, TimeAndSale.class);
      dxEndpoint.connect("lessona.dxfeed.com:7905[login=entitle:" + token + "]");
      subscription.addEventListener(System.out::println);
      subscription.addSymbols(
          Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
              "BTC/USDT:CXBINA"));
      Thread.sleep(1000);
      dxEndpoint.close();
    }
    {
      final DXEndpoint dxEndpoint = DXEndpoint.getInstance();
      dxEndpoint.connect("tape.csv[format=csv,readAs=ticker_data,cycle,speed=max]");
      final var subscription = dxEndpoint.getFeed().createSubscription(Quote.class);
      subscription.addEventListener(System.out::println);
      subscription.addSymbols("AAPL");
      Thread.sleep(1000);
      dxEndpoint.close();
    }
  }

}