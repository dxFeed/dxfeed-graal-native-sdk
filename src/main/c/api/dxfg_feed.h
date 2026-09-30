// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK Feed functions and types declarations
 */

#ifndef DXFG_FEED_H
#define DXFG_FEED_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "dxfg_events.h"
#include "dxfg_catch_exception.h"
#include "dxfg_javac.h"
#include "graal_isolate.h"

/** @defgroup Feed
 *  @{
 */

/**
 * @brief Forward declarations.
 */
typedef struct dxfg_subscription_t dxfg_subscription_t;
typedef struct dxfg_time_series_subscription_t dxfg_time_series_subscription_t;
typedef struct dxfg_executor_t dxfg_executor_t;

/**
 * @brief The DXFeed.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html">Javadoc</a>
 */
typedef struct dxfg_feed_t {
    dxfg_java_object_handler handler;
} dxfg_feed_t;

/**
 * @brief The Promise.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/promise/Promise.html">Javadoc</a>
 */
typedef struct dxfg_promise_t {
    dxfg_java_object_handler handler;
} dxfg_promise_t;


typedef struct dxfg_promise_list {
    dxfg_java_object_handler_list list;
} dxfg_promise_list;

typedef struct dxfg_promise_events_t {
    dxfg_promise_t base;
} dxfg_promise_events_t;


typedef struct dxfg_promise_event_t {
    dxfg_promise_t handler;
} dxfg_promise_event_t;

dxfg_feed_t*                      dxfg_DXFeed_getInstance(graal_isolatethread_t *thread);
dxfg_subscription_t*              dxfg_DXFeed_createSubscription(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t eventClazz);
dxfg_subscription_t*              dxfg_DXFeed_createSubscription2(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_list_t *eventClazzes);
dxfg_time_series_subscription_t*  dxfg_DXFeed_createTimeSeriesSubscription(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t eventClazz);
dxfg_time_series_subscription_t*  dxfg_DXFeed_createTimeSeriesSubscription2(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_list_t *eventClazzes);
int32_t                           dxfg_DXFeed_attachSubscription(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_subscription_t *sub);
int32_t                           dxfg_DXFeed_detachSubscription(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_subscription_t *sub);
int32_t                           dxfg_DXFeed_detachSubscriptionAndClear(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_subscription_t *sub);
dxfg_event_type_t*                dxfg_DXFeed_getLastEventIfSubscribed(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t eventClazz, dxfg_symbol_t *symbol);
dxfg_event_type_list*             dxfg_DXFeed_getIndexedEventsIfSubscribed(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t eventClazz, dxfg_symbol_t *symbol, const char *source);
dxfg_event_type_list*             dxfg_DXFeed_getTimeSeriesIfSubscribed(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t eventClazz, dxfg_symbol_t *symbol, int64_t from_time, int64_t to_time);

