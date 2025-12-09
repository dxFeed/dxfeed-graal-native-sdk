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

#include "dxfg_javac.h"
#include "graal_isolate.h"
#include "dxfg_events.h"

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
 * https://docs.dxfeed.com/orcs/com/dxfeed/orcs/api/AuthOrderSource.html
 */
typedef struct dxfg_auth_order_source_t {
    dxfg_java_object_handler handler;
} dxfg_auth_order_source_t;

// Map.Entry<Integer, Set<String>
typedef struct dxfg_symbols_by_order_source_id_map_entry_t {
    int32_t order_source_id;
    dxfg_symbol_list *symbols;
} dxfg_symbols_by_order_source_id_map_entry_t;

// Collection<? extends Map.Entry<Integer, Set<String>>
typedef struct dxfg_symbols_by_order_source_id_map_entry_list_t {
    int32_t size;
    dxfg_symbols_by_order_source_id_map_entry_t **elements;
} dxfg_symbols_by_order_source_id_map_entry_list_t;

// Map.Entry<? extends IndexedEventSource, Set<String>
typedef struct dxfg_symbols_by_order_source_map_entry_t {
    dxfg_indexed_event_source_t *order_source;
    dxfg_symbol_list *symbols;
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

int32_t dxfg_PriceLevelService_new(graal_isolatethread_t *thread, const char *address,
                                   DXFG_OUT dxfg_price_level_service_t **service);

int32_t dxfg_PriceLevelService_getOrders(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                         dxfg_symbol_t *candleSymbol, dxfg_indexed_event_source_t *orderSource,
                                         int64_t from, int64_t to, const char *caller,
                                         DXFG_OUT dxfg_event_type_list **orders);

int32_t dxfg_PriceLevelService_getOrders2(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                          dxfg_symbol_t *candleSymbol, dxfg_indexed_event_source_t *orderSource,
                                          int64_t from, int64_t to, DXFG_OUT dxfg_event_type_list **orders);

int32_t dxfg_PriceLevelService_getAuthOrderSource(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                                  DXFG_OUT dxfg_auth_order_source_t **authOrderSource);

int32_t dxfg_PriceLevelService_getQuotes(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                         dxfg_symbol_t *candleSymbol, int64_t from, int64_t to, const char *caller,
                                         DXFG_OUT dxfg_event_type_list **quotes);

int32_t dxfg_PriceLevelService_getQuotes2(graal_isolatethread_t *thread, dxfg_price_level_service_t *service,
                                          dxfg_symbol_t *candleSymbol, int64_t from, int64_t to,
                                          DXFG_OUT dxfg_event_type_list **quotes);

int32_t dxfg_PriceLevelService_close(graal_isolatethread_t *thread, dxfg_price_level_service_t *service);

/// @} end of PriceLevelServiceFunctions

/**
 * @defgroup AuthOrderSourceFunctions AuthOrderSource' functions
 * @ingroup AuthOrderSource
 * @{
 */

int32_t dxfg_AuthOrderSource_getByIds(
    graal_isolatethread_t *thread, dxfg_auth_order_source_t *authOrderSource,
    DXFG_OUT dxfg_symbols_by_order_source_id_map_entry_list_t **symbolsByOrderSourceIdMapEntryList);

int32_t dxfg_AuthOrderSource_getByOrderSources(
    graal_isolatethread_t *thread, dxfg_auth_order_source_t *authOrderSource,
    DXFG_OUT dxfg_symbols_by_order_source_map_entry_list_t **symbolsByOrderSourceMapEntryList);

/// @} end of AuthOrderSourceFunctions

/**
 * @defgroup PriceLevelCheckerFunctions PriceLevelChecker' functions
 * @ingroup OrcsApi
 * @{
 */

int32_t dxfg_PriceLevelChecker_validate(graal_isolatethread_t *thread, dxfg_event_type_list *orders,
                                        int64_t timeGapBound, int32_t /*bool*/ printQuotes, DXFG_OUT int32_t *isValid);

/// @} end of AuthOrderSourceFunctions

#ifdef __cplusplus
}
#endif

#endif // DXFG_ORCS_H