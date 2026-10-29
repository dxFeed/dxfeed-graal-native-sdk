// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

package com.dxfeed.sdk.subscription;

import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.EventType;
import com.dxfeed.sdk.NativeUtils;
import com.dxfeed.sdk.common.DxfgOut;
import com.dxfeed.sdk.events.DxfgEventTypeListPointer;
import com.dxfeed.sdk.exception.ExceptionHandlerReturnMinusOne;
import java.util.List;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.word.WordFactory;

/**
 * The functions of dxfg_v4_event_queue.h: the {@link EventQueue} of subscriptions.
 */
@CContext(EventQueueDirectives.class)
public final class EventQueueV4Native {

    // DXFG_EVENT_QUEUE_NO_EVENTS and DXFG_EVENT_QUEUE_CLOSED of dxfg_v4_event_queue.h.
    public static final int NO_EVENTS = 1;
    public static final int CLOSED = 2;

    private EventQueueV4Native() {
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_new",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_new(
            final IsolateThread ignoredThread,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription,
            @DxfgOut final WordPointer queue
    ) {
        if (queue.isNull()) {
            throw new IllegalArgumentException("The `queue` pointer is null");
        }

        queue.write(NativeUtils.MAPPER_EVENT_QUEUE.toNative(
                EventQueue.create(NativeUtils.MAPPER_SUBSCRIPTION.toJava(subscription))));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_create",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_create(
            final IsolateThread ignoredThread,
            @DxfgOut final WordPointer queue
    ) {
        if (queue.isNull()) {
            throw new IllegalArgumentException("The `queue` pointer is null");
        }

        queue.write(NativeUtils.MAPPER_EVENT_QUEUE.toNative(EventQueue.create()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_attach",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_attach(
            final IsolateThread ignoredThread,
            final DxfgEventQueueHandle queue,
            final DxfgSubscription<DXFeedSubscription<EventType<?>>> subscription,
            final long tag
    ) {
        NativeUtils.MAPPER_EVENT_QUEUE.toJava(queue).attach(NativeUtils.MAPPER_SUBSCRIPTION.toJava(subscription), tag);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_getExecutor",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_getExecutor(
            final IsolateThread ignoredThread,
            final DxfgEventQueueHandle queue,
            @DxfgOut final WordPointer executor
    ) {
        if (executor.isNull()) {
            throw new IllegalArgumentException("The `executor` pointer is null");
        }

        executor.write(NativeUtils.MAPPER_EXECUTOR.toNative(NativeUtils.MAPPER_EVENT_QUEUE.toJava(queue).getExecutor()));

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_poll",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_poll(
            final IsolateThread ignoredThread,
            final DxfgEventQueueHandle queue,
            final long timeoutInMilliseconds,
            final int limit,
            @DxfgOut final WordPointer events
    ) throws InterruptedException {
        if (events.isNull()) {
            throw new IllegalArgumentException("The `events` pointer is null");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("The `limit` is negative: " + limit);
        }

        final EventQueue javaQueue = NativeUtils.MAPPER_EVENT_QUEUE.toJava(queue);
        final List<EventType<?>> batch = javaQueue.poll(timeoutInMilliseconds, limit);

        if (batch == null || batch.isEmpty()) {
            events.write(WordFactory.nullPointer());

            return batch == null ? CLOSED : NO_EVENTS;
        }

        // Not through a generic helper: a word type cannot pass through a type parameter in a native image.
        final DxfgEventTypeListPointer nativeEvents;

        try {
            nativeEvents = NativeUtils.MAPPER_EVENTS.toNativeList(batch);
        } catch (final RuntimeException | Error e) {
            javaQueue.dropBatch(batch);
            throw e;
        }

        events.write(nativeEvents);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_pollTagged",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_pollTagged(
            final IsolateThread ignoredThread,
            final DxfgEventQueueHandle queue,
            final long timeoutInMilliseconds,
            final int limit,
            @DxfgOut final WordPointer events
    ) throws InterruptedException {
        if (events.isNull()) {
            throw new IllegalArgumentException("The `events` pointer is null");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("The `limit` is negative: " + limit);
        }

        final EventQueue javaQueue = NativeUtils.MAPPER_EVENT_QUEUE.toJava(queue);
        final EventQueue.Batch batch = javaQueue.pollTagged(timeoutInMilliseconds, limit);

        if (batch == null || batch.isEmpty()) {
            events.write(WordFactory.nullPointer());

            return batch == null ? CLOSED : NO_EVENTS;
        }

        final DxfgTaggedEventList nativeEvents;

        try {
            nativeEvents = toNativeTaggedList(batch);
        } catch (final RuntimeException | Error e) {
            javaQueue.dropBatch(batch.events());
            throw e;
        }

        events.write(nativeEvents);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_tagged_event_list_release",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_tagged_event_list_release(
            final IsolateThread ignoredThread,
            final DxfgTaggedEventList events
    ) {
        releaseTaggedList(events);

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_wakeUp",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_wakeUp(
            final IsolateThread ignoredThread,
            final DxfgEventQueueHandle queue
    ) {
        NativeUtils.MAPPER_EVENT_QUEUE.toJava(queue).wakeUp();

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_close",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_close(
            final IsolateThread ignoredThread,
            final DxfgEventQueueHandle queue
    ) {
        NativeUtils.MAPPER_EVENT_QUEUE.toJava(queue).close();

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    @CEntryPoint(
            name = "dxfg_v4_EventQueue_getDroppedEvents",
            exceptionHandler = ExceptionHandlerReturnMinusOne.class
    )
    public static int dxfg_v4_EventQueue_getDroppedEvents(
            final IsolateThread ignoredThread,
            final DxfgEventQueueHandle queue,
            @DxfgOut final CLongPointer droppedEvents
    ) {
        if (droppedEvents.isNull()) {
            throw new IllegalArgumentException("The `droppedEvents` pointer is null");
        }

        droppedEvents.write(NativeUtils.MAPPER_EVENT_QUEUE.toJava(queue).getDroppedEvents());

        return ExceptionHandlerReturnMinusOne.EXECUTE_SUCCESSFULLY;
    }

    // The events go through the list mapper of the events; its list shell is freed once its size and elements move to
    // the tagged list.
    private static DxfgTaggedEventList toNativeTaggedList(final EventQueue.Batch batch) {
        final DxfgTaggedEventList nativeEvents = UnmanagedMemory.calloc(SizeOf.get(DxfgTaggedEventList.class));

        try {
            final int size = batch.size();
            final CLongPointer tags = UnmanagedMemory.calloc(size * Long.BYTES);
            nativeEvents.setTags(tags);

            for (int i = 0; i < size; i++) {
                tags.write(i, batch.tag(i));
            }

            final DxfgEventTypeListPointer list = NativeUtils.MAPPER_EVENTS.toNativeList(batch.events());
            nativeEvents.setSize(list.getSize());
            nativeEvents.setElements(list.getElements());
            UnmanagedMemory.free(list);
        } catch (final RuntimeException | Error e) {
            releaseTaggedList(nativeEvents);
            throw e;
        }

        return nativeEvents;
    }

    // Frees the tags, then the events and the list through the list mapper of the events: the first two fields of
    // the tagged list are those of an event list.
    private static void releaseTaggedList(final DxfgTaggedEventList events) {
        if (events.isNull()) {
            return;
        }

        if (events.getTags().isNonNull()) {
            UnmanagedMemory.free(events.getTags());
            events.setTags(WordFactory.nullPointer());
        }

        NativeUtils.MAPPER_EVENTS.release(events);
    }
}
