// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

/**
 * A {@link ByteArrayOutputStream} for the data that a function returns as a {@code dxfg_v4_byte_list}: it fails with
 * {@link IllegalStateException} instead of growing over the limit (the size of the list is an {@code int32_t}, and a
 * Java array cannot hold more than {@link #MAX_SIZE} bytes), and it gives its buffer without a copy.
 */
public final class ByteListOutputStream extends ByteArrayOutputStream {

    /**
     * The largest number of bytes: the largest array length that the JDK allocates ({@code Integer.MAX_VALUE - 8}).
     */
    public static final int MAX_SIZE = Integer.MAX_VALUE - 8;

    private final int limit;

    /**
     * Creates a stream that holds at most {@link #MAX_SIZE} bytes.
     */
    public ByteListOutputStream() {
        this(MAX_SIZE);
    }

    /**
     * Creates a stream that holds at most {@code limit} bytes.
     *
     * @param limit The largest number of bytes, from 0 to {@link #MAX_SIZE}.
     */
    ByteListOutputStream(final int limit) {
        if (limit < 0 || limit > MAX_SIZE) {
            throw new IllegalArgumentException("The limit " + limit + " is out of [0, " + MAX_SIZE + "]");
        }

        this.limit = limit;
    }

    @Override
    public synchronized void write(final int b) {
        ensureRoom(1);
        super.write(b);
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        ensureRoom(len);
        super.write(b, off, len);
    }

    /**
     * Returns the buffer of the stream, not a copy: its first {@link #size()} bytes are the data.
     *
     * @return The buffer.
     */
    public synchronized byte[] buffer() {
        return buf;
    }

    private void ensureRoom(final int length) {
        if (length > limit - count) {
            throw new IllegalStateException(
                    "The data does not fit a list of bytes: it takes more than " + limit + " bytes");
        }
    }
}
