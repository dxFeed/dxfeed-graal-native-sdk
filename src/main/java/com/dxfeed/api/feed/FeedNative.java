package com.dxfeed.api.feed;

import static com.dxfeed.api.NativeUtils.EVENT_MAPPER;
import static com.dxfeed.api.NativeUtils.createHandler;
import static com.dxfeed.api.NativeUtils.extractHandler;
import static com.dxfeed.api.NativeUtils.toJavaString;
import static com.dxfeed.api.NativeUtils.toJavaSymbol;
import static com.dxfeed.api.endpoint.EndpointNative.FEED_HANDLES;
import static com.dxfeed.api.endpoint.EndpointNative.INSTANCES;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgEventPointer;
import com.dxfeed.api.events.DxfgSymbol;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.subscription.DxfgSubscription;
import com.dxfeed.api.subscription.DxfgTimeSeriesSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.LastingEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.event.market.OrderSource;
import com.dxfeed.promise.Promise;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.WordFactory;

@CContext(Directives.class)
public class FeedNative {

  @CEntryPoint(
      name = "dxfg_feed_get_instance",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getInstance(
      final IsolateThread ignoredThread,
      final DxfgFeed dxfgFeed
  ) {
    INSTANCES.computeIfAbsent(
        DXEndpoint.getInstance().getRole(),
        role -> createHandler(DXEndpoint.getInstance(role)).rawValue()
    );
    dxfgFeed.setJavaObjectHandler(
        WordFactory.signed(
            FEED_HANDLES.computeIfAbsent(
                DXEndpoint.getInstance(),
                k -> createHandler(k.getFeed()).rawValue()
            )
        )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_feed_create_subscription",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int createSubscription(
      final IsolateThread ignoredThread,
      final DxfgFeed dxfgFeed,
      final CIntPointer eventTypes,
      final int eventTypesSize,
      final DxfgSubscription dxfgSubscription
  ) {
    final Class<? extends EventType<?>>[] types = new Class[eventTypesSize];
    for (int i = 0; i < eventTypesSize; ++i) {
      types[i] = DxfgEventKind.fromCValue(eventTypes.read(i)).clazz;
    }
    final DXFeed feed = extractHandler(dxfgFeed.getJavaObjectHandler());
    dxfgSubscription.setJavaObjectHandler(
        createHandler(feed.createSubscription(types))
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_feed_create_time_series_subscription",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int createTimeSeriesSubscription(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventKind eventType,
      final DxfgTimeSeriesSubscription dxfgTimeSeriesSubscription
  ) {
    dxfgTimeSeriesSubscription.setJavaObjectHandler(
        createHandler(
            toJavaFeed(feed)
                .createTimeSeriesSubscription(
                    (Class<TimeSeriesEvent<?>>) eventType.clazz
                )
        )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @SafeVarargs
  public final <E extends TimeSeriesEvent<?>> DXFeedTimeSeriesSubscription<E> createTimeSeriesSubscription(
      final Class<? extends E>... eventTypes
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  @CEntryPoint(
      name = "dxfg_feed_attach_subscription",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int attachSubscription(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgSubscription subscription
  ) {
    toJavaFeed(feed).attachSubscription(toJavaSubscription(subscription));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_feed_detach_subscription",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int detachSubscription(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgSubscription subscription
  ) {
    toJavaFeed(feed).detachSubscription(toJavaSubscription(subscription));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_feed_detach_subscription_and_clear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int detachSubscriptionAndClear(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgSubscription subscription
  ) {
    toJavaFeed(feed).detachSubscriptionAndClear(toJavaSubscription(subscription));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_feed_get_last_event_if_subscribed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getLastEventIfSubscribed(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventKind eventType,
      final DxfgSymbol dxfgSymbol,
      final DxfgEventPointer event
  ) {
    final LastingEvent<?> result = toJavaFeed(feed)
        .getLastEventIfSubscribed(
            (Class<LastingEvent<?>>) eventType.clazz,
            toJavaSymbol(dxfgSymbol)
        );
    if (result == null) {
      event.write(WordFactory.nullPointer());
    } else {
      event.write(EVENT_MAPPER.nativeObject(Collections.singletonList(result)).read());
    }
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_feed_get_indexed_event_if_subscribed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getIndexedEventIfSubscribed(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventKind eventType,
      final DxfgSymbol dxfgSymbol,
      final CCharPointer source,
      final DxfgEventPointer events,
      final CIntPointer eventsSize
  ) {
    final List<? extends EventType<?>> result = toJavaFeed(feed)
        .getIndexedEventsIfSubscribed(
            (Class<IndexedEvent<?>>) eventType.clazz,
            toJavaSymbol(dxfgSymbol),
            OrderSource.valueOf(toJavaString(source))
        );
    if (result == null) {
      events.write(WordFactory.nullPointer());
      eventsSize.write(0);
    } else {
      events.write(EVENT_MAPPER.nativeObject(result).read());
      eventsSize.write(result.size());
    }
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_feed_get_time_series_event_if_subscribed",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int getTimeSeriesEventIfSubscribed(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventKind eventType,
      final DxfgSymbol dxfgSymbol,
      final long fromTime,
      final long toTime,
      final DxfgEventPointer events,
      final CIntPointer eventsSize
  ) {
    final List<TimeSeriesEvent<?>> result = toJavaFeed(feed)
        .getTimeSeriesIfSubscribed(
            (Class<TimeSeriesEvent<?>>) eventType.clazz,
            toJavaSymbol(dxfgSymbol),
            fromTime,
            toTime
        );
    if (result == null) {
      events.write(WordFactory.nullPointer());
      eventsSize.write(0);
    } else {
      events.write(EVENT_MAPPER.nativeObject(result).read());
      eventsSize.write(result.size());
    }
    return EXECUTE_SUCCESSFULLY;
  }

  public <E extends LastingEvent<?>> E getLastEvent(
      final E event
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public <E extends LastingEvent<?>> Collection<E> getLastEvents(
      final Collection<E> events
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public <E extends LastingEvent<?>> Promise<E> getLastEventPromise(
      final Class<E> eventType,
      final Object symbol
  ) {
//        if (symbol instanceof CandleSymbol)
//            return symbol;
//        if (symbol instanceof String)
//            return CandleSymbol.valueOf((String) symbol);
//        if (symbol instanceof WildcardSymbol)
//            return symbol;
    DXFeed.getInstance().getLastEventPromise(null, null);
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public <E extends LastingEvent<?>> List<Promise<E>> getLastEventsPromises(
      final Class<E> eventType,
      final Collection<?> symbols
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public <E extends IndexedEvent<?>> Promise<List<E>> getIndexedEventsPromise(
      final Class<E> eventType,
      final Object symbol,
      final IndexedEventSource source
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public <E extends TimeSeriesEvent<?>> Promise<List<E>> getTimeSeriesPromise(
      final Class<E> eventType,
      final Object symbol,
      final long fromTime,
      final long toTime
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  private static DXFeed toJavaFeed(final DxfgFeed feed) {
    return extractHandler(feed.getJavaObjectHandler());
  }

  private static DXFeedSubscription<?> toJavaSubscription(final DxfgSubscription sub) {
    return extractHandler(sub.getJavaObjectHandler());
  }
}
