// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.system;

import com.devexperts.io.SerialClassContext;
import com.devexperts.logging.Logging;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The allow-list of the classes that QD deserializes in the SDK: event attachments (Message, Configuration), object
 * fields of the QTP records and RMI results (ORCS) from a remote peer. The list is
 * {@value #RESOURCE}; a class outside it is rejected (the attachment is NULL in C, an RMI call fails).
 *
 * <p>QD reads the default list of a class loader from "/META-INF/SerialClassWhitelist.txt" with
 * {@link ClassLoader#getResources(String)}, which finds nothing because of the leading slash (QD 3.357), so QD
 * accepts every class. Until QD reads it, the SDK puts the context of the list into the default contexts of QD
 * ({@link SerialClassContext#getDefaultSerialContext(ClassLoader)}) before the first endpoint is created.
 */
public final class SerialClassAllowList {

    /**
     * The resource with the allowed class names: a full name or a prefix ending with "*" per line.
     */
    public static final String RESOURCE = "META-INF/SerialClassWhitelist.txt";

    private static final Logging log = Logging.getLogging(SerialClassAllowList.class);

    private static boolean installed;

    private SerialClassAllowList() {
    }

    /**
     * Installs the allow-list into the default serial class contexts of QD (once). Must be called before a QD
     * endpoint is created: a context is captured when the data is received.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }

        installed = true;

        try {
            final List<String> names = readNames(SerialClassAllowList.class.getClassLoader());
            final Map<ClassLoader, SerialClassContext> contexts = defaultContexts();

            for (final ClassLoader loader : loaders()) {
                contexts.put(loader, SerialClassContext.createSerialClassContext(loader, names, null));
            }
        } catch (final IOException | ReflectiveOperationException | RuntimeException | LinkageError e) {
            log.error("Cannot install the serialization allow-list " + RESOURCE + ", QD accepts every class", e);
        }
    }

    /**
     * Reads the class names of {@value #RESOURCE} as QD does: the lines, several names on a line separated by commas.
     */
    public static List<String> readNames(final ClassLoader loader) throws IOException {
        final List<String> names = new ArrayList<>();

        try (InputStream stream = loader.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IOException("No resource " + RESOURCE);
            }

            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

            for (String line; (line = reader.readLine()) != null; ) {
                names.addAll(Arrays.asList(line.split(",")));
            }
        }

        names.removeIf(String::isEmpty);

        if (names.isEmpty()) {
            throw new IOException("No class names in " + RESOURCE);
        }

        return names;
    }

    // The class loaders that QD resolves for the default context: the context class loader of the threads (the
    // application class loader in the native image) and the loader of QD.
    private static Set<ClassLoader> loaders() {
        final Set<ClassLoader> result = new LinkedHashSet<>();

        for (final ClassLoader loader : new ClassLoader[] {ClassLoader.getSystemClassLoader(),
                Thread.currentThread().getContextClassLoader(), SerialClassContext.class.getClassLoader()})
        {
            if (loader != null) {
                result.add(loader);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<ClassLoader, SerialClassContext> defaultContexts() throws ReflectiveOperationException {
        final Field field = SerialClassContext.class.getDeclaredField("defaultSerialContextMap");

        field.setAccessible(true);

        return (Map<ClassLoader, SerialClassContext>) field.get(null);
    }
}
