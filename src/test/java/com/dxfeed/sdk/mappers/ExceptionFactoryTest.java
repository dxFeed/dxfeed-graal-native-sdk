// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.mappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import org.junit.Test;

/**
 * {@link ExceptionFactory}: the classes of the table, the fallback to {@link RuntimeException} and the messages.
 */
public class ExceptionFactoryTest {

    private static final List<Class<? extends Throwable>> CLASSES = List.of(
            RuntimeException.class,
            IllegalArgumentException.class,
            IllegalStateException.class,
            UnsupportedOperationException.class,
            IOException.class,
            CancellationException.class,
            TimeoutException.class
    );

    @Test
    public void everyClassOfTheTableIsCreatedWithTheMessage() {
        for (final Class<? extends Throwable> exceptionClass : CLASSES) {
            final Throwable exception = ExceptionFactory.create(exceptionClass.getName(), "boom");

            assertSame(exceptionClass, exception.getClass());
            assertEquals("boom", exception.getMessage());
            assertNull(exception.getCause());
        }
    }

    @Test
    public void theTableHasExactlyTheListedClasses() {
        assertEquals(Set.of(CLASSES.stream().map(Class::getName).toArray(String[]::new)), ExceptionFactory.classNames());
    }

    @Test
    public void aClassOfTheTableKeepsANullMessage() {
        final Throwable exception = ExceptionFactory.create("java.lang.IllegalStateException", null);

        assertSame(IllegalStateException.class, exception.getClass());
        assertNull(exception.getMessage());
    }

    @Test
    public void anExceptionOutsideTheTableIsARuntimeExceptionWithTheName() {
        final Throwable exception = ExceptionFactory.create("java.lang.ArithmeticException", "boom");

        assertSame(RuntimeException.class, exception.getClass());
        assertEquals("java.lang.ArithmeticException: boom", exception.getMessage());
    }

    @Test
    public void aClassThatIsNotAnExceptionIsARuntimeExceptionWithTheName() {
        final Throwable exception = ExceptionFactory.create("java.lang.String", "boom");

        assertSame(RuntimeException.class, exception.getClass());
        assertEquals("java.lang.String: boom", exception.getMessage());
    }

    @Test
    public void aSimpleNameIsNotResolved() {
        final Throwable exception = ExceptionFactory.create("IllegalArgumentException", "boom");

        assertSame(RuntimeException.class, exception.getClass());
        assertEquals("IllegalArgumentException: boom", exception.getMessage());
    }

    @Test
    public void anUnknownNameWithoutAMessageKeepsTheName() {
        final Throwable exception = ExceptionFactory.create("com.example.NoSuchException", null);

        assertSame(RuntimeException.class, exception.getClass());
        assertEquals("com.example.NoSuchException", exception.getMessage());
    }

    @Test
    public void aNullOrEmptyNameIsARuntimeExceptionWithTheMessage() {
        for (final String className : new String[] {null, ""}) {
            final Throwable exception = ExceptionFactory.create(className, "boom");

            assertSame(RuntimeException.class, exception.getClass());
            assertEquals("boom", exception.getMessage());
        }

        assertNull(ExceptionFactory.create(null, null).getMessage());
    }
}
