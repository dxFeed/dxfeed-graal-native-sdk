// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscriptioncontroller;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgExecutorHandlePointer;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandlePointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CLongPointer;

/**
 * The v4 twins of the functions of {@link SubscriptionControllerNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(SubscriptionControllerDirectives.class)
public final class SubscriptionControllerV4Native {

    private SubscriptionControllerV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_attach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_attach(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_attach(
                        thread,
                        subscriptionController,
                        feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_detach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_detach(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_detach(
                        thread,
                        subscriptionController,
                        feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_getEventsBatchLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_getEventsBatchLimit(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            @DxfgOut final CInt32Pointer eventsBatchLimit
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_getEventsBatchLimit(
                        thread,
                        subscriptionController,
                        eventsBatchLimit);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_setEventsBatchLimit",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_setEventsBatchLimit(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final int eventsBatchLimit
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_setEventsBatchLimit(
                        thread,
                        subscriptionController,
                        eventsBatchLimit);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_getAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_getAggregationPeriod(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            @DxfgOut final DxfgTimePeriodHandlePointer aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_getAggregationPeriod(
                        thread,
                        subscriptionController,
                        aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_getAggregationPeriodMillis",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_getAggregationPeriodMillis(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            @DxfgOut final CLongPointer aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_getAggregationPeriodMillis(
                        thread,
                        subscriptionController,
                        aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_setAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_setAggregationPeriod(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final DxfgTimePeriodHandle aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_setAggregationPeriod(
                        thread,
                        subscriptionController,
                        aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_setAggregationPeriodMillis",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_setAggregationPeriodMillis(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final long aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_setAggregationPeriodMillis(
                        thread,
                        subscriptionController,
                        aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_getExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_getExecutor(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            @DxfgOut final DxfgExecutorHandlePointer executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_getExecutor(
                        thread,
                        subscriptionController,
                        executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_SubscriptionController_setExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_SubscriptionController_setExecutor(
            final IsolateThread thread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final DxfgExecutorHandle executor
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = SubscriptionControllerNative.dxfg_SubscriptionController_setExecutor(
                        thread,
                        subscriptionController,
                        executor);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
