// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

/**
 * @file
 * @brief dxFeed Graal Native SDK EventModel functions and types declarations
 */

#ifndef DXFG_EVENT_MODEL_H
#define DXFG_EVENT_MODEL_H

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_common.h"

#include "graal_isolate.h"
#include "dxfg_javac.h"
#include "dxfg_feed.h"

/** @defgroup EventModel
 *  @{
 */

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/market/OrderBookModel.html">Javadoc</a>
 */
typedef struct dxfg_order_book_model_t {
    dxfg_java_object_handler handler;
} dxfg_order_book_model_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/IndexedEventModel.html">Javadoc</a>
 */
typedef struct dxfg_indexed_event_model_t {
    dxfg_java_object_handler handler;
} dxfg_indexed_event_model_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/TimeSeriesEventModel.html">Javadoc</a>
 */
typedef struct dxfg_time_series_event_model_t {
    dxfg_java_object_handler handler;
} dxfg_time_series_event_model_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/ObservableListModel.html">Javadoc</a>
 */
typedef struct dxfg_observable_list_model_t {
    dxfg_java_object_handler handler;
} dxfg_observable_list_model_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/ObservableListModelListener.html">Javadoc</a>
 */
typedef struct dxfg_observable_list_model_listener_t {
    dxfg_java_object_handler handler;
} dxfg_observable_list_model_listener_t;

typedef void (*dxfg_observable_list_model_listener_function)(graal_isolatethread_t *thread, dxfg_event_type_list* orders, void *user_data);

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/market/OrderBookModelListener.html">Javadoc</a>
 */
typedef struct dxfg_order_book_model_listener_t {
    dxfg_java_object_handler handler;
} dxfg_order_book_model_listener_t;

typedef void (*dxfg_order_book_model_listener_function)(graal_isolatethread_t *thread, dxfg_order_book_model_t* model, void *user_data);

dxfg_order_book_model_t*          dxfg_OrderBookModel_new(graal_isolatethread_t *thread);
dxfg_indexed_event_model_t*       dxfg_IndexedEventModel_new(graal_isolatethread_t *thread, dxfg_event_clazz_t clazz);
dxfg_time_series_event_model_t*   dxfg_TimeSeriesEventModel_new(graal_isolatethread_t *thread, dxfg_event_clazz_t clazz);

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/market/OrderBookModelFilter.html">Javadoc</a>
 */
typedef enum dxfg_order_book_model_filter_t {
    COMPOSITE,
    REGIONAL,
    AGGREGATE,
    ORDER,
    COMPOSITE_REGIONAL,
    COMPOSITE_REGIONAL_AGGREGATE,
    ALL,
} dxfg_order_book_model_filter_t;

