// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.devexperts.qd.tools;

import java.time.Instant;

/** The wall clock of the tool with nanosecond resolution. */
final class NetTestApiClock {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private NetTestApiClock() {}

    /** The wall clock in nanoseconds since the epoch, from {@link Instant#now()}. */
    static long epochNanos() {
        Instant now = Instant.now();
        return now.getEpochSecond() * NANOS_PER_SECOND + now.getNano();
    }
}
