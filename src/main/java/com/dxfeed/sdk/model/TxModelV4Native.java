// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.model;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgIndexedEventSourceList;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import com.dxfeed.sdk.feed.DxfgFeed;
import com.dxfeed.sdk.javac.DxfgExecutorHandle;
import com.dxfeed.sdk.javac.DxfgTimePeriodHandle;
import com.dxfeed.sdk.symbol.DxfgSymbol;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link TxModelNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(ModelDirectives.class)
public final class TxModelV4Native {

    private TxModelV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_newBuilder",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_newBuilder(
            final IsolateThread thread,
            final DxfgEventClazz eventType,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_newBuilder(thread, eventType);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_getSources",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_getSources(
            final IsolateThread thread,
            final DxfgIndexedTxModelHandle model,
            @DxfgOut final WordPointer sources
    ) {
        if (sources.isNull()) {
            throw new IllegalArgumentException("The `sources` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedEventSourceList result = TxModelNative.dxfg_IndexedTxModel_getSources(thread, model);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        sources.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_setSources",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_setSources(
            final IsolateThread thread,
            final DxfgIndexedTxModelHandle model,
            final DxfgIndexedEventSourceList sources
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_IndexedTxModel_setSources(thread, model, sources);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_isBatchProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_isBatchProcessing(
            final IsolateThread thread,
            final DxfgIndexedTxModelHandle model,
            @DxfgOut final CInt32Pointer batchProcessing
    ) {
        if (batchProcessing.isNull()) {
            throw new IllegalArgumentException("The `batchProcessing` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_IndexedTxModel_isBatchProcessing(thread, model);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        batchProcessing.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_isSnapshotProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_isSnapshotProcessing(
            final IsolateThread thread,
            final DxfgIndexedTxModelHandle model,
            @DxfgOut final CInt32Pointer snapshotProcessing
    ) {
        if (snapshotProcessing.isNull()) {
            throw new IllegalArgumentException("The `snapshotProcessing` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_IndexedTxModel_isSnapshotProcessing(thread, model);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        snapshotProcessing.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_attach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_attach(
            final IsolateThread thread,
            final DxfgIndexedTxModelHandle model,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_IndexedTxModel_attach(thread, model, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_detach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_detach(
            final IsolateThread thread,
            final DxfgIndexedTxModelHandle model,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_IndexedTxModel_detach(thread, model, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_setAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_setAggregationPeriod(
            final IsolateThread thread,
            final DxfgIndexedTxModelHandle model,
            final DxfgTimePeriodHandle aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_IndexedTxModel_setAggregationPeriod(thread, model, aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_close(
            final IsolateThread thread,
            final DxfgIndexedTxModelHandle model
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_IndexedTxModel_close(thread, model);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_withSources",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_withSources(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgIndexedEventSourceList sources,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_withSources(thread, builder, sources);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_build",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_build(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            @DxfgOut final WordPointer indexedTxModel
    ) {
        if (indexedTxModel.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModel` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_build(thread, builder);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModel.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_withBatchProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_withBatchProcessing(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final boolean isBatchProcessing,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_withBatchProcessing(
                        thread,
                        builder,
                        isBatchProcessing);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_withSnapshotProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_withSnapshotProcessing(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final boolean isSnapshotProcessing,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_withSnapshotProcessing(
                        thread,
                        builder,
                        isSnapshotProcessing);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_withFeed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_withFeed(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgFeed feed,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_withFeed(thread, builder, feed);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_withAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_withAggregationPeriod(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgTimePeriodHandle aggregationPeriod,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_withAggregationPeriod(
                        thread,
                        builder,
                        aggregationPeriod);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_withSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_withSymbol(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgSymbol symbol,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_withSymbol(thread, builder, symbol);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_withListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_withListener(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgTxModelListenerHandle listener,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_withListener(thread, builder, listener);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_IndexedTxModel_Builder_withExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_IndexedTxModel_Builder_withExecutor(
            final IsolateThread thread,
            final DxfgIndexedTxModelBuilderHandle builder,
            final DxfgExecutorHandle executor,
            @DxfgOut final WordPointer indexedTxModelBuilder
    ) {
        if (indexedTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `indexedTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgIndexedTxModelBuilderHandle result = TxModelNative.dxfg_IndexedTxModel_Builder_withExecutor(thread, builder, executor);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        indexedTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_newBuilder",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_newBuilder(
            final IsolateThread thread,
            final DxfgEventClazz eventType,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_newBuilder(thread, eventType);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_getFromTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_getFromTime(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelHandle model,
            @DxfgOut final CLongPointer fromTime
    ) {
        if (fromTime.isNull()) {
            throw new IllegalArgumentException("The `fromTime` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final long result = TxModelNative.dxfg_TimeSeriesTxModel_getFromTime(thread, model);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        fromTime.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_setFromTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_setFromTime(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelHandle model,
            final long fromTime
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_TimeSeriesTxModel_setFromTime(thread, model, fromTime);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_isBatchProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_isBatchProcessing(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelHandle model,
            @DxfgOut final CInt32Pointer batchProcessing
    ) {
        if (batchProcessing.isNull()) {
            throw new IllegalArgumentException("The `batchProcessing` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_TimeSeriesTxModel_isBatchProcessing(thread, model);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        batchProcessing.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_isSnapshotProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_isSnapshotProcessing(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelHandle model,
            @DxfgOut final CInt32Pointer snapshotProcessing
    ) {
        if (snapshotProcessing.isNull()) {
            throw new IllegalArgumentException("The `snapshotProcessing` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_TimeSeriesTxModel_isSnapshotProcessing(thread, model);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        snapshotProcessing.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_attach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_attach(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelHandle model,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_TimeSeriesTxModel_attach(thread, model, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_detach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_detach(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelHandle model,
            final DxfgFeed feed
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_TimeSeriesTxModel_detach(thread, model, feed);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_setAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_setAggregationPeriod(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelHandle model,
            final DxfgTimePeriodHandle aggregationPeriod
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_TimeSeriesTxModel_setAggregationPeriod(
                        thread,
                        model,
                        aggregationPeriod);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_close(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelHandle model
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = TxModelNative.dxfg_TimeSeriesTxModel_close(thread, model);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withFromTime",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withFromTime(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final long fromTime,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withFromTime(thread, builder, fromTime);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withSorting",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withSorting(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgTimeSeriesTxModelSortOrder sortOrder,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withSorting(thread, builder, sortOrder);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_ignoreRemoveEvents",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_ignoreRemoveEvents(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean ignoreRemoveEvents,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_ignoreRemoveEvents(
                        thread,
                        builder,
                        ignoreRemoveEvents);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_ignoreEventsFromPast",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_ignoreEventsFromPast(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean ignoreEventFromPast,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_ignoreEventsFromPast(
                        thread,
                        builder,
                        ignoreEventFromPast);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withConfirmationTick",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withConfirmationTick(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean confirmationTick,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withConfirmationTick(
                        thread,
                        builder,
                        confirmationTick);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withEventPeek",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withEventPeek(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final VoidPointer ignoredEventPeek,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withEventPeek(
                        thread,
                        builder,
                        ignoredEventPeek);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_build",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_build(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            @DxfgOut final WordPointer timeSeriesTxModel
    ) {
        if (timeSeriesTxModel.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModel` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_build(thread, builder);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModel.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withBatchProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withBatchProcessing(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean isBatchProcessing,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withBatchProcessing(
                        thread,
                        builder,
                        isBatchProcessing);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withSnapshotProcessing",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withSnapshotProcessing(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final boolean isSnapshotProcessing,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withSnapshotProcessing(
                        thread,
                        builder,
                        isSnapshotProcessing);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withFeed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withFeed(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgFeed feed,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withFeed(thread, builder, feed);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withAggregationPeriod",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withAggregationPeriod(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgTimePeriodHandle aggregationPeriod,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withAggregationPeriod(
                        thread,
                        builder,
                        aggregationPeriod);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withSymbol",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withSymbol(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgSymbol symbol,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withSymbol(thread, builder, symbol);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withListener(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgTxModelListenerHandle listener,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withListener(thread, builder, listener);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TimeSeriesTxModel_Builder_withExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TimeSeriesTxModel_Builder_withExecutor(
            final IsolateThread thread,
            final DxfgTimeSeriesTxModelBuilderHandle builder,
            final DxfgExecutorHandle executor,
            @DxfgOut final WordPointer timeSeriesTxModelBuilder
    ) {
        if (timeSeriesTxModelBuilder.isNull()) {
            throw new IllegalArgumentException("The `timeSeriesTxModelBuilder` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTimeSeriesTxModelBuilderHandle result = TxModelNative.dxfg_TimeSeriesTxModel_Builder_withExecutor(thread, builder, executor);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        timeSeriesTxModelBuilder.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_TxModelListener_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_TxModelListener_new(
            final IsolateThread thread,
            final DxfgTxModelListenerEventsReceivedFunction eventsReceived,
            final VoidPointer userData,
            @DxfgOut final WordPointer txModelListener
    ) {
        if (txModelListener.isNull()) {
            throw new IllegalArgumentException("The `txModelListener` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgTxModelListenerHandle result = TxModelNative.dxfg_TxModelListener_new(thread, eventsReceived, userData);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        txModelListener.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }
}
