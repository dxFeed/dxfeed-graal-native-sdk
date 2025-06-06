// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK Endpoint functions and types declarations
 */

#ifndef DXFG_ENDPOINT_H
#define DXFG_ENDPOINT_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "graal_isolate.h"
#include "dxfg_javac.h"

/** @defgroup Endpoint
 *  @{
 */

/**
 * @brief Forward declarations.
 */
typedef struct dxfg_feed_t dxfg_feed_t;
typedef struct dxfg_publisher_t dxfg_publisher_t;
typedef struct dxfg_event_clazz_list_t dxfg_event_clazz_list_t;

/**
 * @brief The DXEndpoint.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html">Javadoc</a>
 */
typedef struct dxfg_endpoint_t {
    dxfg_java_object_handler handler;
} dxfg_endpoint_t;

/**
 * @brief The DXEndpoint.Builder.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Builder.html">Javadoc</a>
 */
typedef struct dxfg_endpoint_builder_t {
    dxfg_java_object_handler handler;
} dxfg_endpoint_builder_t;

/**
 * @brief The PropertyChangeListener.
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/beans/PropertyChangeListener.html">Javadoc</a>
 */
typedef struct dxfg_endpoint_state_change_listener_t {
    dxfg_java_object_handler handler;
} dxfg_endpoint_state_change_listener_t;

/**
 * @brief List of endpoint roles.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Role.html">Javadoc</a>
 */
typedef enum dxfg_endpoint_role_t {
    DXFG_ENDPOINT_ROLE_FEED = 0,
    DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED,
    DXFG_ENDPOINT_ROLE_STREAM_FEED,
    DXFG_ENDPOINT_ROLE_PUBLISHER,
    DXFG_ENDPOINT_ROLE_STREAM_PUBLISHER,
    DXFG_ENDPOINT_ROLE_LOCAL_HUB,
} dxfg_endpoint_role_t;

/**
 * @brief List of endpoint states.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.State.html">Javadoc</a>
 */
typedef enum dxfg_endpoint_state_t {
    DXFG_ENDPOINT_STATE_NOT_CONNECTED = 0,
    DXFG_ENDPOINT_STATE_CONNECTING,
    DXFG_ENDPOINT_STATE_CONNECTED,
    DXFG_ENDPOINT_STATE_CLOSED,
} dxfg_endpoint_state_t;

/**
 * @brief Function pointer to the endpoint state change listener.
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] old_state The old endpoint state.
 * @param[in] new_state The new endpoint state.
 * @param[in,out] user_data The pointer to user data.
 */
typedef void (*dxfg_endpoint_state_change_listener_func)(graal_isolatethread_t *thread, dxfg_endpoint_state_t old_state,
                                                         dxfg_endpoint_state_t new_state, void *user_data);

/** @defgroup Builder
 *  @{
 */

dxfg_endpoint_builder_t*    dxfg_DXEndpoint_newBuilder(graal_isolatethread_t *thread);
int32_t                     dxfg_DXEndpoint_Builder_withRole(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, dxfg_endpoint_role_t role);
int32_t                     dxfg_DXEndpoint_Builder_withName(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, const char *name);
int32_t                     dxfg_DXEndpoint_Builder_withProperty(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, const char *key, const char *value);
int32_t                     dxfg_DXEndpoint_Builder_withProperties(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, const char *file_path);
int32_t                     dxfg_DXEndpoint_Builder_supportsProperty(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, const char *key);
dxfg_endpoint_t*            dxfg_DXEndpoint_Builder_build(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder);

/** @} */ // end of Builder

dxfg_endpoint_t*                dxfg_DXEndpoint_getInstance(graal_isolatethread_t *thread);
dxfg_endpoint_t*                dxfg_DXEndpoint_getInstance2(graal_isolatethread_t *thread, dxfg_endpoint_role_t role);
dxfg_endpoint_t*                dxfg_DXEndpoint_create(graal_isolatethread_t *thread);
dxfg_endpoint_t*                dxfg_DXEndpoint_create2(graal_isolatethread_t *thread, dxfg_endpoint_role_t role);
int32_t                         dxfg_DXEndpoint_close(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t                         dxfg_DXEndpoint_closeAndAwaitTermination(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
dxfg_endpoint_role_t            dxfg_DXEndpoint_getRole(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t                         dxfg_DXEndpoint_user(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, const char *user);
int32_t                         dxfg_DXEndpoint_password(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, const char *password);
int32_t                         dxfg_DXEndpoint_connect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, const char *address);
int32_t                         dxfg_DXEndpoint_reconnect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t                         dxfg_DXEndpoint_disconnect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t                         dxfg_DXEndpoint_disconnectAndClear(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t                         dxfg_DXEndpoint_awaitProcessed(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t                         dxfg_DXEndpoint_awaitNotConnected(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
dxfg_endpoint_state_t           dxfg_DXEndpoint_getState(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t                         dxfg_DXEndpoint_addStateChangeListener(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, dxfg_endpoint_state_change_listener_t *listener);
int32_t                         dxfg_DXEndpoint_removeStateChangeListener(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, dxfg_endpoint_state_change_listener_t *listener);
dxfg_feed_t*                    dxfg_DXEndpoint_getFeed(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
dxfg_publisher_t*               dxfg_DXEndpoint_getPublisher(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t                         dxfg_DXEndpoint_executor(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, dxfg_executor_t *executor);

/**
 * Returns a set of all event types supported by this endpoint and the current version of the dxFeed Native Graal SDK
 * (all other event types will be filtered out).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint.
 * @return A set of supported event types.
 * Use dxfg_CList_EventClazz_release() to free the list's handle.
 */
dxfg_event_clazz_list_t *dxfg_DXEndpoint_getEventTypes(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

dxfg_endpoint_state_change_listener_t* dxfg_PropertyChangeListener_new(graal_isolatethread_t *thread, dxfg_endpoint_state_change_listener_func user_func, void *user_data);

/** @} */ // end of Endpoint

#ifdef __cplusplus
}
#endif

#endif // DXFG_ENDPOINT_H
