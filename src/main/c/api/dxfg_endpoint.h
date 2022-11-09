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
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html">Javadoc</a>
 */
typedef struct dxfg_endpoint_t {
    void *java_object_handle;
} dxfg_endpoint_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.State.html">Javadoc</a>
 */
typedef enum dxfg_endpoint_state_t {
    DXFG_ENDPOINT_STATE_NOT_CONNECTED = 0,
    DXFG_ENDPOINT_STATE_CONNECTING,
    DXFG_ENDPOINT_STATE_CONNECTED,
    DXFG_ENDPOINT_STATE_CLOSED,
} dxfg_endpoint_state_t;

/**
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/beans/PropertyChangeListener.html">Javadoc</a>
 */
typedef void (*dxfg_endpoint_state_change_listener)(graal_isolatethread_t *thread, dxfg_endpoint_state_t old_state,
                                                dxfg_endpoint_state_t new_state, void *user_data);

int32_t dxfg_endpoint_create(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t dxfg_endpoint_close(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t dxfg_endpoint_close_and_await_termination(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t dxfg_endpoint_connect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, const char *address);
int32_t dxfg_endpoint_reconnect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t dxfg_endpoint_disconnect(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t dxfg_endpoint_disconnect_and_clear(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t dxfg_endpoint_await_not_connected(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint);
int32_t dxfg_endpoint_get_state(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint, dxfg_endpoint_state_t *state);
int32_t dxfg_endpoint_add_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint,
                                                dxfg_endpoint_state_change_listener listener, void *user_data);
int32_t dxfg_endpoint_remove_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_t *endpoint,
                                                   dxfg_endpoint_state_change_listener listener);

/** @} */ // end of Endpoint

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_ENDPOINT_H_
