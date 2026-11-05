// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import static org.junit.Assert.assertEquals;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.Test;

/**
 * The categories of connection errors by the exception types alone: the cases that real QD paths do not reproduce in a
 * test (read timeout, wrapped exceptions, unknown exceptions).
 */
public class ConnectionErrorClassifierTest {

    @Test
    public void connectPhaseExceptions() {
        assertEquals(ConnectionError.CATEGORY_DNS, ConnectionErrorClassifier.classify(new UnknownHostException("x")));
        assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED,
                ConnectionErrorClassifier.classify(new ConnectException("refused")));
        assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED,
                ConnectionErrorClassifier.classify(new NoRouteToHostException("no route")));
        assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED,
                ConnectionErrorClassifier.classify(new PortUnreachableException("unreachable")));
    }

    @Test
    public void establishedConnectionExceptions() {
        assertEquals(ConnectionError.CATEGORY_READ_TIMEOUT,
                ConnectionErrorClassifier.classify(new SocketTimeoutException("Read timed out")));
        assertEquals(ConnectionError.CATEGORY_CLOSED_BY_REMOTE,
                ConnectionErrorClassifier.classify(new EOFException("Connection was closed")));
        assertEquals(ConnectionError.CATEGORY_IO,
                ConnectionErrorClassifier.classify(new SocketException("Connection reset")));
        assertEquals(ConnectionError.CATEGORY_IO, ConnectionErrorClassifier.classify(new IOException("other")));
    }

    @Test
    public void tlsExceptionsAlsoAsCauses() {
        assertEquals(ConnectionError.CATEGORY_TLS, ConnectionErrorClassifier.classify(new SSLException("unwrap")));
        assertEquals(ConnectionError.CATEGORY_TLS,
                ConnectionErrorClassifier.classify(new IOException("wrapped", new SSLHandshakeException("cert"))));
        // The first known type from the top wins: a TLS failure caused by a reset is a TLS failure.
        assertEquals(ConnectionError.CATEGORY_TLS,
                ConnectionErrorClassifier.classify(new SSLException("unwrap", new SocketException("reset"))));
    }

    @Test
    public void causesAreWalkedAndCyclesEnd() {
        assertEquals(ConnectionError.CATEGORY_CONNECT_FAILED,
                ConnectionErrorClassifier.classify(new RuntimeException(new ConnectException("refused"))));

        final IOException first = new IOException("first");
        final IOException second = new IOException("second", first);
        first.initCause(second);
        assertEquals(ConnectionError.CATEGORY_IO, ConnectionErrorClassifier.classify(first));
    }

    @Test
    public void unknownExceptionsAreOther() {
        assertEquals(ConnectionError.CATEGORY_OTHER,
                ConnectionErrorClassifier.classify(new IllegalStateException("not I/O")));
        assertEquals(ConnectionError.CATEGORY_OTHER, ConnectionErrorClassifier.classify(new OutOfMemoryError()));
    }
}
