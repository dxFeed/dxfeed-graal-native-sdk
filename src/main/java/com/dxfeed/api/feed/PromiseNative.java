package com.dxfeed.api.feed;

import static com.dxfeed.api.NativeUtils.MAPPER_EVENT;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENTS;
import static com.dxfeed.api.NativeUtils.MAPPER_EXCEPTION;
import static com.dxfeed.api.NativeUtils.MAPPER_EXECUTOR;
import static com.dxfeed.api.NativeUtils.MAPPER_FEED;
import static com.dxfeed.api.NativeUtils.MAPPER_INDEXED_EVENT_SOURCE;
import static com.dxfeed.api.NativeUtils.MAPPER_JAVA_OBJECT_HANDLER;
import static com.dxfeed.api.NativeUtils.MAPPER_PROMISE;
import static com.dxfeed.api.NativeUtils.MAPPER_PROMISES;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOL;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOLS;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.api.events.DxfgEventTypeList;
import com.dxfeed.api.exception.DxfgException;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.api.source.DxfgIndexedEventSource;
import com.dxfeed.api.symbol.DxfgSymbol;
import com.dxfeed.api.symbol.DxfgSymbolList;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.LastingEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.promise.Promise;
import com.dxfeed.promise.PromiseHandler;
import com.dxfeed.promise.Promises;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class PromiseNative {

  @CEntryPoint(
      name = "dxfg_DXFeed_getLastEventPromise",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise dxfg_DXFeed_getLastEventPromise(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol
  ) {
    return MAPPER_PROMISE.toNative(
        MAPPER_FEED.toJava(feed).getLastEventPromise(
            (Class<? extends LastingEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJava(dxfgSymbol)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getLastEventsPromises",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromiseList dxfg_DXFeed_getLastEventsPromises(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbolList dxfgSymbols
  ) {
    return MAPPER_PROMISES.toNativeList(
        MAPPER_FEED.toJava(feed).getLastEventsPromises(
            (Class<? extends LastingEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOLS.toJavaList(dxfgSymbols)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getIndexedEventsPromise",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise dxfg_DXFeed_getIndexedEventsPromise(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol,
      final DxfgIndexedEventSource source
  ) {
    return MAPPER_PROMISE.toNative(
        MAPPER_FEED.toJava(feed).getIndexedEventsPromise(
            (Class<? extends IndexedEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJava(dxfgSymbol),
            MAPPER_INDEXED_EVENT_SOURCE.toJava(source)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getTimeSeriesPromise",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise dxfg_DXFeed_getTimeSeriesPromise(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol,
      final long fromTime,
      final long toTime
  ) {
    return MAPPER_PROMISE.toNative(
        MAPPER_FEED.toJava(feed).getTimeSeriesPromise(
            (Class<? extends TimeSeriesEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJava(dxfgSymbol),
            fromTime,
            toTime
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_Promises_allOf",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise dxfg_Promises_allOf(
      final IsolateThread ignoredThread,
      final DxfgPromiseList dxfgPromiseList
  ) {
    return MAPPER_PROMISE.toNative(
        Promises.allOf(MAPPER_PROMISES.toJavaList(dxfgPromiseList))
    );
  }

  @CEntryPoint(
      name = "dxfg_Promise_isDone",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_isDone(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    return MAPPER_PROMISE.toJava(dxfgPromise).isDone() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Promise_hasResult",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_hasResult(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    return MAPPER_PROMISE.toJava(dxfgPromise).hasResult() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Promise_hasException",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_hasException(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    return MAPPER_PROMISE.toJava(dxfgPromise).hasException() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Promise_isCancelled",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_isCancelled(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    return MAPPER_PROMISE.toJava(dxfgPromise).isCancelled() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Promise_EventType_getResult",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventType dxfg_Promise_EventType_getResult(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    return MAPPER_EVENT.toNative(
        (EventType<?>) MAPPER_PROMISE.toJava(dxfgPromise).getResult()
    );
  }

  @CEntryPoint(
      name = "dxfg_Promise_List_EventType_getResult",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventTypeList dxfg_Promise_List_EventType_getResult(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    return MAPPER_EVENTS.toNativeList(
        (Collection<? extends EventType<?>>) MAPPER_PROMISE.toJava(dxfgPromise).getResult()
    );
  }

  @CEntryPoint(
      name = "dxfg_Promise_getException",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static DxfgException dxfg_Promise_getException(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    return MAPPER_EXCEPTION.toNative(MAPPER_PROMISE.toJava(dxfgPromise).getException());
  }

  @CEntryPoint(
      name = "dxfg_Promise_await",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_await(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    MAPPER_PROMISE.toJava(dxfgPromise).await();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_await2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_await(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final int timeoutInMilliseconds
  ) {
    MAPPER_PROMISE.toJava(dxfgPromise).await(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_awaitWithoutException",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_awaitWithoutException(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final int timeoutInMilliseconds
  ) {
    MAPPER_PROMISE.toJava(dxfgPromise)
        .awaitWithoutException(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_cancel",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_cancel(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise
  ) {
    MAPPER_PROMISE.toJava(dxfgPromise).cancel();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_List_EventType_complete",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_List_EventType_complete(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final DxfgEventTypeList dxfgEventTypeList
  ) {
    ((Promise<Collection<EventType<?>>>) MAPPER_PROMISE.toJava(dxfgPromise))
        .complete(MAPPER_EVENTS.toJavaList(dxfgEventTypeList));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_EventType_complete",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_EventType_complete(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final DxfgEventType dxfgEventType
  ) {
    ((Promise<EventType<?>>) MAPPER_PROMISE.toJava(dxfgPromise))
        .complete(MAPPER_EVENT.toJava(dxfgEventType));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_completeExceptionally",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_completeExceptionally(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final DxfgException dxfgException
  ) {
    MAPPER_PROMISE.toJava(dxfgPromise)
        .completeExceptionally(MAPPER_EXCEPTION.toJava(dxfgException));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_whenDone",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_whenDone(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final DxfgPromiseHandlerFunction dxfgPromiseHandlerFunction,
      final VoidPointer userData
  ) {
    MAPPER_PROMISE.toJava(dxfgPromise).whenDone(
        (PromiseHandler<Object>) promise -> dxfgPromiseHandlerFunction.invoke(
            CurrentIsolate.getCurrentThread(),
            dxfgPromise,
            userData
        )
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_whenDoneAsync",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_whenDoneAsync(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final DxfgPromiseHandlerFunction dxfgPromiseHandlerFunction,
      final VoidPointer userData,
      final DxfgExecuter dxfgExecuter
  ) {
    MAPPER_PROMISE.toJava(dxfgPromise).whenDoneAsync(
        (PromiseHandler<Object>) promise -> dxfgPromiseHandlerFunction.invoke(
            CurrentIsolate.getCurrentThread(),
            dxfgPromise,
            userData
        ),
        MAPPER_EXECUTOR.toJava(dxfgExecuter)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_completed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise dxfg_Promise_completed(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final JavaObjectHandler<?> javaObjectHandler
  ) {
    return MAPPER_PROMISE.toNative(
        MAPPER_PROMISE.toJava(dxfgPromise).completed(
            MAPPER_JAVA_OBJECT_HANDLER.toJava((JavaObjectHandler<Object>) javaObjectHandler)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_Promise_failed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise dxfg_Promise_failed(
      final IsolateThread ignoredThread,
      final DxfgPromise dxfgPromise,
      final DxfgException dxfgException
  ) {
    return MAPPER_PROMISE.toNative(
        MAPPER_PROMISE.toJava(dxfgPromise).completed(MAPPER_EXCEPTION.toJava(dxfgException))
    );
  }
}
