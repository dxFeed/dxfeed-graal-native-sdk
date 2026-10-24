// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.publisher;

import com.dxfeed.sdk.common.CInt32Pointer;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.OlderEntryPoint;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.WordPointer;

/**
 * The v4 twins of the functions of {@link PublisherNative}: each one calls the old function as a Java method
 * ({@link OlderEntryPoint} tells whether it failed) and returns its result through an out parameter (see
 * dxfg_v4_api.h).
 */
@CContext(PublisherDirectives.class)
public final class PublisherV4Native {

    private PublisherV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_DXPublisher_getInstance",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXPublisher_getInstance(
            final IsolateThread thread,
            @DxfgOut final WordPointer publisher
    ) {
        if (publisher.isNull()) {
            throw new IllegalArgumentException("The `publisher` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgPublisher result = PublisherNative.dxfg_DXPublisher_getInstance(thread);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        publisher.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXPublisher_publishEvents",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXPublisher_publishEvents(
            final IsolateThread thread,
            final DxfgPublisher dxfgPublisher,
            final DxfgEventTypeListPointer events
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PublisherNative.dxfg_DXPublisher_publishEvents(thread, dxfgPublisher, events);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_DXPublisher_getSubscription",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_DXPublisher_getSubscription(
            final IsolateThread thread,
            final DxfgPublisher dxfgPublisher,
            final DxfgEventClazz dxfgClazz,
            @DxfgOut final WordPointer subscription
    ) {
        if (subscription.isNull()) {
            throw new IllegalArgumentException("The `subscription` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgObservableSubscription result = PublisherNative.dxfg_DXPublisher_getSubscription(thread, dxfgPublisher, dxfgClazz);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        subscription.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableSubscription_isClosed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableSubscription_isClosed(
            final IsolateThread thread,
            final DxfgObservableSubscription dxfgObservableSubscription,
            @DxfgOut final CInt32Pointer closed
    ) {
        if (closed.isNull()) {
            throw new IllegalArgumentException("The `closed` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PublisherNative.dxfg_ObservableSubscription_isClosed(thread, dxfgObservableSubscription);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        closed.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableSubscription_getEventTypes",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableSubscription_getEventTypes(
            final IsolateThread thread,
            final DxfgObservableSubscription dxfgObservableSubscription,
            @DxfgOut final WordPointer eventTypes
    ) {
        if (eventTypes.isNull()) {
            throw new IllegalArgumentException("The `eventTypes` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final DxfgEventClazzList result = PublisherNative.dxfg_ObservableSubscription_getEventTypes(
                        thread,
                        dxfgObservableSubscription);

        if (OlderEntryPoint.failed(stored, result.isNull())) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        eventTypes.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableSubscription_containsEventType",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableSubscription_containsEventType(
            final IsolateThread thread,
            final DxfgObservableSubscription dxfgObservableSubscription,
            final DxfgEventClazz dxfgClazz,
            @DxfgOut final CInt32Pointer contains
    ) {
        if (contains.isNull()) {
            throw new IllegalArgumentException("The `contains` pointer is null");
        }

        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PublisherNative.dxfg_ObservableSubscription_containsEventType(
                        thread,
                        dxfgObservableSubscription,
                        dxfgClazz);

        if (OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)) {
            return ExceptionHandlerReturnMinusOne.EXECUTE_FAIL;
        }

        contains.write(result);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableSubscription_addChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableSubscription_addChangeListener(
            final IsolateThread thread,
            final DxfgObservableSubscription dxfgObservableSubscription,
            final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PublisherNative.dxfg_ObservableSubscription_addChangeListener(
                        thread,
                        dxfgObservableSubscription,
                        dxfgFeedEventListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }

    @CEntryPoint(
            name = "dxfg_v4_ObservableSubscription_removeChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_ObservableSubscription_removeChangeListener(
            final IsolateThread thread,
            final DxfgObservableSubscription dxfgObservableSubscription,
            final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
    ) {
        final Throwable stored = OlderEntryPoint.takeStoredException();
        final int result = PublisherNative.dxfg_ObservableSubscription_removeChangeListener(
                        thread,
                        dxfgObservableSubscription,
                        dxfgFeedEventListener);

        return OlderEntryPoint.failed(stored, result == ExceptionHandlerReturnMinusOne.EXECUTE_FAIL)
                ? ExceptionHandlerReturnMinusOne.EXECUTE_FAIL
                : result;
    }
}
