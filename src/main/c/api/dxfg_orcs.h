// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK ORCS API functions and types declarations
 */

#ifndef DXFG_ORCS_H
#define DXFG_ORCS_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "dxfg_events.h"
#include "dxfg_javac.h"
#include "graal_isolate.h"

/**
 * @defgroup OrcsApi ORCS API
 */

/**
 * @defgroup PriceLevelService
 * @ingroup OrcsApi
 *
 * @see PriceLevelServiceFunctions
 * @{
 */

/**
 * https://docs.dxfeed.com/orcs/com/dxfeed/orcs/api/PriceLevelService.html
 */
typedef struct dxfg_price_level_service_t {
    dxfg_java_object_handler handler;
} dxfg_price_level_service_t;

/// @} end of PriceLevelService

/**
 * @defgroup AuthOrderSource
 * @ingroup OrcsApi
 *
 * @see AuthOrderSourceFunctions
 * @{
 */

/**
 * Represents information about available symbols to the client for the
 * entitled collection of OrderSource.
 *
 * https://docs.dxfeed.com/orcs/com/dxfeed/orcs/api/AuthOrderSource.html
 */
typedef struct dxfg_auth_order_source_t {
    dxfg_java_object_handler handler;
} dxfg_auth_order_source_t;

// Map.Entry<Integer, Set<String>
typedef struct dxfg_symbols_by_order_source_id_map_entry_t {
    int32_t order_source_id;
    dxfg_string_list *symbols;
} dxfg_symbols_by_order_source_id_map_entry_t;

// Collection<? extends Map.Entry<Integer, Set<String>>
typedef struct dxfg_symbols_by_order_source_id_map_entry_list_t {
    int32_t size;
    dxfg_symbols_by_order_source_id_map_entry_t **elements;
} dxfg_symbols_by_order_source_id_map_entry_list_t;

// Map.Entry<? extends IndexedEventSource, Set<String>
typedef struct dxfg_symbols_by_order_source_map_entry_t {
    dxfg_indexed_event_source_t *order_source;
    dxfg_string_list *symbols;
} dxfg_symbols_by_order_source_map_entry_t;

// Collection<? extends Map.Entry<? extends IndexedEventSource, Set<String>>
typedef struct dxfg_symbols_by_order_source_map_entry_list_t {
    int32_t size;
    dxfg_symbols_by_order_source_map_entry_t **elements;
} dxfg_symbols_by_order_source_map_entry_list_t;

/// @} end of AuthOrderSource

/**
 * @defgroup PriceLevelServiceFunctions PriceLevelService' functions
 * @ingroup PriceLevelService
 * @{
 */

/**
 * Creates a new PriceLevelService instance and connects it to an RMI endpoint.
 * The handle of this instance will be written to the passed pointer.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] address The RMI endpoint's address.
 * @param[out] service The pointer to the service's handle.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the service handle.
 */
int32_t dxfg_PriceLevelService_new(graal_isolatethread_t *thread, const char *address,
                                   DXFG_OUT dxfg_price_level_service_t **service);

/**
 * Returns a list of price levels for the specified CandleSymbol within passed `from` and `to` times.
 * The events are ordered by time in the collection.
 *
 * Passed candle symbol shall contain supported CandlePeriod and a custom attribute called granularity with the
 * key `GRANULARITY_ATTRIBUTE_KEY` ("gr"). Granularity value shall be represented by an integer value in seconds.
 * The minimal value for granularity is 1 second.
 *
 * If passed CandlePeriod or granularity value are not supported by the service, the empty list will be returned.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The PriceLevelService.
 * @param[in] candleSymbol The CandleSymbol to request.
 * @param[in] orderSource The OrderSource to request.
 * @param[in] from The "from" time in UTC.
 * @param[in] to The "to" time in UTC.
 * @param[in] caller The caller identifier.
 * @param[out] orders The pointer to the orders list.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_CList_EventType_release() to free the orders list.
 */
int32_t dxfg_PriceLevelService_getOrders(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                         dxfg_symbol_t *candleSymbol, dxfg_indexed_event_source_t *orderSource,
                                         int64_t from, int64_t to, const char *caller,
                                         DXFG_OUT dxfg_event_type_list **orders);

/**
 * Returns a list of price levels for the specified CandleSymbol within passed `from` and `to` times.
 * The events are ordered by time in the collection.
 *
 * Passed candle symbol shall contain supported CandlePeriod and a custom attribute called granularity with the
 * key `GRANULARITY_ATTRIBUTE_KEY` ("gr"). Granularity value shall be represented by an integer value in seconds.
 * The minimal value for granularity is 1 second.
 *
 * If passed CandlePeriod or granularity value are not supported by the service, the empty list will be returned.
 *
 * The caller is set to a default value = "dxfg".
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The PriceLevelService.
 * @param[in] candleSymbol The CandleSymbol to request.
 * @param[in] orderSource The OrderSource to request.
 * @param[in] from The "from" time in UTC.
 * @param[in] to The "to" time in UTC.
 * @param[out] orders The pointer to the orders list.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_CList_EventType_release() to free the orders list.
 */
int32_t dxfg_PriceLevelService_getOrders2(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                          dxfg_symbol_t *candleSymbol, dxfg_indexed_event_source_t *orderSource,
                                          int64_t from, int64_t to, DXFG_OUT dxfg_event_type_list **orders);

/**
 * Returns available to the client order sources and symbols for each OrderSource. Order source and symbols
 * are filtered according to the client permissions. Symbols and order sources view is built as of now, e.g.
 * the response contains only existing data (for example, no symbols that were delisted)
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The PriceLevelService.
 * @param[out] authOrderSource The pointer to the AuthOrderSource.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_JavaObjectHandler_release() to free the AuthOrderSource handle.
 */
