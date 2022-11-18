// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_SUBSCRIPTION_H_
#define DXFEED_GRAAL_NATIVE_API_SUBSCRIPTION_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "graal_isolate.h"
#include "dxfg_events.h"

/** @defgroup Subscription
 *  @{
 */

/**
 * @brief Forward declarations.
 */
typedef struct dxfg_feed_t dxfg_feed_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedSubscription.html">Javadoc</a>
 */
typedef struct dxfg_subscription_t {
    void *f_java_object_handle;
} dxfg_subscription_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedTimeSeriesSubscription.html">Javadoc</a>
 */
typedef struct dxfg_time_series_subscription_t {
    void *f_java_object_handle;
} dxfg_time_series_subscription_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedEventListener.html">Javadoc</a>
 */
typedef void (*dxfg_subscription_event_listener)(graal_isolatethread_t *thread, const dxfg_event_type_t **events,
                                                 int32_t size, void *user_data);

int32_t dxfg_subscription_close(graal_isolatethread_t *thread, dxfg_subscription_t *sub);
int32_t dxfg_subscription_add_event_listener(graal_isolatethread_t *thread, dxfg_subscription_t *sub,
                                             dxfg_subscription_event_listener listener, void *user_data);
int32_t dxfg_subscription_remove_event_listener(graal_isolatethread_t *thread, dxfg_subscription_t *sub,
                                                dxfg_subscription_event_listener listener);
int32_t dxfg_subscription_add_symbol(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_t *symbol);
int32_t dxfg_subscription_add_symbols(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_t **symbol,
                                      int32_t size);
int32_t dxfg_subscription_remove_symbol(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_t *symbol);
int32_t dxfg_subscription_remove_symbols(graal_isolatethread_t *thread, dxfg_subscription_t *sub, dxfg_symbol_t **symbol,
                                         int32_t size);
int32_t dxfg_subscription_clear(graal_isolatethread_t *thread, dxfg_subscription_t *sub);

/** @} */ // end of Subscription

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_SUBSCRIPTION_H_
