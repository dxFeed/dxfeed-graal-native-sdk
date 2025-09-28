// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.indexedeventtxmodel;

import com.devexperts.util.TimePeriod;
import com.dxfeed.api.osub.IndexedEventSubscriptionSymbol;
import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.model.IndexedEventTxModel;
import com.dxfeed.model.IndexedEventTxModel.Builder;
import com.dxfeed.model.IndexedEventTxModel.Listener;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.events.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.source.DxfgIndexedEventSource;
import com.dxfeed.sdk.subscriptioncontroller.DxfgSubscriptionControllerHandlePointer;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import com.dxfeed.sdk.symbol.DxfgSymbolPointer;
import java.util.Collection;
import java.util.List;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CConst;
import org.graalvm.nativeimage.c.type.VoidPointer;

@CContext(Directives.class)
public class IndexedEventTxModelNative {

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_newBuilder", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_newBuilder(final IsolateThread ignoredThread, DxfgEventClazz eventType,
            @DxfgOut final DxfgIndexedEventTxModelBuilderHandlePointer builder) {
        if (builder.isNull()) {
            throw new IllegalArgumentException("The `builder` pointer is null");
        }

        //noinspection rawtypes,unchecked
        builder.write(NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toNative(
                IndexedEventTxModel.newBuilder((Class<IndexedEvent>) (Object) eventType.clazz)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_getIndexedEventSubscriptionSymbol", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_getIndexedEventSubscriptionSymbol(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelHandle model, @DxfgOut final DxfgSymbolPointer symbol) {
        if (symbol.isNull()) {
            throw new IllegalArgumentException("The `symbol` pointer is null");
        }

        //noinspection DataFlowIssue
        symbol.write(NativeUtils.MAPPER_SYMBOL.toNative(
                NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL.toJava(model).getIndexedEventSubscriptionSymbol()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_getSymbol", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_getSymbol(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelHandle model, @DxfgOut final CCharPointerPointer symbol) {
        if (symbol.isNull()) {
            throw new IllegalArgumentException("The `symbol` pointer is null");
        }

        //noinspection DataFlowIssue
        symbol.write(NativeUtils.MAPPER_STRING.toNative(
                NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL.toJava(model).getSymbol()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_getSource", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_getSource(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelHandle model, @DxfgOut final DxfgIndexedEventSourcePointer source) {
        if (source.isNull()) {
            throw new IllegalArgumentException("The `source` pointer is null");
        }

        //noinspection DataFlowIssue
        source.write(NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toNative(
                NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL.toJava(model).getSource()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_getSubscriptionController", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_getSubscriptionController(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelHandle model,
            @DxfgOut final DxfgSubscriptionControllerHandlePointer subscriptionController) {
        if (subscriptionController.isNull()) {
            throw new IllegalArgumentException("The `subscriptionController` pointer is null");
        }

        //noinspection DataFlowIssue
        subscriptionController.write(NativeUtils.MAPPER_SUBSCRIPTION_CONTROLLER.toNative(
                NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL.toJava(model).getSubscriptionController()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_isClosed", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_isClosed(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelHandle model, @DxfgOut final CInt32Pointer isClosed) {
        if (isClosed.isNull()) {
            throw new IllegalArgumentException("The `isClosed` pointer is null");
        }

        //noinspection DataFlowIssue
        isClosed.write(NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL.toJava(model).isClosed() ? 1 : 0);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_close", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_close(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelHandle model) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL.toJava(model).close();

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_new", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_new(final IsolateThread ignoredThread, DxfgEventClazz eventType,
            @DxfgOut final DxfgIndexedEventTxModelBuilderHandlePointer builder) {
        if (builder.isNull()) {
            throw new IllegalArgumentException("The `builder` pointer is null");
        }

        //noinspection rawtypes,unchecked
        builder.write(NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toNative(
                new Builder((Class<IndexedEvent>) (Object) eventType.clazz)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withFeed", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withFeed(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, DxfgFeed feed) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withFeed(NativeUtils.MAPPER_FEED.toJava(feed));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withSymbol", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withSymbol(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, DxfgSymbol symbol) {
        //noinspection DataFlowIssue,unchecked
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withSymbol((IndexedEventSubscriptionSymbol<?>) NativeUtils.MAPPER_SYMBOL.toJava(symbol));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withSymbol2", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withSymbol(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, @CConst final CCharPointer symbol) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withSymbol(NativeUtils.MAPPER_STRING.toJava(symbol));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withSource", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withSource(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, DxfgIndexedEventSource source) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withSource(NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toJava(source));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Listener_new", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Listener_new(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelListenerEventsReceivedFunctionPointer eventsReceived, VoidPointer userData,
            @DxfgOut final DxfgIndexedEventTxModelListenerHandlePointer listener) {
        if (listener.isNull()) {
            throw new IllegalArgumentException("The `listener` pointer is null");
        }

        listener.write(NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_LISTENER.toNative(new Listener() {
            @Override
            public void eventsReceived(final List events, final boolean isSnapshot) {
                @SuppressWarnings("unchecked") DxfgEventTypeListPointer eventsNative = NativeUtils.MAPPER_EVENTS.toNativeList(
                        (Collection<? extends EventType<?>>) events);
                int isSnapshotNative = isSnapshot ? 1 : 0;
                eventsReceived.invoke(CurrentIsolate.getCurrentThread(), eventsNative, isSnapshotNative, userData);
                NativeUtils.MAPPER_EVENTS.release(eventsNative);
            }
        }));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withListener", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withListener(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, DxfgIndexedEventTxModelListenerHandle listener) {
        //noinspection DataFlowIssue,unchecked
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withListener(NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_LISTENER.toJava(listener));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withEventsBatchLimit", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withEventsBatchLimit(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, int eventsBatchLimit) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withEventsBatchLimit(eventsBatchLimit);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withAggregationPeriod", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withAggregationPeriod(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, DxfgTimePeriodHandle aggregationPeriod) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withAggregationPeriodMillis", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withAggregationPeriod(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, long aggregationPeriod) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withAggregationPeriod(TimePeriod.valueOf(aggregationPeriod));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_withExecutor", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_withExecutor(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, DxfgExecutorHandle executor) {
        //noinspection DataFlowIssue
        NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                .withExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(executor));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedEventTxModel_Builder_build", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedEventTxModel_Builder_build(final IsolateThread ignoredThread,
            DxfgIndexedEventTxModelBuilderHandle builder, @DxfgOut final DxfgIndexedEventTxModelHandlePointer model) {
        if (model.isNull()) {
            throw new IllegalArgumentException("The `model` pointer is null");
        }

        //noinspection DataFlowIssue
        model.write(NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL.toNative(
                NativeUtils.MAPPER_INDEXED_EVENT_TX_MODEL_BUILDER.toJava(builder)
                        .build()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

}
