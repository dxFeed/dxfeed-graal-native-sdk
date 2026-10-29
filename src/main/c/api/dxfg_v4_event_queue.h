// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief The event queue: the events of subscriptions received on the application's own thread, without
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
 *  @brief The events of subscriptions received on the application's own thread, without callbacks.
 *
 *  The application polls an event queue (dxfg_v4_EventQueue_poll(), dxfg_v4_EventQueue_pollTagged()) on a thread
 *  of its own: the call waits for events and returns them as a list. No thread of the SDK calls the application, which
 *  suits the languages that cannot take a call from a foreign thread.
 *
 *  A queue has the subscriptions attached to it as its sources:
 *  - dxfg_v4_EventQueue_new() creates a queue bound to one subscription: the queue closes when the subscription
 *    closes, and nothing else can be attached to it. Its events have the tag 0.
 *  - dxfg_v4_EventQueue_create() creates a shared queue, and dxfg_v4_EventQueue_attach() attaches subscriptions to
 *    it, each with a tag of the application's choice (tags need not be unique). dxfg_v4_EventQueue_pollTagged()
 *    returns each event with the tag of its subscription, so one thread serves many subscriptions. A subscription
 *    leaves the queue only by being closed: then the queue stays open, a poll in progress goes on, the events the
 *    subscription delivered before stay in the queue, and its later events are discarded and counted as dropped.
 *
 *  An attached subscription gets an executor of the queue (see dxfg_v4_DXFeedSubscription_setExecutor()): the SDK
 *  retrieves the events from its buffers only when a poll runs, on the polling thread. So the queue keeps no copy of
 *  the events, and until the next poll, as with a listener that is slow to return:
 *  - The lasting events (e.g. Quote, Trade, Summary, Profile) are conflated: the SDK keeps the last event of each
 *    symbol, and none is counted as dropped.
 *  - The other events (e.g. TimeAndSale) wait in a buffer of the subscription, up to 1000000 events by default. Beyond
 *    it, the oldest ones are dropped and counted (dxfg_v4_EventQueue_getDroppedEvents()). The limit is the system
 *    property `com.devexperts.qd.impl.matrix.Agent.MaxBufferSize`, set with dxfg_v4_system_set_property() before the
 *    first endpoint of the isolate is created; it applies to all the subscriptions and connections of the isolate.
 *  - An endpoint with the role #DXFG_ENDPOINT_ROLE_STREAM_FEED drops nothing: once the buffer of a subscription is
 *    full, the endpoint stops receiving data, also for its other subscriptions, until the queue is polled.
 *
 *  The models and the instrument profile collector run on the queue too: give them the executor of a shared queue
 *  (dxfg_v4_EventQueue_getExecutor()), e.g. with dxfg_v4_IndexedTxModel_Builder_withExecutor(),
 *  dxfg_v4_IndexedEventModel_setExecutor() or dxfg_v4_InstrumentProfileCollector_setExecutor(). Their listeners are
 *  then called by the polls, on the polling thread, between the events of the subscriptions. A poll that ran their
 *  work and has no events returns #DXFG_EVENT_QUEUE_NO_EVENTS at once, so #DXFG_EVENT_QUEUE_NO_EVENTS also means
 *  that the listeners of the models may have been called. Their own data is not in the list: it reaches their
 *  listeners. Limits:
 *  - The calls of a model that notify on the calling thread (`setSymbol`, `clear`, dxfg_v4_OrderBookModel_setFilter())
 *    call its listener there, not in a poll.
 *  - The work posted before the executor was set runs on the previous executor.
 *  - The events that a model drops are not counted by dxfg_v4_EventQueue_getDroppedEvents().
 *  - The collector posts its work once per update listener and update: poll a queue with a collector regularly.
 *  - A subscription whose executor is the executor of a queue (set with dxfg_v4_DXFeedSubscription_setExecutor(), or
 *    inherited from an endpoint built with dxfg_v4_DXEndpoint_Builder_withExecutor()) is not attached: its events reach
 *    its listeners, not the list. dxfg_v4_DXFeed_detachSubscriptionAndClear() of it and
 *    dxfg_v4_DXEndpoint_closeAndAwaitTermination() of its endpoint wait until a poll runs its pending work or the queue
 *    is closed, so do not call them on the polling thread.
 *
 *  Executors:
 *  - The executor of an attached subscription (dxfg_v4_DXFeedSubscription_getExecutor()) belongs to it: it cannot be
 *    changed, and the functions that accept an executor (the setters of the subscriptions, the controllers, the models
 *    and the collector, the endpoint builder, the asynchronous promise handlers) refuse it with
 *    `IllegalStateException`.
 *  - The executor of a shared queue (dxfg_v4_EventQueue_getExecutor()) is accepted by all of them, except
 *    dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(), which fails with `IllegalArgumentException`.
 *    A subscription that has it as its executor may change it later; it cannot be attached, since it has an executor.
 *
 *  Rules:
 *  - Do not set an executor on a subscription to be attached: a subscription with an executor or a queue cannot be
 *    attached (`IllegalStateException`). Symbols may be added before or after, but as for any listener in QD, a
 *    subscription attached to a feed that already has symbols cannot be attached (`IllegalStateException`): attach it
 *    before the symbols are added, or before the subscription is attached to the feed. For QD a queue is an attachment
 *    too: on a subscription that has symbols, add the event listeners before the queue, a listener added after it is
 *    refused (`IllegalStateException`).
 *  - One consumer: the polls of a queue are serialized, a poll waits for the one in progress on another thread.
 *    Poll a queue from one thread at a time, so that a snapshot of indexed or time-series events is not split.
 *  - The event listeners added to an attached subscription are called by the poll, on the polling thread, in the
 *    order of the SDK, and on the thread that closes the queue or the subscription, or detaches the subscription. Do
 *    not poll the queue from such a listener or from a listener of a model on the queue: the poll fails. A listener
 *    that fails makes the poll fail too, and the events it has retrieved, of every tag, are dropped and counted. To
 *    receive the same events by a listener and by a queue, use two subscriptions: the subscriptions of a feed, with or
 *    without a queue, do not affect each other.
 *  - A list returned by a poll is the caller's: it does not depend on the queue, the subscriptions or the endpoint,
 *    and any thread attached to the isolate may release it (dxfg_v4_event_type_list_release(),
 *    dxfg_v4_tagged_event_list_release()).
 *  - Keep the handle of a shared queue, or the handle of its executor, until the models and the collector on the queue
 *    are closed: the release of the last of them closes the queue.
 *
 *  A poll ends when events arrive, when the work of the models has run, when the timeout elapses, when the queue is
 *  woken up (dxfg_v4_EventQueue_wakeUp()) and when the queue is closed. The queue is closed by
 *  dxfg_v4_EventQueue_close(), by the release of the last handle of the queue and of its executor, by the close of
 *  the subscription of a bound queue, by dxfg_v4_NativeListener_deactivateAll() and by dxfg_v4_system_shutdown(); the
 *  last two also close the queues created after them. A closed queue runs the work posted to it at once, on the
 *  posting thread, without calling any listener of the application: from then on the queue, its models and its
 *  collector call nothing back, while their objects stay usable until the application closes them. The events of a
 *  closed queue are discarded and counted as dropped. Closing an endpoint does not close a queue and does not end a
 *  poll: an application that closes the endpoints itself should poll with a finite timeout. While an open queue
 *  holds events that no poll has returned, dxfg_v4_DXEndpoint_closeAndAwaitTermination() of the endpoint of an attached
 *  subscription waits until the queue is polled or closed. dxfg_v4_DXFeed_detachSubscriptionAndClear() of an
 *  attached subscription does not wait for a poll: the events that are delivered while it runs arrive in the poll in
 *  progress or the next one, and the work of the other subscriptions and of the models stays in the queue.
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
 * @brief The result of dxfg_v4_EventQueue_poll() when no events arrived before the timeout or the queue was woken up,
 * or when only the work of the models and the collector on the queue ran.
 *
 * The value is 1. It is not an error: the list is NULL and no exception is pending.
 * @since 3.13.0
 */
