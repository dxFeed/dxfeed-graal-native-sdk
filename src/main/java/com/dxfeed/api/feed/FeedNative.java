package com.dxfeed.api.feed;

import static com.dxfeed.api.NativeUtils.MAPPER_EVENT;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENTS;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENT_TYPES;
import static com.dxfeed.api.NativeUtils.MAPPER_FEED;
import static com.dxfeed.api.NativeUtils.MAPPER_STRING;
import static com.dxfeed.api.NativeUtils.MAPPER_SUBSCRIPTION;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOL;
import static com.dxfeed.api.NativeUtils.MAPPER_TIME_SERIES_SUBSCRIPTION;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgEventClazzList;
import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.api.events.DxfgEventTypeList;
import com.dxfeed.api.events.DxfgSymbol;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.subscription.DxfgSubscription;
import com.dxfeed.api.subscription.DxfgTimeSeriesSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.LastingEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.event.market.OrderSource;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;

@CContext(Directives.class)
public class FeedNative {

  @CEntryPoint(
      name = "dxfg_DXFeed_getInstance",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgFeed dxfg_DXFeed_getInstance(
      final IsolateThread ignoredThread
  ) {
    return MAPPER_FEED.toNative(DXFeed.getInstance());
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_createSubscription",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfg_DXFeed_createSubscription(
      final IsolateThread ignoredThread,
      final DxfgFeed dxfgFeed,
      final DxfgEventClazz dxfgClazz
  ) {
    return MAPPER_SUBSCRIPTION.toNative(
        MAPPER_FEED.toJava(dxfgFeed).createSubscription(dxfgClazz.clazz)
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_createSubscription2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgSubscription<DXFeedSubscription<EventType<?>>> dxfg_DXFeed_createSubscription2(
      final IsolateThread ignoredThread,
      final DxfgFeed dxfgFeed,
      final DxfgEventClazzList eventClazzList
  ) {
    return MAPPER_SUBSCRIPTION.toNative(
        MAPPER_FEED.toJava(dxfgFeed).createSubscription(
            MAPPER_EVENT_TYPES.toJavaList(eventClazzList).toArray(new Class[0])
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_createTimeSeriesSubscription",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeSeriesSubscription dxfg_DXFeed_createTimeSeriesSubscription(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz
  ) {
    return MAPPER_TIME_SERIES_SUBSCRIPTION.toNative(
        MAPPER_FEED.toJava(feed)
            .createTimeSeriesSubscription((Class<TimeSeriesEvent<?>>) dxfgClazz.clazz)
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_createTimeSeriesSubscription2",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgTimeSeriesSubscription dxfg_DXFeed_createTimeSeriesSubscription2(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazzList eventClazzList
  ) {
    return MAPPER_TIME_SERIES_SUBSCRIPTION.toNative(
        MAPPER_FEED.toJava(feed).createTimeSeriesSubscription(
            MAPPER_EVENT_TYPES.toJavaList(eventClazzList).toArray(new Class[0])
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeedTimeSeriesSubscription_setFromTime",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeedTimeSeriesSubscription_setFromTime(
      final IsolateThread ignoredThread,
      final DxfgTimeSeriesSubscription dxfgTimeSeriesSubscription,
      final long fromTime
  ) {
    MAPPER_TIME_SERIES_SUBSCRIPTION.toJava(dxfgTimeSeriesSubscription).setFromTime(fromTime);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_attachSubscription",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeed_attachSubscription(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
  ) {
    MAPPER_FEED.toJava(feed).attachSubscription(MAPPER_SUBSCRIPTION.toJava(subscription));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_detachSubscription",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeed_detachSubscription(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
  ) {
    MAPPER_FEED.toJava(feed).detachSubscription(MAPPER_SUBSCRIPTION.toJava(subscription));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_detachSubscriptionAndClear",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeed_detachSubscriptionAndClear(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription
  ) {
    MAPPER_FEED.toJava(feed).detachSubscriptionAndClear(MAPPER_SUBSCRIPTION.toJava(subscription));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getLastEventIfSubscribed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventType dxfg_DXFeed_getLastEventIfSubscribed(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol
  ) {
    return MAPPER_EVENT.toNative(
        MAPPER_FEED.toJava(feed).getLastEventIfSubscribed(
            (Class<LastingEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJava(dxfgSymbol)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getIndexedEventsIfSubscribed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventTypeList dxfg_DXFeed_getIndexedEventsIfSubscribed(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol,
      final CCharPointer source
  ) {
    return MAPPER_EVENTS.toNativeList(
        MAPPER_FEED.toJava(feed).getIndexedEventsIfSubscribed(
            (Class<IndexedEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJava(dxfgSymbol),
            OrderSource.valueOf(MAPPER_STRING.toJava(source))
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getTimeSeriesIfSubscribed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventTypeList dxfg_DXFeed_getTimeSeriesIfSubscribed(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol,
      final long fromTime,
      final long toTime
  ) {
    return MAPPER_EVENTS.toNativeList(
        MAPPER_FEED.toJava(feed).getTimeSeriesIfSubscribed(
            (Class<TimeSeriesEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJava(dxfgSymbol),
            fromTime,
            toTime
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getLastEvent",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeed_getLastEvent(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventType nEvent
  ) {
    final LastingEvent<?> jEvent = (LastingEvent<?>) MAPPER_EVENT.toJava(nEvent);
    MAPPER_FEED.toJava(feed).getLastEvent(jEvent);
    MAPPER_EVENT.fillNative(jEvent, nEvent);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getLastEvents",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_DXFeed_getLastEvents(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventTypeList nEvents
  ) {
    for (int i = 0; i < nEvents.getSize(); i++) {
      final DxfgEventType nEvent = nEvents.getElements().addressOf(i).read();
      final LastingEvent<?> jEvent = (LastingEvent<?>) MAPPER_EVENT.toJava(nEvent);
      MAPPER_FEED.toJava(feed).getLastEvent(jEvent);
      MAPPER_EVENT.fillNative(jEvent, nEvent);
    }
    return EXECUTE_SUCCESSFULLY;
  }
}
