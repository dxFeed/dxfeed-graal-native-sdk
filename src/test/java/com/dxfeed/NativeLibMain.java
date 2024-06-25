// SPDX-License-Identifier: MPL-2.0
package com.dxfeed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXEndpoint.Role;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.api.osub.WildcardSymbol;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.event.candle.Candle;
import com.dxfeed.event.candle.DailyCandle;
import com.dxfeed.event.market.MarketEvent;
import com.dxfeed.event.market.OptionSale;
import com.dxfeed.event.market.Order;
import com.dxfeed.event.market.Profile;
import com.dxfeed.event.market.Quote;
import com.dxfeed.event.market.SpreadOrder;
import com.dxfeed.event.market.Summary;
import com.dxfeed.event.market.TimeAndSale;
import com.dxfeed.event.market.Trade;
import com.dxfeed.event.market.TradeETH;
import com.dxfeed.event.misc.Configuration;
import com.dxfeed.event.misc.Message;
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
import com.dxfeed.schedule.Day;
import com.dxfeed.schedule.DayFilter;
import com.dxfeed.schedule.Schedule;
import com.dxfeed.schedule.Session;
import com.dxfeed.schedule.SessionFilter;
import com.dxfeed.scheme.impl.DefaultEmbeddedTypes;
import com.dxfeed.sdk.maper.InstrumentProfileMapper;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

  public static void main(final String[] args) throws Exception {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
    objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    objectMapper.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);

    System.out.println(new DefaultEmbeddedTypes().getSerialType("wide_decimal"));
    dxLink();
    dxEndpointMonitoring();
    tapeFile();
    dxEndpointWriteToTape();
    liveIpf(objectMapper);
    dxEndpointInstance(objectMapper);
    dxEndpointAuther(objectMapper);
    dxEndpointTimeSeriesSubscription(objectMapper);
    dxEndpointReadFromTape();
    dxEndpointOrderBookModel();
    schedule();
    reflectAllMethodsForMarketEvents();
    connectionOrderAndRestoreTime();
    System.exit(1);
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
    dxEndpoint.connect("********");
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
        .build().user("********").password("********");
    dxEndpoint.connect("tape.csv[format=csv,readAs=ticker_data,cycle,speed=max]");
    final var subscription3 = dxEndpoint.getFeed().createSubscription(Quote.class);
    subscription3.addEventListener(System.out::println);
    subscription3.addSymbols("AAPL");
    Thread.sleep(1000);
    dxEndpoint.close();
  }

  private static void dxLink() throws InterruptedException {
    System.setProperty("dxfeed.experimental.dxlink.enable", "true");
    System.setProperty("scheme", "ext:resource:dxlink.xml");
    final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build();
    dxEndpoint.connect("dxlink:wss://********/dxlink-ws[login=dxlink:token]");

    DXFeedTimeSeriesSubscription<TimeSeriesEvent<?>> tsSubscription = dxEndpoint.getFeed()
        .createTimeSeriesSubscription(
            TimeAndSale.class,
            TheoPrice.class,
            Underlying.class,
            Candle.class,
            Greeks.class,
            DailyCandle.class
        );
    tsSubscription.setFromTime(0);
    tsSubscription.addEventListener(events -> events.forEach(System.out::println));
    tsSubscription.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA"));

    final DXFeedSubscription<EventType<?>> subscription = dxEndpoint.getFeed().createSubscription(
        Candle.class,
        DailyCandle.class,
        Configuration.class,
        Greeks.class,
        OptionSale.class,
        Order.class,
        SpreadOrder.class,
        Profile.class,
        Quote.class,
        Series.class,
        Summary.class,
        TheoPrice.class,
        TimeAndSale.class,
        Trade.class,
        TradeETH.class,
        Underlying.class,
        Message.class
    );
    subscription.addEventListener(events -> events.forEach(System.out::println));
    subscription.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA"));

    Thread.sleep(7000);

    tsSubscription.close();
    subscription.close();
    dxEndpoint.close();
  }

  private static void dxEndpointMonitoring() throws InterruptedException {
    final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .withProperty("monitoring.stat", "1")
        .build().user("********").password("********");
    dxEndpoint.connect("********");
    final DXFeedSubscription<Quote> subscription = dxEndpoint.getFeed().createSubscription(Quote.class);
    subscription.addEventListener(events -> {
    });
    subscription.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA"));
    Thread.sleep(7000);
    subscription.close();
    dxEndpoint.close();
  }

  private static void dxEndpointTimeSeriesSubscription(final ObjectMapper objectMapper) throws InterruptedException {
    final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build().user("********").password("********");
    dxEndpoint.connect("********");
    DXFeedTimeSeriesSubscription<TimeSeriesEvent<?>> subscription = dxEndpoint.getFeed()
        .createTimeSeriesSubscription(
            TimeAndSale.class,
            TheoPrice.class,
            Underlying.class,
            Candle.class,
            Greeks.class,
            DailyCandle.class
        );
    subscription.setFromTime(0);
    subscription.addEventListener(events -> print(events, objectMapper));
    subscription.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA"));
    Thread.sleep(1000);
    dxEndpoint.close();
  }

  private static void dxEndpointAuther(final ObjectMapper objectMapper) throws InterruptedException {
    final String token = System.getProperty("token");
    final DXEndpoint dxEndpoint = DXEndpoint.newBuilder()
        .withRole(DXEndpoint.Role.FEED)
        .withProperties(System.getProperties())
        .build().user("********").password("********");
    final DXFeedSubscription<MarketEvent> subscription1 = dxEndpoint.getFeed()
        .createSubscription(Quote.class, TimeAndSale.class);
    dxEndpoint.connect("********[login=entitle:" + token + "]");
    subscription1.addEventListener(events -> print(events, objectMapper));
    subscription1.addSymbols(
        Arrays.asList("AAPL", "MSFT", "AMZN", "YHOO", "IBM", "SPX", "ETH/USD:GDAX", "EUR/USD",
            "BTC/USDT:CXBINA", "/BTCUSDT:CXBINA"));
    Thread.sleep(1000);
    dxEndpoint.close();
  }

  private static void dxEndpointInstance(final ObjectMapper objectMapper) {
    final DXEndpoint dxEndpoint = DXEndpoint.getInstance();
    dxEndpoint.connect("********");
    final DXFeedSubscription<Order> subscription = dxEndpoint.getFeed()
        .createSubscription(Order.class);
    subscription.addEventListener(events -> print(events, objectMapper));
    subscription.addSymbols("AAPL");
    final Promise<List<Series>> aapl = dxEndpoint.getFeed()
        .getIndexedEventsPromise(Series.class, "AAPL", IndexedEventSource.DEFAULT);
    aapl.awaitWithoutException(30, TimeUnit.SECONDS);
    final List<Series> result = aapl.getResult();
    final Throwable exception = aapl.getException();
    dxEndpoint.close();
  }

  private static void print(final List<? extends EventType<?>> events, final ObjectMapper objectMapper) {
    for (final EventType<?> event : events) {
      printObject(objectMapper, event);
    }
  }

  private static void printObject(final ObjectMapper objectMapper, final Object event) {
    try {
      final String content = objectMapper.writeValueAsString(event);
      System.out.println(content);
      System.out.println(objectMapper.readValue(content, event.getClass()));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static void liveIpf(final ObjectMapper objectMapper) throws InterruptedException, IOException {
    final InstrumentProfile instrumentProfile1 = new InstrumentProfile();
    final String[] customFields = InstrumentProfileMapper.getCustomFields(instrumentProfile1);
    InstrumentProfileMapper.setCustomFields(instrumentProfile1, customFields);

    for (final InstrumentProfile instrumentProfile : new InstrumentProfileReader().readFromFile(
        "ipf.txt")) {
      System.out.println(instrumentProfile);
    }

    InstrumentProfileCollector collector = new InstrumentProfileCollector();
    InstrumentProfileConnection connection = InstrumentProfileConnection.createConnection("https://********/ipf", collector);
    // Update period can be used to re-read IPF files, not needed for services supporting IPF "live-update"
    connection.setUpdatePeriod(5_000L);
    connection.addStateChangeListener(event -> System.out.println("Connection state: " + event.getNewValue()));
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
        printObject(objectMapper, profile);
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

  public static void schedule() throws Exception {
    System.setProperty("com.dxfeed.schedule.download", "auto");
    InstrumentProfile profile = null;
    for (final InstrumentProfile prfl : new InstrumentProfileReader().readFromFile(
        "ipf.txt")) {
      profile = prfl;
      Schedule.getInstance(prfl);
      for (final String venue : Schedule.getTradingVenues(prfl))
        Schedule.getInstance(prfl, venue);
    }
    final long time = System.currentTimeMillis();
    printNext5Holidays(profile, time);
    printCurrentSession(profile, time);
    printNextTradingSession(profile, time);
    printNearestTradingSession(profile, time);
  }

  private static void printNext5Holidays(InstrumentProfile profile, long time) {
    Schedule schedule = Schedule.getInstance(profile);
    Day day = schedule.getDayByTime(time);
    String output = "5 next holidays for " + profile.getSymbol() + ":";
    for (int i = 0; i < 5; i++) {
      day = day.findNextDay(DayFilter.HOLIDAY);
      if (day != null)
        output = output + " " + day.getYearMonthDay();
      else
        break;
    }
    System.out.println(output);
  }

  private static void printCurrentSession(InstrumentProfile profile, long time) {
    Schedule schedule = Schedule.getInstance(profile);
    Session session = schedule.getSessionByTime(time);
    System.out.println("Current session for " + profile.getSymbol() + ": " + session + " in " + session.getDay());
  }

  private static void printNextTradingSession(InstrumentProfile profile, long time) {
    Schedule schedule = Schedule.getInstance(profile);
    Session session = schedule.getSessionByTime(time);
    if (!session.isTrading())
      session = session.getNextSession(SessionFilter.TRADING);
    System.out.println("Next trading session for " + profile.getSymbol() +  ": " + session + " in " + session.getDay());
  }

  private static void printNearestTradingSession(InstrumentProfile profile, long time) {
    Schedule schedule = Schedule.getInstance(profile);
    Session session = schedule.getNearestSessionByTime(time, SessionFilter.TRADING);
    System.out.println("Nearest trading session for " + profile.getSymbol() + ": " + session + " in " + session.getDay());
  }

  private static void connectionOrderAndRestoreTime() {
    DXEndpoint.create().connect("(********,********[connectOrder=priority,restoreTime=00:00])");
  }

  private static void reflectAllMethodsForMarketEvents() {
    Set<Class<? extends EventType<?>>> types = DXEndpoint.create(Role.STREAM_FEED).getEventTypes();
    for (Class<? extends EventType<?>> type : types) {
      List<Method> methods = getDeclaredMethods(type);
      for (Method method : methods) {
        try {
          method.invoke(type.getConstructor().newInstance());
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
        }
        System.out.println(method);
      }
    }
  }

  private static List<Method> getDeclaredMethods(Class<?> clazz) {
    List<Method> methods = new ArrayList<>();
    while (clazz != null && clazz != Object.class) {
      methods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
      clazz = clazz.getSuperclass();
    }
    return methods;
  }
}
