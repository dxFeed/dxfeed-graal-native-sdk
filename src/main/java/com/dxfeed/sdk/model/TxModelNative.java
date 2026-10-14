// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.javac.NativeListener;
import com.dxfeed.sdk.model.tx.IndexedTxModel;
import com.dxfeed.sdk.model.tx.TimeSeriesTxModel;
import com.dxfeed.sdk.model.tx.TxModelListener;
import com.dxfeed.sdk.source.DxfgIndexedEventSourcePointer;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import java.util.HashSet;
import java.util.List;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.VoidPointer;

/**
 * The C API of {@link IndexedTxModel}, {@link TimeSeriesTxModel} and {@link TxModelListener}
 * ({@code dxfg_event_model.h}). Every builder function returns a new handle of the same builder.
 */
@CContext(ModelDirectives.class)
public class TxModelNative {

    /**
     * The listener for a model of the events of type {@code E}. The listeners of dxfg_TxModelListener_new take the
     * events of any type (they pass them to the C code), so they fit a model of any events.
     */
    @SuppressWarnings("unchecked")
    private static <E extends IndexedEvent<?>> TxModelListener<E> forModel(
            final TxModelListener<IndexedEvent<?>> listener
    ) {
        return (TxModelListener<E>) (TxModelListener<?>) listener;
    }

    // IndexedTxModel

    @CEntryPoint(name = "dxfg_IndexedTxModel_newBuilder", exceptionHandler = ExceptionHandlerReturnNullWord.class)
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_newBuilder(
            final IsolateThread ignoredThread,
            final DxfgEventClazz eventType
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(IndexedTxModel.newBuilder(eventType.eventClass()));
    }

    @CEntryPoint(name = "dxfg_IndexedTxModel_getSources", exceptionHandler = ExceptionHandlerReturnNullWord.class)
    public static DxfgIndexedEventSourceList dxfg_IndexedTxModel_getSources(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelHandle model
    ) {
        return NativeUtils.MAPPER_INDEXED_EVENT_SOURCES.toNativeList(
                NativeUtils.MAPPER_INDEXED_TX_MODEL.toJava(model).getSources());
    }

