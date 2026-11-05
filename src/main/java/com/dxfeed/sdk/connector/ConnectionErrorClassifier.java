// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.net.ssl.SSLException;

/**
 * The category of a connection error by the types of the exception and its causes; the messages are never read.
 */
final class ConnectionErrorClassifier {

    private ConnectionErrorClassifier() {
    }

    /**
     * Walks the exception and its causes from the top: the first exception of a known type gives the category; with
     * none, any {@link IOException} gives {@link ConnectionError#CATEGORY_IO}, anything else
     * {@link ConnectionError#CATEGORY_OTHER}.
     */
    static int classify(final Throwable thrown) {
        final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean io = false;

        for (Throwable current = thrown; current != null && seen.add(current); current = current.getCause()) {
            final int category = classifyOne(current);

            if (category != ConnectionError.CATEGORY_OTHER) {
                return category;
            }

            io |= current instanceof IOException;
        }

        return io ? ConnectionError.CATEGORY_IO : ConnectionError.CATEGORY_OTHER;
    }

    private static int classifyOne(final Throwable thrown) {
        if (thrown instanceof UnknownHostException) {
            return ConnectionError.CATEGORY_DNS;
        }

        if (thrown instanceof ConnectException || thrown instanceof NoRouteToHostException
                || thrown instanceof PortUnreachableException) {
            return ConnectionError.CATEGORY_CONNECT_FAILED;
        }

        if (thrown instanceof SSLException) {
            return ConnectionError.CATEGORY_TLS;
        }

        if (thrown instanceof SocketTimeoutException) {
            return ConnectionError.CATEGORY_READ_TIMEOUT;
        }

        if (thrown instanceof EOFException) {
            return ConnectionError.CATEGORY_CLOSED_BY_REMOTE;
        }

        return ConnectionError.CATEGORY_OTHER;
    }
}
