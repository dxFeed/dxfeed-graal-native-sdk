// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Creates a Java exception from the name of its class and a message, for the functions of the C API that fail a
 * promise. The native image cannot create a class by its name without reflection metadata, so the classes come from a
 * fixed table of constructors; any other name gives a {@link RuntimeException} whose message keeps the name.
 */
public final class ExceptionFactory {

    private static final Map<String, Function<String, Throwable>> CONSTRUCTORS = Map.of(
            "java.lang.RuntimeException", RuntimeException::new,
            "java.lang.IllegalArgumentException", IllegalArgumentException::new,
            "java.lang.IllegalStateException", IllegalStateException::new,
            "java.lang.UnsupportedOperationException", UnsupportedOperationException::new,
            "java.io.IOException", IOException::new,
            "java.util.concurrent.CancellationException", CancellationException::new,
            "java.util.concurrent.TimeoutException", TimeoutException::new
    );

    private ExceptionFactory() {
    }

    /**
     * The fully qualified names of the classes that {@link #create(String, String)} creates as they are.
     *
     * @return The names.
     */
    public static Set<String> classNames() {
        return CONSTRUCTORS.keySet();
    }

    /**
     * Creates an exception of the named class with the message. A name outside {@link #classNames()} (a class of
     * another kind, not a Throwable, or no class at all) gives a {@link RuntimeException} with the message
     * {@code className + ": " + message}, or {@code className} when the message is null. A null or empty name gives a
     * {@link RuntimeException} with the message as is.
     *
     * @param className The fully qualified name of the class (e.g. {@code java.lang.IllegalArgumentException}), or
     *                  null.
     * @param message   The message of the exception, or null.
     * @return The new exception; never null.
     */
    public static Throwable create(final String className, final String message) {
        if (className == null || className.isEmpty()) {
            return new RuntimeException(message);
        }

        final Function<String, Throwable> constructor = CONSTRUCTORS.get(className);

        if (constructor != null) {
            return constructor.apply(message);
        }

        return new RuntimeException(message == null ? className : className + ": " + message);
    }
}
