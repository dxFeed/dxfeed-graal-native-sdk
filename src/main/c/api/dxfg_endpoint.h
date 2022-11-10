// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_ENDPOINT_H_
#define DXFEED_GRAAL_NATIVE_API_ENDPOINT_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "graal_isolate.h"

/** @defgroup Endpoint
 *  @{
 */

/**
 * @brief The DXEndpoint.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html">Javadoc</a>
 */
typedef struct dxfg_endpoint_t {
    void *java_object_handle;
} dxfg_endpoint_t;

/**
 * @brief The DXEndpoint.Builder.
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Builder.html">Javadoc</a>
 */
typedef struct dxfg_endpoint_builder_t {
    void *java_object_handle;
} dxfg_endpoint_builder_t;

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
 * @brief Function pointer to endpoint state change listener.
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] old_state The old endpoint state.
 * @param[in] new_state The new endpoint state.
 * @param[in,out] user_data The pointer to user data.
 */
typedef void (*dxfg_endpoint_state_change_listener)(graal_isolatethread_t *thread, dxfg_endpoint_state_t old_state,
                                                    dxfg_endpoint_state_t new_state, void *user_data);

/** @defgroup Builder
 *  @{
 */

/**
 * @brief Creates new dxfg_endpoint_builder_t instance.
 * The created builder must be released with dxfg_endpoint_builder_close() after use.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#newBuilder--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[out] builder The created endpoint builder.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_builder_create(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder);

/**
 * @brief Closes this dxfg_endpoint_builder_t and release all associated resources.
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[out] builder The endpoint builder.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_builder_close(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder);

/**
 * @brief Sets role for the created endpoint.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Builder.html#withRole-com.dxfeed.api.DXEndpoint.Role-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] builder The endpoint builder.
 * @param[in] role The role of endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_builder_with_role(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder,
                                        dxfg_endpoint_role_t role);

/**
 * @brief Sets name for the created endpoint.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Builder.html#withName-java.lang.String-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] builder The endpoint builder.
 * @param[in] name The endpoint name.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_builder_with_name(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder,
                                        const char *name);

/**
 * @brief Sets the specified property. Unsupported properties are ignored.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Builder.html#withProperties-java.util.Properties-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] builder The endpoint builder.
 * @param[in] key The name of the property.
 * @param[in] value The value of the property.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_builder_with_property(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder,
                                            const char *key, const char *value);

/**
 * @brief Checks if the property is supported.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Builder.html#supportsProperty-java.lang.String-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] builder The endpoint builder.
 * @param[in] key The name of the property.
 * @param[out] isSupports Sets to true (non-zero) if the property with the specified name supported; otherwise false.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_builder_supports_property(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder,
                                                const char *key, int32_t *isSupports);

/**
 * @brief Builds dxfg_endpoint_t instance.
 * <br><a href= "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.Builder.html#build--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] builder The endpoint builder.
 * @param[out] endpoint The created endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_builder_build(graal_isolatethread_t *thread, dxfg_endpoint_builder_t *builder,
                                    dxfg_endpoint_t *endpoint);

/** @} */ // end of Builder

/**
 * @brief Creates new dxfg_endpoint_t instance.
 * The shortcut for  dxfg_endpoint_builder_create()->dxfg_endpoint_builder_build().
 * The created endpoint should be closed with dxfg_endpoint_close()
 * or dxfg_endpoint_close_and_await_termination() after use.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#create--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[out] endpoint The created endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_create(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Creates an dxfg_endpoint_t with a specified role. The shortcut for
 * dxfg_endpoint_builder_create()->dxfg_endpoint_builder_with_role(role)->dxfg_endpoint_builder_build().
 * The created endpoint should be closed with dxfg_endpoint_close()
 * or dxfg_endpoint_close_and_await_termination() after use.
 * <br><a
 * href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#create-com.dxfeed.api.DXEndpoint.Role-">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[out] endpoint The created endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_create_with_role(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint,
                                       dxfg_endpoint_role_t role);

/**
 * @brief Closes this endpoint and release all associated resources.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#close--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_close(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Closes this endpoint, wait until all pending data processing tasks are completed
 * and release all associated resources.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#closeAndAwaitTermination--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_close_and_await_termination(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Returns the role of this endpoint.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#getRole--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @param[out] role The role of this endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_get_role(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, dxfg_endpoint_role_t *role);

/**
 * @brief Changes user name for this endpoint.
 * This method shall be called before dxfg_endpoint_connect() together with dxfg_endpoint_set_password()
 * to configure service access credentials.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#user-java.lang.String-">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @param[in] user The user name.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_set_user(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, const char *user);

/**
 * @brief Changes password for this endpoint.
 * This method shall be called before dxfg_endpoint_connect() together with dxfg_endpoint_set_user()
 * to configure service access credentials.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#password-java.lang.String-">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @param[in] password The user password.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_set_password(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, const char *password);

/**
 * @brief Connects to the specified remote address.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#connect-java.lang.String-">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @param[in] address The data source address.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_connect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, const char *address);

/**
 * @brief Terminates all established network connections and initiates connecting again with the same address.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#reconnect--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_reconnect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Terminates all remote network connections.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#disconnect--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_disconnect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Terminates all remote network connections and clears stored data.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#disconnectAndClear--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_disconnect_and_clear(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Waits until this endpoint stops processing data (becomes quiescent).
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#awaitProcessed--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_await_processed(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Waits while this endpoint state becomes DXFG_ENDPOINT_STATE_NOT_CONNECTED or DXFG_ENDPOINT_STATE_CLOSED.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#awaitNotConnected--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_await_not_connected(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Returns the state of this endpoint.
 * <br><a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#getState--">Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @param[out] state The current endpoint state.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_get_state(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, dxfg_endpoint_state_t *state);

/**
 * @brief Adds listener that is notified about changes in state connections.
 * Installed listener can be removed with dxfg_endpoint_remove_state_change_listener() function.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#addStateChangeListener-java.beans.PropertyChangeListener-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @param[in] listener The listener to add.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_add_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint,
                                                dxfg_endpoint_state_change_listener listener, void *user_data);

/**
 * @brief Removes listener that is notified about changes in state connections.
 * It removes the listener that was previously installed with dxfg_endpoint_add_state_change_listener() function.
 * <br><a href=
 * "https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#removeStateChangeListener-java.beans.PropertyChangeListener-">
 * Javadoc</a>
 * @param[in] thread The pointer to a run-time data structure for the thread.
 * @param[in] endpoint The endpoint.
 * @param[in] listener The listener to remove.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
int32_t dxfg_endpoint_remove_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint,
                                                   dxfg_endpoint_state_change_listener listener);

/** @} */ // end of Endpoint

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_ENDPOINT_H_