    @CEntryPoint(name = "dxfg_IndexedTxModel_setSources", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedTxModel_setSources(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelHandle model,
            final DxfgIndexedEventSourceList sources
    ) {
        NativeUtils.MAPPER_INDEXED_TX_MODEL.toJava(model)
                .setSources(new HashSet<>(NativeUtils.MAPPER_INDEXED_EVENT_SOURCES.toJavaList(sources)));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_isBatchProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_IndexedTxModel_isBatchProcessing(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelHandle model
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL.toJava(model).isBatchProcessing() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_isSnapshotProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_IndexedTxModel_isSnapshotProcessing(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelHandle model
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL.toJava(model).isSnapshotProcessing() ? 1 : 0;
    }

    @CEntryPoint(name = "dxfg_IndexedTxModel_attach", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedTxModel_attach(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelHandle model,
            final DxfgFeed feed
    ) {
        NativeUtils.MAPPER_INDEXED_TX_MODEL.toJava(model).attach(NativeUtils.MAPPER_FEED.toJava(feed));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedTxModel_detach", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedTxModel_detach(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelHandle model,
            final DxfgFeed feed
    ) {
        NativeUtils.MAPPER_INDEXED_TX_MODEL.toJava(model).detach(NativeUtils.MAPPER_FEED.toJava(feed));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_setAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_IndexedTxModel_setAggregationPeriod(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelHandle model,
            final DxfgTimePeriodHandle aggregationPeriod
    ) {
        NativeUtils.MAPPER_INDEXED_TX_MODEL.toJava(model)
                .setAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_IndexedTxModel_close", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_IndexedTxModel_close(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelHandle model
    ) {
        NativeUtils.MAPPER_INDEXED_TX_MODEL.toJava(model).close();

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    // IndexedTxModel.Builder

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_Builder_withSources",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_Builder_withSources(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgIndexedEventSourceList sources
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder)
                        .withSources(NativeUtils.MAPPER_INDEXED_EVENT_SOURCES.toJavaList(sources)));
    }

    @CEntryPoint(name = "dxfg_IndexedTxModel_Builder_build", exceptionHandler = ExceptionHandlerReturnNullWord.class)
    public static DxfgIndexedTxModelHandle dxfg_IndexedTxModel_Builder_build(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder).build());
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_Builder_withBatchProcessing",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_Builder_withBatchProcessing(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final boolean isBatchProcessing
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder).withBatchProcessing(isBatchProcessing));
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_Builder_withSnapshotProcessing",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_Builder_withSnapshotProcessing(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final boolean isSnapshotProcessing
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder)
                        .withSnapshotProcessing(isSnapshotProcessing));
    }

    @CEntryPoint(name = "dxfg_IndexedTxModel_Builder_withFeed", exceptionHandler = ExceptionHandlerReturnNullWord.class)
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_Builder_withFeed(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgFeed feed
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder)
                        .withFeed(NativeUtils.MAPPER_FEED.toJava(feed)));
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_Builder_withAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_Builder_withAggregationPeriod(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgTimePeriodHandle aggregationPeriod
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder)
                        .withAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod)));
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_Builder_withSymbol",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_Builder_withSymbol(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgSymbol symbol
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder)
                        .withSymbol(NativeUtils.MAPPER_SYMBOL.toJava(symbol)));
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_Builder_withListener",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_Builder_withListener(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgTxModelListenerHandle listener
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder)
                        .withListener(forModel(NativeUtils.MAPPER_TX_MODEL_LISTENER.toJava(listener))));
    }

    @CEntryPoint(
            name = "dxfg_IndexedTxModel_Builder_withExecutor",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgIndexedTxModelBuilderHandle dxfg_IndexedTxModel_Builder_withExecutor(
            final IsolateThread ignoredThread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgExecutorHandle executor
    ) {
        return NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_INDEXED_TX_MODEL_BUILDER.toJava(builder)
                        .withExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(executor)));
    }

    // TimeSeriesTxModel

    @CEntryPoint(name = "dxfg_TimeSeriesTxModel_newBuilder", exceptionHandler = ExceptionHandlerReturnNullWord.class)
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_newBuilder(
            final IsolateThread ignoredThread,
            final DxfgEventClazz eventType
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                TimeSeriesTxModel.newBuilder(eventType.eventClass()));
    }

    @CEntryPoint(name = "dxfg_TimeSeriesTxModel_getFromTime", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static long dxfg_TimeSeriesTxModel_getFromTime(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelHandle model
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toJava(model).getFromTime();
    }

    @CEntryPoint(name = "dxfg_TimeSeriesTxModel_setFromTime", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_TimeSeriesTxModel_setFromTime(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelHandle model,
            final long fromTime
    ) {
        NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toJava(model).setFromTime(fromTime);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_isBatchProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_TimeSeriesTxModel_isBatchProcessing(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelHandle model
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toJava(model).isBatchProcessing() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_isSnapshotProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_TimeSeriesTxModel_isSnapshotProcessing(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelHandle model
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toJava(model).isSnapshotProcessing() ? 1 : 0;
    }

    @CEntryPoint(name = "dxfg_TimeSeriesTxModel_attach", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_TimeSeriesTxModel_attach(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelHandle model,
            final DxfgFeed feed
    ) {
        NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toJava(model).attach(NativeUtils.MAPPER_FEED.toJava(feed));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_TimeSeriesTxModel_detach", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_TimeSeriesTxModel_detach(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelHandle model,
            final DxfgFeed feed
    ) {
        NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toJava(model).detach(NativeUtils.MAPPER_FEED.toJava(feed));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_setAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_TimeSeriesTxModel_setAggregationPeriod(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelHandle model,
            final DxfgTimePeriodHandle aggregationPeriod
    ) {
        NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toJava(model)
                .setAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(name = "dxfg_TimeSeriesTxModel_close", exceptionHandler = ExceptionHandlerReturnMinusOne.class)
    public static int dxfg_TimeSeriesTxModel_close(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelHandle model
    ) {
        NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toJava(model).close();

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    // TimeSeriesTxModel.Builder

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withFromTime",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withFromTime(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final long fromTime
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder).withFromTime(fromTime));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withSorting",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withSorting(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgTimeSeriesTxModelSortOrder sortOrder
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder).withSorting(sortOrder.sortOrder));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_ignoreRemoveEvents",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_ignoreRemoveEvents(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean ignoreRemoveEvents
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder).ignoreRemoveEvents(ignoreRemoveEvents));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_ignoreEventsFromPast",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_ignoreEventsFromPast(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean ignoreEventFromPast
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder)
                        .ignoreEventsFromPast(ignoreEventFromPast));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withConfirmationTick",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withConfirmationTick(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean confirmationTick
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder)
                        .withConfirmationTick(confirmationTick));
    }

    // The event peek is a Java Consumer, there is no C callback for it: the pointer is ignored.
    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withEventPeek",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withEventPeek(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final VoidPointer ignoredEventPeek
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder).withEventPeek(null));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_build",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelHandle dxfg_TimeSeriesTxModel_Builder_build(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder).build());
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withBatchProcessing",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withBatchProcessing(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean isBatchProcessing
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder).withBatchProcessing(isBatchProcessing));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withSnapshotProcessing",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withSnapshotProcessing(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean isSnapshotProcessing
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder)
                        .withSnapshotProcessing(isSnapshotProcessing));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withFeed",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withFeed(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgFeed feed
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder)
                        .withFeed(NativeUtils.MAPPER_FEED.toJava(feed)));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withAggregationPeriod(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgTimePeriodHandle aggregationPeriod
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder)
                        .withAggregationPeriod(NativeUtils.MAPPER_TIME_PERIOD.toJava(aggregationPeriod)));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withSymbol",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withSymbol(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgSymbol symbol
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder)
                        .withSymbol(NativeUtils.MAPPER_SYMBOL.toJava(symbol)));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withListener",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withListener(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgTxModelListenerHandle listener
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder)
                        .withListener(forModel(NativeUtils.MAPPER_TX_MODEL_LISTENER.toJava(listener))));
    }

    @CEntryPoint(
            name = "dxfg_TimeSeriesTxModel_Builder_withExecutor",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgTimeSeriesTxModelBuilderHandle dxfg_TimeSeriesTxModel_Builder_withExecutor(
            final IsolateThread ignoredThread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgExecutorHandle executor
    ) {
        return NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toNative(
                NativeUtils.MAPPER_TIME_SERIES_TX_MODEL_BUILDER.toJava(builder)
                        .withExecutor(NativeUtils.MAPPER_EXECUTOR.toJava(executor)));
    }

    // TxModelListener

    @CEntryPoint(name = "dxfg_TxModelListener_new", exceptionHandler = ExceptionHandlerReturnNullWord.class)
    public static DxfgTxModelListenerHandle dxfg_TxModelListener_new(
            final IsolateThread ignoredThread,
            final DxfgTxModelListenerEventsReceivedFunction eventsReceived,
            final VoidPointer userData
    ) {
        final class NativeTxModelListener extends NativeListener implements TxModelListener<IndexedEvent<?>> {
            @Override
            public void eventsReceived(
                    final IndexedEventSource source,
                    final List<IndexedEvent<?>> events,
                    final boolean isSnapshot
            ) {
                if (!enter()) {
                    return;
                }

                try {
                    final DxfgIndexedEventSourcePointer sourceNative =
                            NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.toNative(source);

                    try {
                        final DxfgEventTypeListPointer eventsNative = NativeUtils.MAPPER_EVENTS.toNativeList(events);

                        try {
                            eventsReceived.invoke(CurrentIsolate.getCurrentThread(), sourceNative, eventsNative,
                                    isSnapshot ? 1 : 0, userData);
                        } finally {
                            NativeUtils.MAPPER_EVENTS.release(eventsNative);
                        }
                    } finally {
                        NativeUtils.MAPPER_INDEXED_EVENT_SOURCE.release(sourceNative);
                    }
                } finally {
                    exit();
                }
            }
        }

        return NativeUtils.MAPPER_TX_MODEL_LISTENER.toNative(new NativeTxModelListener());
    }
}
