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
 *
 * The recommended way to create an endpoint: the builder allows to set the role, the name and the properties
 * (see <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Builder.html">DXEndpoint.Builder</a>).
 * Every dxfg_DXEndpoint_Builder_build() call creates a new endpoint.
 *
 *  @{
 */

/**
 * Creates a new endpoint builder.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @return The builder or NULL on error. Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 * Use dxfg_JavaObjectHandler_release() to free the builder's handle.
 */
dxfg_endpoint_builder_t*    dxfg_DXEndpoint_newBuilder(graal_isolatethread_t *thread);
int32_t                     dxfg_DXEndpoint_Builder_withRole(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, dxfg_endpoint_role_t role);
int32_t                     dxfg_DXEndpoint_Builder_withName(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, const char *name);
int32_t                     dxfg_DXEndpoint_Builder_withProperty(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, const char *key, const char *value);
int32_t                     dxfg_DXEndpoint_Builder_withProperties(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, const char *file_path);
int32_t                     dxfg_DXEndpoint_Builder_supportsProperty(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder, const char *key);

/**
 * Builds a new endpoint with the builder's settings.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] builder The builder.
 * @return The new endpoint or NULL on error. Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 * The caller owns the endpoint: close it with dxfg_DXEndpoint_close() or dxfg_DXEndpoint_closeAndAwaitTermination()
 * and free its handle with dxfg_JavaObjectHandler_release() (releasing the handle does not close the endpoint).
 */
dxfg_endpoint_t*            dxfg_DXEndpoint_Builder_build(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder);

/** @} */ // end of Builder

/**
 * Returns the default application-wide singleton endpoint with the #DXFG_ENDPOINT_ROLE_FEED role
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#getInstance--">DXEndpoint.getInstance()</a>),
 * the same one that dxfg_DXFeed_getInstance() uses. It is configured by the system properties
 * (see dxfg_system_set_property()) when it is first used.
 *
 * All callers share this endpoint: closing it closes it for everyone in the process.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @return The endpoint or NULL on error. Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 * Use dxfg_JavaObjectHandler_release() to free the handle.
 */
dxfg_endpoint_t*                dxfg_DXEndpoint_getInstance(graal_isolatethread_t *thread);

/**
 * Creates a <b>new</b> endpoint with the given role on every call, the same as dxfg_DXEndpoint_create2().
 *
 * Unlike the Java `DXEndpoint.getInstance(role)`, this function does not return a singleton.
 * Prefer dxfg_DXEndpoint_newBuilder() (or dxfg_DXEndpoint_create2()) in the new code.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] role The role of the endpoint.
 * @return The new endpoint or NULL on error. Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 * The caller owns the endpoint: close it with dxfg_DXEndpoint_close() or dxfg_DXEndpoint_closeAndAwaitTermination()
 * and free its handle with dxfg_JavaObjectHandler_release().
 */
dxfg_endpoint_t*                dxfg_DXEndpoint_getInstance2(graal_isolatethread_t *thread, dxfg_endpoint_role_t role);

/**
 * Creates a new endpoint with the #DXFG_ENDPOINT_ROLE_FEED role
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#create--">DXEndpoint.create()</a>).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @return The new endpoint or NULL on error. Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 * The caller owns the endpoint: close it with dxfg_DXEndpoint_close() or dxfg_DXEndpoint_closeAndAwaitTermination()
 * and free its handle with dxfg_JavaObjectHandler_release().
 */
dxfg_endpoint_t*                dxfg_DXEndpoint_create(graal_isolatethread_t *thread);

/**
 * Creates a new endpoint with the given role
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html">DXEndpoint.create(role)</a>).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] role The role of the endpoint.
 * @return The new endpoint or NULL on error. Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 * The caller owns the endpoint: close it with dxfg_DXEndpoint_close() or dxfg_DXEndpoint_closeAndAwaitTermination()
 * and free its handle with dxfg_JavaObjectHandler_release().
 */
dxfg_endpoint_t*                dxfg_DXEndpoint_create2(graal_isolatethread_t *thread, dxfg_endpoint_role_t role);

/**
 * Closes the endpoint: disconnects it and releases its resources. The endpoint cannot be used after that.
 *
 * The function does not wait for the notifications that are already scheduled: a state change listener
 * (including the change to #DXFG_ENDPOINT_STATE_CLOSED) or an event listener may still be called after it returns.
 * Use dxfg_DXEndpoint_closeAndAwaitTermination() to wait for them, or dxfg_JavaObjectHandler_release() of the listener
 * to stop its calls (see dxfg_JavaObjectHandler_release()).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on success or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 */
int32_t                         dxfg_DXEndpoint_close(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * Closes the endpoint and waits until all the data processing and the scheduled notifications are completed
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#closeAndAwaitTermination--">DXEndpoint.closeAndAwaitTermination()</a>).
 * Do not call it from a listener of this endpoint: it would wait for the notification that is calling it.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on success or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 */
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

/**
 * Adds the state change listener (see dxfg_PropertyChangeListener_new()).
 *
 * The state changes are delivered by a single task on the endpoint's executor that reports the change from
 * the last reported state to the current one, so the fast changes are coalesced (e.g. #DXFG_ENDPOINT_STATE_NOT_CONNECTED
 * -> #DXFG_ENDPOINT_STATE_CONNECTED without #DXFG_ENDPOINT_STATE_CONNECTING). A notification may come after
 * dxfg_DXEndpoint_close() or dxfg_DXEndpoint_removeStateChangeListener() returns.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint.
 * @param[in] listener The listener.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on success or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 */
int32_t                         dxfg_DXEndpoint_addStateChangeListener(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, dxfg_endpoint_state_change_listener_t *listener);
int32_t                         dxfg_DXEndpoint_removeStateChangeListener(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, dxfg_endpoint_state_change_listener_t *listener);
dxfg_feed_t*                    dxfg_DXEndpoint_getFeed(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
dxfg_publisher_t*               dxfg_DXEndpoint_getPublisher(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * Changes the executor of the endpoint's notifications
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html">DXEndpoint.executor(executor)</a>).
 *
 * Call it before dxfg_DXEndpoint_getFeed() (and before the other functions that use the endpoint's feed,
 * e.g. dxfg_OnDemandService_getInstance2()). Otherwise, the feed keeps using the default executor, which is shut down,
 * and the last events requests (dxfg_DXFeed_getLastEventPromise(), etc.) never complete
 * (a `RejectedExecutionException` is logged).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint.
 * @param[in] executor The executor.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on success or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 */
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

/**
 * Creates a new endpoint state change listener.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] user_func The function to call on the state changes (see dxfg_DXEndpoint_addStateChangeListener()).
 * @param[in] user_data The user data to pass to the function.
 * @return The listener or NULL on error. Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 * Releasing the last handle of the listener with dxfg_JavaObjectHandler_release() (or dxfg_NativeListener_deactivate())
 * stops the calls of the function: the user data can be freed after that.
 */
dxfg_endpoint_state_change_listener_t* dxfg_PropertyChangeListener_new(graal_isolatethread_t *thread, dxfg_endpoint_state_change_listener_func user_func, void *user_data);

/** @} */ // end of Endpoint

#ifdef __cplusplus
}
#endif

#endif // DXFG_ENDPOINT_H
