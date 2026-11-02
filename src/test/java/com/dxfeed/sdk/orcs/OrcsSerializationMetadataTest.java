// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.orcs;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;

/**
 * The classes that an ORCS server sends to PriceLevelService clients over RMI are registered for serialization in the
 * native image. Without the registration, an exception of the server (for example, IllegalArgumentException for an
 * unsupported order source) reaches C as UndeclaredThrowableException over java.lang.Exception instead of itself.
 */
public class OrcsSerializationMetadataTest {

    // The serialization whitelist of the ORCS server (SerialClassWhitelist.txt) without its own classes (PLData,
    // AuthException, ErrorCode), which a client does not have either. The whitelist is the RMI vocabulary of ORCS; an
    // exception of a class outside it still reaches the client as java.lang.Exception. Update the list with the
    // whitelist.
    private static final List<String> CLASSES_FROM_THE_SERVER = List.of(
            "com.devexperts.auth.AuthToken",
            "com.devexperts.io.MarshallingException",
            "com.devexperts.rmi.RMIException",
            "com.devexperts.rmi.RMIExceptionType",
            "com.devexperts.util.InvalidFormatException",
            "com.dxfeed.event.candle.CandleSymbol",
            "com.dxfeed.event.market.MarketEvent",
            "com.dxfeed.event.market.Order",
            "com.dxfeed.event.market.OrderBase",
            "com.dxfeed.event.market.Quote",
            "com.dxfeed.orcs.api.AuthOrderSource",
            "com.dxfeed.orcs.qd.PriceLevelCandle",
            "java.io.EOFException",
            "java.io.IOException",
            "java.io.InvalidClassException",
            "java.io.ObjectStreamException",
            "java.io.UTFDataFormatException",
            "java.io.UncheckedIOException",
            "java.lang.Enum",
            "java.lang.Exception",
            "java.lang.IllegalArgumentException",
            "java.lang.IllegalStateException",
            "java.lang.IndexOutOfBoundsException",
            "java.lang.Integer",
            "java.lang.NegativeArraySizeException",
            "java.lang.NullPointerException",
            "java.lang.Number",
            "java.lang.NumberFormatException",
            "java.lang.RuntimeException",
            "java.lang.SecurityException",
            "java.lang.StackTraceElement",
            "java.lang.Throwable",
            "java.lang.UnsupportedOperationException",
            "java.rmi.RemoteException",
            "java.util.ArrayList",
            "java.util.Collections$EmptyList",
            "java.util.Collections$EmptyMap",
            "java.util.ConcurrentModificationException",
            "java.util.HashMap",
            "java.util.HashSet");

    @Test
    public void theClassesFromTheServerAreRegisteredForSerialization() throws IOException {
        final Set<String> registered = registeredForSerialization();
        final List<String> missing = new ArrayList<>();

        for (final String className : CLASSES_FROM_THE_SERVER) {
            if (!registered.contains(className)) {
                missing.add(className);
            }
        }

        assertTrue("Not registered for serialization: " + missing, missing.isEmpty());
    }

    @Test
    public void theClassesFromTheServerAreOnTheClassPath() {
        final List<String> absent = new ArrayList<>();

        for (final String className : CLASSES_FROM_THE_SERVER) {
            try {
                Class.forName(className, false, getClass().getClassLoader());
            } catch (final ClassNotFoundException e) {
                absent.add(className);
            }
        }

        assertTrue("Not on the class path: " + absent, absent.isEmpty());
    }

    // serialization-config.json and the serializable types of reachability-metadata.json.
    private static Set<String> registeredForSerialization() throws IOException {
        final Set<String> result = new TreeSet<>();

        for (final JsonNode type : read("serialization-config.json").path("types")) {
            result.add(type.path("name").asText());
        }

        for (final JsonNode type : read("reachability-metadata.json").path("reflection")) {
            if (type.path("serializable").asBoolean(false) && type.path("type").isTextual()) {
                result.add(type.path("type").asText());
            }
        }

        return result;
    }

    private static JsonNode read(final String fileName) throws IOException {
        final String resource = "META-INF/native-image/" + fileName;

        try (InputStream stream = OrcsSerializationMetadataTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertTrue("No resource " + resource, stream != null);

            return new ObjectMapper().readTree(stream);
        }
    }
}
