package com.dxfeed.api.feed;

import static com.dxfeed.api.NativeUtils.createHandler;
import static com.dxfeed.api.NativeUtils.extractHandler;
import static com.dxfeed.api.NativeUtils.toJavaString;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.events.DxfgEventPointer;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.subscription.DxfgSubscription;
import com.dxfeed.api.subscription.SubscriptionNative;
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

  public static DXFeed getInstance() {
    throw new UnsupportedOperationException("It has not yet been implemented.");
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
    final DXFeed feed = extractHandler(dxfgFeed.getJavaObjectHandler());
    final Class<? extends EventType<?>>[] types = new Class[eventTypesSize];
    for (int i = 0; i < eventTypesSize; ++i) {
      types[i] = DxfgEventKind.fromCValue(eventTypes.read(i)).clazz;
    }
    dxfgSubscription.setJavaObjectHandler(
        createHandler(feed.createSubscription(types))
    );
    return EXECUTE_SUCCESSFULLY;
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
    getFeed(feed)
        .attachSubscription(getSubscription(subscription));
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
    getFeed(feed)
        .detachSubscription(getSubscription(subscription));
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
    getFeed(feed)
        .detachSubscriptionAndClear(getSubscription(subscription));
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
      final CCharPointer symbol,
      final DxfgEventPointer event
  ) {
    var result = getFeed(feed)
        .getLastEventIfSubscribed(
            (Class<LastingEvent<?>>) eventType.clazz,
            toJavaString(symbol)
        );
    if (result == null) {
      event.write(WordFactory.nullPointer());
    } else {
      event.write(
          SubscriptionNative.EVENT_MAPPER.nativeObject(Collections.singletonList(result)).read());
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
      final CCharPointer symbol,
      final CCharPointer source,
      final DxfgEventPointer events,
      final CIntPointer eventsSize
  ) {
    final var result = (List<? extends EventType<?>>) getFeed(feed)
        .getIndexedEventsIfSubscribed(
            (Class<IndexedEvent<?>>) eventType.clazz,
            toJavaString(symbol),
            OrderSource.valueOf(toJavaString(source))
        );
    if (result == null) {
      events.write(WordFactory.nullPointer());
      eventsSize.write(0);
    } else {
      events.write(
          SubscriptionNative.EVENT_MAPPER.nativeObject((List<EventType<?>>) result).read());
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
      final CCharPointer symbol,
      final long fromTime,
      final long toTime,
      final DxfgEventPointer events,
      final CIntPointer eventsSize
  ) {
    final var result = (List<? extends EventType<?>>) getFeed(feed)
        .getTimeSeriesIfSubscribed(
            (Class<TimeSeriesEvent<?>>) eventType.clazz,
            toJavaString(symbol),
            fromTime,
            toTime
        );
    if (result == null) {
      events.write(WordFactory.nullPointer());
      eventsSize.write(0);
    } else {
      events.write(
          SubscriptionNative.EVENT_MAPPER.nativeObject((List<EventType<?>>) result).read()
      );
      eventsSize.write(result.size());
    }
    return EXECUTE_SUCCESSFULLY;
  }

  public final <E extends TimeSeriesEvent<?>> DXFeedTimeSeriesSubscription<E> createTimeSeriesSubscription(
      final Class<? extends E> eventType
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  @SafeVarargs
  public final <E extends TimeSeriesEvent<?>> DXFeedTimeSeriesSubscription<E> createTimeSeriesSubscription(
      final Class<? extends E>... eventTypes
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
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

  private static DXFeed getFeed(DxfgFeed feed) {
    return extractHandler(feed.getJavaObjectHandler());
  }

  private static DXFeedSubscription<?> getSubscription(DxfgSubscription sub) {
    return extractHandler(sub.getJavaObjectHandler());
  }
}
