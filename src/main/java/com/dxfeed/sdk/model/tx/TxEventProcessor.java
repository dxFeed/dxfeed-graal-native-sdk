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
// Copied from Stash mdapi/dxfeed-java-api-experimental, tag v2.3.1: dxfeed-model/src/main/java/com/dxfeed/api/experimental/model/TxEventProcessor.java
package com.dxfeed.sdk.model.tx;

import com.dxfeed.event.EventType;
import com.dxfeed.event.IndexedEvent;
import com.dxfeed.event.IndexedEventSource;
import com.dxfeed.event.market.OrderBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Accumulates transaction for single {@link EventType event type}, {@link EventType#getEventSymbol() symbol}
 * and {@link IndexedEventSource source}.
 * If multiple symbols/source need to be processed, create multiple instances of this class, one for each.
 *
 * <p>This class maintains the state of events and ensures they are processed correctly based on their
 * {@link IndexedEvent#getEventFlags() eventFlags}. This class manages pending events, handles snapshots
 * and stores the transactions; the user receives transaction notifications using the {@link Listener listener}).
 *
 * <p>Users of this class only see the list of events (passed to the {@link Listener listener}) in a consistent state.
 * For example, due to possible reconnections and retransmissions, the snapshots can overlap each other,
 * so in the event stream the flags can intersect. It is possible to find {@link IndexedEvent#SNAPSHOT_END SNAPSHOT_END}
 * before {@link IndexedEvent#SNAPSHOT_BEGIN SNAPSHOT_BEGIN}, or it is possible to find
 * {@link IndexedEvent#SNAPSHOT_BEGIN SNAPSHOT_BEGIN} after {@link IndexedEvent#SNAPSHOT_END SNAPSHOT_END}, and so on.
 * This class correctly handles such cases.
 *
 * <h3>Threads and locks</h3>
 *
 * <p>This class is <b>NOT</b> thread-safe and cannot be used from multiple threads without external synchronization.
 *
 * @param <E> the type of indexed events processed by this class.
 */
class TxEventProcessor<E extends IndexedEvent<?>> {
    private final List<E> pendingEvents = new ArrayList<>();
    private final TransactionProcessor<E> transactionProcessor;
    private final SnapshotProcessor<E> snapshotProcessor;
    private final boolean ignoreRemoveEvents;
    private final boolean ignoreEventsFromPast;
    private final TimeSeriesTxModel.SortOrder sortOrder;
    private boolean isPartialSnapshot;
    private boolean isCompleteSnapshot;
    private long lastIndex = Long.MIN_VALUE;

    public interface Listener<E> {
        void notify(Collection<E> events, boolean isSnapshot);
    }

    public TxEventProcessor(boolean isBatchProcessing, boolean isSnapshotProcessing, Listener<E> listener) {
        this(isBatchProcessing, isSnapshotProcessing, TimeSeriesTxModel.SortOrder.NONE, false, false, false, listener);
    }

    public TxEventProcessor(boolean isBatchProcessing, boolean isSnapshotProcessing,
                            TimeSeriesTxModel.SortOrder sortOrder, boolean ignoreRemoveEvents,
                            boolean ignoreEventsFromPast,
                            boolean confirmationTick, Listener<E> listener)
    {
        this.ignoreRemoveEvents = ignoreRemoveEvents;
        this.ignoreEventsFromPast = ignoreEventsFromPast;
        this.sortOrder = sortOrder;
        Listener<E> effectiveListener = confirmationTick
            ? new ConfirmationTickListener<>(listener)
            : listener;
        transactionProcessor = isBatchProcessing ? new BatchTransactionProcessor(effectiveListener) :
            new NotifyTransactionProcessor<>(effectiveListener);
        snapshotProcessor = isSnapshotProcessing ? new ProcessingSnapshotProcessor(sortOrder, effectiveListener) :
            new NotifySnapshotProcessor<>(effectiveListener);
    }

    /**
     * Processes the passed event, managing snapshots and transactions, and deferring events as needed.
     *
     * <p>This method returns {@code true} if the passed event completes the transaction.
     * It is the responsibility of the user to determine if all events in the batch have been processed
     * by using {@link TxEventProcessor#receiveAllEventsInBatch()}.
     *
     * <p><b>Warning:</b> This method does not check the {@link EventType#getEventSymbol() symbol}
     * or {@link IndexedEventSource source} of the event, but it is expected that only each instance of
     * this class will be passed events with the same symbol and source; otherwise, it's undefined behavior.
     * If multiple symbols/source need to be processed, create multiple instances of this class, one for each.
     *
     * @param event the event to process.
     * @return {@code true} if the passed event completes the transaction; {@code false} otherwise.
     */
    public boolean processEvent(E event) {
        if (isSnapshotBegin(event)) {
            isPartialSnapshot = true;
            isCompleteSnapshot = false;
            lastIndex = Long.MIN_VALUE;
            pendingEvents.clear(); // remove any unprocessed leftovers on new snapshot
        }
        if (isPartialSnapshot && isSnapshotEndOrSnip(event)) {
            isPartialSnapshot = false;
            isCompleteSnapshot = true;
        }

        pendingEvents.add(event);
        if (isPending(event) || isPartialSnapshot)
            return false; // waiting for the end of snapshot or transaction

        // we have completed transaction or snapshot
        if (isCompleteSnapshot) { // completed snapshot
            snapshotProcessor.processSnapshot(pendingEvents);
            isCompleteSnapshot = false;
        } else { // completed transaction
            transactionProcessor.processTransaction(pendingEvents);
        }
        pendingEvents.clear();
        return true;
    }

    /**
     * Notifies the processor that all events in the current batch have been received.
     *
     * <p>This method should be called after all events in a batch have been processed using
     * {@link #processEvent(E)}.
     */
    public void receiveAllEventsInBatch() {
        transactionProcessor.processingBatch();
    }

    private interface TransactionProcessor<E> {
        void processTransaction(List<E> events);
        void processingBatch();
    }

    private class BatchTransactionProcessor implements TransactionProcessor<E> {
        private final Listener<E> listener;
        private final List<E> transactions = new ArrayList<>();

        BatchTransactionProcessor(Listener<E> listener) {
            this.listener = listener;
        }

        @Override
        public void processTransaction(List<E> events) {
            transactions.addAll(events);
        }

        @Override
        public void processingBatch() {
            switch (sortOrder) {
                case ASCENDING:
                    transactions.sort(Comparator.comparingLong(E::getIndex));
                    break;
                case DESCENDING:
                    transactions.sort(Comparator.comparingLong(E::getIndex).reversed());
                    break;
            }
            Iterator<E> it = transactions.iterator();
            while (it.hasNext()) {
                E event = it.next();
                if (ignoreRemoveEvents && isRemove(event)) {
                    it.remove();
                    continue;
                }
                if (ignoreEventsFromPast && event.getIndex() < lastIndex) {
                    it.remove();
                    continue;
                }
                if (event.getIndex() > lastIndex) {
                    lastIndex = event.getIndex();
                }
            }
            if (transactions.isEmpty())
                return;
            listener.notify(transactions, false);
            transactions.clear();
        }
    }

    private static class NotifyTransactionProcessor<E> implements TransactionProcessor<E> {
        private final Listener<E> listener;

        NotifyTransactionProcessor(Listener<E> listener) {
            this.listener = listener;
        }

        @Override
        public void processTransaction(List<E> events) {
            listener.notify(events, false);
        }

        @Override
        public void processingBatch() {
            // nothing to do
        }
    }

    private interface SnapshotProcessor<E extends IndexedEvent<?>> {
        void processSnapshot(List<E> events);
    }

    private static class NotifySnapshotProcessor<E extends IndexedEvent<?>> implements SnapshotProcessor<E> {
        private final Listener<E> listener;

        NotifySnapshotProcessor(Listener<E> listener) {
            this.listener = listener;
        }

        @Override
        public void processSnapshot(List<E> events) {
            listener.notify(events, true);
        }
    }

    private class ProcessingSnapshotProcessor implements SnapshotProcessor<E> {
        private final Listener<E> listener;
        private final Map<Long, E> snapshot;

        ProcessingSnapshotProcessor(TimeSeriesTxModel.SortOrder sortOrder, Listener<E> listener) {
            this.listener = listener;
            switch (sortOrder) {
                case NONE:
                    snapshot = new LinkedHashMap<>();
                    break;
                case ASCENDING:
                    snapshot = new TreeMap<>(Comparator.naturalOrder());
                    break;
                case DESCENDING:
                    snapshot = new TreeMap<>(Comparator.reverseOrder());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown sort order: " + sortOrder);
            }
        }

        @Override
        public void processSnapshot(List<E> events) {
            for (E event : events) {
                if (isRemove(event)) {
                    snapshot.remove(event.getIndex());
                } else {
                    if (event.getIndex() > lastIndex) {
                        lastIndex = event.getIndex();
                    }
                    event.setEventFlags(0);
                    snapshot.put(event.getIndex(), event);
                }
            }
            listener.notify(snapshot.values(), true);
            snapshot.clear();
        }

        private boolean isRemove(E event) {
            if ((event.getEventFlags() & IndexedEvent.REMOVE_EVENT) != 0)
                return true;
            if (event instanceof OrderBase)
                return !((OrderBase) event).hasSize();
            return false;
        }
    }

    private boolean isSnapshotBegin(E event) {
        return (event.getEventFlags() & IndexedEvent.SNAPSHOT_BEGIN) != 0;
    }

    private boolean isSnapshotEnd(E event) {
        return (event.getEventFlags() & IndexedEvent.SNAPSHOT_END) != 0;
    }

    private boolean isSnapshotSnip(E event) {
        return (event.getEventFlags() & IndexedEvent.SNAPSHOT_SNIP) != 0;
    }

    private boolean isSnapshotEndOrSnip(E event) {
        return isSnapshotEnd(event) || isSnapshotSnip(event);
    }

    private boolean isPending(E event) {
        return (event.getEventFlags() & IndexedEvent.TX_PENDING) != 0;
    }

    private boolean isRemove(E event) {
        return (event.getEventFlags() & IndexedEvent.REMOVE_EVENT) != 0;
    }

    private static class ConfirmationTickListener<E extends IndexedEvent<?>> implements Listener<E> {
        private final Listener<E> delegate;
        private long lastBarIndex = Long.MIN_VALUE;
        private E lastBar;

        ConfirmationTickListener(Listener<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void notify(Collection<E> events, boolean isSnapshot) {
            if (events.isEmpty()) {
                delegate.notify(Collections.emptyList(), isSnapshot);
                return;
            }

            E first = events.iterator().next();
            boolean prepend = !isSnapshot && lastBar != null && first.getIndex() > lastBarIndex;

            List<E> result = new ArrayList<>(events.size() + (prepend ? 1 : 0));
            if (prepend)
                result.add(lastBar);
            result.addAll(events);

            E last = result.get(result.size() - 1);
            lastBarIndex = last.getIndex();
            lastBar = last;

            delegate.notify(result, isSnapshot);
        }
    }
}
