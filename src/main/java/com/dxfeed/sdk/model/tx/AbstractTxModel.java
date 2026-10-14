/*
 * !++
 * QDS - Quick Data Signalling Library
 * !-
 * Copyright (C) 2002 - 2024 Devexperts LLC
 * !-
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 * !__
 */
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/main/java/com/dxfeed/api/experimental/model/AbstractTxModel.java
package com.dxfeed.sdk.model.tx;

import com.devexperts.util.TimePeriod;
import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Abstract base class for models that handle transactions of {@link IndexedEvent}.
 * This class manages all snapshot and transaction logic, subscription handling, and listener notifications.
 *
 * <p>This model is designed to handle incremental transactions. Users of this model only see the list
 * of events in a consistent state. This model delays incoming events that are part of an incomplete snapshot
 * or ongoing transaction until the snapshot is complete or the transaction has ended.
 *
 * <h3>Configuration</h3>
 *
 * <p>This model must be configured using the {@link Builder builder}. Specific implementations can add additional
 * configuration options as needed. This model requires a call to the {@link #setSymbols(Set) setSymbols} method
 * (all inheritors must call this method) for subscription, and it must be {@link #attach(DXFeed) attached}
 * to a {@link DXFeed} instance to begin operation.
 *
 * <h3>Resource management and closed models</h3>
 *
 * <p>Attached model is a potential memory leak. If the pointer to attached model is lost, then there is no way
 * to detach this model from the feed and the model will not be reclaimed by the garbage collector as long as the
 * corresponding feed is still used. Detached model can be reclaimed by the garbage collector, but detaching model
 * requires knowing the pointer to the feed at the place of the call, which is not always convenient.
 *
 * <p>The convenient way to detach model from the feed is to call its {@link #close close} method. Closed model
 * becomes permanently detached from all feeds, removes all its listeners and is guaranteed to be reclaimable by
 * the garbage collector as soon as all external references to it are cleared.
 *
 * <h3>Threads and locks</h3>
 *
 * <p>This class is thread-safe and can be used concurrently from multiple threads without external synchronization.
 *
 * <p>Notification on model changes are invoked from a separate thread via the executor.
 * Default executor for all models is configured with {@link DXEndpoint#executor(Executor) DXEndpoint.executor}
 * method. Each model can individually override its executor with
 * {@link Builder#withExecutor(Executor) Builder.withExecutor} method.
 * The corresponding {@link TxModelListener#eventsReceived(IndexedEventSource, List, boolean) eventsReceived}
 * to never be concurrent, even though it may happen from different threads if executor is multi-threaded.
 *
 * @param <E> the type of indexed events processed by this model.
 */
public abstract class AbstractTxModel<E extends IndexedEvent<?>> implements AutoCloseable {
    private final Map<IndexedEventSource, TxEventProcessor<E>> processorsBySource = new HashMap<>();
    private final Set<TxEventProcessor<E>> readyProcessors = new LinkedHashSet<>();
    private final DXFeedSubscription<E> subscription;
    private final Object undecoratedSymbol;
    protected final boolean isBatchProcessing;
    protected final boolean isSnapshotProcessing;
    protected final TxModelListener<E> listener;

    protected AbstractTxModel(Builder<E, ?> builder) {
        if (builder.symbol == null)
            throw new IllegalStateException("The 'symbol' must not be null.");
        undecoratedSymbol = builder.symbol;
        isBatchProcessing = builder.isBatchProcessing;
        isSnapshotProcessing = builder.isSnapshotProcessing;
        subscription = new DXFeedSubscription<>(builder.eventType);
        if (builder.aggregationPeriod != null)
            subscription.setAggregationPeriod(builder.aggregationPeriod);
        subscription.addEventListener(this::processEvents);
        subscription.setExecutor(builder.executor);
        listener = builder.listener == null ? (source, events, isSnapshot) -> { } : builder.listener;
        if (builder.feed != null)
            subscription.attach(builder.feed);
    }

    /**
     * Returns whether batch processing is enabled.
     * See {@link Builder#withBatchProcessing(boolean) withBatchProcessing}.
     *
     * @return {@code true} if batch processing is enabled; {@code false} otherwise.
     */
    public boolean isBatchProcessing() {
        return isBatchProcessing;
    }

    /**
     * Returns whether snapshot processing is enabled.
     * See {@link Builder#withSnapshotProcessing(boolean) withSnapshotProcessing}.
     *
     * @return {@code true} if snapshot processing is enabled; {@code false} otherwise.
     */
    public boolean isSnapshotProcessing() {
        return isSnapshotProcessing;
    }

    /**
     * Attaches this model to the specified feed.
     * Technically, this model can be attached to multiple feeds at once,
     * but this is rarely needed and not recommended.
     *
     * @param feed the feed to attach to.
     */
    public void attach(DXFeed feed) {
        subscription.attach(feed);
    }

    /**
     * Detaches this model from the specified feed.
     *
     * @param feed the feed to detach from.
     */
    public void detach(DXFeed feed) {
        subscription.detach(feed);
    }

    /**
     * Sets the aggregation period for data.
     * This method sets a new aggregation period for data, which will only take effect on the next iteration of
     * data notification. For example, if the current aggregation period is 5 seconds and it is changed
     * to 1 second, the next call to the next call to the retrieve method may take up to 5 seconds, after which
     * the new aggregation period will take effect.
     *
     * @param aggregationPeriod the new aggregation period for data
     */
    public void setAggregationPeriod(TimePeriod aggregationPeriod) {
        subscription.setAggregationPeriod(aggregationPeriod);
    }

    /**
     * Closes this model and makes it <i>permanently detached</i>.
     *
     * <p>This method clears installed listener and ensures that the model
     * can be safely garbage-collected when all outside references to it are lost.
     */
    @Override
    public void close() {
        subscription.close();
    }

    /**
     * Processes a list of events, updating the relevant processors and handling batch processing.
     *
     * @param events the list of events to process.
     */
    void processEvents(List<E> events) {
        for (E event : events) {
            TxEventProcessor<E> processor =
                processorsBySource.computeIfAbsent(event.getSource(), this::createEventProcessor);
            if (processor.processEvent(event))
                readyProcessors.add(processor);
        }
        readyProcessors.forEach(TxEventProcessor::receiveAllEventsInBatch);
        readyProcessors.clear();
    }

    /**
     * Creates a new {@link TxEventProcessor} for processing events from the specified source.
     *
     * @param source the {@link IndexedEventSource} from which events will be processed.
     * @return a configured {@link TxEventProcessor} instance for handling events.
     */
    TxEventProcessor<E> createEventProcessor(IndexedEventSource source) {
        return new TxEventProcessor<>(isBatchProcessing, isSnapshotProcessing,
            (transactions, isSnapshot) ->
                listener.eventsReceived(source, new ArrayList<>(transactions), isSnapshot));
    }

    /**
     * Returns the undecorated symbol associated with the model.
     */
    protected Object getUndecoratedSymbol() {
        return undecoratedSymbol;
    }

    /**
     * Sets the subscription symbols for the model.
     *
     * @param symbols the set of symbols to subscribe to.
     */
    protected void setSymbols(Set<?> symbols) {
        subscription.setSymbols(symbols);
    }

    /**
     * Abstract builder for building models inherited from {@link AbstractTxModel}.
     * Specific implementations can add additional configuration options to this builder.
     *
     * <p>Inheritors of this class must override abstract method {@link #build()} to build a specific model.
     *
     * @param <E> type of events processed by the model being created.
     * @param <B> the type of the builder subclass.
     */
    public abstract static class Builder<E extends IndexedEvent<?>, B extends Builder<E, B>> {
        private final Class<E> eventType;
        private boolean isBatchProcessing = true;
        private boolean isSnapshotProcessing;
        private DXFeed feed;
        private Object symbol;
        private TxModelListener<E> listener;
        private Executor executor;
        private TimePeriod aggregationPeriod;

        protected Builder(Class<E> eventType) {
            this.eventType = eventType;
        }

        /**
         * Enables or disables batch processing.
         * <b>This is enabled by default</b>.
         *
         * <p>If batch processing is disabled, the model will notify listener
         * <b>separately for each transaction</b> (even if it is represented by a single event);
         * otherwise, transactions can be combined in a single listener call.
         *
         * <p>A transaction may represent either a snapshot or update events that are received after a snapshot.
         * Whether this flag is set or not, the model will always notify listeners that a snapshot has been received
         * and will not combine multiple snapshots or a snapshot with another transaction
         * into a single listener notification.
         *
         * @param isBatchProcessing {@code true} to enable batch processing; {@code false} otherwise.
         * @return {@code this} builder.
         */
        @SuppressWarnings("unchecked")
        public B withBatchProcessing(boolean isBatchProcessing) {
            this.isBatchProcessing = isBatchProcessing;
            return (B) this;
        }

        /**
         * Enables or disables snapshot processing.
         * <b>This is disabled by default</b>.
         *
         * <p>If snapshot processing is enabled, transactions representing a snapshot will be processed as follows:
         * events that are marked for removal will be removed, repeated indexes will be merged, and
         * {@link IndexedEvent#getEventFlags() eventFlags} of events are set to zero;
         * otherwise, the user will see the snapshot in raw form, with possible repeated indexes,
         * events marked for removal, and {@link IndexedEvent#getEventFlags() eventFlags} unchanged.
         *
         * <p>Whether this flag is set or not, in transactions that are not a snapshot, events that are marked
         * for removal will not be removed, repeated indexes will not be merged, and
         * {@link IndexedEvent#getEventFlags() eventFlags} of events will not be changed.
         * This flag only affects the processing of transactions that are a snapshot.
         *
         * @param isSnapshotProcessing {@code true} to enable snapshot processing; {@code false} otherwise.
         * @return {@code this} builder.
         */
        @SuppressWarnings("unchecked")
        public B withSnapshotProcessing(boolean isSnapshotProcessing) {
            this.isSnapshotProcessing = isSnapshotProcessing;
            return (B) this;
        }

        /**
         * Sets the {@link DXFeed feed} for the model being created.
         * The {@link DXFeed feed} can also be attached later, after the model has been created,
         * by calling {@link #attach(DXFeed) attachFeed}.
         *
         * @param feed the {@link DXFeed feed}.
         * @return {@code this} builder.
         */
        @SuppressWarnings("unchecked")
        public B withFeed(DXFeed feed) {
            this.feed = feed;
            return (B) this;
        }

        /**
         * Sets the aggregation period for data.
         * The aggregation period can also set later, after the model has been created.
         *
         * @param aggregationPeriod the new aggregation period for data
         */
        @SuppressWarnings("unchecked")
        public B withAggregationPeriod(TimePeriod aggregationPeriod) {
            this.aggregationPeriod = aggregationPeriod;
            return (B) this;
        }

        /**
         * Sets the subscription symbol for the model being created.
         * The symbol cannot be added or changed after the model has been built.
         *
         * @param symbol the subscription symbol.
         * @return {@code this} builder.
         */
        @SuppressWarnings("unchecked")
        public B withSymbol(Object symbol) {
            this.symbol = symbol;
            return (B) this;
        }

        /**
         * Sets the listener for transaction notifications.
         * The listener cannot be changed or added once the model has been built.
         *
         * @param listener the transaction listener.
         * @return {@code this} builder.
         */
        @SuppressWarnings("unchecked")
        public B withListener(TxModelListener<E> listener) {
            this.listener = listener;
            return (B) this;
        }

        /**
         * Sets the executor for processing transaction notifications.
         * The executor cannot be changed or added once the model has been built.
         *
         * <p>Default executor for all models is configured with
         * {@link DXEndpoint#executor(Executor) DXEndpoint.executor} method.
         *
         * @param executor the executor instance.
         * @return {@code this} builder.
         */
        @SuppressWarnings("unchecked")
        public B withExecutor(Executor executor) {
            this.executor = executor;
            return (B) this;
        }

        /**
         * Builds an instance of model based on the provided parameters.
         *
         * @return the created model.
         */
        public abstract AbstractTxModel<E> build();
    }
}
