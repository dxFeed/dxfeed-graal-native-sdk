// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK Logging functions and types declarations
 */

#ifndef DXFG_LOGGING_H
#define DXFG_LOGGING_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "dxfg_catch_exception.h"
#include "dxfg_javac.h"
#include "graal_isolate.h"

/** @defgroup Logging
 *  @{
 */

/**
 * Defines a set of standard logging levels that can be used to control logging output.
 */
typedef enum dxfg_logging_level_t {
    /// Indicates that all messages should be logged.
    DXFG_LOGGING_LEVEL_ALL = 0,

    /// Indicates a highly detailed tracing message.
    DXFG_LOGGING_LEVEL_TRACE,

    /// It is a message level providing tracing debug information.
    DXFG_LOGGING_LEVEL_DEBUG,

    /// It is a message level for informational messages.
    DXFG_LOGGING_LEVEL_INFO,

    /// It is a message level indicating a potential problem.
    DXFG_LOGGING_LEVEL_WARN,

    /// It is a message level indicating a serious failure.
    DXFG_LOGGING_LEVEL_ERROR,

    /// It is a special level that can be used to turn off logging.
    DXFG_LOGGING_LEVEL_OFF,
} dxfg_logging_level_t;

/**
 * A handle of a wrapper class for a listener that allows to intercept logged messages.
 */
typedef struct dxfg_logging_listener_t {
    dxfg_java_object_handler handler;
} dxfg_logging_listener_t;

/// A callback type for the logging listener.
typedef void (*dxfg_logging_listener_function_t)(graal_isolatethread_t *thread, dxfg_logging_level_t level,
                                                 int64_t timestamp, const char *thread_name, int64_t thread_id,
                                                 const char *logger_name, const char *message,
                                                 dxfg_exception_t *exception, const char *formatted_message,
                                                 void *user_data);

// dxfg_JavaObjectHandler_release

/**
 * Creates a new listener.
 *
 * ```c
 * //
 *
 * void callback(graal_isolatethread_t *thread, dxfg_logging_level_t level, int64_t timestamp, const char *thread_name,
 *     int64_t thread_id, const char *logger_name, const char *message, dxfg_exception_t *exception,
 *     const char *formatted_message, void *user_data) {
 *     printf("[%" PRId64 "]: %s", thread_id, formatted_message);
 * }
 *
 * // ...
 *
 * // Set the logging Java class
 * dxfg_system_set_property(isolate_thread, "log.className", "com.devexperts.logging.InterceptableLogging");
 * // Turn off logging.
 * dxfg_system_set_property(isolate_thread, "log.level", "OFF");
 *
 * dxfg_logging_listener_t *listener = {};
 *
 * dxfg_logging_listener_new(isolate_thread, &callback, NULL, &listener);
 * dxfg_logging_set_listener(isolate_thread, listener);
 *
 * dxfg_endpoint_t* ep = dxfg_DXEndpoint_getInstance(isolate_thread);
 *
 * // Turn on logging
 * dxfg_logging_set_log_level(isolate_thread, DXFG_LOGGING_LEVEL_INFO);
 * dxfg_DXEndpoint_close(isolate_thread, ep);
 * dxfg_JavaObjectHandler_release(isolate_thread, &listener->handler);
 * dxfg_JavaObjectHandler_release(isolate_thread, &ep->handler);
 * ```
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] user_func The listener callback.
 * @param[in] user_data The user data that will be passed to the callback.
 * @param[out] listener The new listener.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the listener handle.
 */
int32_t dxfg_logging_listener_new(graal_isolatethread_t *thread, dxfg_logging_listener_function_t user_func,
                                  void *user_data, DXFG_OUT dxfg_logging_listener_t **listener);

/**
 * Sets the logging listener. This function will toggle logging. Messages will not be logged to console and files.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] listener The logging listener.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_logging_set_listener(graal_isolatethread_t *thread, dxfg_logging_listener_t *listener);

/**
 * Sets the logging level for the listener or console or log file.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] level The new logging level.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_logging_set_log_level(graal_isolatethread_t *thread, dxfg_logging_level_t level);

/**
 * Sets the logging level for the err file.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] level The new logging level.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_logging_set_err_level(graal_isolatethread_t *thread, dxfg_logging_level_t level);

/** @} */ // end of Logging

#ifdef __cplusplus
}
#endif

#endif // DXFG_LOGGING_H