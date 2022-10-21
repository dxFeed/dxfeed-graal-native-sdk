// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_ENDPOINT_H_
#define DXFEED_GRAAL_NATIVE_API_ENDPOINT_H_

#ifdef __cplusplus
extern "C" {
#endif

#include "dxfg_error_codes.h"
#include "graal_isolate.h"

/** @defgroup Endpoint
 *  @{
 */

/**
 * @brief Manages network connections to feed.
 * Used for connect/disconnect from remote hosts, receive instances of other classes.
 * Represents Java class
 * com.dxfeed.api.DXEndpoint
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html">Javadoc</a>).
 * The opaque representation of a handle to a Java object given out to unmanaged code.
 * Clients must not interpret or dereference the value.
 */
typedef int64_t dxfg_endpoint_t;

/**
 * @brief Represents the current state of endpoint.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.State.html">Javadoc</a>)
 */
typedef enum dxfg_endpoint_state_t {
    /**
     * @brief Endpoint was created by is not connected to remote endpoints.
     * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.State.html#NOT_CONNECTED">Javadoc</a>)
     */
    DXFG_ENDPOINT_STATE_NOT_CONNECTED = 0,

    /**
     * @brief The dxfg_endpoint_connect() method was called to establish connection to remove endpoint,
     * but connection is not actually established yet or was lost.
     * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.State.html#CONNECTING">Javadoc</a>)
     */
    DXFG_ENDPOINT_STATE_CONNECTING,

    /**
     * @brief The connection to remote endpoint is established.
     * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.State.html#CONNECTED">Javadoc</a>)
     */
    DXFG_ENDPOINT_STATE_CONNECTED,

    /**
     * @brief Endpoint was dxfg_endpoint_close() closed.
     * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.State.html#CLOSED">Javadoc</a>)
     */
    DXFG_ENDPOINT_STATE_CLOSED,
    /**
     * @brief Unknown state.
     */
    DXFG_ENDPOINT_STATE_UNKNOWN,
} dxfg_endpoint_state_t;

/**
 * @brief Endpoint state change listener.
 * (<a href="https://docs.oracle.com/javase/8/docs/api/java/beans/PropertyChangeListener.html">Javadoc</a>)
 */
typedef void (*dxfg_endpoint_on_change_state)(dxfg_endpoint_state_t old_state,
                                                     dxfg_endpoint_state_t new_state);

/**
 * @brief Creates new dxfg_endpoint_t instance.
 * dxfg_endpoint_t is used for connect/disconnect from remote hosts, receive instances of other classes.
 * The created endpoint should be closed with dxfg_endpoint_close().
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#create--">Javadoc</a>)
 * @param[in]  thread The pointer to the runtime data structure for a thread.
 * @param[out] endpoint The created endpoint.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_endpoint_create(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);

/**
 * @brief Closes this endpoint.
 * All network connection are terminated as with disconnect method and no further connections can be established.
 * The endpoint state immediately becomes DXFG_ENDPOINT_STATE_CLOSED. All resources associated with this endpoint
 * are released. Once closed, the dxfg_endpoint_t descriptor cannot be used in other functions.
 * Removes all internal ref to DXEndpoint.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#close--">Javadoc</a>)
 * @param[in] thread  The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The descriptor that was created by dxfg_endpoint_create().
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_endpoint_close(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint);

/**
 * @brief Connects to the specified remote address.
 * Previously established connections are closed if the new address is different from the old one.
 * This method does nothing if address does not change or if this endpoint is DXFG_ENDPOINT_STATE_CLOSED.
 * The endpoint state immediately becomes DXFG_ENDPOINT_STATE_CONNECTING otherwise.
 * The address string is provided with the market data vendor agreement.
 * Use "demo.dxfeed.com:7300" for a demo quote feed.
 * The simplest address strings have the following format:
 * <ul>
 *  <li> host:port       To establish a TCP/IP connection.
 *  <li> file:filename   Connects to a file.
 * </ul>
 * @note This method does not wait until connection actually gets established.
 * The actual connection establishment happens asynchronously after the invocation of this method.
 * However, this method waits until notification about state transition
 * from DXFG_ENDPOINT_STATE_NOT_CONNECTED to DXFG_ENDPOINT_STATE_CONNECTING gets processed
 * by all listeners that were installed via dxfg_endpoint_add_state_change_listener() method.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#connect-java.lang.String-">Javadoc</a>)
 * @param[in] thread  The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The descriptor that was created by dxfg_endpoint_create().
 * @param[in] address  The address string.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_endpoint_connect(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint,
                                      const char *address);

/**
 * @brief Terminates all established network connections and initiates connecting again with the same address.
 * The effect of the method is alike to invoking dxfg_endpoint_disconnect()
 * and dxfg_endpoint_connect() with the current address,
 * but internal resources used for connections may be reused by implementation.
 * @note The method will not connect endpoint that was not initially connected
 * with dxfg_endpoint_connect() method or was disconnected with dxfg_endpoint_disconnect() method.
 * The method initiates a short-path way for reconnecting, so whether observers
 * will have a chance to see an intermediate state
 * DXFG_ENDPOINT_STATE_NOT_CONNECTED depends on the implementation.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#reconnect--">Javadoc</a>)
 * @param[in] thread  The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The descriptor that was created by dxfg_endpoint_create().
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_endpoint_reconnect(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint);

/**
 * @brief Terminates all remote network connections.
 * This method does nothing if this endpoint is DXFG_ENDPOINT_STATE_CLOSED.
 * The endpoint state immediately becomes DXFG_ENDPOINT_STATE_NOT_CONNECTED otherwise.
 * This method does not release all resources that are associated with this endpoint.
 * Use dxfg_endpoint_close() method to release all resources.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#disconnect--">Javadoc</a>)
 * @param[in] thread  The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The descriptor that was created by dxfg_endpoint_create().
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_endpoint_disconnect(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint);

/**
 * @brief Terminates all remote network connections and clears stored data.
 * This method does nothing if this endpoint is DXFG_ENDPOINT_STATE_CLOSED.
 * The endpoint state immediately becomes DXFG_ENDPOINT_STATE_NOT_CONNECTED otherwise.
 * This method does not release all resources that are associated with this endpoint.
 * Use dxfg_endpoint_close() method to release all resources.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#disconnectAndClear--">Javadoc</a>)
 * @param[in] thread  The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The descriptor that was created by dxfg_endpoint_create().
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_endpoint_disconnect_and_clear(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint);

/**
 * @brief Returns the state of this endpoint.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#getState--">Javadoc</a>)
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The descriptor that was created by dxfg_endpoint_create().
 * @return The endpoint state. State may be DXFG_ENDPOINT_STATE_UNKNOWN if descriptor not exist
 * or Java implementation returns unknown state.
 */
dxfg_endpoint_state_t dxfg_endpoint_get_state(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint);

/**
 * @brief Adds listener that is notified about changes in state connections.
 * Installed listener can be removed with dxfg_endpoint_remove_state_change_listener() method.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#addStateChangeListener-java.beans.PropertyChangeListener-">Javadoc</a>)
 * @param[in] thread  The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The descriptor that was created by dxfg_endpoint_create().
 * @param[in] listener The listener to add.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_endpoint_add_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint,
                                                        dxfg_endpoint_on_change_state listener);

/**
 * @brief Removes listener that is notified about changes in state property.
 * It removes the listener that was previously installed with dxfg_endpoint_add_state_change_listener() method.
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html#removeStateChangeListener-java.beans.PropertyChangeListener-">Javadoc</a>)
 * @param[in] thread  The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The descriptor that was created by dxfg_endpoint_create().
 * @param[in] listener The listener to remove.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_endpoint_remove_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint,
                                                           dxfg_endpoint_on_change_state listener);

/** @} */ // end of Endpoint

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_ENDPOINT_H_