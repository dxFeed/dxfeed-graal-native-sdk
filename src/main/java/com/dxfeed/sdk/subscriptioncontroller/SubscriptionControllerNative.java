// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscriptioncontroller;

import com.devexperts.util.TimePeriod;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgExecutorHandlePointer;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandlePointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CLongPointer;

@CContext(Directives.class)
public class SubscriptionControllerNative {

    @CEntryPoint(name = "dxfg_SubscriptionController_attach", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_attach(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController, final DxfgFeed feed) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                .attach(NativeUtils.MAPPER_FEED.toJava(feed));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_detach", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_detach(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController, final DxfgFeed feed) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                .detach(NativeUtils.MAPPER_FEED.toJava(feed));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_getEventsBatchLimit", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_getEventsBatchLimit(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            @DxfgOut final CInt32Pointer eventsBatchLimit) {
        if (eventsBatchLimit.isNull()) {
            throw new IllegalArgumentException("The `eventsBatchLimit` pointer is null");
        }

        //noinspection DataFlowIssue
        eventsBatchLimit.write(NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                .getEventsBatchLimit());

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_setEventsBatchLimit", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_setEventsBatchLimit(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final int eventsBatchLimit) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                .setEventsBatchLimit(eventsBatchLimit);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_getAggregationPeriod", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_getAggregationPeriod(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            @DxfgOut final DxfgTimePeriodHandlePointer aggregationPeriod) {
        if (aggregationPeriod.isNull()) {
            throw new IllegalArgumentException("The `aggregationPeriod` pointer is null");
        }

        //noinspection DataFlowIssue
        aggregationPeriod.write(NativeUtils.MAPPER_TIME_PERIOD.toNative(
                NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                        .getAggregationPeriod()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_getAggregationPeriodMillis", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_getAggregationPeriodMillis(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            @DxfgOut final CLongPointer aggregationPeriod) {
        if (aggregationPeriod.isNull()) {
            throw new IllegalArgumentException("The `aggregationPeriod` pointer is null");
        }

        //noinspection DataFlowIssue
        aggregationPeriod.write(NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                .getAggregationPeriod().getTime());

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_setAggregationPeriod", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_setAggregationPeriod(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final DxfgTimePeriodHandle aggregationPeriod) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                .setAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_setAggregationPeriodMillis", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_setAggregationPeriodMillis(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final long aggregationPeriod) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                .setAggregationPeriod(TimePeriod.valueOf(aggregationPeriod));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_getExecutor", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_getExecutor(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            @DxfgOut final DxfgExecutorHandlePointer executor) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        //noinspection DataFlowIssue
        executor.write(NativeUtils.MAPPER_EXECUTOR.toNative(
                NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                        .getExecutor()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_SubscriptionController_setExecutor", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_SubscriptionController_setExecutor(final IsolateThread ignoredThread,
            final DxfgSubscriptionControllerHandle subscriptionController,
            final DxfgExecutorHandle executor) {
        //noinspection DataFlowIssue

        NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toJava(subscriptionController)
                .setExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(executor));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
