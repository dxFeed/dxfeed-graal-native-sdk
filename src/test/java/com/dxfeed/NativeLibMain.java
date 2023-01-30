// SPDX-License-Identifier: MPL-2.0
package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
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
import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.model.market.OrderBookModelFilter;
import com.dxfeed.model.market.OrderBookModelListener;
import com.dxfeed.promise.Promise;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NativeLibMain {

  /**
   * The function name must be converted to run_main in native library.
   */
  public static void main(final String[] args) throws InterruptedException {

    final DXEndpoint dxEndpointInstance = DXEndpoint.getInstance();
    dxEndpointInstance.connect("demo.dxfeed.com:7300");
    final DXFeedSubscription<Order> subscription = dxEndpointInstance.getFeed()
        .createSubscription(Order.class);
    subscription.addEventListener(System.out::println);
    subscription.addSymbols("AAPL");
    final Promise<List<Series>> aapl = dxEndpointInstance.getFeed()
        .getIndexedEventsPromise(Series.class, "AAPL", IndexedEventSource.DEFAULT);
    aapl.awaitWithoutException(30, TimeUnit.SECONDS);
    final List<Series> result = aapl.getResult();
    final Throwable exception = aapl.getException();
    dxEndpointInstance.close();

    final String token = System.getProperty("token");
    final DXEndpoint dxEndpoint1 = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build().user("demo").password("demo");
    final DXFeedSubscription<MarketEvent> subscription1 = dxEndpoint1.getFeed()
        .createSubscription(Quote.class, TimeAndSale.class);
    dxEndpoint1.connect("lessona.dxfeed.com:7905[login=entitle:" + token + "]");
    subscription1.addEventListener(System.out::println);
    subscription1.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA", "/BTCUSDT:CXBINA"));
    Thread.sleep(1000);
    dxEndpoint1.close();

    final DXEndpoint dxEndpoint2 = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build().user("demo").password("demo");
    dxEndpoint2.connect("demo.dxfeed.com:7300");
    DXFeedTimeSeriesSubscription<TimeSeriesEvent<?>> subscription2 = dxEndpoint2.getFeed()
        .createTimeSeriesSubscription(TimeAndSale.class, TheoPrice.class, Underlying.class,
            Candle.class,
            Greeks.class, DailyCandle.class);
    subscription2.setFromTime(0);
    subscription2.addEventListener(System.out::println);
    subscription2.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA"));
    Thread.sleep(1000);
    dxEndpoint2.close();

    final DXEndpoint dxEndpoint3 = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build().user("demo").password("demo");
    dxEndpoint3.connect("tape.csv[format=csv,readAs=ticker_data,cycle,speed=max]");
    final var subscription3 = dxEndpoint3.getFeed().createSubscription(Quote.class);
    subscription3.addEventListener(System.out::println);
    subscription3.addSymbols("AAPL");
    Thread.sleep(1000);
    dxEndpoint3.close();

    final DXEndpoint dxEndpoint4 = DXEndpoint.newBuilder().build();
    dxEndpoint4.connect("demo.dxfeed.com:7300");
    DXFeed feed = dxEndpoint4.getFeed();
    OrderBookModel model = new OrderBookModel();
    model.setFilter(OrderBookModelFilter.ALL);
    model.setSymbol("AAPL");
    model.addListener(new OrderBookModelListener() {
      public void modelChanged(OrderBookModelListener.Change change) {
        System.out.println("Buy orders:");
        for (Order order : model.getBuyOrders()) {
          System.out.println(order);
        }
        System.out.println("Sell orders:");
        for (Order order : model.getSellOrders()) {
          System.out.println(order);
        }
        System.out.println();
      }
    });
    model.attach(feed);
    Thread.sleep(1000);
    dxEndpoint4.close();
  }
}
