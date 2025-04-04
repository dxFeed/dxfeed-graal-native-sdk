// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.publisher;

import static com.dxfeed.sdk.NativeUtils.MAPPER_EVENTS;
import static com.dxfeed.sdk.NativeUtils.MAPPER_EVENT_TYPES;
import static com.dxfeed.sdk.NativeUtils.MAPPER_OBSERVABLE_SUBSCRIPTION;
import static com.dxfeed.sdk.NativeUtils.MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER;
import static com.dxfeed.sdk.NativeUtils.MAPPER_PUBLISHER;
import static com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;

import com.dxfeed.api.DXPublisher;
import com.dxfeed.sdk.events.DxfgEventClazz;
import com.dxfeed.sdk.events.DxfgEventClazzList;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.events.DxfgObservableSubscriptionChangeListener;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnNullWord;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

@CContext(Directives.class)
public class PublisherNative {

    @CEntryPoint(
            name = "dxfg_DXPublisher_getInstance",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgPublisher dxfg_DXPublisher_getInstance(
            final IsolateThread ignoredThread
    ) {
        return MAPPER_PUBLISHER.toNative(DXPublisher.getInstance());
    }

    @CEntryPoint(
            name = "dxfg_DXPublisher_publishEvents",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_DXPublisher_publishEvents(
            final IsolateThread ignoredThread,
            final DxfgPublisher dxfgPublisher,
            final DxfgEventTypeListPointer events
    ) {
        MAPPER_PUBLISHER.toJava(dxfgPublisher)
                .publishEvents(MAPPER_EVENTS.toJavaList(events));
        return EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_DXPublisher_getSubscription",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgObservableSubscription dxfg_DXPublisher_getSubscription(
            final IsolateThread ignoredThread,
            final DxfgPublisher dxfgPublisher,
            final DxfgEventClazz dxfgClazz
    ) {
        return MAPPER_OBSERVABLE_SUBSCRIPTION.toNative(
                MAPPER_PUBLISHER.toJava(dxfgPublisher).getSubscription(dxfgClazz.clazz));
    }

    @CEntryPoint(
            name = "dxfg_ObservableSubscription_isClosed",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_ObservableSubscription_isClosed(
            final IsolateThread ignoredThread,
            final DxfgObservableSubscription dxfgObservableSubscription
    ) {
        return MAPPER_OBSERVABLE_SUBSCRIPTION.toJava(dxfgObservableSubscription).isClosed() ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_ObservableSubscription_getEventTypes",
            exceptionHandler = ExceptionHandlerReturnNullWord.class
    )
    public static DxfgEventClazzList dxfg_ObservableSubscription_getEventTypes(
            final IsolateThread ignoredThread,
            final DxfgObservableSubscription dxfgObservableSubscription
    ) {
        return MAPPER_EVENT_TYPES.toNativeList(
                MAPPER_OBSERVABLE_SUBSCRIPTION.toJava(dxfgObservableSubscription).getEventTypes());
    }

    @CEntryPoint(
            name = "dxfg_ObservableSubscription_containsEventType",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_ObservableSubscription_containsEventType(
            final IsolateThread ignoredThread,
            final DxfgObservableSubscription dxfgObservableSubscription,
            final DxfgEventClazz dxfgClazz
    ) {
        return MAPPER_OBSERVABLE_SUBSCRIPTION.toJava(dxfgObservableSubscription)
                .containsEventType(dxfgClazz.clazz) ? 1 : 0;
    }

    @CEntryPoint(
            name = "dxfg_ObservableSubscription_addChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_ObservableSubscription_addChangeListener(
            final IsolateThread ignoreThread,
            final DxfgObservableSubscription dxfgObservableSubscription,
            final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
    ) {
        MAPPER_OBSERVABLE_SUBSCRIPTION.toJava(dxfgObservableSubscription).addChangeListener(
                MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toJava(dxfgFeedEventListener)
        );
        return EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_ObservableSubscription_removeChangeListener",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_ObservableSubscription_removeChangeListener(
            final IsolateThread ignoreThread,
            final DxfgObservableSubscription dxfgObservableSubscription,
            final DxfgObservableSubscriptionChangeListener dxfgFeedEventListener
    ) {
        MAPPER_OBSERVABLE_SUBSCRIPTION.toJava(dxfgObservableSubscription).removeChangeListener(
                MAPPER_OBSERVABLE_SUBSCRIPTION_CHANGE_LISTENER.toJava(dxfgFeedEventListener));
        return EXECUTE_SUCCESSFULLY;
    }
}
