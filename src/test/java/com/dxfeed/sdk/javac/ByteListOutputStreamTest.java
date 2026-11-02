// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.javac;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import org.junit.Test;

/**
 * {@link ByteListOutputStream}: the data up to the limit, the failure over it and the buffer without a copy.
 */
public class ByteListOutputStreamTest {

    @Test
    public void holdsTheDataUpToTheLimit() {
        final ByteListOutputStream stream = new ByteListOutputStream(4);

        stream.write(1);
        stream.write(new byte[]{9, 2, 3, 9}, 1, 2);
        stream.write(4);

        assertEquals(4, stream.size());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, Arrays.copyOf(stream.buffer(), stream.size()));
        assertArrayEquals(stream.toByteArray(), Arrays.copyOf(stream.buffer(), stream.size()));
    }

    @Test
    public void failsOverTheLimitAndKeepsTheData() {
        final ByteListOutputStream stream = new ByteListOutputStream(3);

        stream.write(new byte[]{1, 2}, 0, 2);

        final IllegalStateException arrayFailure =
                assertThrows(IllegalStateException.class, () -> stream.write(new byte[]{3, 4}, 0, 2));
        assertTrue(arrayFailure.getMessage(), arrayFailure.getMessage().contains("more than 3 bytes"));

        stream.write(3);
        assertThrows(IllegalStateException.class, () -> stream.write(4));
        assertArrayEquals(new byte[]{1, 2, 3}, stream.toByteArray());
    }

    @Test
    public void aCompressingStreamFailsOverTheLimit() {
        final ByteListOutputStream stream = new ByteListOutputStream(8);

        // The gzip header alone takes 10 bytes.
        assertThrows(IllegalStateException.class, () -> new GZIPOutputStream(stream).close());
    }

    @Test
    public void checksTheBoundsAndTheLimit() {
        final ByteListOutputStream stream = new ByteListOutputStream(10);

        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(new byte[2], 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new ByteListOutputStream(-1));
        assertThrows(IllegalArgumentException.class, () -> new ByteListOutputStream(ByteListOutputStream.MAX_SIZE + 1));
        assertEquals(Integer.MAX_VALUE - 8, ByteListOutputStream.MAX_SIZE);
        assertEquals(0, new ByteListOutputStream().size());
    }
}