int32_t dxfg_PriceLevelService_getAuthOrderSource(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                                  DXFG_OUT dxfg_auth_order_source_t **authOrderSource);

/**
 * Returns a list of quotes for the specified CandleSymbol within passed `from` and `to` times.
 * The quotes are ordered by time in the collection.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The PriceLevelService.
 * @param[in] candleSymbol The CandleSymbol to request.
 * @param[in] from The "from" time in UTC.
 * @param[in] to The "to" time in UTC.
 * @param[in] caller The caller identifier.
 * @param[out] quotes The pointer to the quotes list.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_CList_EventType_release() to free the quotes list.
 */
int32_t dxfg_PriceLevelService_getQuotes(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                         dxfg_symbol_t *candleSymbol, int64_t from, int64_t to, const char *caller,
                                         DXFG_OUT dxfg_event_type_list **quotes);

/**
 * Returns a list of quotes for the specified CandleSymbol within passed `from` and `to` times.
 * The quotes are ordered by time in the collection.
 *
 * The caller is set to a default value = "dxfg".
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The PriceLevelService.
 * @param[in] candleSymbol The CandleSymbol to request.
 * @param[in] from The "from" time in UTC.
 * @param[in] to The "to" time in UTC.
 * @param[out] quotes The pointer to the quotes list.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_CList_EventType_release() to free the quotes list.
 */
int32_t dxfg_PriceLevelService_getQuotes2(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                          dxfg_symbol_t *candleSymbol, int64_t from, int64_t to,
                                          DXFG_OUT dxfg_event_type_list **quotes);

/**
 * Closes the PriceLevelService and its RMI endpoint.
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] service The PriceLevelService.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_PriceLevelService_close(graal_isolatethread_t *thread, dxfg_price_level_service_t *service);

/// @} end of PriceLevelServiceFunctions

/**
 * @defgroup AuthOrderSourceFunctions AuthOrderSource' functions
 * @ingroup AuthOrderSource
 * @{
 */

/**
 * Returns a list of map entries (OrderSource::id, [Symbol, ...]).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] authOrderSource The AuthOrderSource.
 * @param[out] symbolsByOrderSourceIdMapEntryList The pointer to the list of entries.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_symbols_by_order_source_id_map_entry_list_free() to free the list of entries.
 */
int32_t dxfg_AuthOrderSource_getByIds(
    graal_isolatethread_t *thread, dxfg_auth_order_source_t *authOrderSource,
    DXFG_OUT dxfg_symbols_by_order_source_id_map_entry_list_t **symbolsByOrderSourceIdMapEntryList);

/**
 * Frees the list of map entries (OrderSource::id, [Symbol, ...]).
 *
 * @param thread The current GraalVM Isolate's thread.
 * @param list The list of entries.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_symbols_by_order_source_id_map_entry_list_free(graal_isolatethread_t *thread,
                                                            dxfg_symbols_by_order_source_id_map_entry_list_t *list);

/**
 * Returns a list of map entries (OrderSource, [Symbol, ...]).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] authOrderSource The AuthOrderSource.
 * @param[out] symbolsByOrderSourceMapEntryList The pointer to the list of entries.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 * Use dxfg_symbols_by_order_source_map_entry_list_free() to free the list of entries.
 */
int32_t dxfg_AuthOrderSource_getByOrderSources(
    graal_isolatethread_t *thread, dxfg_auth_order_source_t *authOrderSource,
    DXFG_OUT dxfg_symbols_by_order_source_map_entry_list_t **symbolsByOrderSourceMapEntryList);

/**
 * Frees the list of map entries (OrderSource, [Symbol, ...]).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] list The list of entries.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_symbols_by_order_source_map_entry_list_free(graal_isolatethread_t *thread,
                                                         dxfg_symbols_by_order_source_map_entry_list_t *list);

/// @} end of AuthOrderSourceFunctions

/**
 * @defgroup PriceLevelCheckerFunctions PriceLevelChecker' functions
 * @ingroup OrcsApi
 * @{
 */

/**
 * Validates the order list.
 *
* PriceLevelChecker:  utility class to check the consistency of an Order list in terms that in each time bid price is lower than ask price.
 * There is additional information may be gathered during the check:
 * <ul>
 *     <li>Gap detection for subsequent events. In other words, if there was a pause between orders greater than {@code timeGapBound}.</li>
 *     <li>Last bid and ask change with the minimal period of 1 second.</li>
 *     <li>Spike in quotes.</li>
 * </ul>
 *
 * Frees the list of map entries (OrderSource, [Symbol, ...]).
 *
 * @param[in] thread The current GraalVM Isolate's thread.
 * @param[in] orders The list of orders.
 * @param[in] timeGapBound The gap bound to check.
 * @param[in] printQuotes Enable a quotes logging during the check.
 * @param[out] isValid Set to 1 if orders are valid, 0 otherwise.
 * @return #DXFG_EXECUTE_SUCCESSFULLY (0) on successful function execution or #DXFG_EXECUTE_FAIL (-1) on error. Use
 * dxfg_get_and_clear_thread_exception_t() to determine if an exception was thrown.
 */
int32_t dxfg_PriceLevelChecker_validate(graal_isolatethread_t *thread, dxfg_event_type_list *orders,
                                        int64_t timeGapBound, int32_t /*bool*/ printQuotes, DXFG_OUT int32_t *isValid);

/// @} end of AuthOrderSourceFunctions

#ifdef __cplusplus
}
#endif

#endif // DXFG_ORCS_H