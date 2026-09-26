// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK System functions and types declarations
 */

#ifndef DXFG_SYSTEM_H
#define DXFG_SYSTEM_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "graal_isolate.h"

/** @defgroup System
 *  @{
 */

/**
 * @brief Sets the system property indicated by the specified key.
 *
 * The properties that configure endpoints must be set before the first endpoint is created. For example:
 * - `com.dxfeed.sdk.TimeSyncTracker.enable` = `true` enables the QD `TimeSyncTracker`, which sends UDP multicast
 *   packets (239.192.51.45:5145 by default, see `com.devexperts.qd.tools.TimeSyncTracker.addr` and `.port`)
 *   to compare the clocks of the hosts. The SDK disables it by default.
 */
int32_t dxfg_system_set_property(graal_isolatethread_t *thread, const char *key, const char *value);

/**
 * @brief Gets the system property indicated by the specified key.
 */
const char *dxfg_system_get_property(graal_isolatethread_t *thread, const char *key);

/**
 * @brief Frees pointer that was previously allocated in Java method (by UnmanagedMemory).
 */
int32_t dxfg_system_release_property(graal_isolatethread_t *thread, const char *value);

/**
 * @brief Closes all the endpoints and instrument profile connections created through the SDK in this isolate
 * and waits for the termination of the endpoints.
 *
 * It includes the default endpoints of dxfg_DXEndpoint_getInstance(), dxfg_DXFeed_getInstance(),
 * dxfg_DXPublisher_getInstance() and dxfg_OnDemandService_getInstance(). The handles of the objects stay valid
 * and must be released as usual.
 *
 * Call it before graal_tear_down_isolate() (or before the process exits): the threads of an open endpoint
 * ("DXEndpoint-DXExecutorThread-N", etc.) do not stop on `Thread.interrupt()`, so graal_tear_down_isolate()
 * waits for them forever while any endpoint is open. To stop the calls of the native listeners, release them
 * (see dxfg_JavaObjectHandler_release()) before tearing down the isolate. To find the threads that block
 * the tear-down, pass the `-XX:TearDownWarningSeconds=<seconds>` option in `graal_create_isolate_params_t.argv`.
 *
 * Recommended shutdown sequence:
 * 1. Release the listeners (dxfg_JavaObjectHandler_release() or dxfg_NativeListener_deactivate()).
 * 2. dxfg_system_close_all_and_await_termination().
 * 3. Release the remaining handles.
 * 4. graal_tear_down_isolate().
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on success or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to get the exception.
 */
int32_t dxfg_system_close_all_and_await_termination(graal_isolatethread_t *thread);

/** @} */ // end of System

#ifdef __cplusplus
}
#endif

#endif // DXFG_SYSTEM_H
