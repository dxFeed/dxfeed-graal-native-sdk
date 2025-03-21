// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.candlewebservice;

import com.devexperts.io.ByteArrayInput;
import com.devexperts.io.ByteArrayOutput;
import com.devexperts.io.StreamCompression;
import com.devexperts.io.URLInputStream;
import com.devexperts.qd.DataIterator;
import com.devexperts.qd.DataRecord;
import com.devexperts.qd.QDContract;
import com.devexperts.qd.QDFactory;
import com.devexperts.qd.ng.RecordCursor;
import com.devexperts.qd.ng.RecordSource;
import com.devexperts.qd.qtp.AbstractQTPParser;
import com.devexperts.qd.qtp.BinaryQTPParser;
import com.devexperts.qd.qtp.MessageConsumerAdapter;
import com.devexperts.qd.qtp.MessageType;
import com.devexperts.qd.qtp.text.TextDelimiters;
import com.devexperts.qd.qtp.text.TextQTPParser;
import com.devexperts.services.Services;
import com.dxfeed.api.impl.EventDelegate;
import com.dxfeed.api.impl.EventDelegateFactory;
import com.dxfeed.event.EventType;
import com.dxfeed.event.TimeSeriesEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an endpoint for accessing historical data, supporting configurable parameters such as compression and data
 * format. This class provides functionality to retrieve and process time-series data using a flexible API.
 */
public class HistoryEndpoint {

    /**
     * The Compression enum represents different compression algorithms that can be applied to data during transmission
     * or storage.
     * <p>
     * It provides three types of compression:
     * <ul>
     *     <li><code>NONE</code>: No compression is applied.
     *     <li><code>ZIP</code>: ZIP compression is applied.
     *     <li><code>GZIP</code>: GZIP compression is applied.
     * </ul>
     * <p>
     * This enum is used to dictate the type of compression to be applied
     * in various data handling scenarios.
     */
    public enum Compression {
        NONE,
        ZIP,
        GZIP;

        /**
         * Returns a string representation of this compression type.
         *
         * @return the string representation of the compression type: "none" for NONE, "zip" for ZIP, and "gzip" for
         * GZIP.
         */
        public String toString() {
            return switch (this) {
                case NONE -> "none";
                case ZIP -> "zip";
                case GZIP -> "gzip";
            };
        }
    }

    /**
     * The Format enum represents different formats that can be used to handle data. It provides three types of
     * formats:
     * <p>
     * It provides three types of compression:
     * <ul>
     *     <li><code>TEXT</code>: Data is represented as plain text.
     *     <li><code>CSV</code>: Data is represented as comma-separated values.
     *     <li><code>BINARY</code>: Data is represented in binary format.
     * </ul>
     * <p>
     * This enum is utilized to specify the format in various data transmission and processing scenarios.
     */
    public enum Format {
        TEXT,
        CSV,
        BINARY;

        /**
         * Returns the string representation of the enum constant for this format.
         *
         * @return the string representation of the format: "text" for TEXT, "csv" for CSV, and "binary" for BINARY.
         */
        public String toString() {
            return switch (this) {
                case TEXT -> "text";
                case CSV -> "csv";
                case BINARY -> "binary";
            };
        }
    }

    /**
     * Builder is a static inner class that provides a flexible and readable way to construct instances of the
     * HistoryEndpoint class.
     * <p>
     * This class follows the builder pattern, allowing users to specify various configuration parameters for a
     * HistoryEndpoint instance through fluent method chaining. The final built object is created using the
     * {@link #build()} method.
     */
    public static class Builder {

        private String address;
        private String userName;
        private String password;
        private String authToken;
        private Compression compression = Compression.NONE;
        private Format format = Format.TEXT;

        /**
         * Specifies the address for the target endpoint.
         *
         * @param address the address of the endpoint to be set
         * @return the Builder instance with the updated address value
         */
        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        /**
         * Sets the username for the target endpoint.
         *
         * @param userName the username to be set for the endpoint
         * @return the Builder instance with the updated username value
         */
        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        /**
         * Sets the password for the target endpoint.
         *
         * @param password the password to be set for the endpoint
         * @return the Builder instance with the updated password value
         */
        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the authentication token for the target endpoint.
         *
         * @param authToken the authentication token to be used for access
         * @return the Builder instance with the updated authentication token value
         */
        public Builder withAuthToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        /**
         * Sets the compression type to be used for data transmission or storage.
         *
         * @param compression the compression type to be applied, represented by the {@link Compression} enum
         * @return the Builder instance with the updated compression value
         */
        private Builder withCompression(Compression compression) {
            this.compression = compression;
            return this;
        }

        /**
         * Sets the format to be used for data handling.
         *
         * @param format the format type to be applied, represented by the {@link Format} enum
         * @return the Builder instance with the updated format value
         */
        private Builder withFormat(Format format) {
            this.format = format;
            return this;
        }

        /**
         * Builds and returns a configured instance of {@code HistoryEndpoint}.
         * <p>
         * This method uses the values set in the {@code Builder} instance, such as address, username, password,
         * compression, format, and authentication token, to create a new {@code HistoryEndpoint} object.
         *
         * @return a new instance of {@code HistoryEndpoint} configured with the provided settings
         */
        public HistoryEndpoint build() {
            return new HistoryEndpoint(address, userName, password, compression, format, authToken);
        }
    }

