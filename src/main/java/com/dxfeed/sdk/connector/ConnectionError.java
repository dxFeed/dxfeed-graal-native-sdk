// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.connector;

/**
 * The last connection error of a message connector or an endpoint: copied values, no reference to the exception.
 *
 * <p>The categories are the values of {@code DXFG_V4_CONNECTION_ERROR_*} in dxfg_v4_message_connector.h.
 */
public final class ConnectionError {

    /**
     * The category could not be determined.
     */
    public static final int CATEGORY_OTHER = 0;

    /**
     * The host name could not be resolved.
     */
    public static final int CATEGORY_DNS = 1;

    /**
     * The connect failed: refused, unreachable, or timed out in the operating system.
     */
    public static final int CATEGORY_CONNECT_FAILED = 2;

    /**
     * Nothing was read from an established connection within the socket read timeout.
     */
    public static final int CATEGORY_READ_TIMEOUT = 3;

    /**
     * No heartbeat or data came within the heartbeat timeout of the connection.
     */
    public static final int CATEGORY_HEARTBEAT_TIMEOUT = 4;

    /**
     * The remote side closed the connection.
     */
    public static final int CATEGORY_CLOSED_BY_REMOTE = 5;

    /**
     * A TLS handshake or record failed.
     */
    public static final int CATEGORY_TLS = 6;

    /**
     * Another I/O error, e.g. a connection reset.
     */
    public static final int CATEGORY_IO = 7;

    private final long time;
    private final String address;
    private final int category;
    private final boolean dropped;
    private final String exceptionClassName;
    private final String message;
    private final long count;

    ConnectionError(final long time, final String address, final int category, final boolean dropped,
            final String exceptionClassName, final String message, final long count) {
        this.time = time;
        this.address = address;
        this.category = category;
        this.dropped = dropped;
        this.exceptionClassName = exceptionClassName;
        this.message = message;
        this.count = count;
    }

    /**
     * @return the time of the error, in milliseconds since the epoch (UTC)
     */
    public long getTime() {
        return time;
    }

    /**
     * @return the address the connector was configured with, credentials masked (the whole list of a multi-host
     * address, not the host that failed)
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return one of the {@code CATEGORY_*} values
     */
    public int getCategory() {
        return category;
    }

    /**
     * @return {@code true} if an established connection was dropped, {@code false} if a connect attempt failed
     */
    public boolean isDropped() {
        return dropped;
    }

    /**
     * @return the class name of the exception, or {@code null} when there is none (a heartbeat timeout)
     */
    public String getExceptionClassName() {
        return exceptionClassName;
    }

    /**
     * @return the message of the exception (or of the log record when there is no exception), credentials masked and
     * bounded in length; {@code null} when there is none
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the number of errors of the connector up to this one, since the SDK started to watch it
     */
    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "ConnectionError{time=" + time + ", address=" + address + ", category=" + category + ", dropped="
                + dropped + ", exceptionClassName=" + exceptionClassName + ", message=" + message + ", count=" + count
                + "}";
    }
}
