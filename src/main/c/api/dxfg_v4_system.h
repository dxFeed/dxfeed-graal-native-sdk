// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief The v4 functions of dxfg_system.h and the v4 names of its types (see the group @ref V4_System).
 */

#ifndef DXFG_V4_SYSTEM_H
#define DXFG_V4_SYSTEM_H

#include "dxfg_system.h"
#include "dxfg_v4_common.h"

#ifdef __cplusplus
extern "C" {
#endif

/** @defgroup V4_System System (v4)
 *  @brief The v4 functions and type names of the group @ref System. Every function returns
 *  #DXFG_EXECUTE_SUCCESSFULLY or #DXFG_EXECUTE_FAIL and writes its result to a #DXFG_OUT parameter,
 *  see @ref V4_Conventions.
 *  @{
 */

/**
 * @brief Sets the Java system property indicated by the specified key.
 *
 * The property is a property of the Java system of the isolate (`System.setProperty`): the QD library and the SDK
 * read their settings from it, e.g. the properties of the endpoints (`dxfeed.address`, `dxfeed.threadPoolSize`, ...)
 * and of the logging (`log.className`, `log.level`, see the group @ref Logging). The properties that configure
 * endpoints must be set before the first endpoint is created. For example:
 * - `com.dxfeed.sdk.TimeSyncTracker.enable` = `true` enables the QD `TimeSyncTracker`, which sends UDP multicast
 *   packets (239.192.51.45:5145 by default, see `com.devexperts.qd.tools.TimeSyncTracker.addr` and `.port`)
 *   to compare the clocks of the hosts. The SDK disables it by default.
 *
 * Java: `System.setProperty(String, String)`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] key The name of the property, a NUL-terminated UTF-8 string; the SDK copies it. It must not be NULL or
 * empty.
 * @param[in] value The value of the property, a NUL-terminated UTF-8 string; the SDK copies it. It must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_system_set_property(graal_isolatethread_t *thread, const char *key, const char *value);

/**
 * @brief Gets the Java system property indicated by the specified key.
 *
 * Java: `System.getProperty(String)`.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] key The name of the property, a NUL-terminated UTF-8 string. It must not be NULL or empty.
 * @param[out] value The pointer to the variable that receives the value of the property as a NUL-terminated UTF-8
 * string allocated by the SDK (release it with dxfg_v4_system_release_property()), or NULL if the property is not set;
 * must not be NULL.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_system_get_property(graal_isolatethread_t *thread, const char *key, DXFG_OUT const char **value);

/**
 * @brief Releases a property value that dxfg_v4_system_get_property() returned.
 *
 * The string is freed: do not use it after that. Passing NULL does nothing.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] value The value returned by dxfg_v4_system_get_property().
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_v4_get_and_clear_thread_exception() to determine if an exception was thrown.
 */
int32_t dxfg_v4_system_release_property(graal_isolatethread_t *thread, const char *value);

/**
 * @brief Closes all the endpoints and instrument profile connections created through the SDK in this isolate
 * and waits for the termination of the endpoints.
 *
 * It includes the default endpoints of dxfg_v4_DXEndpoint_getInstance(), dxfg_v4_DXFeed_getInstance(),
 * dxfg_v4_DXPublisher_getInstance() and dxfg_v4_OnDemandService_getInstance(). The handles of the objects stay valid
 * and must be released as usual.
 *
 * Call it before graal_tear_down_isolate() (or before the process exits): the threads of an open endpoint
 * ("DXEndpoint-DXExecutorThread-N", etc.) do not stop on `Thread.interrupt()`, so graal_tear_down_isolate()
 * waits for them forever while any endpoint is open. To stop the calls of the native listeners, release them
 * (see dxfg_v4_JavaObjectHandler_release()) before tearing down the isolate. To find the threads that block
 * the tear-down, pass the `-XX:TearDownWarningSeconds=<seconds>` option in `graal_create_isolate_params_t.argv`.
 *
 * Recommended shutdown sequence:
 * 1. Release the listeners (dxfg_v4_JavaObjectHandler_release() or dxfg_v4_NativeListener_deactivate()).
 * 2. dxfg_v4_system_close_all_and_await_termination().
 * 3. Release the remaining handles.
 * 4. graal_tear_down_isolate().
 *
 * The function blocks the calling thread until the endpoints are terminated: do not call it from a listener of an
 * endpoint, it would wait for the notification that is calling it. The endpoints and connections created after the
 * call are not closed by it. The function is a function of the SDK: there is no QD method that it calls.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error (also if
 * the waiting thread is interrupted). Use dxfg_v4_get_and_clear_thread_exception() to determine if an exception was
 * thrown.
 */
int32_t dxfg_v4_system_close_all_and_await_termination(graal_isolatethread_t *thread);

/** @} */ // end of V4_System

#ifdef __cplusplus
}
#endif

#endif // DXFG_V4_SYSTEM_H