    /**
     * Creates a new instance of {@link HistoryEndpoint.Builder} with default configurations. The default settings
     * include:
     * <ul>
     *     <li>{@link Compression#ZIP} as the compression type.
     *     <li>{@link Format#BINARY} as the data format.
     * </ul>
     *
     * @return a new {@link HistoryEndpoint.Builder} instance with preset default values
     */
    public static Builder newBuilder() {
        return new Builder()
                .withCompression(Compression.ZIP)
                .withFormat(Format.BINARY);
    }

    private final String address;
    private final String userName;
    private final String password;
    private final String token;
    private final Compression compression;
    private final Format format;
    private final DateFormat serviceDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");

    private HistoryEndpoint(String address, String userName, String password, Compression compression, Format format,
            String token) {
        this.address = address;
        this.userName = userName;
        this.password = password;
        this.compression = compression;
        this.format = format;
        this.token = token;
    }

    /**
     * Retrieves a list of time series events for a specific type of event and symbol within the given time range.
     *
     * @param <E>       the subclass of {@link TimeSeriesEvent} that specifies the type of event to retrieve
     * @param eventType the class object representing the type of event to retrieve
     * @param symbol    the identifier of the symbol for which the time series data is requested
     * @param from      the start timestamp for the time series query, in milliseconds since epoch
     * @param to        the end timestamp for the time series query, in milliseconds since epoch
     * @return a list of events of the specified type and symbol within the given time range
     * @throws IOException if an error occurs while fetching or processing the data
     */
    public <E extends TimeSeriesEvent<?>> List<E> getTimeSeries(Class<? extends E> eventType, Object symbol, Long from,
            Long to) throws IOException {
        String fullAddress = getFullAddress(eventType, Collections.singleton(symbol), from, to);
        byte[] bytes = responseToBytes(fullAddress);

        return getEventsFromBytes(bytes, createDelegate(eventType), createParser(format));
    }

    private <E extends TimeSeriesEvent<?>> String getFullAddress(Class<? extends E> eventType,
            Collection<Object> symbols, Long from, Long to) {
        return address + "?" +
                "records=" + eventType.getSimpleName() +
                "&symbols=" + new HashSet<>(symbols).stream().map(Object::toString).collect(Collectors.joining(",")) +
                "&start=" + serviceDateFormat.format(new Date(from)) +
                "&stop=" + serviceDateFormat.format(new Date(to)) +
                "&format=" + format +
                "&compression=" + compression;
    }

    private AbstractQTPParser createParser(Format format) {
        switch (format) {
            case TEXT -> {
                return new TextQTPParser(QDFactory.getDefaultScheme(), MessageType.STREAM_DATA);
            }
            case CSV -> {
                TextQTPParser parser = new TextQTPParser(QDFactory.getDefaultScheme(), MessageType.STREAM_DATA);
                parser.setDelimiters(TextDelimiters.COMMA_SEPARATED);
                return parser;
            }
            case BINARY -> {
                return new BinaryQTPParser(QDFactory.getDefaultScheme());
            }
            default -> throw new IllegalArgumentException("Unexpected format " + format);
        }
    }

    private byte[] responseToBytes(String urlStr) throws IOException {
        URLConnection connection = URLInputStream.openConnection(URLInputStream.resolveURL(urlStr), userName, password);

        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }

        return zipToByteArray(connection);
    }

    private byte[] zipToByteArray(URLConnection connection) throws IOException {
        StreamCompression streamCompression = createCompression();

        try (InputStream inputStream = streamCompression.decompress(
                connection.getInputStream()); ByteArrayOutput bao = new ByteArrayOutput()) {
            int read;
            byte[] data = new byte[16384];

            while ((read = inputStream.read(data, 0, data.length)) != -1) {
                bao.write(data, 0, read);
            }

            return bao.toByteArray();
        }
    }

    private StreamCompression createCompression() {
        return switch (compression) {
            case NONE -> StreamCompression.NONE;
            case ZIP -> StreamCompression.ZIP;
            case GZIP -> StreamCompression.GZIP;
        };
    }

    @SuppressWarnings("unchecked")
    private <E extends TimeSeriesEvent<?>> List<E> getEventsFromBytes(byte[] bytes, EventDelegate<?> delegate,
            AbstractQTPParser parser) {
        parser.setInput(new ByteArrayInput(bytes));

        List<E> result = new ArrayList<>();

        parser.parse(new MessageConsumerAdapter() {
            public void processData(DataIterator iterator, MessageType message) {
                RecordSource source = (RecordSource) iterator;
                RecordCursor cursor;
                while ((cursor = source.next()) != null) {
                    EventType<?> event = delegate.createEvent(cursor);
                    if (event != null) {
                        result.add((E) event);
                    }
                }
            }
        });

        return result;
    }

    private <E extends TimeSeriesEvent<?>> EventDelegate<?> createDelegate(Class<? extends E> eventType) {
        EventDelegate<?> delegate = null;
        DataRecord dataRecord = QDFactory.getDefaultScheme().findRecordByName(eventType.getSimpleName());

        for (EventDelegateFactory factory : Services.createServices(EventDelegateFactory.class, null)) {
            Collection<EventDelegate<?>> cDelegates = factory.createDelegates(dataRecord);

            if (cDelegates == null) {
                continue;
            }

            for (EventDelegate<?> cDelegate : cDelegates) {
                if (cDelegate.getContract() == QDContract.HISTORY && cDelegate.getEventType() == eventType) {
                    delegate = cDelegate;
                }
            }
        }

        return delegate;
    }
}
