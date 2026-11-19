// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * The intended schedule of the publisher (NetTest specification, section "Rate limit and schedule"): one budget for
 * all instances that grants whole batches. Batch {@code k}, counted over all instances from the first grant
 * {@code t0}, is due at {@code T(k) = t0 + k * batch / rate}; ahead of it the generator sleeps, behind it the generator
 * catches up without a bound and never drops a batch. The schedule slip of a batch is its start minus {@code T(k)}.
 * Without {@code -r} there is no schedule and no slip, but the first grant still anchors the warm-up.
 */
class NetTestApiSchedule {
    static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final double batchNanos; // the time of one batch at the rate; 0 without a rate
    private final AtomicLong slipMax = new AtomicLong(); // the largest slip since the last tick
    private volatile long lastSlip; // the slip of the last batch
    private volatile long firstGrantEpochNanos; // 0 before the first grant
    private long t0; // System.nanoTime of the first grant, guarded by this
    private long granted; // the batches granted, guarded by this

    /** The schedule of {@code batch} events per grant at {@code rate} events per second, or none for null. */
    NetTestApiSchedule(int batch, Double rate) {
        this.batchNanos = rate == null ? 0 : batch * (double) NANOS_PER_SECOND / rate;
    }

    boolean isLimited() {
        return batchNanos > 0;
    }

    /**
     * Grants the next batch and returns the {@link System#nanoTime} at which it is due; the first grant also sets
     * {@code t0} and the epoch time that anchors the warm-up.
     */
    long grant() {
        long k;
        long start;
        synchronized (this) {
            if (granted == 0) {
                t0 = System.nanoTime();
                firstGrantEpochNanos = NetTestApiClock.epochNanos();
            }
            k = granted++;
            start = t0;
        }
        return dueNanos(start, k);
    }

    /** {@code T(k)}: the {@link System#nanoTime} at which batch {@code k} is due; a test may shift it. */
    long dueNanos(long t0, long k) {
        return isLimited() ? t0 + Math.round(k * batchNanos) : t0;
    }

    /** Sleeps until {@code due} of {@link System#nanoTime} with the platform's sleep; at once without a schedule. */
    void awaitDue(long due) throws InterruptedException {
        if (!isLimited()) {
            return;
        }
        for (long left = due - System.nanoTime(); left > 0; left = due - System.nanoTime()) {
            LockSupport.parkNanos(this, left);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    /** Records the slip of a batch that started at {@code startNanos} of {@link System#nanoTime} and was due at
     * {@code due}; 0 when on time. */
    long recordSlip(long due, long startNanos) {
        if (!isLimited()) {
            return 0;
        }
        long slip = Math.max(0, startNanos - due);
        lastSlip = slip;
        slipMax.accumulateAndGet(slip, Math::max);
        return slip;
    }

    /** The largest slip since the previous call, and the start of a new count. */
    long takeSlipMax() {
        return slipMax.getAndSet(0);
    }

    /** The slip of the last batch. */
    long lastSlip() {
        return lastSlip;
    }

    /** The epoch nanoseconds of the first grant, or 0 before it. */
    long firstGrantEpochNanos() {
        return firstGrantEpochNanos;
    }
}