/**
 * Fills the given event with the last event for its type and symbol
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getLastEvent-E-">DXFeed.getLastEvent</a>).
 *
 * @deprecated Use dxfg_DXFeed_getLastEvent2(). This function writes the result into the given event: it frees the
 * strings of the event with the SDK's allocator and writes the strings allocated by the SDK there. So the event must
 * be created by dxfg_EventType_new() (or come from the SDK) and be freed by dxfg_EventType_release(); an event
 * allocated by the caller leaks the SDK's strings or corrupts memory. When the last event is not available, the event
 * keeps the values it had (as in Java), but the event created by dxfg_EventType_new() has only its symbol, so the
 * values of the caller's event are lost.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] feed The feed.
 * @param[in,out] event The event created by dxfg_EventType_new(): its type and symbol, and the result.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
DXFG_DEPRECATED("use dxfg_DXFeed_getLastEvent2(), it does not write into the given event")
int32_t                           dxfg_DXFeed_getLastEvent(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_type_t *event);

/**
 * The bulk version of dxfg_DXFeed_getLastEvent().
 *
 * @deprecated Use dxfg_DXFeed_getLastEvents2(), see dxfg_DXFeed_getLastEvent() for the reasons. Every event of the list
 * must be created by dxfg_EventType_new().
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] feed The feed.
 * @param[in,out] events The events created by dxfg_EventType_new(), the results are written into them.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
DXFG_DEPRECATED("use dxfg_DXFeed_getLastEvents2(), it does not write into the given events")
int32_t                           dxfg_DXFeed_getLastEvents(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_type_list *events);

/**
 * Gets the last event for the type and symbol of the given event
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getLastEvent-E-">DXFeed.getLastEvent</a>):
 * a new event with the values of the last event, or a copy of the given event when the last event is not available
 * (no subscription, the data have not arrived yet, etc.), since Java leaves the event unchanged then.
 *
 * The given event is only read, as by dxfg_DXPublisher_publishEvents(): the caller allocates it (with any allocator),
 * fills it and frees it; the SDK copies it during the call. The event must be of a lasting type (Quote, Profile, etc.).
 *
 * The function does not make remote calls, it reads the local cache of the feed. The events are in the cache only if
 * an attached subscription is subscribed to the event type and symbol (a wildcard subscription does not count). With
 * the #DXFG_ENDPOINT_ROLE_STREAM_FEED role the last event is never available.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] feed The feed.
 * @param[in] event The event: its type and symbol, and the values to return when the last event is not available.
 * @param[out] lastEvent The pointer to the new event (NULL on error). Free the event with dxfg_EventType_release().
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error (e.g.,
 * the event is NULL or of a type that is not lasting). Use dxfg_get_and_clear_thread_exception_t() to determine if an
 * exception was thrown.
 */
int32_t                           dxfg_DXFeed_getLastEvent2(graal_isolatethread_t *thread, dxfg_feed_t *feed, const dxfg_event_type_t *event, DXFG_OUT dxfg_event_type_t **lastEvent);

/**
 * The bulk version of dxfg_DXFeed_getLastEvent2(): gets a new list with the last event for every event of the given
 * list, in the same order.
 *
 * The given list and its events are only read (see dxfg_DXFeed_getLastEvent2()).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] feed The feed.
 * @param[in] events The events.
 * @param[out] lastEvents The pointer to the new list of new events (NULL on error). Free the list with
 * dxfg_CList_EventType_release().
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error (e.g.,
 * the list or one of its events is NULL, or an event is of a type that is not lasting). Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t                           dxfg_DXFeed_getLastEvents2(graal_isolatethread_t *thread, dxfg_feed_t *feed, const dxfg_event_type_list *events, DXFG_OUT dxfg_event_type_list **lastEvents);

dxfg_promise_event_t*             dxfg_DXFeed_getLastEventPromise(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t eventClazz, dxfg_symbol_t *symbol);
dxfg_promise_list*                dxfg_DXFeed_getLastEventsPromises(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t eventClazz, dxfg_symbol_list *symbols);
dxfg_promise_events_t*            dxfg_DXFeed_getIndexedEventsPromise(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t eventClazz, dxfg_symbol_t *symbol, dxfg_indexed_event_source_t* source);
dxfg_promise_events_t*            dxfg_DXFeed_getTimeSeriesPromise(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_clazz_t clazz, dxfg_symbol_t *symbol, int64_t fromTime, int64_t toTime);

int32_t                           dxfg_DXFeedTimeSeriesSubscription_setFromTime(graal_isolatethread_t *thread, dxfg_time_series_subscription_t *sub, int64_t fromTime);

typedef void (*dxfg_promise_handler_function)(graal_isolatethread_t *thread, dxfg_promise_t *promise, void *user_data);

int32_t               dxfg_Promise_isDone(graal_isolatethread_t *thread, dxfg_promise_t *promise);
int32_t               dxfg_Promise_hasResult(graal_isolatethread_t *thread, dxfg_promise_t *promise);
int32_t               dxfg_Promise_hasException(graal_isolatethread_t *thread, dxfg_promise_t *promise);
int32_t               dxfg_Promise_isCancelled(graal_isolatethread_t *thread, dxfg_promise_t *promise);
dxfg_event_type_t*    dxfg_Promise_EventType_getResult(graal_isolatethread_t *thread, dxfg_promise_event_t *promise);
dxfg_event_type_list* dxfg_Promise_List_EventType_getResult(graal_isolatethread_t *thread, dxfg_promise_events_t *promise);
dxfg_exception_t*     dxfg_Promise_getException(graal_isolatethread_t *thread, dxfg_promise_t *promise);

/**
 * The result of dxfg_Promise_awaitWithoutException() when the wait timed out (the promise is cancelled then).
 */
