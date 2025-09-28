// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK SubscriptionController functions and types declarations
 */

#ifndef DXFG_SUBSCRIPTION_CONTROLLER_H
#define DXFG_SUBSCRIPTION_CONTROLLER_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "dxfg_javac.h"
#include "graal_isolate.h"

/** @defgroup SubscriptionController
 *  @{
 */

typedef struct dxfg_feed_t dxfg_feed_t;

/**
 * The SubscriptionController instance's handle.
 *
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/SubscriptionController.html">Javadoc</a>
 */
typedef struct dxfg_subscription_controller_t {
    dxfg_java_object_handler handler;
} dxfg_subscription_controller_t;

/**
 * Attaches the underlying subscription to the specified feed.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[in] feed The feed to attach to.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * @see https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/SubscriptionController.html#attach-com.dxfeed.api.DXFeed-
 */
int32_t dxfg_SubscriptionController_attach(graal_isolatethread_t *thread,
                                           dxfg_subscription_controller_t *subscriptionController, dxfg_feed_t *feed);

/**
 * Detaches the underlying subscription from the specified feed.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[in] feed The feed to detach from.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * @see https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/SubscriptionController.html#detach-com.dxfeed.api.DXFeed-
 */
int32_t dxfg_SubscriptionController_detach(graal_isolatethread_t *thread,
                                           dxfg_subscription_controller_t *subscriptionController, dxfg_feed_t *feed);

/**
 * Returns maximum number of events in the single notification of the listener.
 * This is not a strict guarantee, some implementations may exceed the limit.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[out] eventsBatchLimit The maximum number of events in the single notification of the listener.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_SubscriptionController_getEventsBatchLimit(graal_isolatethread_t *thread,
                                                        dxfg_subscription_controller_t *subscriptionController,
                                                        DXFG_OUT int32_t *eventsBatchLimit);

/**
 * Sets maximum number of events in the single notification of the listener.
 * This is not a strict guarantee, some implementations may exceed the limit.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[in] eventsBatchLimit The notification events limit
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_SubscriptionController_setEventsBatchLimit(graal_isolatethread_t *thread,
                                                        dxfg_subscription_controller_t *subscriptionController,
                                                        int32_t eventsBatchLimit);

/**
 * Returns the aggregation period for data for this underlying subscription.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[out] aggregationPeriod The aggregation period for data, represented as a TimePeriod object.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the TimePeriod handle.
 */
int32_t dxfg_SubscriptionController_getAggregationPeriod(graal_isolatethread_t *thread,
                                                         dxfg_subscription_controller_t *subscriptionController,
                                                         DXFG_OUT dxfg_time_period_t **aggregationPeriod);

/**
 * Returns the aggregation period for data for this underlying subscription in millis.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[out] aggregationPeriod The aggregation period for data, represented as a timestamp in millis.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_SubscriptionController_getAggregationPeriodMillis(graal_isolatethread_t *thread,
                                                               dxfg_subscription_controller_t *subscriptionController,
                                                               DXFG_OUT int64_t *aggregationPeriod);

/**
 * Sets the aggregation period for data that limits the rate of data notifications.
 * For example, setting the value to `0.1s` limits notification to once every 100ms (at most 10 per second).
 * The new aggregation period will take effect during the next iteration of data notification.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[in] aggregationPeriod The aggregation period for data.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_SubscriptionController_setAggregationPeriod(graal_isolatethread_t *thread,
                                                         dxfg_subscription_controller_t *subscriptionController,
                                                         dxfg_time_period_t *aggregationPeriod);

/**
 * Sets the aggregation period for data that limits the rate of data notifications.
 * For example, setting the value to `100` limits notification to once every 100ms (at most 10 per second).
 * The new aggregation period will take effect during the next iteration of data notification.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[in] aggregationPeriod The aggregation period for data.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_SubscriptionController_setAggregationPeriodMillis(graal_isolatethread_t *thread,
                                                               dxfg_subscription_controller_t *subscriptionController,
                                                               int64_t aggregationPeriod);

/**
 * Returns executor for processing event notifications on this underlying subscription.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[out] executor The executor for processing event notifications on this underlying subscription.
 * Or `NULL` if the default executor is used.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the Executor handle.
 */
int32_t dxfg_SubscriptionController_getExecutor(graal_isolatethread_t *thread,
                                                dxfg_subscription_controller_t *subscriptionController,
                                                DXFG_OUT dxfg_executor_t **executor);

/**
 * Changes executor for processing event notifications on this underlying subscription.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] subscriptionController The subscription controller handle.
 * @param[out] executor The executor for processing event notifications on this underlying subscription.
 * Or `NULL` if the default executor is used.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_SubscriptionController_setExecutor(graal_isolatethread_t *thread,
                                                dxfg_subscription_controller_t *subscriptionController,
                                                dxfg_executor_t *executor);

/** @} */ // end of SubscriptionController

#ifdef __cplusplus
}
#endif

#endif // DXFG_SUBSCRIPTION_CONTROLLER_H