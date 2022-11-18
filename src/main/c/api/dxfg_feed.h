// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_FEED_H_
#define DXFEED_GRAAL_NATIVE_API_FEED_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_events.h"
#include "graal_isolate.h"

/** @defgroup Feed
 *  @{
 */

/**
 * @brief Forward declarations.
 */
typedef struct dxfg_subscription_t dxfg_subscription_t;

/**
 * @brief Forward declarations.
 */
typedef struct dxfg_time_series_subscription_t dxfg_time_series_subscription_t;

/**
 * @brief The DXFeed.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html">Javadoc</a>
 */
typedef struct dxfg_feed_t {
    void *java_object_handle;
} dxfg_feed_t;

/**
 * @brief Returns a default application-wide singleton instance of DXFeed with a DXFG_ENDPOINT_ROLE_FEED role.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getInstance--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[out] feed The feed instance.
 * @return 0 - if the operation was successful; otherwise -1.
 */
int32_t dxfg_feed_get_instance(graal_isolatethread_t *thread, dxfg_feed_t *feed);

/**
 * @brief Creates new subscription for a single event type that is attached to this feed.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#createSubscription-java.lang.Class-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] feed The feed.
 * @param[in] eventTypes The list of event types.
 * @param[in] eventTypesSize The count of event types.
 * @param[out] sub The created subscription.
 * @return 0 - if the operation was successful; otherwise -1.
 */
int32_t dxfg_feed_create_subscription(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_kind_t *eventTypes,
                                      int32_t eventTypesSize, dxfg_subscription_t *sub);




/**
 * <a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#createTimeSeriesSubscription-java.lang.Class-">
 * Javadoc</a>
 */
int32_t dxfg_feed_create_time_series_subscription(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_event_kind_t *eventType, dxfg_time_series_subscription_t *sub);

/**
 * @brief Attaches the given subscription to this feed.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#attachSubscription-com.dxfeed.api.DXFeedSubscription-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] feed The feed.
 * @param[in] sub The subscription.
 * @return 0 - if the operation was successful; otherwise -1.
 */
int32_t dxfg_feed_attach_subscription(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_subscription_t *sub);

/**
 * @brief Detaches the given subscription from this feed.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#detachSubscription-com.dxfeed.api.DXFeedSubscription-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] feed The feed.
 * @param[in] sub The subscription.
 * @return 0 - if the operation was successful; otherwise -1.
 */
int32_t dxfg_feed_detach_subscription(graal_isolatethread_t *thread, dxfg_feed_t *feed, dxfg_subscription_t *sub);

/**
 * @brief Detaches the given subscription from this feed
 * and clears data delivered to this subscription by publishing empty events.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#detachSubscriptionAndClear-com.dxfeed.api.DXFeedSubscription-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] feed The feed.
 * @param[in] sub The subscription.
 * @return 0 - if the operation was successful; otherwise -1.
 */
int32_t dxfg_feed_detach_subscription_and_clear(graal_isolatethread_t *thread, dxfg_feed_t *feed,
                                                dxfg_subscription_t *sub);

/**
 * @brief Returns the last event for the specified event type and symbol if there is a subscription for it.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getLastEventIfSubscribed-java.lang.Class-java.lang.Object-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] feed The feed.
 * @param[in] eventType The event type.
 * @param[in] symbol The event symbol.
 * @param[out] event The event or null if there is no subscription for the specified event type and symbol.
 * @return 0 - if the operation was successful; otherwise -1.
 */
int32_t dxfg_feed_get_last_event_if_subscribed(graal_isolatethread_t *thread, dxfg_feed_t *feed,
                                               dxfg_event_kind_t eventType,
                                               dxfg_symbol_t *symbol,
                                               dxfg_event_type_t **event);

/**
 * @brief Returns a list of indexed events for the specified event type, symbol,
 * and source if there is a subscription for it.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getIndexedEventsIfSubscribed-java.lang.Class-java.lang.Object-com.dxfeed.event.IndexedEventSource-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] feed The feed.
 * @param[in] eventType The event type.
 * @param[in] symbol The event symbol.
 * @param[in] source The order source.
 * @param[out] events The event or null if there is no subscription for the specified event type and symbol.
 * @param[out] eventsSize The count of events.
 * @return 0 - if the operation was successful; otherwise -1.
 */
int32_t dxfg_feed_get_indexed_event_if_subscribed(graal_isolatethread_t *thread, dxfg_feed_t *feed,
                                                  dxfg_event_kind_t eventType,
                                                  dxfg_symbol_t *symbol, const char *source,
                                                  dxfg_event_type_t **events, int32_t *eventsSize);

/**
 * @brief Returns time series of events for the specified event type, symbol,
 * and a range of time if there is a subscription for it.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html#getTimeSeriesIfSubscribed-java.lang.Class-java.lang.Object-long-long-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] feed The feed.
 * @param[in] eventType The event type.
 * @param[in] symbol The event symbol.
 * @param[in] from_time The time, inclusive, to return events from (in milliseconds).
 * @param[in] to_time The time, inclusive, to return events to (in milliseconds).
 * @param[out] events The list of events or null if there is no subscription
 * for the specified event type, symbol, and time range.
 * @param[out] eventsSize The count of events.
 * @return 0 - if the operation was successful; otherwise -1.
 */
int32_t dxfg_feed_get_time_series_event_if_subscribed(graal_isolatethread_t *thread, dxfg_feed_t *feed,
                                                      dxfg_event_kind_t type,
                                                      dxfg_symbol_t *symbol, int64_t from_time,
                                                      int64_t to_time, dxfg_event_type_t **event, int32_t *eventsSize);

/** @} */ // end of Feed

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_FEED_H_
