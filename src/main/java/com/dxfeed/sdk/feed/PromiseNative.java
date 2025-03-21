// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feed;

import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.LastingEvent;
import com.dxfeed.event.TimeSeriesEvent;
import com.dxfeed.promise.Promise;
import com.dxfeed.promise.PromiseHandler;
import com.dxfeed.promise.Promises;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.exception.DxfgException;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.javac.DxfgExecutor;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolList;
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
        return NativeUtils.MAPPER_PROMISE.toNative(
                NativeUtils.MAPPER_FEED.toJava(feed).getLastEventPromise(
                        (Class<? extends LastingEvent<?>>) dxfgClazz.clazz,
                        NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol)
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
        return NativeUtils.MAPPER_PROMISES.toNativeList(
                NativeUtils.MAPPER_FEED.toJava(feed).getLastEventsPromises(
                        (Class<? extends LastingEvent<?>>) dxfgClazz.clazz,
                        NativeUtils.MAPPER_SYMBOLS.toJavaList(dxfgSymbols)
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
        return NativeUtils.MAPPER_PROMISE.toNative(
                NativeUtils.MAPPER_FEED.toJava(feed).getIndexedEventsPromise(
                        (Class<? extends IndexedEvent<?>>) dxfgClazz.clazz,
                        NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol),
                        NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toJava(source)
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
        return NativeUtils.MAPPER_PROMISE.toNative(
                NativeUtils.MAPPER_FEED.toJava(feed).getTimeSeriesPromise(
                        (Class<? extends TimeSeriesEvent<?>>) dxfgClazz.clazz,
                        NativeUtils.MAPPER_SYMBOL.toJava(dxfgSymbol),
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
        return NativeUtils.MAPPER_PROMISE.toNative(
                Promises.allOf(NativeUtils.MAPPER_PROMISES.toJavaList(dxfgPromiseList))
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
        return NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).isDone() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_Promise_hasResult",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_Promise_hasResult(
            final IsolateThread ignoredThread,
            final DxfgPromise dxfgPromise
    ) {
        return NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).hasResult() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_Promise_hasException",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_Promise_hasException(
            final IsolateThread ignoredThread,
            final DxfgPromise dxfgPromise
    ) {
        return NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).hasException() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_Promise_isCancelled",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_Promise_isCancelled(
            final IsolateThread ignoredThread,
            final DxfgPromise dxfgPromise
    ) {
        return NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).isCancelled() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_Promise_EventType_getResult",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgEventType dxfg_Promise_EventType_getResult(
            final IsolateThread ignoredThread,
            final DxfgPromise dxfgPromise
    ) {
        return NativeUtils.MAPPER_EVENT.toNative(
                (EventType<?>) NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).getResult()
        );
    }

    @CEntryPoint(
            name = "dxfg_Promise_List_EventType_getResult",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgEventTypeListPointer dxfg_Promise_List_EventType_getResult(
            final IsolateThread ignoredThread,
            final DxfgPromise dxfgPromise
    ) {
        return NativeUtils.MAPPER_EVENTS.toNativeList(
                (Collection<? extends EventType<?>>) NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).getResult()
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
        return NativeUtils.MAPPER_EXCEPTION.toNative(NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).getException());
    }

    @CEntryPoint(
            name = "dxfg_Promise_await",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_Promise_await(
            final IsolateThread ignoredThread,
            final DxfgPromise dxfgPromise
    ) {
        NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).await();
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
        NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).await(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
        NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise)
                .awaitWithoutException(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_Promise_cancel",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_Promise_cancel(
            final IsolateThread ignoredThread,
            final DxfgPromise dxfgPromise
    ) {
        NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).cancel();
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_Promise_List_EventType_complete",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_Promise_List_EventType_complete(
            final IsolateThread ignoredThread,
            final DxfgPromise dxfgPromise,
            final DxfgEventTypeListPointer events
    ) {
        ((Promise<Collection<EventType<?>>>) NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise))
                .complete(NativeUtils.MAPPER_EVENTS.toJavaList(events));
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
        ((Promise<EventType<?>>) NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise))
                .complete(NativeUtils.MAPPER_EVENT.toJava(dxfgEventType));
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
        NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise)
                .completeExceptionally(NativeUtils.MAPPER_EXCEPTION.toJava(dxfgException));
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
        NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).whenDone(
                new PromiseHandler<Object>() {
                    @Override
                    public void promiseDone(final Promise<?> promise) {
                        dxfgPromiseHandlerFunction.invoke(
                                CurrentIsolate.getCurrentThread(),
                                dxfgPromise,
                                userData
                        );
                    }
                }
        );
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
            final DxfgExecutor dxfgExecutor
    ) {
        NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).whenDoneAsync(
                new PromiseHandler<Object>() {
                    @Override
                    public void promiseDone(final Promise<?> promise) {
                        dxfgPromiseHandlerFunction.invoke(
                                CurrentIsolate.getCurrentThread(),
                                dxfgPromise,
                                userData
                        );
                    }
                },
                NativeUtils.MAPPER_EXECUTOR.toJava(dxfgExecutor)
        );
        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
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
        return NativeUtils.MAPPER_PROMISE.toNative(
                NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise).completed(
                        NativeUtils.MAPPER_JAVA_OBJECT_HANDLER.toJava((JavaObjectHandler<Object>) javaObjectHandler)
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
        return NativeUtils.MAPPER_PROMISE.toNative(
                NativeUtils.MAPPER_PROMISE.toJava(dxfgPromise)
                        .completed(NativeUtils.MAPPER_EXCEPTION.toJava(dxfgException))
        );
    }
}