int32_t                                       dxfg_OrderBookModel_attach(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_feed_t *feed);
int32_t                                       dxfg_OrderBookModel_detach(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_feed_t *feed);
int32_t                                       dxfg_OrderBookModel_close(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
dxfg_executor_t*                              dxfg_OrderBookModel_getExecutor(graal_isolatethread_t *thread, dxfg_order_book_model_t *model); // use dxfg_JavaObjectHandler_release to release the allocated memory
int32_t                                       dxfg_OrderBookModel_setExecutor(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_executor_t *executor);
int32_t                                       dxfg_OrderBookModel_clear(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
int32_t                                       dxfg_IndexedEventModel_attach(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model, dxfg_feed_t *feed);
int32_t                                       dxfg_IndexedEventModel_detach(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model, dxfg_feed_t *feed);
int32_t                                       dxfg_IndexedEventModel_close(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model);
dxfg_executor_t*                              dxfg_IndexedEventModel_getExecutor(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model); // use dxfg_JavaObjectHandler_release to release the allocated memory
int32_t                                       dxfg_IndexedEventModel_setExecutor(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model, dxfg_executor_t *executor);
int32_t                                       dxfg_IndexedEventModel_clear(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model);
int32_t                                       dxfg_TimeSeriesEventModel_attach(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model, dxfg_feed_t *feed);
int32_t                                       dxfg_TimeSeriesEventModel_detach(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model, dxfg_feed_t *feed);
int32_t                                       dxfg_TimeSeriesEventModel_close(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model);
dxfg_executor_t*                              dxfg_TimeSeriesEventModel_getExecutor(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model); // use dxfg_JavaObjectHandler_release to release the allocated memory
int32_t                                       dxfg_TimeSeriesEventModel_setExecutor(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model, dxfg_executor_t *executor);
int32_t                                       dxfg_TimeSeriesEventModel_clear(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model);
int32_t                                       dxfg_OrderBookModel_getFilter(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
int32_t                                       dxfg_OrderBookModel_setFilter(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_order_book_model_filter_t filter);
const char*                                   dxfg_OrderBookModel_getSymbol(graal_isolatethread_t *thread, dxfg_order_book_model_t *model); // use dxfg_String_release to release the allocated memory
int32_t                                       dxfg_OrderBookModel_setSymbol(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, const char* symbol);
dxfg_symbol_t*                                dxfg_IndexedEventModel_getSymbol(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model); // use dxfg_Symbol_release to release the allocated memory
int32_t                                       dxfg_IndexedEventModel_setSymbol(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model, dxfg_symbol_t* symbol);
dxfg_symbol_t*                                dxfg_TimeSeriesEventModel_getSymbol(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model); // use dxfg_Symbol_release to release the allocated memory
int32_t                                       dxfg_TimeSeriesEventModel_setSymbol(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model, dxfg_symbol_t* symbol);
int32_t                                       dxfg_OrderBookModel_getLotSize(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
int32_t                                       dxfg_OrderBookModel_setLotSize(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, int32_t lotSize);
int32_t                                       dxfg_IndexedEventModel_getSizeLimit(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model);
int32_t                                       dxfg_IndexedEventModel_setSizeLimit(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model, int32_t sizeLimit);
int32_t                                       dxfg_TimeSeriesEventModel_getSizeLimit(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model);
int32_t                                       dxfg_TimeSeriesEventModel_setSizeLimit(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model, int32_t sizeLimit);
int64_t                                       dxfg_TimeSeriesEventModel_getFromTime(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model);
int32_t                                       dxfg_TimeSeriesEventModel_setFromTime(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model, int64_t fromTime);
dxfg_observable_list_model_t*                 dxfg_OrderBookModel_getBuyOrders(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
dxfg_observable_list_model_t*                 dxfg_OrderBookModel_getSellOrders(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
dxfg_observable_list_model_t*                 dxfg_IndexedEventModel_getEventsList(graal_isolatethread_t *thread, dxfg_indexed_event_model_t *model);
dxfg_observable_list_model_t*                 dxfg_TimeSeriesEventModel_getEventsList(graal_isolatethread_t *thread, dxfg_time_series_event_model_t *model);
dxfg_observable_list_model_listener_t*        dxfg_ObservableListModelListener_new(graal_isolatethread_t *thread, dxfg_observable_list_model_listener_function user_func, void *user_data);
int32_t                                       dxfg_ObservableListModel_addListener(graal_isolatethread_t *thread, dxfg_observable_list_model_t *list, dxfg_observable_list_model_listener_t *listener);
int32_t                                       dxfg_ObservableListModel_removeListener(graal_isolatethread_t *thread, dxfg_observable_list_model_t *list, dxfg_observable_list_model_listener_t *listener);
dxfg_order_book_model_listener_t*             dxfg_OrderBookModelListener_new(graal_isolatethread_t *thread, dxfg_order_book_model_listener_function user_func, void *user_data);
int32_t                                       dxfg_OrderBookModel_addListener(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_order_book_model_listener_t *listener);
int32_t                                       dxfg_OrderBookModel_removeListener(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_order_book_model_listener_t *listener);
dxfg_event_type_list*                         dxfg_ObservableListModel_toArray(graal_isolatethread_t *thread, dxfg_observable_list_model_t *list); // use dxfg_CList_EventType_release to release the allocated memory

/// Represents the available sorting orders.
typedef enum dxfg_TimeSeriesTxModelSortOrder_t {
    /// Indicates that no sorting should be applied. Events will be passed in the same order in which they were received.
    NONE,
    /// Indicates that sorting should be in ascending order by index.
    ASCENDING,
    /// Indicates that sorting should be in descending order by index.
    DESCENDING,
} dxfg_TimeSeriesTxModelSortOrder_t;

/**
 * @name Transactional models
 * IndexedTxModel and TimeSeriesTxModel deliver the complete transactions of indexed events to a listener.
 * Every builder function returns a new handle of the same builder: release every handle the functions return.
 * @{
 */

/**
 * @brief The handle of an incremental model of indexed events (Java `com.dxfeed.sdk.model.tx.IndexedTxModel`).
 *
 * It delivers the complete transactions of the events of its symbol and sources to its listener.
 * Close the model with dxfg_IndexedTxModel_close() and release the handle with dxfg_JavaObjectHandler_release().
 */
typedef struct dxfg_indexed_tx_model_t { dxfg_java_object_handler handler; } dxfg_indexed_tx_model_t;

/**
 * @brief The handle of a builder of dxfg_indexed_tx_model_t (Java `com.dxfeed.sdk.model.tx.IndexedTxModel.Builder`).
 */
typedef struct dxfg_indexed_tx_model_builder_t { dxfg_java_object_handler handler; } dxfg_indexed_tx_model_builder_t;

/**
 * @brief The handle of an incremental model of time-series events (Java `com.dxfeed.sdk.model.tx.TimeSeriesTxModel`).
 *
 * It delivers the complete transactions of the events of its symbol from its time to its listener.
 * Close the model with dxfg_TimeSeriesTxModel_close() and release the handle with dxfg_JavaObjectHandler_release().
 */
typedef struct dxfg_time_series_tx_model_t { dxfg_java_object_handler handler; } dxfg_time_series_tx_model_t;

/**
 * @brief The handle of a builder of dxfg_time_series_tx_model_t (Java `com.dxfeed.sdk.model.tx.TimeSeriesTxModel.Builder`).
 */
typedef struct dxfg_time_series_tx_model_builder_t { dxfg_java_object_handler handler; } dxfg_time_series_tx_model_builder_t;

/**
 * @brief The handle of a listener of the transactional models, see dxfg_TxModelListener_new().
 */
typedef struct dxfg_tx_model_listener_t { dxfg_java_object_handler handler; } dxfg_tx_model_listener_t;

/**
 * @brief The function of a dxfg_tx_model_listener_t: receives one or more complete transactions.
 *
 * @param thread The current thread.
 * @param source The source of the events. It is valid during the call only.
 * @param events The events. They are valid during the call only: the SDK frees them after the call.
 * @param isSnapshot 1 if the events are a snapshot, 0 otherwise.
 * @param user_data The user data passed to dxfg_TxModelListener_new().
 */
typedef void (*dxfg_TxModelListener_function_eventsReceived)(graal_isolatethread_t* thread, dxfg_indexed_event_source_t* source, dxfg_event_type_list* events, int32_t isSnapshot, void* user_data);

/**
 * @brief Creates a new builder of a model of the indexed events of the type.
 *
 * @return The builder, or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_newBuilder(graal_isolatethread_t* thread, dxfg_event_clazz_t eventType);

/**
 * @brief Returns the sources of the model.
 *
 * @return The sources (release them with dxfg_CList_IndexedEventSource_release()), or NULL on an error.
 */
dxfg_indexed_event_source_list* dxfg_IndexedTxModel_getSources(graal_isolatethread_t* thread, dxfg_indexed_tx_model_t* source);

/**
 * @brief Sets the sources of the model: it subscribes to the new ones and unsubscribes from the old ones.
 *
 * An empty list means all the sources.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_IndexedTxModel_setSources(graal_isolatethread_t* thread, dxfg_indexed_tx_model_t* source, dxfg_indexed_event_source_list* sources);

/**
 * @brief Returns whether the listener gets all the pending transactions in one call.
 *
 * @return 1 or 0, -1 on an error.
 */
int32_t dxfg_IndexedTxModel_isBatchProcessing(graal_isolatethread_t* thread, dxfg_indexed_tx_model_t* source);

/**
 * @brief Returns whether a snapshot is delivered as its resulting state (without the removed events).
 *
 * @return 1 or 0, -1 on an error.
 */
int32_t dxfg_IndexedTxModel_isSnapshotProcessing(graal_isolatethread_t* thread, dxfg_indexed_tx_model_t* source);

/**
 * @brief Attaches the model to the feed.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_IndexedTxModel_attach(graal_isolatethread_t* thread, dxfg_indexed_tx_model_t* source, dxfg_feed_t* feed);

/**
 * @brief Detaches the model from the feed.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_IndexedTxModel_detach(graal_isolatethread_t* thread, dxfg_indexed_tx_model_t* source, dxfg_feed_t* feed);

/**
 * @brief Sets the aggregation period of the events.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_IndexedTxModel_setAggregationPeriod(graal_isolatethread_t* thread, dxfg_indexed_tx_model_t* source, dxfg_time_period_t* aggregationPeriod);

/**
 * @brief Closes the model: it is detached from all the feeds for good. Closing it again does nothing.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_IndexedTxModel_close(graal_isolatethread_t* thread, dxfg_indexed_tx_model_t* source);

/**
 * @brief Sets the sources (an empty list means all the sources).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_Builder_withSources(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source, dxfg_indexed_event_source_list* sources);

/**
 * @brief Builds the model (the symbol must be set).
 *
 * @return The model, or NULL on an error.
 */
dxfg_indexed_tx_model_t* dxfg_IndexedTxModel_Builder_build(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source);

/**
 * @brief Sets whether the listener gets all the pending transactions in one call (1, default) or one by one (0).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_Builder_withBatchProcessing(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source, int32_t isBatchProcessing);

/**
 * @brief Sets whether a snapshot is delivered as its resulting state (1) or as it comes (0, default).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_Builder_withSnapshotProcessing(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source, int32_t isSnapshotProcessing);

/**
 * @brief Sets the feed the model is attached to.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_Builder_withFeed(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source, dxfg_feed_t* feed);

/**
 * @brief Sets the aggregation period of the events.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_Builder_withAggregationPeriod(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source, dxfg_time_period_t* aggregationPeriod);

/**
 * @brief Sets the symbol of the subscription.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_Builder_withSymbol(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source, dxfg_symbol_t* symbol);

/**
 * @brief Sets the listener of the transactions.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_Builder_withListener(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source, dxfg_tx_model_listener_t* listener);

/**
 * @brief Sets the executor that calls the listener (by default, the executor of the feed).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_indexed_tx_model_builder_t* dxfg_IndexedTxModel_Builder_withExecutor(graal_isolatethread_t* thread, dxfg_indexed_tx_model_builder_t* source, dxfg_executor_t* executor);

/**
 * @brief Creates a new builder of a model of the time-series events of the type.
 *
 * @return The builder, or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_newBuilder(graal_isolatethread_t* thread, dxfg_event_clazz_t eventType);

/**
 * @brief Returns the time the model is subscribed from.
 *
 * @return The time in milliseconds since the epoch (INT64_MAX if the model is not subscribed), -1 on an error.
 */
int64_t dxfg_TimeSeriesTxModel_getFromTime(graal_isolatethread_t* thread, dxfg_time_series_tx_model_t* source);

/**
 * @brief Sets the time the model is subscribed from (INT64_MAX unsubscribes); the model gets a new snapshot.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_TimeSeriesTxModel_setFromTime(graal_isolatethread_t* thread, dxfg_time_series_tx_model_t* source, int64_t fromTime);

/**
 * @brief Returns whether the listener gets all the pending transactions in one call.
 *
 * @return 1 or 0, -1 on an error.
 */
int32_t dxfg_TimeSeriesTxModel_isBatchProcessing(graal_isolatethread_t* thread, dxfg_time_series_tx_model_t* source);

/**
 * @brief Returns whether a snapshot is delivered as its resulting state (without the removed events).
 *
 * @return 1 or 0, -1 on an error.
 */
int32_t dxfg_TimeSeriesTxModel_isSnapshotProcessing(graal_isolatethread_t* thread, dxfg_time_series_tx_model_t* source);

/**
 * @brief Attaches the model to the feed.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_TimeSeriesTxModel_attach(graal_isolatethread_t* thread, dxfg_time_series_tx_model_t* source, dxfg_feed_t* feed);

/**
 * @brief Detaches the model from the feed.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_TimeSeriesTxModel_detach(graal_isolatethread_t* thread, dxfg_time_series_tx_model_t* source, dxfg_feed_t* feed);

/**
 * @brief Sets the aggregation period of the events.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_TimeSeriesTxModel_setAggregationPeriod(graal_isolatethread_t* thread, dxfg_time_series_tx_model_t* source, dxfg_time_period_t* aggregationPeriod);

/**
 * @brief Closes the model: it is detached from all the feeds for good. Closing it again does nothing.
 *
 * @return 0 on success, -1 on an error.
 */
int32_t dxfg_TimeSeriesTxModel_close(graal_isolatethread_t* thread, dxfg_time_series_tx_model_t* source);

/**
 * @brief Sets the time to subscribe from, in milliseconds since the epoch (by default INT64_MAX: not subscribed).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withFromTime(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, int64_t fromTime);

/**
 * @brief Sets the order of the events of a snapshot.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withSorting(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, dxfg_TimeSeriesTxModelSortOrder_t sortOrder);

/**
 * @brief Sets whether the removed events are dropped (1) or delivered (0, default).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_ignoreRemoveEvents(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, int32_t ignoreRemoveEvents);

/**
 * @brief Sets whether the events with an index not greater than the last processed one are dropped (1)
 * or delivered (0, default).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_ignoreEventsFromPast(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, int32_t ignoreEventFromPast);

/**
 * @brief Sets whether a new bar comes after the previous (closing) one, to confirm it (1), or alone (0, default).
 *
 * With 1, the snapshot processing and the ascending sorting are on.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withConfirmationTick(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, int32_t confirmationTick);

/**
 * @brief Accepted for compatibility: the pointer is ignored, the model gets no event peek (a Java `Consumer`).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withEventPeek(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, void* eventPeek);

/**
 * @brief Builds the model (the symbol must be set).
 *
 * @return The model, or NULL on an error.
 */
dxfg_time_series_tx_model_t* dxfg_TimeSeriesTxModel_Builder_build(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source);

/**
 * @brief Sets whether the listener gets all the pending transactions in one call (1, default) or one by one (0).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withBatchProcessing(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, int32_t isBatchProcessing);

/**
 * @brief Sets whether a snapshot is delivered as its resulting state (1) or as it comes (0, default).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withSnapshotProcessing(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, int32_t isSnapshotProcessing);

/**
 * @brief Sets the feed the model is attached to.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withFeed(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, dxfg_feed_t* feed);

/**
 * @brief Sets the aggregation period of the events.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withAggregationPeriod(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, dxfg_time_period_t* aggregationPeriod);

/**
 * @brief Sets the symbol of the subscription.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withSymbol(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, dxfg_symbol_t* symbol);

/**
 * @brief Sets the listener of the transactions.
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withListener(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, dxfg_tx_model_listener_t* listener);

/**
 * @brief Sets the executor that calls the listener (by default, the executor of the feed).
 *
 * @return A new handle of the same builder (release it with dxfg_JavaObjectHandler_release()), or NULL on an error.
 */
dxfg_time_series_tx_model_builder_t* dxfg_TimeSeriesTxModel_Builder_withExecutor(graal_isolatethread_t* thread, dxfg_time_series_tx_model_builder_t* source, dxfg_executor_t* executor);

/**
 * @brief Creates a listener that calls the function with the user data.
 *
 * After the last handle of the listener is released (dxfg_JavaObjectHandler_release()), the function is not called
 * anymore and none of its calls is in progress, so the user data can be freed.
 *
 * @return The listener, or NULL on an error.
 */
dxfg_tx_model_listener_t* dxfg_TxModelListener_new(graal_isolatethread_t* thread, dxfg_TxModelListener_function_eventsReceived  function_eventsReceived, void* user_data);

/** @} */

/** @} */ // end of EventModel

#ifdef __cplusplus
}
#endif

#endif // DXFG_EVENT_MODEL_H
