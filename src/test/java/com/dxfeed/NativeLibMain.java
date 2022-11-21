// SPDX-License-Identifier: MPL-2.0
package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.DailyCandle;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.Order;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.option.Greeks;
import com.dxfeed.event.option.Series;
import com.dxfeed.event.option.TheoPrice;
import com.dxfeed.event.option.Underlying;
import com.dxfeed.promise.Promise;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NativeLibMain {

  /**
   * The function name must be converted to run_main in native library.
   */
  public static void main(final String[] args) throws InterruptedException {
    {
      final DXEndpoint dxEndpoint = DXEndpoint.getInstance();
      dxEndpoint.connect("demo.dxfeed.com:7300");
      final DXFeedSubscription<Order> subscription = dxEndpoint.getFeed()
          .createSubscription(Order.class);
      subscription.addEventListener(System.out::println);
      subscription.addSymbols("AAPL");
      final Promise<List<Series>> aapl = dxEndpoint.getFeed()
          .getIndexedEventsPromise(Series.class, "AAPL", IndexedEventSource.DEFAULT);
      aapl.awaitWithoutException(30, TimeUnit.SECONDS);
      final List<Series> result = aapl.getResult();
      final Throwable exception = aapl.getException();
      dxEndpoint.close();
    }
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
      dxEndpoint.connect("demo.dxfeed.com:7300");
      DXFeedTimeSeriesSubscription<TimeSeriesEvent<?>> subscription = dxEndpoint.getFeed()
          .createTimeSeriesSubscription(TimeAndSale.class, TheoPrice.class, Underlying.class,
              Candle.class,
              Greeks.class, DailyCandle.class);
      subscription.setFromTime(0);
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
