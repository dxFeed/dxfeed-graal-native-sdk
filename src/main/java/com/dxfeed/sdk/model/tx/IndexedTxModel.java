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
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/main/java/com/dxfeed/api/experimental/model/IndexedTxModel.java
package com.dxfeed.sdk.model.tx;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.osub.IndexedEventSubscriptionSymbol;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * An incremental model for indexed events.
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
 * a {@link Builder#withSymbol(Object) symbol} and a {@link Builder#withSources(IndexedEventSource...) sources}
 * for subscription, and it must be {@link Builder#withFeed(DXFeed) attached} to a {@link DXFeed}
 * instance to begin operation.
 * For ease of use, some of these configurations can be changed after the model is built, see
 * {@link #setSources(IndexedEventSource...) setSources} and {@link #attach(DXFeed) attach} and their overloads.
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
 * @param <E> the type of indexed events processed by this model.
 */
public final class IndexedTxModel<E extends IndexedEvent<?>> extends AbstractTxModel<E> {
    private Set<IndexedEventSource> sources;

    private IndexedTxModel(Builder<E> builder) {
        super(builder);
        sources = new HashSet<>(builder.sources);
        updateSubscription(getUndecoratedSymbol(), sources);
    }

    /**
     * Factory method to create a new builder for this model.
     *
     * @param eventType the class type of time series event.
     * @return a new {@link Builder builder} instance.
     */
    public static <E extends IndexedEvent<?>> Builder<E> newBuilder(Class<E> eventType) {
        return new Builder<>(eventType);
    }

    /**
     * Returns the current set of sources.
     * If no sources have been set, an empty set is returned,
     * indicating that all possible sources have been subscribed to.
     *
     * @return the set of current sources.
     */
    public synchronized Set<IndexedEventSource> getSources() {
        return sources.isEmpty() ? Collections.emptySet() : new HashSet<>(sources);
    }

    /**
     * Sets the sources from which to subscribe for indexed events.
     * If an empty list is provided, subscriptions will default to all available sources.
     * If these sources have already been set, nothing happens.
     *
     * @param sources the specified sources.
     */
    public synchronized void setSources(IndexedEventSource... sources) {
        setSources(new HashSet<>(Arrays.asList(sources)));
    }

    /**
     * Sets the sources from which to subscribe for indexed events.
     * If an empty set is provided, subscriptions will default to all available sources.
     * If these sources have already been set, nothing happens.
     *
     * @param sources the specified sources.
     */
    public synchronized void setSources(Set<? extends IndexedEventSource> sources) {
        if (this.sources.equals(sources))
            return;
        this.sources = new HashSet<>(sources);
        updateSubscription(getUndecoratedSymbol(), this.sources);
    }

    private void updateSubscription(Object symbol, Set<? extends IndexedEventSource> sources) {
        setSymbols(decorateSymbol(symbol, sources));
    }

    private Set<Object> decorateSymbol(Object symbol, Set<? extends IndexedEventSource> sources) {
        if (symbol == null)
            return Collections.emptySet();
        if (sources.isEmpty())
            return Collections.singleton(symbol);
        Set<Object> symbols = new HashSet<>();
        for (IndexedEventSource source : sources) {
            symbols.add(new IndexedEventSubscriptionSymbol<>(symbol, source));
        }
        return symbols;
    }

    /**
     * A builder class for creating an instance of {@link IndexedTxModel}.
     *
     * @param <E> the type of events processed by the model being created.
     */
    public static final class Builder<E extends IndexedEvent<?>> extends AbstractTxModel.Builder<E, Builder<E>> {
        private Set<IndexedEventSource> sources = new HashSet<>();

        /**
         * Constructs a new {@link Builder builder} for the specified event type.
         *
         * @param eventType the class type of indexed event.
         */
        public Builder(Class<E> eventType) {
            super(eventType);
        }

        /**
         * Sets the sources from which to subscribe for indexed events.
         * If no sources have been set, subscriptions will default to all possible sources.
         *
         * <p>The default value for this source is {@link Set#isEmpty() empty},
         * which means that this model subscribes to all available sources.
         * These sources can be changed later, after the model has been created,
         * by calling {@link #setSources(IndexedEventSource...) setSources}.
         *
         * @param sources the specified sources.
         * @return {@code this} builder.
         */
        public Builder<E> withSources(IndexedEventSource... sources) {
            this.sources = new HashSet<>(Arrays.asList(sources));
            return this;
        }

        /**
         * Sets the sources from which to subscribe for indexed events.
         * If no sources have been set, subscriptions will default to all possible sources.
         *
         * <p>The default value for this source is {@link Set#isEmpty() empty},
         * which means that this model subscribes to all available sources.
         * These sources can be changed later, after the model has been created,
         * by calling {@link #setSources(IndexedEventSource...) setSources}.
         *
         * @param sources the specified sources.
         * @return {@code this} builder.
         */
        public Builder<E> withSources(Collection<? extends IndexedEventSource> sources) {
            this.sources = new HashSet<>(sources);
            return this;
        }

        /**
         * Builds an instance of {@link IndexedTxModel} based on the provided parameters.
         *
         * @return the created {@link IndexedTxModel}.
         */
        @Override
        public IndexedTxModel<E> build() {
            return new IndexedTxModel<>(this);
        }
    }
}
