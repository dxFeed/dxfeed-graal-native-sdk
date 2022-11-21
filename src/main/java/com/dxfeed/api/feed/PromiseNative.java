package com.dxfeed.api.feed;

import static com.dxfeed.api.NativeUtils.MAPPER_EVENT;
import static com.dxfeed.api.NativeUtils.MAPPER_EVENTS;
import static com.dxfeed.api.NativeUtils.MAPPER_EXCEPTION;
import static com.dxfeed.api.NativeUtils.MAPPER_PROMISES;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOL;
import static com.dxfeed.api.NativeUtils.MAPPER_SYMBOLS;
import static com.dxfeed.api.NativeUtils.newJavaObjectHandler;
import static com.dxfeed.api.NativeUtils.toJava;
import static com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.events.DxfgEventClazz;
import com.dxfeed.api.events.DxfgEventType;
import com.dxfeed.api.events.DxfgEventTypeList;
import com.dxfeed.api.events.DxfgIndexedEventSource;
import com.dxfeed.api.events.DxfgSymbol;
import com.dxfeed.api.events.DxfgSymbolList;
import com.dxfeed.api.exception.DxfgException;
import com.dxfeed.api.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.api.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.api.javac.DxfgExecuter;
import com.dxfeed.api.javac.JavaObjectHandler;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.LastingEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.promise.PromiseHandler;
import com.dxfeed.promise.Promises;
import java.util.Collection;
import java.util.List;
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
  public static DxfgPromise<? extends LastingEvent<?>> dxfg_DXFeed_getLastEventPromise(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol
  ) {
    return newJavaObjectHandler(
        toJava(feed).getLastEventPromise(
            (Class<? extends LastingEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJavaObject(dxfgSymbol)
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
    final var eventsPromises = toJava(feed).getLastEventsPromises(
        (Class<? extends LastingEvent<?>>) dxfgClazz.clazz,
        MAPPER_SYMBOLS.toJavaList(dxfgSymbols)
    );
    return MAPPER_PROMISES.toNativeList(eventsPromises);
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getIndexedEventsPromise",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise<List<? extends IndexedEvent<?>>> dxfg_DXFeed_getIndexedEventsPromise(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol,
      final DxfgIndexedEventSource source
  ) {
    return newJavaObjectHandler(
        toJava(feed).getIndexedEventsPromise(
            (Class<? extends IndexedEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJavaObject(dxfgSymbol),
            toJava(source)
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_DXFeed_getTimeSeriesPromise",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise<List<? extends TimeSeriesEvent<?>>> dxfg_DXFeed_getTimeSeriesPromise(
      final IsolateThread ignoredThread,
      final DxfgFeed feed,
      final DxfgEventClazz dxfgClazz,
      final DxfgSymbol dxfgSymbol,
      final long fromTime,
      final long toTime
  ) {
    return newJavaObjectHandler(
        toJava(feed).getTimeSeriesPromise(
            (Class<? extends TimeSeriesEvent<?>>) dxfgClazz.clazz,
            MAPPER_SYMBOL.toJavaObject(dxfgSymbol),
            fromTime,
            toTime
        )
    );
  }

  @CEntryPoint(
      name = "dxfg_Promises_allOf",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise<Void> dxfg_Promises_allOf(
      final IsolateThread ignoredThread,
      final DxfgPromiseList dxfgPromiseList
  ) {
    return newJavaObjectHandler(Promises.allOf(MAPPER_PROMISES.toJavaList(dxfgPromiseList)));
  }

  @CEntryPoint(
      name = "dxfg_Promise_isDone",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_isDone(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise
  ) {
    return toJava(dxfgPromise).isDone() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Promise_hasResult",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_hasResult(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise
  ) {
    return toJava(dxfgPromise).hasResult() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Promise_hasException",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_hasException(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise
  ) {
    return toJava(dxfgPromise).hasException() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Promise_isCancelled",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_isCancelled(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise
  ) {
    return toJava(dxfgPromise).isCancelled() ? 1 : 0;
  }

  @CEntryPoint(
      name = "dxfg_Promise_EventType_getResult",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventType dxfg_Promise_EventType_getResult(
      final IsolateThread ignoredThread,
      final DxfgPromise<? extends EventType<?>> dxfgPromise
  ) {
    return MAPPER_EVENT.toNativeObject(toJava(dxfgPromise).getResult());
  }

  @CEntryPoint(
      name = "dxfg_Promise_List_EventType_getResult",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgEventTypeList dxfg_Promise_List_EventType_getResult(
      final IsolateThread ignoredThread,
      final DxfgPromise<List<EventType<?>>> dxfgPromise
  ) {
    return MAPPER_EVENTS.toNativeList(toJava(dxfgPromise).getResult());
  }

  @CEntryPoint(
      name = "dxfg_Promise_getException",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static DxfgException dxfg_Promise_getException(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise
  ) {
    return MAPPER_EXCEPTION.toNativeObject(toJava(dxfgPromise).getException());
  }

  @CEntryPoint(
      name = "dxfg_Promise_await",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_await(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise
  ) {
    toJava(dxfgPromise).await();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_await2",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_await(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise,
      final int timeoutInMilliseconds
  ) {
    toJava(dxfgPromise).await(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_awaitWithoutException",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_awaitWithoutException(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise,
      final int timeoutInMilliseconds
  ) {
    toJava(dxfgPromise).awaitWithoutException(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_cancel",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_cancel(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise
  ) {
    toJava(dxfgPromise).cancel();
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_List_EventType_complete",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_List_EventType_complete(
      final IsolateThread ignoredThread,
      final DxfgPromise<Collection<EventType<?>>> dxfgPromise,
      final DxfgEventTypeList dxfgEventTypeList
  ) {
    toJava(dxfgPromise).complete(MAPPER_EVENTS.toJavaList(dxfgEventTypeList));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_EventType_complete",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_EventType_complete(
      final IsolateThread ignoredThread,
      final DxfgPromise<EventType<?>> dxfgPromise,
      final DxfgEventType dxfgEventType
  ) {
    toJava(dxfgPromise).complete(MAPPER_EVENT.toJavaObject(dxfgEventType));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_completeExceptionally",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_completeExceptionally(
      final IsolateThread ignoredThread,
      final DxfgPromise<EventType<?>> dxfgPromise,
      final DxfgException dxfgException
  ) {
    toJava(dxfgPromise).completeExceptionally(MAPPER_EXCEPTION.toJavaObject(dxfgException));
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_whenDone",
      exceptionHandler = ExceptionHandlerReturnMinusOne.class
  )
  public static int dxfg_Promise_whenDone(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise,
      final DxfgPromiseHandlerFunction dxfgPromiseHandlerFunction,
      final VoidPointer userData
  ) {
    toJava(dxfgPromise).whenDone(
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
      final DxfgPromise<?> dxfgPromise,
      final DxfgPromiseHandlerFunction dxfgPromiseHandlerFunction,
      final VoidPointer userData,
      final DxfgExecuter dxfgExecuter
  ) {
    toJava(dxfgPromise).whenDoneAsync(
        (PromiseHandler<Object>) promise -> dxfgPromiseHandlerFunction.invoke(
            CurrentIsolate.getCurrentThread(),
            dxfgPromise,
            userData
        ),
        toJava(dxfgExecuter)
    );
    return EXECUTE_SUCCESSFULLY;
  }

  @CEntryPoint(
      name = "dxfg_Promise_completed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise<?> dxfg_Promise_completed(
      final IsolateThread ignoredThread,
      final DxfgPromise<?> dxfgPromise,
      final JavaObjectHandler<?> javaObjectHandler
  ) {
    return newJavaObjectHandler(
        toJava(dxfgPromise).completed(toJava(javaObjectHandler))
    );
  }

  @CEntryPoint(
      name = "dxfg_Promise_failed",
      exceptionHandler = ExceptionHandlerReturnNullWord.class
  )
  public static DxfgPromise<?> dxfg_Promise_failed(
      final IsolateThread ignoredThread,
      final DxfgPromise<EventType<?>> dxfgPromise,
      final DxfgException dxfgException
  ) {
    return newJavaObjectHandler(
        toJava(dxfgPromise).completed(MAPPER_EXCEPTION.toJavaObject(dxfgException))
    );
  }
}
