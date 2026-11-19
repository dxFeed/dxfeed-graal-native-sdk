// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.devexperts.io.Marshalled;
import com.devexperts.io.MarshallingException;
import com.devexperts.io.SerialClassContext;
import com.dxfeed.event.candle.CandleSymbol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;

/**
 * The allow-list of the classes that QD deserializes in the SDK ({@link SerialClassAllowList}): it accepts every class
 * registered for serialization in the native image except the RMI, JMX and JAAS ones, and nothing else, and it is in
 * effect in QD once installed.
 */
public class SerialClassAllowListTest {

    // Registered for serialization (JMX and RMI monitoring), never deserialized from a peer.
    private static final List<String> REJECTED_PREFIXES = List.of("java.rmi.", "javax.management.", "javax.security.");

    // The exception that an ORCS server may send (see OrcsSerializationMetadataTest).
    private static final Set<String> ALLOWED_DESPITE_THE_PREFIX = Set.of("java.rmi.RemoteException");

    /**
     * The context of the allow-list, as the SDK installs it into QD.
     */
    public static SerialClassContext allowList() throws IOException {
        final ClassLoader loader = SerialClassAllowListTest.class.getClassLoader();

        return SerialClassContext.createSerialClassContext(loader, SerialClassAllowList.readNames(loader), null);
    }

    @Test
    public void theRegisteredClassesAreAcceptedExceptRmiJmxAndJaas() throws IOException {
        final SerialClassContext allowList = allowList();
        final List<String> wrong = new ArrayList<>();

        for (final String className : registeredForSerialization()) {
            if (allowList.accept(className) != expectedToBeAccepted(className)) {
                wrong.add(className);
            }
        }

        assertTrue("Accepted or rejected by mistake: " + wrong, wrong.isEmpty());
    }

    @Test
    public void theKnownGadgetClassesAreRejected() throws IOException {
        final SerialClassContext allowList = allowList();

        for (final String className : List.of("java.rmi.dgc.VMID", "java.rmi.MarshalledObject",
                "java.rmi.server.UnicastRemoteObject", "javax.management.Attribute",
                "javax.management.BadAttributeValueExpException", "javax.security.auth.Subject",
                "javax.naming.Reference", "java.lang.reflect.Proxy", "java.lang.Runtime", "java.util.PriorityQueue",
                "java.util.concurrent.ConcurrentHashMap", "java.net.URL", "java.net.InetAddress",
                "sun.rmi.server.UnicastRef", "org.apache.commons.collections.functors.InvokerTransformer"))
        {
            assertFalse(className, allowList.accept(className));
        }
    }

    @Test
    public void qdRejectsTheClassesOutsideTheListOnceInstalled() throws IOException {
        SerialClassAllowList.install();

        final IllegalStateException exception = new IllegalStateException("boom");

        exception.setStackTrace(new StackTraceElement[0]);

        // A Marshalled received from a peer: the default serial class context of QD.
        assertEquals(42, Marshalled.forBytes(serialize(42)).getObject());
        assertEquals("java.lang.IllegalStateException: boom",
            Marshalled.forBytes(serialize(exception)).getObject().toString());
        assertEquals(CandleSymbol.valueOf("AAPL{=d}"),
            Marshalled.forBytes(serialize(CandleSymbol.valueOf("AAPL{=d}"))).getObject());

        for (final Serializable object : List.of(new javax.management.Attribute("a", 1), new java.rmi.dgc.VMID())) {
            try {
                Marshalled.forBytes(serialize(object)).getObject();
                fail("Deserialized " + object.getClass().getName());
            } catch (final MarshallingException expected) {
                // The class is rejected.
            }
        }
    }

    @Test
    public void theNamesAreReadLikeQdAndAMissingOrEmptyListFails() throws IOException {
        assertEquals(List.of("a.B", "c.*", "d.E"), SerialClassAllowList.readNames(loaderOf("a.B,c.*\n\nd.E,\n")));

        for (final String content : new String[] {null, "", "\n,\n"}) {
            try {
                SerialClassAllowList.readNames(loaderOf(content));
                fail("Read names from " + content);
            } catch (final IOException expected) {
                // No list: install() logs the error and QD keeps its default.
            }
        }
    }

    // A class loader whose allow-list resource has the content (none for null).
    private static ClassLoader loaderOf(final String content) {
        return new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(final String name) {
                return SerialClassAllowList.RESOURCE.equals(name) && content != null
                    ? new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
                    : null;
            }
        };
    }

    private static byte[] serialize(final Serializable object) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }

        return bytes.toByteArray();
    }

    private static boolean expectedToBeAccepted(final String className) {
        return ALLOWED_DESPITE_THE_PREFIX.contains(className) ||
            REJECTED_PREFIXES.stream().noneMatch(className::startsWith);
    }

    // serialization-config.json and the serializable types of reachability-metadata.json, without the arrays (QD
    // accepts the arrays, the classes of their elements are checked).
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

        result.removeIf(className -> className.endsWith("[]") || className.startsWith("["));
        assertFalse("No classes registered for serialization", result.isEmpty());

        return result;
    }

    private static JsonNode read(final String fileName) throws IOException {
        final String resource = "META-INF/native-image/" + fileName;

        try (InputStream stream = SerialClassAllowListTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertTrue("No resource " + resource, stream != null);

            return new ObjectMapper().readTree(stream);
        }
    }
}
