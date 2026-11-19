// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

/**
 * The random source and the value table of one publisher instance, as the NetTest specification defines them (section
 * "Events and pools"), so that every tool puts the same payload on the wire: xorshift64* seeded by the instance, a
 * table of 1024 prices and sizes, and one draw per event for its symbol and its row. All arithmetic is unsigned 64-bit
 * with wrap-around.
 */
final class NetTestApiPool {
    static final int TABLE_ROWS = 1024;

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;
    private static final long MULTIPLIER = 0x2545F4914F6CDD1DL;

    private final double[] prices = new double[TABLE_ROWS];
    private final double[] sizes = new double[TABLE_ROWS];
    private final int poolSize;
    private long state;

    /** The source of instance {@code instance} (from 1) with its table built, for a pool of {@code poolSize}. */
    NetTestApiPool(int instance, int poolSize) {
        this.poolSize = poolSize;
        this.state = initialState(instance);
        for (int row = 0; row < TABLE_ROWS; row++) {
            prices[row] = (100 + Long.remainderUnsigned(next(), 100_000)) / 100.0;
            sizes[row] = 1 + Long.remainderUnsigned(next(), 1_000);
        }
    }

    /** The seed of instance {@code instance}: {@code 0x9E3779B97F4A7C15 * i}. */
    static long seed(int instance) {
        return GOLDEN * instance;
    }

    /** The first state of instance {@code instance}, never 0. */
    static long initialState(int instance) {
        long state = seed(instance) * GOLDEN + MULTIPLIER;
        return state == 0 ? MULTIPLIER : state;
    }

    /** The next draw of xorshift64*. */
    long next() {
        long s = state;
        s ^= s >>> 12;
        s ^= s << 25;
        s ^= s >>> 27;
        state = s;
        return s * MULTIPLIER;
    }

    /** The symbol of the draw {@code r} of an event: {@code (r >> 32) % poolSize}. */
    int symbolIndex(long r) {
        return (int) Long.remainderUnsigned(r >>> 32, poolSize);
    }

    /** The row of the draw {@code r} of an event: {@code (r >> 8) % 1024}. */
    static int row(long r) {
        return (int) Long.remainderUnsigned(r >>> 8, TABLE_ROWS);
    }

    double price(int row) {
        return prices[row];
    }

    double size(int row) {
        return sizes[row];
    }
}
