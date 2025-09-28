// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK Subscription functions and types declarations
 */

#ifndef DXFG_SUBSCRIPTION_H
#define DXFG_SUBSCRIPTION_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "graal_isolate.h"
#include "dxfg_events.h"
#include "dxfg_javac.h"

/** @defgroup Subscription
 *  @{
 */

/**
 * @brief Forward declarations.
 */
typedef struct dxfg_feed_t dxfg_feed_t;
typedef struct dxfg_subscription_controller_t dxfg_subscription_controller_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedSubscription.html">Javadoc</a>
 */
typedef struct dxfg_subscription_t {
    dxfg_java_object_handler handler;
} dxfg_subscription_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedTimeSeriesSubscription.html">Javadoc</a>
 */
typedef struct dxfg_time_series_subscription_t {
    dxfg_subscription_t sub;
} dxfg_time_series_subscription_t;

typedef struct dxfg_feed_event_listener_t {
    dxfg_java_object_handler handler;
} dxfg_feed_event_listener_t;
typedef void (*dxfg_feed_event_listener_function)(graal_isolatethread_t *thread, dxfg_event_type_list *events, void *user_data);

dxfg_subscription_t*      dxfg_DXFeedSubscription_new(graal_isolatethread_t *thread, dxfg_event_clazz_t eventClazz);
dxfg_subscription_t*      dxfg_DXFeedSubscription_new2(graal_isolatethread_t *thread, dxfg_event_clazz_list_t *eventClazzes);

/**
 * Returns the subscription controller for this subscription.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] sub The subscription handle.
 * @param[out] subscriptionController The subscription controller for this subscription.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the SubscriptionController handle.
 */
int32_t                   dxfg_DXFeedSubscription_getSubscriptionController(graal_isolatethread_t *thread, dxfg_subscription_t *sub, DXFG_OUT dxfg_subscription_controller_t *subscriptionController);
int32_t                   dxfg_DXFeedSubscription_attach(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_feed_t *feed);
int32_t                   dxfg_DXFeedSubscription_detach(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_feed_t *feed);
int32_t                   dxfg_DXFeedSubscription_isClosed(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
int32_t                   dxfg_DXFeedSubscription_close(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
dxfg_event_clazz_list_t*  dxfg_DXFeedSubscription_getEventTypes(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
int32_t                   dxfg_DXFeedSubscription_containsEventType(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_event_clazz_t eventClazz);
int32_t                   dxfg_DXFeedSubscription_clear(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
dxfg_symbol_list*         dxfg_DXFeedSubscription_getSymbols(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
dxfg_symbol_list*         dxfg_DXFeedSubscription_getDecoratedSymbols(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
int32_t                   dxfg_DXFeedSubscription_setSymbol(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_t *symbol);
int32_t                   dxfg_DXFeedSubscription_setSymbols(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_list *symbols);
int32_t                   dxfg_DXFeedSubscription_addSymbol(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_t *symbol);
int32_t                   dxfg_DXFeedSubscription_addSymbols(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_list *symbols);
int32_t                   dxfg_DXFeedSubscription_removeSymbol(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_t *symbol);
int32_t                   dxfg_DXFeedSubscription_removeSymbols(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_list *symbols);
dxfg_executor_t*          dxfg_DXFeedSubscription_getExecutor(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
int32_t                   dxfg_DXFeedSubscription_setExecutor(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_executor_t *executor);
dxfg_time_period_t*       dxfg_DXFeedSubscription_getAggregationPeriod(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
int32_t                   dxfg_DXFeedSubscription_setAggregationPeriod(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_time_period_t *period);
dxfg_feed_event_listener_t* dxfg_DXFeedEventListener_new(graal_isolatethread_t *thread, dxfg_feed_event_listener_function user_func, void *user_data);
int32_t                   dxfg_DXFeedSubscription_addEventListener(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_feed_event_listener_t *listener);
int32_t                   dxfg_DXFeedSubscription_removeEventListener(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_feed_event_listener_t *listener);
int32_t                   dxfg_DXFeedSubscription_addChangeListener(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_observable_subscription_change_listener_t *listener);
int32_t                   dxfg_DXFeedSubscription_removeChangeListener(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_observable_subscription_change_listener_t *listener);
int32_t                   dxfg_DXFeedSubscription_getEventsBatchLimit(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
int32_t                   dxfg_DXFeedSubscription_setEventsBatchLimit(graal_isolatethread_t *thread, dxfg_subscription_t *sub, int32_t eventsBatchLimit);

/** @} */ // end of Subscription

#ifdef __cplusplus
}
#endif

#endif // DXFG_SUBSCRIPTION_H