#define DXFG_EVENT_QUEUE_NO_EVENTS ((int32_t)1)

/**
 * @brief A list of events with a tag for each: the list returned by dxfg_v4_EventQueue_pollTagged().
 *
 * Its first two fields are those of #dxfg_v4_event_type_list, so the same code can read the events. Release it with
 * dxfg_v4_tagged_event_list_release(), not with dxfg_v4_event_type_list_release() (the tags would leak).
 * @since 3.13.0
 */
typedef struct dxfg_v4_tagged_event_list {
    /** The number of events. */
    int32_t size;
    /** The array of `size` events. */
    dxfg_event_type_t **elements;
    /** The array of `size` tags: `tags[i]` is the tag of the subscription that delivered `elements[i]`. */
    int64_t *tags;
} dxfg_v4_tagged_event_list;

/**
 * @brief The result of dxfg_v4_EventQueue_poll() when the queue is closed.
 *
 * The value is 2. It is not an error: the list is NULL and no exception is pending. Every later poll returns it at
 * once.
 * @since 3.13.0
 */
#define DXFG_EVENT_QUEUE_CLOSED ((int32_t)2)

/**
 * Creates an event queue bound to the subscription: the subscription is attached with the tag 0, the queue closes when
 * the subscription closes, and dxfg_v4_EventQueue_attach() and dxfg_v4_EventQueue_getExecutor() fail on it with
 * `IllegalStateException`.
 *
 * Create it before the symbols are added to a subscription attached to a feed (QD refuses a listener then); a detached
 * subscription may already have symbols, but then add its event listeners before the queue. The executor of the queue
 * stays the executor of the subscription for the life of the subscription, also after the queue is closed.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] sub The subscription; it must have no executor and no queue.
 * @param[out] queue The pointer to the variable that receives the queue (release it with
 * dxfg_v4_JavaObjectHandler_release(), which also closes it); must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error
 * (`IllegalStateException` if the subscription has an executor or a queue, or is attached to a feed and has symbols).
 * Use dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_new(graal_isolatethread_t *thread, dxfg_v4_subscription_t *sub,
                               DXFG_OUT dxfg_v4_event_queue_t **queue);

/**
 * Creates a shared event queue with no subscription: attach them with dxfg_v4_EventQueue_attach(), and give its
 * executor (dxfg_v4_EventQueue_getExecutor()) to the models and the collector. A queue with nothing attached stays open,
 * and its polls wait as usual.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[out] queue The pointer to the variable that receives the queue (release it with
 * dxfg_v4_JavaObjectHandler_release(); the release of the last handle of the queue and of its executor closes it);
 * must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_create(graal_isolatethread_t *thread, DXFG_OUT dxfg_v4_event_queue_t **queue);

/**
 * Attaches the subscription to the shared queue: its events go to the polls of the queue with the tag.
 *
 * Attach it before the symbols are added to a subscription attached to a feed (QD refuses a listener then); a detached
 * subscription may already have symbols, but then add its event listeners before attaching it. The executor of the
 * queue for this subscription stays its executor for the life of the subscription. The subscription leaves the queue
 * only by being closed. A closed subscription, or a subscription attached to a closed queue, is attached as left: its
 * events are discarded and counted as dropped.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue, created by dxfg_v4_EventQueue_create().
 * @param[in] sub The subscription; it must have no executor and no queue. A subscription of any endpoint may be
 * attached.
 * @param[in] tag The tag of the events of the subscription in dxfg_v4_EventQueue_pollTagged(); need not be unique.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error
 * (`IllegalStateException` if the queue was created by dxfg_v4_EventQueue_new(), or if the subscription has an
 * executor or a queue, or is attached to a feed and has symbols). Use dxfg_v4_get_and_clear_thread_exception() to
 * determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_attach(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue,
                                  dxfg_v4_subscription_t *sub, int64_t tag);

/**
 * Returns the executor of the shared queue: the work posted to it runs inside the polls, on the polling thread. Give
 * it to the models, the collector, the endpoint builder or the asynchronous promise handlers whose listeners must run
 * on the polling thread (see the group description for the limits).
 *
 * A handle of the executor keeps the queue open as a handle of the queue does. The executor of a closed queue runs the
 * work at once, on the posting thread, without calling any listener of the application.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue, created by dxfg_v4_EventQueue_create().
 * @param[out] executor The pointer to the variable that receives the executor (release it with
 * dxfg_v4_JavaObjectHandler_release()); must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error
 * (`IllegalStateException` if the queue was created by dxfg_v4_EventQueue_new()). Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_getExecutor(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue,
                                       DXFG_OUT dxfg_v4_executor_t **executor);

/**
 * Waits for the events of the attached subscriptions and returns them, without their tags (see
 * dxfg_v4_EventQueue_pollTagged()).
 *
 * The SDK retrieves the events on the calling thread and calls the event listeners of the subscriptions, and the
 * listeners of the models and the collector on the queue, on it. After the first events, or after the first work of
 * the models, the poll only completes the work that is already pending, so a steady flow does not keep it from
 * returning. A batch may exceed the limit: the limit stops the retrieval, it does not split it. The limit counts the
 * events only. The events of one subscription keep their order.
 *
 * Compare dxfg_v4_EventQueue_getDroppedEvents() after each poll to detect a gap before the returned events.
 *
 * If the events cannot be converted to the C structures, or an event listener of the subscription fails during the
 * poll, the function fails, the events it has retrieved are dropped and counted, and the next poll continues with
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
 * before the timeout or the queue was woken up, or only the work of the models ran, #DXFG_EVENT_QUEUE_CLOSED (2) if the
 * queue is closed, or #DXFG_EXECUTE_FAIL (-1) on error (also if called from a listener during a poll of the same
 * queue, or if the waiting thread is interrupted). Use dxfg_v4_get_and_clear_thread_exception() to determine if an
 * exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_poll(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue,
                                 int64_t timeoutInMilliseconds, int32_t limit,
                                 DXFG_OUT dxfg_v4_event_type_list **events);

/**
 * Waits for the events of the attached subscriptions and returns them with their tags.
 *
 * The function works as dxfg_v4_EventQueue_poll() and has the same results; the list also holds the tag of each
 * event, the one given to dxfg_v4_EventQueue_attach() for its subscription (0 for a queue created by
 * dxfg_v4_EventQueue_new()).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue.
 * @param[in] timeoutInMilliseconds How long to wait for events, as in dxfg_v4_EventQueue_poll().
 * @param[in] limit The number of events after which no more events are retrieved, 0 for no limit; must not be
 * negative.
 * @param[out] events The pointer to the variable that receives the list of events and tags (release it with
 * dxfg_v4_tagged_event_list_release()), or NULL if the result is not 0; must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) with a non-empty list, #DXFG_EVENT_QUEUE_NO_EVENTS (1),
 * #DXFG_EVENT_QUEUE_CLOSED (2) or #DXFG_EXECUTE_FAIL (-1) on error, as dxfg_v4_EventQueue_poll(). Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_pollTagged(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue,
                                       int64_t timeoutInMilliseconds, int32_t limit,
                                       DXFG_OUT dxfg_v4_tagged_event_list **events);

/**
 * Releases a list returned by dxfg_v4_EventQueue_pollTagged(): its events, its tags and the list itself.
 *
 * Any thread attached to the isolate may call it, also after the queue, the subscriptions and the endpoint are gone.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] events The list; NULL does nothing.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_tagged_event_list_release(graal_isolatethread_t *thread, dxfg_v4_tagged_event_list *events);

/**
 * Makes the poll in progress, or the next poll if none is in progress, return #DXFG_EVENT_QUEUE_NO_EVENTS at once
 * (a poll that has events returns them instead). The wake-up is used by one poll: several calls before a poll wake
 * up one poll.
 *
 * Use it to stop a thread that polls with a long or infinite timeout without closing the queue.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_wakeUp(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue);

/**
 * Closes the queue: the poll in progress returns the events it has already retrieved or #DXFG_EVENT_QUEUE_CLOSED,
 * every later poll returns #DXFG_EVENT_QUEUE_CLOSED. The events that no poll has returned are discarded and counted as
 * dropped, and so are the events of the subscriptions that arrive later. The pending work of the subscriptions, the
 * models and the collector runs on the calling thread without calling any listener of the application, and their
 * later work runs at once on the posting thread the same way. The function does not wait for the polls in progress.
 * Closing a closed queue does nothing.
 *
 * Releasing the last handle of the queue and of its executor (dxfg_v4_JavaObjectHandler_release()) closes it as well.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] queue The queue.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.13.0
 */
int32_t dxfg_v4_EventQueue_close(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue);

/**
 * Returns the number of the events of the attached subscriptions that were lost: the events that the SDK dropped as of
 * the last retrieval (see `DXFeedSubscription.getTotalDroppedEvents()`; the last number for a closed subscription),
 * plus the events that the queue dropped itself (a batch that failed to convert, the events of a closed queue or of a
 * closed subscription). The number never decreases.
 *
 * If the number grew since the previous poll, events were lost before the events that the last poll
 * returned. For the indexed and time-series events (e.g. Order, Candle) the state built from the earlier events is then
 * incomplete: subscribe again, or wait for the next snapshot. The number is one for the whole queue: to tell which
 * subscription lost events, compare dxfg_v4_DXFeedSubscription_getTotalDroppedEvents() of each.
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
