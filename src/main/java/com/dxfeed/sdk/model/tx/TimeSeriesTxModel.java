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
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/main/java/com/dxfeed/api/experimental/model/TimeSeriesTxModel.java
package com.dxfeed.sdk.model.tx;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.osub.TimeSeriesSubscriptionSymbol;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.TimeSeriesEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * An incremental model for time-series events.
 * This model manages all snapshot and transaction logic, subscription handling, and listener notifications.
 *
 * <p>This model is designed to handle incremental transactions. Users of this model only see the list
 * of events in a consistent state. This model delays incoming events that are part of an incomplete snapshot
 * or ongoing transaction until the snapshot is complete or the transaction has ended. This model notifies
 * the user of received transactions through an installed {@link TxModelListener listener}.
 *
 * <h3>Configuration</h3>
 *
 * <p>This model must be configured using the {@link Builder builder}, as most configuration
 * settings cannot be changed once the model is built. This model requires configuration with
 * a {@link Builder#withSymbol(Object) symbol} and a {@link Builder#withFromTime(long) fromTime} for subscription,
 * and it must be {@link Builder#withFeed(DXFeed) attached} to a {@link DXFeed} instance to begin operation.
 * For ease of use, some of these configurations can be changed after the model is built, see
 * {@link #setFromTime(long) setFromTime}, {@link #attach(DXFeed) attach} and their overloads.
 *
 * <p>This model only supports single symbol subscriptions; multiple symbols cannot be configured.
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
 * @param <E> the type of time series events processed by this model.
 */
public final class TimeSeriesTxModel<E extends TimeSeriesEvent<?>> extends AbstractTxModel<E> {
    private final SortOrder sortOrder;
    private final boolean ignoreRemoveEvents;
    private final boolean ignoreEventsFromPast;
    private final boolean confirmationTick;
    private final Consumer<E> eventPeek;
    private long fromTime;

    /**
     * Represents the available sorting orders.
     */
    public enum SortOrder {

        /**
         * Indicates that no sorting should be applied.
         * Events will be passed in the same order in which they were received.
         */
        NONE,

        /**
         * Indicates that sorting should be in ascending order by index.
         */
        ASCENDING,

        /**
         * Indicates that sorting should be in descending order by index.
         */
        DESCENDING
    }

    private TimeSeriesTxModel(Builder<E> builder) {
        super(builder);
        sortOrder = builder.confirmationTick ? SortOrder.ASCENDING : builder.sortOrder;
        fromTime = builder.fromTime;
        ignoreRemoveEvents = builder.ignoreRemoveEvents;
        ignoreEventsFromPast = builder.ignoreEventsFromPast;
        confirmationTick = builder.confirmationTick;
        eventPeek = builder.eventPeek;
        updateSubscription(getUndecoratedSymbol(), fromTime);
    }

    /**
     * Factory method to create a new builder for this model.
     *
     * @param eventType the class type of time series event.
     * @return a new {@link Builder builder} instance.
     */
    public static <E extends TimeSeriesEvent<?>> Builder<E> newBuilder(Class<E> eventType) {
        return new Builder<>(eventType);
    }

    /**
     * Returns the time from which to subscribe for time-series,
     * or {@link Long#MAX_VALUE} if this model is not subscribed (this is a default value).
     *
     * @return the time in milliseconds, since Unix epoch of January 1, 1970.
     */
    public synchronized long getFromTime() {
        return fromTime;
    }

    /**
     * Sets the time from which to subscribe for time-series.
     * If this time has already been set, nothing happens.
     *
     * @param fromTime the time in milliseconds, since Unix epoch of January 1, 1970.
     */
    public synchronized void setFromTime(long fromTime) {
        if (this.fromTime == fromTime)
            return;
        this.fromTime = fromTime;
        setSymbols(decorateSymbol(getUndecoratedSymbol(), fromTime));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    TxEventProcessor<E> createEventProcessor(IndexedEventSource source) {
        // ConfirmationTickListener always passes an alienated List, safe to cast, no copy needed
        TxEventProcessor.Listener<E> txListener = (events, isSnapshot) -> {
            List<E> list = confirmationTick ? (List<E>) events : new ArrayList<>(events);
            if (eventPeek != null)
                list.forEach(eventPeek);
            listener.eventsReceived(source, list, isSnapshot);
        };
        return new TxEventProcessor<>(isBatchProcessing, isSnapshotProcessing, sortOrder,
            ignoreRemoveEvents, ignoreEventsFromPast, confirmationTick, txListener);
    }

    private void updateSubscription(Object symbol, long fromTime) {
        setSymbols(decorateSymbol(symbol, fromTime));
    }

    private Set<Object> decorateSymbol(Object symbol, long fromTime) {
        if (symbol == null || fromTime == Long.MAX_VALUE)
            return Collections.emptySet();
        return Collections.singleton(new TimeSeriesSubscriptionSymbol<>(symbol, fromTime));
    }

    /**
     * A builder class for creating an instance of {@link TimeSeriesTxModel}.
     *
     * @param <E> the type of events processed by the model being created.
     */
    public static final class Builder<E extends TimeSeriesEvent<?>> extends AbstractTxModel.Builder<E, Builder<E>> {
        private long fromTime = Long.MAX_VALUE;
        private SortOrder sortOrder = SortOrder.NONE;
        private boolean ignoreRemoveEvents;
        private boolean ignoreEventsFromPast;
        private boolean confirmationTick;
        private Consumer<E> eventPeek;

        /**
         * Constructs a new {@link Builder builder} for the specified event type.
         *
         * @param eventType the class type of time series event.
         */
        public Builder(Class<E> eventType) {
            super(eventType);
        }

        /**
         * Sets the time from which to subscribe for time-series.
         *
         * <p>This time defaults to {@link Long#MAX_VALUE}, which means that this model is not subscribed.
         * This time can be changed later, after the model has been created,
         * by calling {@link #setFromTime(long) setFromTime}.
         *
         * @param fromTime the time in milliseconds, since Unix epoch of January 1, 1970.
         * @return {@code this} builder.
         */
        public Builder<E> withFromTime(long fromTime) {
            this.fromTime = fromTime;
            return this;
        }

        /**
         * Sets the sorting order for snapshot.
         *
         * <p>This method sets the desired {@code SortOrder} to be used for snapshot processing.
         * This method only has an effect if {@link #withSnapshotProcessing(boolean) snapshot processing } is enabled.
         *
         * @param sortOrder the {@link SortOrder} to apply for sorting.
         * @return {@code this} builder.
         */
        public Builder<E> withSorting(SortOrder sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        /**
         * Enables or disables ignoring {@link IndexedEvent#REMOVE_EVENT remove events}.
         * <b>This is disabled by default</b>.
         *
         * <p>If set to {@code true}, remove events will not be passed to the listener,
         * effectively ignoring any events that represent removals.
         *
         * @param ignoreRemoveEvents {@code true} to ignore remove events; {@code false} otherwise.
         * @return {@code this} builder.
         */
        public Builder<E> ignoreRemoveEvents(boolean ignoreRemoveEvents) {
            this.ignoreRemoveEvents = ignoreRemoveEvents;
            return this;
        }

        /**
         * Enables or disables ignoring events from the past.
         *
         * <p>If set to {@code true}, events with an index less than or equal to the last processed index
         * will be skipped and not be passed to the listener.
         *
         * @param ignoreEventFromPast {@code true} to ignore events from the past; {@code false} otherwise.
         * @return {@code this} builder.
         */
        public Builder<E> ignoreEventsFromPast(boolean ignoreEventFromPast) {
            this.ignoreEventsFromPast = ignoreEventFromPast;
            return this;
        }

        /**
         * Enables or disables the confirmation tick.
         * <b>This is disabled by default</b>.
         *
         * <p>When enabled, the model detects bar transitions by comparing event indexes.
         * When a new bar arrives in real-time, the previous (closing) bar is prepended to the event list
         * before the new bar, allowing consumers to identify confirmed bars via index lookahead.
         *
         * <p>When this option is enabled, snapshot processing and ascending sort order are applied
         * automatically so that the last event in a snapshot reliably represents the most recent bar.
         *
         * @param confirmationTick {@code true} to enable confirmation tick; {@code false} otherwise.
         * @return {@code this} builder.
         */
        public Builder<E> withConfirmationTick(boolean confirmationTick) {
            this.confirmationTick = confirmationTick;
            if (confirmationTick)
                withSnapshotProcessing(true);
            return this;
        }

        /**
         * Sets a peek action to be called for each event after filtering and before delivery
         * to the {@link TxModelListener}.
         *
         * <p>The action is invoked for every event that passes the model's filtering options
         * (see {@link #ignoreRemoveEvents(boolean)} and {@link #ignoreEventsFromPast(boolean)}),
         * so it only sees events that will actually be delivered to the listener.
         * Events discarded by filters are not visible to the peek action.
         *
         * <p><b>Note:</b> the list passed to the peek action is the same object that will be
         * passed to the listener; modifications to events affect the listener.
         *
         * @param eventPeek the action to invoke per event; {@code null} disables it.
         * @return {@code this} builder.
         */
        public Builder<E> withEventPeek(Consumer<E> eventPeek) {
            this.eventPeek = eventPeek;
            return this;
        }

        /**
         * Builds an instance of {@link TimeSeriesTxModel} based on the provided parameters.
         *
         * @return the created {@link TimeSeriesTxModel}.
         */
        @Override
        public TimeSeriesTxModel<E> build() {
            return new TimeSeriesTxModel<>(this);
        }
    }
}
