package com.dxfeed.api.feed;

import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.BaseNative;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.DXFeedTimeSeriesSubscription;
import com.dxfeed.api.events.DxfgEventKind;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.subscription.DxfgSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.LastingEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.promise.Promise;
import java.util.Collection;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CIntPointer;

@CContext(Directives.class)
public class FeedNative extends BaseNative {

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
    final DXFeed feed = getJavaObject(dxfgFeed.getJavaObjectHandler());
    final Class<? extends EventType<?>>[] types = new Class[eventTypesSize];
    for (int i = 0; i < eventTypesSize; ++i) {
      types[i] = DxfgEventKind.toEventType(eventTypes.read(i));
    }
    dxfgSubscription.setJavaObjectHandler(
        createJavaObjectHandler(feed.createSubscription(types))
    );
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

  public void attachSubscription(
      final DXFeedSubscription<?> subscription
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public void detachSubscription(
      final DXFeedSubscription<?> subscription
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }

  public void detachSubscriptionAndClear(
      final DXFeedSubscription<?> subscription
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

  public <E extends LastingEvent<?>> E getLastEventIfSubscribed(
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

  public <E extends IndexedEvent<?>> List<E> getIndexedEventsIfSubscribed(
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

  public <E extends TimeSeriesEvent<?>> List<E> getTimeSeriesIfSubscribed(
      final Class<E> eventType,
      final Object symbol,
      final long fromTime,
      final long toTime
  ) {
    throw new UnsupportedOperationException("It has not yet been implemented.");
  }
}
