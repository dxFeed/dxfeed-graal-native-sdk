// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.feed;

import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventType;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.exception.DxfgException;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.JavaObjectHandler;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolList;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link PromiseNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(FeedDirectives.class)
public final class PromiseV4Native {

    private PromiseV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getLastEventPromise",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getLastEventPromise(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            @DxfgOut final WordPointer promise
    ) {
        if (promise.isNull()) {
            throw new IllegalArgumentException("The `promise` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPromise result = PromiseNative.dxfg_DXFeed_getLastEventPromise(thread, feed, dxfgClazz, dxfgSymbol);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        promise.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getLastEventsPromises",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getLastEventsPromises(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbolList dxfgSymbols,
            @DxfgOut final WordPointer promises
    ) {
        if (promises.isNull()) {
            throw new IllegalArgumentException("The `promises` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPromiseList result = PromiseNative.dxfg_DXFeed_getLastEventsPromises(thread, feed, dxfgClazz, dxfgSymbols);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        promises.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getIndexedEventsPromise",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getIndexedEventsPromise(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            final DxfgIndexedEventSourcePointer source,
            @DxfgOut final WordPointer promise
    ) {
        if (promise.isNull()) {
            throw new IllegalArgumentException("The `promise` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPromise result = PromiseNative.dxfg_DXFeed_getIndexedEventsPromise(
                        thread,
                        feed,
                        dxfgClazz,
                        dxfgSymbol,
                        source);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        promise.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXFeed_getTimeSeriesPromise",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXFeed_getTimeSeriesPromise(
            final IsolateThread thread,
            final DxfgFeed feed,
            final DxfgEventClazz dxfgClazz,
            final DxfgSymbol dxfgSymbol,
            final long fromTime,
            final long toTime,
            @DxfgOut final WordPointer promise
    ) {
        if (promise.isNull()) {
            throw new IllegalArgumentException("The `promise` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPromise result = PromiseNative.dxfg_DXFeed_getTimeSeriesPromise(
                        thread,
                        feed,
                        dxfgClazz,
                        dxfgSymbol,
                        fromTime,
                        toTime);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        promise.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_isDone",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_isDone(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            @DxfgOut final CInt32Pointer done
    ) {
        if (done.isNull()) {
            throw new IllegalArgumentException("The `done` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_isDone(thread, dxfgPromise);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        done.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_hasResult",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_hasResult(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            @DxfgOut final CInt32Pointer hasResult
    ) {
        if (hasResult.isNull()) {
            throw new IllegalArgumentException("The `hasResult` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_hasResult(thread, dxfgPromise);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        hasResult.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_hasException",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_hasException(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            @DxfgOut final CInt32Pointer hasException
    ) {
        if (hasException.isNull()) {
            throw new IllegalArgumentException("The `hasException` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_hasException(thread, dxfgPromise);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        hasException.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_isCancelled",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_isCancelled(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            @DxfgOut final CInt32Pointer cancelled
    ) {
        if (cancelled.isNull()) {
            throw new IllegalArgumentException("The `cancelled` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_isCancelled(thread, dxfgPromise);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        cancelled.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_EventType_getResult",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_EventType_getResult(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            @DxfgOut final WordPointer event
    ) {
        if (event.isNull()) {
            throw new IllegalArgumentException("The `event` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventType result = PromiseNative.dxfg_Promise_EventType_getResult(thread, dxfgPromise);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        event.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_List_EventType_getResult",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_List_EventType_getResult(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            @DxfgOut final WordPointer events
    ) {
        if (events.isNull()) {
            throw new IllegalArgumentException("The `events` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventTypeListPointer result = PromiseNative.dxfg_Promise_List_EventType_getResult(thread, dxfgPromise);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        events.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_getException",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_getException(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            @DxfgOut final WordPointer exception
    ) {
        if (exception.isNull()) {
            throw new IllegalArgumentException("The `exception` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgException result = PromiseNative.dxfg_Promise_getException(thread, dxfgPromise);

        if (OlderEntryPoint.failed(stored, result.rawValue() == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        exception.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_await",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_await(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_await(thread, dxfgPromise);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_awaitWithTimeout",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_awaitWithTimeout(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final int timeoutInMilliseconds
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_await(thread, dxfgPromise, timeoutInMilliseconds);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_awaitWithoutException",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_awaitWithoutException(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final int timeoutInMilliseconds
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_awaitWithoutException(
                        thread,
                        dxfgPromise,
                        timeoutInMilliseconds);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_cancel",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_cancel(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_cancel(thread, dxfgPromise);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_List_EventType_complete",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_List_EventType_complete(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final DxfgEventTypeListPointer events
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_List_EventType_complete(thread, dxfgPromise, events);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_EventType_complete",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_EventType_complete(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final DxfgEventType dxfgEventType
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_EventType_complete(thread, dxfgPromise, dxfgEventType);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_completeExceptionally",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_completeExceptionally(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final DxfgException dxfgException
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_completeExceptionally(thread, dxfgPromise, dxfgException);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_whenDone",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_whenDone(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final DxfgPromiseHandlerFunction dxfgPromiseHandlerFunction,
            final VoidPointer userData
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_whenDone(
                        thread,
                        dxfgPromise,
                        dxfgPromiseHandlerFunction,
                        userData);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_whenDoneAsync",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_whenDoneAsync(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final DxfgPromiseHandlerFunction dxfgPromiseHandlerFunction,
            final VoidPointer userData,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PromiseNative.dxfg_Promise_whenDoneAsync(
                        thread,
                        dxfgPromise,
                        dxfgPromiseHandlerFunction,
                        userData,
                        executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_completed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_completed(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final JavaObjectHandler<Object> javaObjectHandler,
            @DxfgOut final WordPointer newPromise
    ) {
        if (newPromise.isNull()) {
            throw new IllegalArgumentException("The `newPromise` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPromise result = PromiseNative.dxfg_Promise_completed(thread, dxfgPromise, javaObjectHandler);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        newPromise.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promise_failed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promise_failed(
            final IsolateThread thread,
            final DxfgPromise dxfgPromise,
            final DxfgException dxfgException,
            @DxfgOut final WordPointer newPromise
    ) {
        if (newPromise.isNull()) {
            throw new IllegalArgumentException("The `newPromise` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPromise result = PromiseNative.dxfg_Promise_failed(thread, dxfgPromise, dxfgException);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        newPromise.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_Promises_allOf",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_Promises_allOf(
            final IsolateThread thread,
            final DxfgPromiseList dxfgPromiseList,
            @DxfgOut final WordPointer promise
    ) {
        if (promise.isNull()) {
            throw new IllegalArgumentException("The `promise` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPromise result = PromiseNative.dxfg_Promises_allOf(thread, dxfgPromiseList);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        promise.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_promise_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_promise_list_release(
            final IsolateThread thread,
            final DxfgPromiseList promises
    ) {
        NativeUtils.MAPPER_PROMISES.release(promises);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
