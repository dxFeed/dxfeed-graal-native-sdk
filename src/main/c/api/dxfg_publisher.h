// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK Publisher functions and types declarations
 */

#ifndef DXFG_PUBLISHER_H
#define DXFG_PUBLISHER_H

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

/** @defgroup Publisher
 *  @{
 */

/**
 * @brief Forward declarations.
 */
typedef struct dxfg_java_object_handler dxfg_java_object_handler;

/**
 * @brief The DXPublisher.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXPublisher.html">Javadoc</a>
 */
typedef struct dxfg_publisher_t {
    dxfg_java_object_handler handler;
} dxfg_publisher_t;

typedef struct dxfg_observable_subscription_t {
    dxfg_java_object_handler handler;
} dxfg_observable_subscription_t;

dxfg_publisher_t*                   dxfg_DXPublisher_getInstance(graal_isolatethread_t *thread);
int32_t                             dxfg_DXPublisher_publishEvents(graal_isolatethread_t *thread, dxfg_publisher_t *publisher, dxfg_event_type_list *events);
dxfg_observable_subscription_t*     dxfg_DXPublisher_getSubscription(graal_isolatethread_t *thread, dxfg_publisher_t *publisher, dxfg_event_clazz_t eventClazz);


int32_t                    dxfg_ObservableSubscription_isClosed(graal_isolatethread_t *thread, dxfg_observable_subscription_t *sub);
dxfg_event_clazz_list_t*   dxfg_ObservableSubscription_getEventTypes(graal_isolatethread_t *thread, dxfg_observable_subscription_t *sub);
int32_t                    dxfg_ObservableSubscription_containsEventType(graal_isolatethread_t *thread, dxfg_observable_subscription_t *sub, dxfg_event_clazz_t eventClazz);
int32_t                    dxfg_ObservableSubscription_addChangeListener(graal_isolatethread_t *thread, dxfg_observable_subscription_t *sub, dxfg_observable_subscription_change_listener_t *listener);
int32_t                    dxfg_ObservableSubscription_removeChangeListener(graal_isolatethread_t *thread, dxfg_observable_subscription_t *sub, dxfg_observable_subscription_change_listener_t *listener);

/** @} */ // end of Publisher

#ifdef __cplusplus
}
#endif

#endif // DXFG_PUBLISHER_H
