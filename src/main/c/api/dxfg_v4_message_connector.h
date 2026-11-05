// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief The message connectors of an endpoint: their state, addresses and statistics (see the group
 * @ref V4_MessageConnector).
 */

#ifndef DXFG_V4_MESSAGE_CONNECTOR_H
#define DXFG_V4_MESSAGE_CONNECTOR_H

#include "dxfg_v4_common.h"
#include "dxfg_v4_endpoint.h"
#include "dxfg_v4_javac.h"

#ifdef __cplusplus
extern "C" {
#endif

/** @defgroup V4_MessageConnector Message connectors (v4)
 *  @brief The message connectors of an endpoint: their state, addresses and statistics.
 *
 *  An endpoint connected with dxfg_v4_DXEndpoint_connect() has one message connector per address of the address
 *  string (e.g. two for `(host1:7300)(host2:7300)`): a client socket connector for `host:port`, a server socket
 *  connector for `:port`, or another kind (a file, a non-blocking or a custom connector).
 *  dxfg_v4_DXEndpoint_getConnectors() returns the current connectors of the endpoint; each call reads them anew. A
 *  connect to another address replaces the connectors: the new list has new connectors, and a replaced one still
 *  answers, as #DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED. A connect to the same address and
 *  dxfg_v4_DXEndpoint_reconnect() keep the connectors.
 *
 *  Each value of a connector is current when it is read; the values are not read together as one consistent set.
 *  Reading them changes nothing: the statistics are not reset, and other readers (QD's JMX beans) see the same values.
 *
 *  A connector handle is released with dxfg_v4_JavaObjectHandler_release() and a list of them with
 *  dxfg_v4_message_connector_list_release(); dxfg_v4_JavaObjectHandler_clone() keeps a connector of a list after the
 *  list is released. The handles and the lists can be released in any order, also after the endpoint is closed.
 *
 *  A listener (dxfg_v4_MessageConnectorListener_new(), experimental) added to an endpoint with
 *  dxfg_v4_DXEndpoint_addMessageConnectorListener() is called after the state changes of its connectors, also of the
 *  connectors that a later connect creates.
 *
 *  #DXFG_V4_FEATURE_MESSAGE_CONNECTORS reports the functions of the group.
 *
 *  Java: `com.devexperts.qd.qtp.MessageConnector` of `com.dxfeed.api.impl.DXEndpointImpl.getQDEndpoint()`.
 *  @{
 */

/**
 * @brief The kind of a message connector: a client socket connector (an address `host:port`).
 * @since 3.14.0
 */
#define DXFG_V4_MESSAGE_CONNECTOR_KIND_CLIENT_SOCKET ((int32_t)0)

/**
 * @brief The kind of a message connector: a server socket connector (an address `:port`).
 * @since 3.14.0
 */
#define DXFG_V4_MESSAGE_CONNECTOR_KIND_SERVER_SOCKET ((int32_t)1)

/**
 * @brief The kind of a message connector: any other connector (a file, a non-blocking or a custom connector).
 *
 * New kinds may be added; an application treats an unknown value as this one.
 * @since 3.14.0
 */
#define DXFG_V4_MESSAGE_CONNECTOR_KIND_OTHER ((int32_t)2)

/**
 * @brief The state of a message connector: started, not connected yet, or reconnecting after a failure or a drop.
 *
 * Java: `MessageConnectorState.CONNECTING`.
 * @since 3.14.0
 */
#define DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTING ((int32_t)0)

/**
 * @brief The state of a message connector: at least one of its connections is established (a server socket connector:
 * it accepts connections).
 *
 * Java: `MessageConnectorState.CONNECTED`.
 * @since 3.14.0
 */
#define DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED ((int32_t)1)

/**
 * @brief The state of a message connector: stopped, e.g. replaced by a connect to another address, or its endpoint
 * disconnected or closed.
 *
 * Java: `MessageConnectorState.DISCONNECTED`.
 * @since 3.14.0
 */
#define DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED ((int32_t)2)

/**
 * @brief The handle of a message connector (dxfg_v4_DXEndpoint_getConnectors()).
 * @since 3.14.0
 */
typedef struct dxfg_v4_message_connector_t {
    /** The handle of the Java object. */
    dxfg_java_object_handler handler;
} dxfg_v4_message_connector_t;

/**
 * @brief A list of message connectors (dxfg_v4_DXEndpoint_getConnectors()).
 *
 * The SDK allocates it with its array of elements and the elements: release it with
 * dxfg_v4_message_connector_list_release().
 * @since 3.14.0
 */
typedef struct dxfg_v4_message_connector_list {
    /** The number of connectors; 0 for an empty list. */
    int32_t size;
    /** The array of `size` pointers to the connector handles; NULL for an empty list. */
    dxfg_v4_message_connector_t **elements;
} dxfg_v4_message_connector_list;

/**
 * @brief The statistics of a message connector (dxfg_v4_MessageConnector_getStatistics()).
 *
 * The counters cover all the connections of the connector since it was created, the closed ones included. The layout
 * of the struct does not change: new counters will come with a new function.
 * @since 3.14.0
 */
typedef struct dxfg_v4_message_connector_stats_t {
    /** The bytes read, heartbeats included: they grow on an idle connection too. */
    int64_t read_bytes;
    /** The bytes written, heartbeats included. */
    int64_t written_bytes;
    /** The data records read: they grow only when data arrives. */
    int64_t read_data_records;
    /** The data records written. */
    int64_t written_data_records;
    /** The open connections. */
    int64_t active_connection_count;
    /** The connections closed since the connector was created. */
    int64_t closed_connection_count;
} dxfg_v4_message_connector_stats_t;

/**
 * @brief The handle of a listener of the state changes of the message connectors of an endpoint
 * (dxfg_v4_MessageConnectorListener_new()).
 * @experimental
 * @since 3.14.0
 */
typedef struct dxfg_v4_message_connector_listener_t {
    /** The handle of the Java object. */
    dxfg_java_object_handler handler;
} dxfg_v4_message_connector_listener_t;

/**
 * @brief The function of a listener of the state changes of the message connectors of an endpoint
 * (dxfg_v4_MessageConnectorListener_new()).
 *
 * It is called after the state of a connector of an endpoint to which the listener is added has changed: it connected,
 * lost its connection, started to reconnect, or was replaced or closed. The function reads the current values of the
 * connector (dxfg_v4_MessageConnector_getState() and the other getters); fast changes of one connector may come as one
 * call. It is called on one thread of the SDK for all the listeners of the isolate (named
 * `dxfg-MessageConnectorListener`), never on a QD thread and never within the call that changes the state (e.g.
 * dxfg_v4_DXEndpoint_connect()), one call at a time. The function may call any function of the SDK on that thread,
 * including dxfg_v4_DXEndpoint_connect() of its endpoint, the getters of the connector and the release of its own
 * listener; it should return quickly, since the next call waits for it.
 *
 * @param[in] thread The current GraalVM Isolate's thread (the thread of the SDK that calls the function).
 * @param[in] connector The connector whose state changed. The handle is valid during the call only and is released by
 * the SDK; dxfg_v4_JavaObjectHandler_clone() keeps the connector after the call.
 * @param[in] user_data The user data given to dxfg_v4_MessageConnectorListener_new().
 * @experimental
 * @since 3.14.0
 */
typedef void (*dxfg_v4_message_connector_listener_func)(graal_isolatethread_t *thread,
                                                        dxfg_v4_message_connector_t *connector, void *user_data);

/**
 * @brief Returns the current message connectors of an endpoint.
 *
 * Each call reads the connectors anew, so the connectors replaced by a connect to another address are never returned.
 * The list is empty for an endpoint that is not connected, that has the role `LOCAL_HUB`, or that is closed.
 *
 * Java: `QDEndpoint.getConnectors()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint; must not be NULL.
 * @param[out] connectors The pointer to the variable that receives the list (release it with
 * dxfg_v4_message_connector_list_release()); must not be NULL. It is written on success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_DXEndpoint_getConnectors(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint,
                                         DXFG_OUT dxfg_v4_message_connector_list **connectors);

/**
 * @brief Releases a list of message connectors: every connector handle of the list, its array of elements and the
 * list itself.
 *
 * The connectors cloned from the list with dxfg_v4_JavaObjectHandler_clone() stay valid.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connectors The list (dxfg_v4_DXEndpoint_getConnectors()); NULL is a no-op.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_message_connector_list_release(graal_isolatethread_t *thread,
                                               dxfg_v4_message_connector_list *connectors);

/**
 * @brief Returns the name of a message connector, as in QD's log and JMX (e.g. `ClientSocket-Distributor`).
 *
 * The connectors of different endpoints may have the same name.
 *
 * Java: `MessageConnector.getName()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] name The pointer to the variable that receives the name (release it with dxfg_v4_String_release());
 * must not be NULL. It is written on success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_getName(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector,
                                         DXFG_OUT const char **name);

/**
 * @brief Returns the kind of a message connector.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] kind The pointer to the variable that receives the kind: #DXFG_V4_MESSAGE_CONNECTOR_KIND_CLIENT_SOCKET,
 * #DXFG_V4_MESSAGE_CONNECTOR_KIND_SERVER_SOCKET or #DXFG_V4_MESSAGE_CONNECTOR_KIND_OTHER; must not be NULL. It is
 * written on success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_getKind(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector,
                                         DXFG_OUT int32_t *kind);

/**
 * @brief Returns the address a message connector was configured with (e.g. `127.0.0.1:7300`, `:7400`), the user and
 * the password masked as in QD's log.
 *
 * Java: `MessageConnector.getAddress()`, `LogUtil.hideCredentials(Object)`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] address The pointer to the variable that receives the address (release it with
 * dxfg_v4_String_release()); must not be NULL. It is written on success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_getAddress(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector,
                                            DXFG_OUT const char **address);

/**
 * @brief Returns the `host:port` addresses a client socket connector is connected to now, one per connected
 * connection (a striped connector has several).
 *
 * The list is empty when the connector is not connected, and always empty for the other kinds of connectors.
 *
 * Java: `ClientSocketConnector.getCurrentAddresses()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] addresses The pointer to the variable that receives the list of the addresses (release it with
 * dxfg_v4_string_list_release()); must not be NULL. It is written on success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_getCurrentAddresses(graal_isolatethread_t *thread,
                                                     dxfg_v4_message_connector_t *connector,
                                                     DXFG_OUT dxfg_v4_string_list **addresses);

/**
 * @brief Returns the state of a message connector.
 *
 * Java: `MessageConnector.getState()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] state The pointer to the variable that receives the state:
 * #DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTING, #DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED or
 * #DXFG_V4_MESSAGE_CONNECTOR_STATE_DISCONNECTED; must not be NULL. It is written on success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_getState(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector,
                                          DXFG_OUT int32_t *state);

/**
 * @brief Checks whether a message connector is started (it connects or reconnects), as opposed to stopped.
 *
 * Java: `MessageConnector.isActive()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] active The pointer to the variable that receives 1 if the connector is started, 0 otherwise; must not
 * be NULL. It is written on success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_isActive(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector,
                                          DXFG_OUT int32_t *active);

/**
 * @brief Returns the number of the open connections of a message connector: those of a client socket connector
 * (more than one for a striped connector) or those that a server socket connector accepted.
 *
 * Java: `MessageConnector.getConnectionCount()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] count The pointer to the variable that receives the number; must not be NULL. It is written on success
 * only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_getConnectionCount(graal_isolatethread_t *thread,
                                                    dxfg_v4_message_connector_t *connector, DXFG_OUT int32_t *count);

/**
 * @brief Returns the number of the connections of a message connector closed since it was created: it grows on each
 * drop and on each reconnect.
 *
 * Java: `MessageConnector.getClosedConnectionCount()`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] count The pointer to the variable that receives the number; must not be NULL. It is written on success
 * only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_getClosedConnectionCount(graal_isolatethread_t *thread,
                                                          dxfg_v4_message_connector_t *connector,
                                                          DXFG_OUT int64_t *count);

/**
 * @brief Reads the statistics of a message connector: the bytes and data records read and written, the open and the
 * closed connections.
 *
 * Nothing is reset: other readers of QD's statistics see the same values. Data arrives when `read_data_records` grows
 * between two calls; `read_bytes` grows on heartbeats too.
 *
 * Java: `MessageConnector.retrieveCompleteEndpointStats()`, and `QDStats.getValue(SValue)` of
 * `MessageConnector.getStats()` with `IO_DATA_READ_RECORDS` and `IO_DATA_WRITE_RECORDS`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] connector The connector; must not be NULL.
 * @param[out] stats The pointer to the structure that receives the statistics; must not be NULL. It is written on
 * success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @since 3.14.0
 */
int32_t dxfg_v4_MessageConnector_getStatistics(graal_isolatethread_t *thread, dxfg_v4_message_connector_t *connector,
                                               DXFG_OUT dxfg_v4_message_connector_stats_t *stats);

/**
 * @brief Creates a new listener of the state changes of the message connectors of an endpoint.
 *
 * The listener does nothing until it is added to an endpoint with dxfg_v4_DXEndpoint_addMessageConnectorListener().
 * The function is called as described at dxfg_v4_message_connector_listener_func.
 *
 * Releasing the last handle of the listener with dxfg_v4_JavaObjectHandler_release() (or
 * dxfg_v4_NativeListener_deactivate(), or dxfg_v4_NativeListener_deactivateAll()) stops the calls of the function:
 * after the release returns the function is not running (except on the releasing thread itself, when the function
 * releases its own listener) and is not called anymore, so the user data can be freed. A released listener that is
 * still added removes itself from the endpoint at the next state change of a connector.
 *
 * Java: `com.devexperts.qd.qtp.MessageConnectorListener.stateChanged(MessageConnector)`, called on a thread of the SDK.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] user_func The function to call after the state changes; must not be NULL.
 * @param[in] user_data The user data to pass to the function; the SDK does not use it. It must stay valid until the
 * listener is deactivated.
 * @param[out] listener The pointer to the variable that receives the listener (release its handle with
 * dxfg_v4_JavaObjectHandler_release()); must not be NULL. It is written on success only.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @experimental
 * @since 3.14.0
 */
DXFG_EXPERIMENTAL
int32_t dxfg_v4_MessageConnectorListener_new(graal_isolatethread_t *thread,
                                             dxfg_v4_message_connector_listener_func user_func, void *user_data,
                                             DXFG_OUT dxfg_v4_message_connector_listener_t **listener);

/**
 * @brief Adds a listener of the state changes of the message connectors to an endpoint.
 *
 * The listener gets the changes of the current connectors of the endpoint and of the connectors that a later
 * dxfg_v4_DXEndpoint_connect() creates; it does not get the current states at the add, so read them with
 * dxfg_v4_DXEndpoint_getConnectors() after it. Adding a listener that is added already changes nothing. One listener
 * may be added to several endpoints.
 *
 * Java: `QDEndpoint.addMessageConnectionListener(MessageConnectorListener)`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint; must not be NULL.
 * @param[in] listener The listener; must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @experimental
 * @since 3.14.0
 */
DXFG_EXPERIMENTAL
int32_t dxfg_v4_DXEndpoint_addMessageConnectorListener(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint,
                                                       dxfg_v4_message_connector_listener_t *listener);

/**
 * @brief Removes a listener of the state changes of the message connectors from an endpoint.
 *
 * Removing a listener that is not added is no error. A call of the function that is already running finishes; to wait
 * until no call runs, release the listener.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The endpoint; must not be NULL.
 * @param[in] listener The listener; must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 * @experimental
 * @since 3.14.0
 */
DXFG_EXPERIMENTAL
int32_t dxfg_v4_DXEndpoint_removeMessageConnectorListener(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint,
                                                          dxfg_v4_message_connector_listener_t *listener);

/** @} */

#ifdef __cplusplus
}
#endif

#endif // DXFG_V4_MESSAGE_CONNECTOR_H
