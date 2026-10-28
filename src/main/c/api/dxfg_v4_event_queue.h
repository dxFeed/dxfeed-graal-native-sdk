// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief The event queue: the events of a subscription received on the application's own thread, without
 * callbacks (see the group @ref V4_EventQueue).
 */

#ifndef DXFG_V4_EVENT_QUEUE_H
#define DXFG_V4_EVENT_QUEUE_H

#include "dxfg_v4_common.h"
#include "dxfg_v4_events.h"
#include "dxfg_v4_javac.h"
#include "dxfg_v4_subscription.h"

#ifdef __cplusplus
extern "C" {
#endif

/** @defgroup V4_EventQueue Event queue (v4)
 *  @brief The events of a subscription received on the application's own thread, without callbacks.
 *
 *  An event queue is created on a subscription (dxfg_v4_EventQueue_new()) and the application drains it
 *  (dxfg_v4_EventQueue_drain()) on a thread of its own: the call waits for events and returns them as a list. No thread
 *  of the SDK calls the application, which suits the languages that cannot take a call from a foreign thread.
 *
 *  The queue becomes the executor of the subscription (see dxfg_v4_DXFeedSubscription_setExecutor()): the SDK
 *  retrieves the events from its buffers only when a drain runs, on the draining thread. So the queue keeps no copy of
 *  the events, and until the next drain, as with a listener that is slow to return:
 *  - The lasting events (e.g. Quote, Trade, Summary, Profile) are conflated: the SDK keeps the last event of each
 *    symbol, and none is counted as dropped.
 *  - The other events (e.g. TimeAndSale) wait in a buffer of the subscription, up to 1000000 events by default. Beyond
 *    it, the oldest ones are dropped and counted (dxfg_v4_EventQueue_getDroppedEvents()). The limit is the system
 *    property `com.devexperts.qd.impl.matrix.Agent.MaxBufferSize`, set with dxfg_v4_system_set_property() before the
 *    first endpoint of the isolate is created; it applies to all the subscriptions and connections of the isolate.
 *  - An endpoint with the role #DXFG_ENDPOINT_ROLE_STREAM_FEED drops nothing: once the buffer of a subscription is
 *    full, the endpoint stops receiving data, also for its other subscriptions, until the queue is drained.
 *
 *  Rules:
 *  - Create the queue before the symbols are added to the subscription, and do not set an executor on the subscription:
 *    a subscription with symbols, an executor or a queue cannot get a queue, and the executor of a subscription with
 *    a queue cannot be changed (dxfg_v4_DXFeedSubscription_setExecutor() and
 *    dxfg_v4_SubscriptionController_setExecutor() fail with `IllegalStateException`).
 *  - One consumer: the drains of a queue are serialized, a drain waits for the one in progress on another thread.
 *    Drain a queue from one thread at a time, so that a snapshot of indexed or time-series events is not split.
 *  - The event listeners added to the subscription with a queue are called by the drain, on the draining thread, in
 *    the order of the SDK, and on the thread that closes the queue or detaches the subscription. Do not drain the
 *    queue from such a listener: the drain fails. A listener that fails makes the drain fail too. To receive the same
 *    events by a listener and by a queue, use two subscriptions: the subscriptions of a feed, with or without a queue,
 *    do not affect each other.
 *  - A list returned by a drain is the caller's: it does not depend on the queue, the subscription or the endpoint,
 *    and any thread attached to the isolate may release it (dxfg_v4_event_type_list_release()).
 *  - The models (the transaction models and the event models, e.g. dxfg_v4_IndexedEventModel_new()) have no queue: to
 *    have their listeners called on a thread of your own, give them an executor created by
 *    dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_new() and call
 *    dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks() on that thread.
 *
 *  A drain ends when events arrive, when the timeout elapses, when the queue is woken up (dxfg_v4_EventQueue_wakeUp())
 *  and when the queue is closed. The queue is closed by dxfg_v4_EventQueue_close(), by the release of its handle, by
 *  the close of the subscription, by dxfg_v4_NativeListener_deactivateAll() and by dxfg_v4_system_shutdown(); the last
 *  two also close the queues created after them. Closing an endpoint does not close a queue and does not end a drain:
 *  an application that closes the endpoints itself should drain with a finite timeout. While an open queue holds
 *  events that were not drained, dxfg_v4_DXEndpoint_closeAndAwaitTermination() of the endpoint of its subscription
 *  waits until the queue is drained or closed. A closed queue discards the events of its subscription and counts them
 *  as dropped. dxfg_v4_DXFeed_detachSubscriptionAndClear() does not wait for a drain: the events that are delivered
 *  while it runs arrive in the next drain.
 *
 *  The functions are functions of the SDK: there are no QD methods that they call.
 *  @{
 */

/**
 * @brief The handle of an event queue (dxfg_v4_EventQueue_new()).
 * @since 3.13.0
 */
typedef struct dxfg_v4_event_queue_t {
    /** The handle of the Java object. */
    dxfg_java_object_handler handler;
} dxfg_v4_event_queue_t;

/**
 * @brief The result of dxfg_v4_EventQueue_drain() when no events arrived before the timeout or the queue was woken up.
 *
 * The value is 1. It is not an error: the list is NULL and no exception is pending.
 * @since 3.13.0
 */
#define DXFG_EVENT_QUEUE_NO_EVENTS ((int32_t)1)

/**
 * @brief The result of dxfg_v4_EventQueue_drain() when the queue is closed.
 *
 * The value is 2. It is not an error: the list is NULL and no exception is pending. Every later drain returns it at
 * once.
 * @since 3.13.0
 */
#define DXFG_EVENT_QUEUE_CLOSED ((int32_t)2)

/**
 * Creates an event queue on the subscription.
 *
 * Create it before the symbols are added to the subscription. The queue stays the executor of the subscription for
 * the life of the subscription, also after the queue is closed.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] sub The subscription; it must have no symbols, no executor and no queue.
 * @param[out] queue The pointer to the variable that receives the queue (release it with
 * dxfg_v4_JavaObjectHandler_release(), which also closes it); must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error
 * (`IllegalStateException` if the subscription has symbols, an executor or a queue). Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_new(graal_isolatethread_t *thread, dxfg_v4_subscription_t *sub,
                               DXFG_OUT dxfg_v4_event_queue_t **queue);

/**
 * Waits for the events of the subscription and returns them.
 *
 * The SDK retrieves the events on the calling thread and calls the event listeners of the subscription on it. After
 * the first events, the drain only completes the retrieval that is already pending, so a steady flow of events does
 * not keep it from returning. A batch may exceed the limit: the limit stops the retrieval, it does not split it.
 *
 * Compare dxfg_v4_EventQueue_getDroppedEvents() after each drain to detect a gap before the returned events.
 *
 * If the events cannot be converted to the C structures, or an event listener of the subscription fails during the
 * drain, the function fails, the events it has retrieved are dropped and counted, and the next drain continues with
 * the next events.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue.
 * @param[in] timeoutInMilliseconds How long to wait for events: 0 returns at once; a negative value, or one above
 * `INT64_MAX / 1000000` (about 292 years), waits until events arrive, the queue is woken up or closed.
 * @param[in] limit The number of events after which no more events are retrieved, 0 for no limit; must not be
 * negative.
 * @param[out] events The pointer to the variable that receives the list of events (release it with
 * dxfg_v4_event_type_list_release()), or NULL if the result is not 0; must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) with a non-empty list, #DXFG_EVENT_QUEUE_NO_EVENTS (1) if no events arrived
 * before the timeout or the queue was woken up, #DXFG_EVENT_QUEUE_CLOSED (2) if the queue is closed, or
 * #DXFG_EXECUTE_FAIL (-1) on error (also if called from an event listener of the subscription during a drain of the
 * same queue, or if the waiting thread is interrupted). Use dxfg_v4_get_and_clear_thread_exception() to determine if an
 * exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_drain(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue,
                                 int64_t timeoutInMilliseconds, int32_t limit,
                                 DXFG_OUT dxfg_v4_event_type_list **events);

/**
 * Makes the drain in progress, or the next drain if none is in progress, return #DXFG_EVENT_QUEUE_NO_EVENTS at once
 * (a drain that has events returns them instead). The wake-up is used by one drain: several calls before a drain wake
 * up one drain.
 *
 * Use it to stop a thread that drains with a long or infinite timeout without closing the queue.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_wakeUp(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue);

/**
 * Closes the queue: the drain in progress returns the events it has already retrieved or #DXFG_EVENT_QUEUE_CLOSED,
 * every later drain returns #DXFG_EVENT_QUEUE_CLOSED. The events that were not drained are discarded and counted as
 * dropped, and so are the events of the subscription that arrive later. The function does not wait for the drains in
 * progress. Closing a closed queue does nothing.
 *
 * Releasing the handle of the queue (dxfg_v4_JavaObjectHandler_release()) closes it as well.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_close(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue);

/**
 * Returns the number of the events of the subscription that were lost: the events that the SDK dropped as of the
 * last retrieval (see `DXFeedSubscription.getTotalDroppedEvents()`), plus the events that the queue dropped itself
 * (a batch that failed to convert, the events of a closed queue).
 *
 * If the number grew since the previous drain, events were lost before the events that the last drain
 * returned. For the indexed and time-series events (e.g. Order, Candle) the state built from the earlier events is then
 * incomplete: subscribe again, or wait for the next snapshot.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue.
 * @param[out] droppedEvents The pointer to the variable that receives the number; must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_getDroppedEvents(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue,
                                            DXFG_OUT int64_t *droppedEvents);

/** @} */

#ifdef __cplusplus
}
#endif

#endif // DXFG_V4_EVENT_QUEUE_H
