// SPDX-License-Identifier: MPL-2.0
package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.maper.InstrumentProfileMapper;
import com.dxfeed.api.osub.WildcardSymbol;
import com.dxfeed.event.EventType;
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
import com.dxfeed.ipf.InstrumentProfile;
import com.dxfeed.ipf.InstrumentProfileReader;
import com.dxfeed.ipf.InstrumentProfileType;
import com.dxfeed.ipf.live.InstrumentProfileCollector;
import com.dxfeed.ipf.live.InstrumentProfileConnection;
import com.dxfeed.model.market.OrderBookModel;
import com.dxfeed.model.market.OrderBookModelFilter;
import com.dxfeed.model.market.OrderBookModelListener;
import com.dxfeed.promise.Promise;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class NativeLibMain {

  public static void main(final String[] args) throws InterruptedException, IOException {
    tapeFile();
    dxEndpointWriteToTape();
    liveIpf();
    dxEndpointInstance();
    dxEndpointAuther();
    dxEndpointTimeSeriesSubscription();
    dxEndpointReadFromTape();
    dxEndpointOrderBookModel();
  }

  public static void tapeFile() throws InterruptedException {
    // Determine input and output tapes and specify appropriate configuration parameters
    String inputAddress = "file:ConvertTapeFile.in[readAs=stream_data,speed=max]";
    String outputAddress = "tape:ConvertTapeFile.out[saveAs=stream_data,format=text]";

    // Create input endpoint configured for tape reading
    DXEndpoint inputEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.STREAM_FEED) // prevents event conflation and loss due to buffer overflow
        .withProperty(DXEndpoint.DXFEED_WILDCARD_ENABLE_PROPERTY, "true") // enables wildcard subscription
        .withProperty(DXEndpoint.DXENDPOINT_EVENT_TIME_PROPERTY, "true") // use provided event times
        .build();
    // Create output endpoint configured for tape writing
    DXEndpoint outputEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.STREAM_PUBLISHER) // prevents event conflation and loss due to buffer overflow
        .withProperty(DXEndpoint.DXFEED_WILDCARD_ENABLE_PROPERTY, "true") // enables wildcard subscription
        .withProperty(DXEndpoint.DXENDPOINT_EVENT_TIME_PROPERTY, "true") // use provided event times
        .build();

    // Create and link event processor for all types of events
    // Note: set of processed event types could be limited if needed
    Class<? extends EventType<?>>[] eventTypes = inputEndpoint.getEventTypes().toArray(new Class[inputEndpoint.getEventTypes().size()]);
    DXFeedSubscription<? extends EventType<?>> sub = inputEndpoint.getFeed().createSubscription(eventTypes);
    sub.addEventListener(events -> {
      // Here event processing occurs. Events could be modified, removed, or new events added.
        /* For example, the below code adds 1 hour to event times:
        for (EventType event : events)
            event.setEventTime(event.getEventTime() + 3600_000);
        */
      // Publish processed events
      outputEndpoint.getPublisher().publishEvents(events);
    });
    // Subscribe to all symbols
    // Note: set of processed symbols could be limited if needed
    sub.addSymbols(WildcardSymbol.ALL);

    // Connect output endpoint and start output tape writing BEFORE starting input tape reading
    outputEndpoint.connect(outputAddress);
    // Connect input endpoint and start input tape reading AFTER starting output tape writing
    inputEndpoint.connect(inputAddress);

    // Wait until all data is read and processed, and then gracefully close input endpoint
    inputEndpoint.awaitNotConnected();
    inputEndpoint.closeAndAwaitTermination();

    // Wait until all data is processed and written, and then gracefully close output endpoint
    outputEndpoint.awaitProcessed();
    outputEndpoint.closeAndAwaitTermination();
  }

  private static void dxEndpointWriteToTape() throws InterruptedException {
    publishQuotes(
        DXEndpoint.newBuilder()
            .withProperty(DXEndpoint.DXFEED_WILDCARD_ENABLE_PROPERTY, "true")
            .withRole(DXEndpoint.Role.PUBLISHER)
            .build()
            .connect("tape:WriteTapeFile.text.txt[format=text]")
    );
    publishQuotes(
        DXEndpoint.newBuilder()
            .withProperty(DXEndpoint.DXFEED_WILDCARD_ENABLE_PROPERTY, "true")
            .withRole(DXEndpoint.Role.PUBLISHER)
            .build()
            .connect("tape:WriteTapeFile.binary.txt[format=binary]")
    );
    publishQuotes(
        DXEndpoint.newBuilder()
            .withProperty(DXEndpoint.DXFEED_WILDCARD_ENABLE_PROPERTY, "true")
            .withRole(DXEndpoint.Role.PUBLISHER)
            .build()
            .connect("tape:WriteTapeFile.csv.txt[format=csv]")
    );
    publishQuotes(
        DXEndpoint.newBuilder()
            .withProperty(DXEndpoint.DXFEED_WILDCARD_ENABLE_PROPERTY, "true")
            .withRole(DXEndpoint.Role.PUBLISHER)
            .build()
            .connect("tape:WriteTapeFile.opt.txt[opt=hs]")
    );
  }

  private static void publishQuotes(final DXEndpoint endpointText) throws InterruptedException {
    DXPublisher pub = endpointText.getPublisher();
    Quote quote1 = new Quote("TEST1");
    quote1.setBidPrice(10.1);
    quote1.setAskPrice(10.2);
    Quote quote2 = new Quote("TEST2");
    quote2.setBidPrice(17.1);
    quote2.setAskPrice(17.2);
    pub.publishEvents(Arrays.asList(quote1, quote2));
    endpointText.awaitProcessed();
    endpointText.closeAndAwaitTermination();
  }

  private static void dxEndpointOrderBookModel() throws InterruptedException {
    final DXEndpoint dxEndpoint = DXEndpoint.newBuilder().build();
    dxEndpoint.connect("demo.dxfeed.com:7300");
    DXFeed feed = dxEndpoint.getFeed();
    OrderBookModel model = new OrderBookModel();
    model.setFilter(OrderBookModelFilter.ALL);
    model.setSymbol("AAPL");
    model.addListener(new OrderBookModelListener() {
      public void modelChanged(Change change) {
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
    dxEndpoint.close();
  }

  private static void dxEndpointReadFromTape() throws InterruptedException {
    final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build().user("demo").password("demo");
    dxEndpoint.connect("tape.csv[format=csv,readAs=ticker_data,cycle,speed=max]");
    final var subscription3 = dxEndpoint.getFeed().createSubscription(Quote.class);
    subscription3.addEventListener(System.out::println);
    subscription3.addSymbols("AAPL");
    Thread.sleep(1000);
    dxEndpoint.close();
  }

  private static void dxEndpointTimeSeriesSubscription() throws InterruptedException {
    final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build().user("demo").password("demo");
    dxEndpoint.connect("demo.dxfeed.com:7300");
    DXFeedTimeSeriesSubscription<TimeSeriesEvent<?>> subscription2 = dxEndpoint.getFeed()
        .createTimeSeriesSubscription(
            TimeAndSale.class,
            TheoPrice.class,
            Underlying.class,
            Candle.class,
            Greeks.class,
            DailyCandle.class
        );
    subscription2.setFromTime(0);
    subscription2.addEventListener(System.out::println);
    subscription2.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA"));
    Thread.sleep(1000);
    dxEndpoint.close();
  }

  private static void dxEndpointAuther() throws InterruptedException {
    final String token = System.getProperty("token");
    final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build().user("demo").password("demo");
    final DXFeedSubscription<MarketEvent> subscription1 = dxEndpoint.getFeed()
        .createSubscription(Quote.class, TimeAndSale.class);
    dxEndpoint.connect("lessona.dxfeed.com:7905[login=entitle:" + token + "]");
    subscription1.addEventListener(System.out::println);
    subscription1.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA", "/BTCUSDT:CXBINA"));
    Thread.sleep(1000);
    dxEndpoint.close();
  }

  private static void dxEndpointInstance() {
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

  public static void liveIpf() throws InterruptedException, IOException {
    final InstrumentProfile instrumentProfile1 = new InstrumentProfile();
    final String[] customFields = InstrumentProfileMapper.getCustomFields(instrumentProfile1);
    InstrumentProfileMapper.setCustomFields(instrumentProfile1, customFields);

    for (final InstrumentProfile instrumentProfile : new InstrumentProfileReader().readFromFile(
        "ipf.txt")) {
      System.out.println(instrumentProfile);
    }

    InstrumentProfileCollector collector = new InstrumentProfileCollector();
    InstrumentProfileConnection connection = InstrumentProfileConnection.createConnection("https://demo:demo@tools.dxfeed.com/ipf", collector);
    // Update period can be used to re-read IPF files, not needed for services supporting IPF "live-update"
    connection.setUpdatePeriod(5_000L);
    connection.addStateChangeListener(event -> {
      System.out.println("Connection state: " + event.getNewValue());
    });
    connection.start();
    // We can wait until we get first full snapshot of instrument profiles
    connection.waitUntilCompleted(10, TimeUnit.SECONDS);

    // Data model to keep all instrument profiles mapped by their ticker symbol
    Map<String, InstrumentProfile> profiles = new ConcurrentHashMap<>();

    // It is possible to add listener after connection is started - updates will not be missed in this case
    collector.addUpdateListener(instruments -> {
      System.out.println("\nInstrument Profiles:");
      // We can observe REMOVED elements - need to add necessary filtering
      // See javadoc for InstrumentProfileCollector for more details

      // (1) We can either process instrument profile updates manually
      instruments.forEachRemaining(profile -> {
        if (InstrumentProfileType.REMOVED.name().equals(profile.getType())) {
          // Profile was removed - remove it from our data model
          profiles.remove(profile.getSymbol());
        } else {
          // Profile was updated - collector only notifies us if profile was changed
          profiles.put(profile.getSymbol(), profile);
        }
      });
      System.out.println("Total number of profiles (1): " + profiles.size());

      // (2) or access the concurrent view of instrument profiles
      Set<String> symbols = StreamSupport.stream(collector.view().spliterator(), false)
          .filter(profile -> !InstrumentProfileType.REMOVED.name().equals(profile.getType()))
          .map(InstrumentProfile::getSymbol)
          .collect(Collectors.toSet());
      System.out.println("Total number of profiles (2): " + symbols.size());

      System.out.println("Last modified: " + new Date(collector.getLastUpdateTime()));
    });

    try {
      Thread.sleep(30_000);
    } finally {
      connection.close();
    }
  }
}
