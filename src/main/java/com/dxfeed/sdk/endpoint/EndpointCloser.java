// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.endpoint;

import com.dxfeed.api.DXEndpoint;

/**
 * Closes endpoints despite the QDS 3.356 bug QD-1801: {@link DXEndpoint#close()} and
 * {@link DXEndpoint#closeAndAwaitTermination()} of an endpoint built with
 * {@code DXEndpoint.Builder.withExecutor(executor)} throw NullPointerException from
 * {@code ExecutorProvider.releaseReference()} (its {@code log} is null). It is the last step of closing, so the
 * endpoint is already closed and the exception is ignored. Remove when QD-1801 is fixed.
 */
public final class EndpointCloser {

    private static final String EXECUTOR_PROVIDER = "com.devexperts.util.ExecutorProvider";

    private EndpointCloser() {
    }

    public static void close(final DXEndpoint endpoint) {
        try {
            endpoint.close();
        } catch (final NullPointerException e) {
            ignoreExecutorReleaseBug(endpoint, e);
        }
    }

    public static void closeAndAwaitTermination(final DXEndpoint endpoint) throws InterruptedException {
        try {
            endpoint.closeAndAwaitTermination();
        } catch (final NullPointerException e) {
            ignoreExecutorReleaseBug(endpoint, e);
        }
    }

    private static void ignoreExecutorReleaseBug(final DXEndpoint endpoint, final NullPointerException e) {
        final StackTraceElement[] stackTrace = e.getStackTrace();

        if (endpoint.getState() != DXEndpoint.State.CLOSED || stackTrace.length == 0
                || !EXECUTOR_PROVIDER.equals(stackTrace[0].getClassName())) {
            throw e;
        }
    }
}
