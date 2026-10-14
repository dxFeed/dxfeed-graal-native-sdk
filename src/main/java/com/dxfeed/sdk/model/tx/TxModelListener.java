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
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/main/java/com/dxfeed/api/experimental/model/TxModelListener.java
package com.dxfeed.sdk.model.tx;

import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;

import java.util.List;

/**
 * The listener interface for receiving indexed events of the specified type {@code E}
 * from the {@link IndexedTxModel} or {@link TimeSeriesTxModel}.
 *
 * @param <E> the type of indexed events.
 */
@FunctionalInterface
public interface TxModelListener<E extends IndexedEvent<?>> {
    /**
     * Invoked when a complete transaction (one or more) is received. This behavior can be changed when building
     * the model; see {@link AbstractTxModel.Builder#withBatchProcessing(boolean) withBatchProcessing}.
     *
     * <p>Only events that have the same {@link IndexedEventSource source} and {@link EventType#getEventSymbol() symbol}
     * can be in the same listener call and cannot be mixed within a single call. If there are multiple sources,
     * listener notifications will happen separately for each source.
     *
     * <p>A transaction can also be a snapshot. In such cases, the {@code isSnapshot} flag is set to {@code true},
     * indicating that all state based on previously received events for the corresponding
     * {@link IndexedEventSource source} should be cleared. A snapshot can also be post-processed or raw;
     * see {@link AbstractTxModel.Builder#withSnapshotProcessing(boolean) withSnapshotProcessing}.
     * If {@link AbstractTxModel.Builder#withSnapshotProcessing(boolean) withSnapshotProcessing} is {@code true},
     * the transaction containing the snapshot can be empty ({@code events.isEmpty()} returns {@code true}),
     * meaning that an empty snapshot was received.
     *
     * @param source the source of the indexed events.
     * @param events the list of received events representing one or more transactions.
     * @param isSnapshot indicates if the events form a snapshot.
     */
    void eventsReceived(IndexedEventSource source, List<E> events, boolean isSnapshot);
}
