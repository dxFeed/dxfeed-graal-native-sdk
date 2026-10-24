// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.endpoint;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandlePointer;
import com.dxfeed.sdk.publisher.DxfgPublisher;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link EndpointNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(EndpointDirectives.class)
public final class EndpointV4Native {

    private EndpointV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getInstance",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getInstance(
            final IsolateThread thread,
            @DxfgOut final WordPointer endpoint
    ) {
        if (endpoint.isNull()) {
            throw new IllegalArgumentException("The `endpoint` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEndpoint result = EndpointNative.dxfg_DXEndpoint_getInstance(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        endpoint.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getInstanceWithRole",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getInstanceWithRole(
            final IsolateThread thread,
            final DxfgEndpointRole dxfgEndpointRole,
            @DxfgOut final WordPointer endpoint
    ) {
        if (endpoint.isNull()) {
            throw new IllegalArgumentException("The `endpoint` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEndpoint result = EndpointNative.dxfg_DXEndpoint_getInstance2(thread, dxfgEndpointRole);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        endpoint.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_create",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_create(
            final IsolateThread thread,
            @DxfgOut final WordPointer endpoint
    ) {
        if (endpoint.isNull()) {
            throw new IllegalArgumentException("The `endpoint` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEndpoint result = EndpointNative.dxfg_DXEndpoint_create(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        endpoint.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_createWithRole",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_createWithRole(
            final IsolateThread thread,
            final DxfgEndpointRole dxfgEndpointRole,
            @DxfgOut final WordPointer endpoint
    ) {
        if (endpoint.isNull()) {
            throw new IllegalArgumentException("The `endpoint` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEndpoint result = EndpointNative.dxfg_DXEndpoint_create2(thread, dxfgEndpointRole);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        endpoint.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_close(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_close(thread, dxfgEndpoint);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_closeAndAwaitTermination",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_closeAndAwaitTermination(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint
    ) throws InterruptedException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_closeAndAwaitTermination(thread, dxfgEndpoint);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getRole",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getRole(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final CInt32Pointer role
    ) {
        if (role.isNull()) {
            throw new IllegalArgumentException("The `role` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_getRole(thread, dxfgEndpoint);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        role.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_user",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_user(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            final CCharPointer user
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_user(thread, dxfgEndpoint, user);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_password",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_password(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            final CCharPointer password
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_password(thread, dxfgEndpoint, password);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_connect",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_connect(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            final CCharPointer address
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_connect(thread, dxfgEndpoint, address);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_reconnect",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_reconnect(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_reconnect(thread, dxfgEndpoint);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_disconnect",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_disconnect(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_disconnect(thread, dxfgEndpoint);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_disconnectAndClear",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_disconnectAndClear(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_disconnectAndClear(thread, dxfgEndpoint);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_awaitProcessed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_awaitProcessed(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint
    ) throws InterruptedException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_awaitProcessed(thread, dxfgEndpoint);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_awaitNotConnected",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_awaitNotConnected(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint
    ) throws InterruptedException {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_awaitNotConnected(thread, dxfgEndpoint);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getState",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getState(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final CInt32Pointer state
    ) {
        if (state.isNull()) {
            throw new IllegalArgumentException("The `state` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_getState(thread, dxfgEndpoint);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        state.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_addStateChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_addStateChangeListener(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            final DxfgEndpointStateChangeListener listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_addStateChangeListener(thread, dxfgEndpoint, listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_removeStateChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_removeStateChangeListener(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            final DxfgEndpointStateChangeListener listener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_removeStateChangeListener(thread, dxfgEndpoint, listener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getFeed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getFeed(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final WordPointer feed
    ) {
        if (feed.isNull()) {
            throw new IllegalArgumentException("The `feed` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgFeed result = EndpointNative.dxfg_DXEndpoint_getFeed(thread, dxfgEndpoint);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        feed.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getPublisher",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getPublisher(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final WordPointer publisher
    ) {
        if (publisher.isNull()) {
            throw new IllegalArgumentException("The `publisher` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPublisher result = EndpointNative.dxfg_DXEndpoint_getPublisher(thread, dxfgEndpoint);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        publisher.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getEventTypes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getEventTypes(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final WordPointer eventTypes
    ) {
        if (eventTypes.isNull()) {
            throw new IllegalArgumentException("The `eventTypes` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventClazzList result = EndpointNative.dxfg_DXEndpoint_getEventTypes(thread, dxfgEndpoint);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        eventTypes.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getRequestedAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getRequestedAggregationPeriod(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final DxfgTimePeriodHandlePointer period
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_getRequestedAggregationPeriod(thread, dxfgEndpoint, period);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getRequestedAggregationPeriodMillis",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getRequestedAggregationPeriodMillis(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final CLongPointer period
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_getRequestedAggregationPeriodMillis(
                        thread,
                        dxfgEndpoint,
                        period);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_setRequestedAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_setRequestedAggregationPeriod(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            final DxfgTimePeriodHandle period
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_setRequestedAggregationPeriod(thread, dxfgEndpoint, period);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_setRequestedAggregationPeriodMillis",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_setRequestedAggregationPeriodMillis(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            final long period
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_setRequestedAggregationPeriodMillis(
                        thread,
                        dxfgEndpoint,
                        period);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXEndpoint_getAggregationPeriodInfo",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXEndpoint_getAggregationPeriodInfo(
            final IsolateThread thread,
            final DxfgEndpoint dxfgEndpoint,
            @DxfgOut final CLongPointer min,
            @DxfgOut final CLongPointer max
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = EndpointNative.dxfg_DXEndpoint_getAggregationPeriodInfo(thread, dxfgEndpoint, min, max);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_PropertyChangeListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_PropertyChangeListener_new(
            final IsolateThread thread,
            final DxfgEndpointStateChangeListenerFunction userFunc,
            final VoidPointer userData,
            @DxfgOut final WordPointer listener
    ) {
        if (listener.isNull()) {
            throw new IllegalArgumentException("The `listener` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEndpointStateChangeListener result = EndpointNative.dxfg_PropertyChangeListener_new(thread, userFunc, userData);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        listener.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