#define DXFG_PROMISE_AWAIT_TIMED_OUT ((int32_t)1)

/**
 * Waits for the promise to complete
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/promise/Promise.html">Promise.await()</a>).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] promise The promise.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) if the promise has completed normally or #DXFG_EXECUTE_FAIL (-1)
 * if it has completed exceptionally, was cancelled or the wait was interrupted.
 * Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 */
int32_t               dxfg_Promise_await(graal_isolatethread_t *thread, dxfg_promise_t *promise);

/**
 * Waits for the promise to complete or the timeout to elapse
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/promise/Promise.html">Promise.await(timeout, unit)</a>).
 * If the wait times out, the promise is cancelled and the function fails with `CancellationException`.
 * Use dxfg_Promise_awaitWithoutException() to distinguish the timeout without an exception.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] promise The promise.
 * @param[in] timeoutInMilliseconds The timeout in milliseconds.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) if the promise has completed normally or #DXFG_EXECUTE_FAIL (-1)
 * if it has completed exceptionally, was cancelled, the wait timed out or was interrupted.
 * Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 */
int32_t               dxfg_Promise_await2(graal_isolatethread_t *thread, dxfg_promise_t *promise, int32_t timeoutInMilliseconds);

/**
 * Waits for the promise to complete or the timeout to elapse
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/promise/Promise.html">Promise.awaitWithoutException(timeout, unit)</a>).
 * If the wait times out, the promise is cancelled and the function returns #DXFG_PROMISE_AWAIT_TIMED_OUT.
 *
 * Before v3.5.0 the function returned #DXFG_EXECUTE_SUCCESSFULLY (0) on timeout as well.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] promise The promise.
 * @param[in] timeoutInMilliseconds The timeout in milliseconds.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) if the promise has completed normally,
 * #DXFG_PROMISE_AWAIT_TIMED_OUT (1) if the wait timed out (the promise is cancelled),
 * or #DXFG_EXECUTE_FAIL (-1) if the promise has completed exceptionally, was cancelled or the wait was interrupted.
 * Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 */
int32_t               dxfg_Promise_awaitWithoutException(graal_isolatethread_t *thread, dxfg_promise_t *promise, int32_t timeoutInMilliseconds);
int32_t               dxfg_Promise_cancel(graal_isolatethread_t *thread, dxfg_promise_t *promise);
int32_t               dxfg_Promise_List_EventType_complete(graal_isolatethread_t *thread, dxfg_promise_t *promise, dxfg_event_type_list* events);
int32_t               dxfg_Promise_EventType_complete(graal_isolatethread_t *thread, dxfg_promise_t *promise, dxfg_event_type_t* event);
int32_t               dxfg_Promise_completeExceptionally(graal_isolatethread_t *thread, dxfg_promise_t *promise, dxfg_exception_t* exception);
int32_t               dxfg_Promise_whenDone(graal_isolatethread_t *thread, dxfg_promise_t *promise, dxfg_promise_handler_function promise_handler_function, void *user_data);
int32_t               dxfg_Promise_whenDoneAsync(graal_isolatethread_t *thread, dxfg_promise_t *promise, dxfg_promise_handler_function promise_handler_function, void *user_data, dxfg_executor_t* executor);
dxfg_promise_t*       dxfg_Promise_completed(graal_isolatethread_t *thread, dxfg_promise_t *promise, dxfg_java_object_handler *handler);
dxfg_promise_t*       dxfg_Promise_failed(graal_isolatethread_t *thread, dxfg_promise_t *promise, dxfg_exception_t* exception);

dxfg_promise_t*       dxfg_Promises_allOf(graal_isolatethread_t *thread, dxfg_promise_list *promises);


/** @} */ // end of Feed

#ifdef __cplusplus
}
#endif

#endif // DXFG_FEED_H
