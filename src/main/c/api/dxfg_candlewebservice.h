// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK CandleWebService, HistoryEndpoint functions and types declarations
 */

#ifndef DXFG_CANDLEWEBSERVICE_H
#define DXFG_CANDLEWEBSERVICE_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "dxfg_javac.h"
#include "graal_isolate.h"

#include "dxfg_events.h"

/** @defgroup CandleWebService
 *  @{
 */

typedef struct dxfg_history_endpoint_t {
    dxfg_java_object_handler handler;
} dxfg_history_endpoint_t;

typedef struct dxfg_history_endpoint_builder_t {
    dxfg_java_object_handler handler;
} dxfg_history_endpoint_builder_t;

typedef enum dxfg_history_endpoint_compression_t {
    DXFG_HISTORY_ENDPOINT_COMPRESSION_NONE = 0,
    DXFG_HISTORY_ENDPOINT_COMPRESSION_ZIP,
    DXFG_HISTORY_ENDPOINT_COMPRESSION_GZIP,
} dxfg_history_endpoint_compression_t;

typedef enum dxfg_history_endpoint_format_t {
    DXFG_HISTORY_ENDPOINT_FORMAT_TEXT = 0,
    DXFG_HISTORY_ENDPOINT_FORMAT_CSV,
    DXFG_HISTORY_ENDPOINT_FORMAT_BINARY,
} dxfg_history_endpoint_format_t;

/**
 * Creates a new instance of `HistoryEndpoint.Builder`(dxfg_history_endpoint_builder_t) with default configurations.
 *
 * The default settings include:
 * - DXFG_HISTORY_ENDPOINT_COMPRESSION_ZIP as the compression type.
 * - DXFG_HISTORY_ENDPOINT_FORMAT_BINARY as the data format.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[out] builder The new builder instance.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_HistoryEndpoint_newBuilder(graal_isolatethread_t *thread,
                                        DXFG_OUT dxfg_history_endpoint_builder_t **builder);

/**
 * Retrieves a list of time series events for a specific type of event and symbol within the given time range.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] endpoint The history endpoint.
 * @param[in] eventClazz The class representing the type of event to retrieve.
 * @param[in] symbol The identifier of the symbol for which the time series data is requested.
 * @param[in] fromTime The start timestamp for the time series query, in milliseconds since epoch.
 * @param[in] toTime The end timestamp for the time series query, in milliseconds since epoch.
 * @param[out] events A list of events of the specified type and symbol within the given time range
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_CList_EventType_release() to free the events.
 */
int32_t dxfg_HistoryEndpoint_getTimeSeries(graal_isolatethread_t *thread, dxfg_history_endpoint_t *endpoint,
                                           dxfg_event_clazz_t eventClazz, dxfg_symbol_t *symbol, int64_t fromTime,
                                           int64_t toTime, DXFG_OUT dxfg_event_type_list **events);

/**
 * Specifies the address for the target endpoint.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] builder The history endpoint's builder.
 * @param[in] address The address of the endpoint to be set.
 * @param[out] newBuilder The builder instance with the updated address value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_HistoryEndpoint_Builder_withAddress(graal_isolatethread_t *thread,
                                                 dxfg_history_endpoint_builder_t *builder, const char *address,
                                                 DXFG_OUT dxfg_history_endpoint_builder_t **newBuilder);

/**
 * Sets the username for the target endpoint.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] builder The history endpoint's builder.
 * @param[in] userName The username to be set for the endpoint.
 * @param[out] newBuilder The builder instance with the updated username value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_HistoryEndpoint_Builder_withUserName(graal_isolatethread_t *thread,
                                                  dxfg_history_endpoint_builder_t *builder, const char *userName,
                                                  DXFG_OUT dxfg_history_endpoint_builder_t **newBuilder);

/**
 * Sets the password for the target endpoint.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] builder The history endpoint's builder.
 * @param[in] password The password to be set for the endpoint.
 * @param[out] newBuilder The builder instance with the updated password value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_HistoryEndpoint_Builder_withPassword(graal_isolatethread_t *thread,
                                                  dxfg_history_endpoint_builder_t *builder, const char *password,
                                                  DXFG_OUT dxfg_history_endpoint_builder_t **newBuilder);

/**
 * Sets the authentication token for the target endpoint.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] builder The history endpoint's builder.
 * @param[in] authToken The authentication token to be used for access.
 * @param[out] newBuilder The builder instance with the updated authentication token value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_HistoryEndpoint_Builder_withAuthToken(graal_isolatethread_t *thread,
                                                   dxfg_history_endpoint_builder_t *builder, const char *authToken,
                                                   DXFG_OUT dxfg_history_endpoint_builder_t **newBuilder);

/**
 * Sets the compression type to be used for data transmission or storage.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] builder The history endpoint's builder.
 * @param[in] compression The compression type to be applied, represented by the dxfg_history_endpoint_compression_t
 * enum
 * @param[out] newBuilder The builder instance with the updated compression value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_HistoryEndpoint_Builder_withCompression(graal_isolatethread_t *thread,
                                                     dxfg_history_endpoint_builder_t *builder,
                                                     dxfg_history_endpoint_compression_t compression,
                                                     DXFG_OUT dxfg_history_endpoint_builder_t **newBuilder);

/**
 * Sets the format to be used for data handling.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] builder The history endpoint's builder.
 * @param[in] format The format type to be applied, represented by the dxfg_history_endpoint_format_t enum.
 * @param[out] newBuilder The builder instance with the updated format value.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_HistoryEndpoint_Builder_withFormat(graal_isolatethread_t *thread, dxfg_history_endpoint_builder_t *builder,
                                                dxfg_history_endpoint_format_t format,
                                                DXFG_OUT dxfg_history_endpoint_builder_t **newBuilder);

/**
 * Builds and returns a configured instance of `HistoryEndpoint`(dxfg_history_endpoint_t).
 *
 * This method uses the values set in the `builder` instance, such as address, username, password,
 * compression, format, and authentication token, to create a new `HistoryEndpoint`(dxfg_history_endpoint_t) object.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] builder The history endpoint's builder.
 * @param[out] endpoint A new instance of `HistoryEndpoint`(dxfg_history_endpoint_t) configured with the provided settings.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error.
 * Use dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the object handle.
 */
int32_t dxfg_HistoryEndpoint_Builder_build(graal_isolatethread_t *thread, dxfg_history_endpoint_builder_t *builder,
                                           DXFG_OUT dxfg_history_endpoint_t **endpoint);

/** @} */ // end of CandleWebService

#ifdef __cplusplus
}
#endif

#endif // DXFG_CANDLEWEBSERVICE_H
