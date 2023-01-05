// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_ORDER_BOOK_MODEL_H_
#define DXFEED_GRAAL_NATIVE_API_ORDER_BOOK_MODEL_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "graal_isolate.h"
#include "dxfg_javac.h"
#include "dxfg_feed.h"

/** @defgroup OrderBookModel
 *  @{
 */

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/market/OrderBookModel.html">Javadoc</a>
 */
typedef struct dxfg_order_book_model_t {
    dxfg_java_object_handler handler;
} dxfg_order_book_model_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/ObservableListModel.html">Javadoc</a>
 */
typedef struct dxfg_observable_list_model_order_t {
    dxfg_java_object_handler handler;
} dxfg_observable_list_model_order_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/ObservableListModelListener.html">Javadoc</a>
 */
typedef struct dxfg_observable_list_model_listener_order_t {
    dxfg_java_object_handler handler;
} dxfg_observable_list_model_listener_order_t;

typedef void (*dxfg_observable_list_model_listener_function)(graal_isolatethread_t *thread, dxfg_order_list_t* orders, void *user_data); // use dxfg_CList_Order_release to release the allocated memory for model

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/model/market/OrderBookModelListener.html">Javadoc</a>
 */
typedef struct dxfg_order_book_model_listener_t {
    dxfg_java_object_handler handler;
} dxfg_order_book_model_listener_t;

typedef void (*dxfg_order_book_model_listener_function)(graal_isolatethread_t *thread, dxfg_order_book_model_t* model, void *user_data); // use dxfg_JavaObjectHandler_release to release the allocated memory for model

dxfg_order_book_model_t* dxfg_OrderBookModel_new(graal_isolatethread_t *thread);

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
int32_t                                       dxfg_OrderBookModel_getFilter(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
int32_t                                       dxfg_OrderBookModel_setFilter(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_order_book_model_filter_t filter);
const char*                                   dxfg_OrderBookModel_getSymbol(graal_isolatethread_t *thread, dxfg_order_book_model_t *model); // use dxfg_String_release to release the allocated memory
int32_t                                       dxfg_OrderBookModel_setSymbol(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, const char* symbol);
int32_t                                       dxfg_OrderBookModel_getLotSize(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
int32_t                                       dxfg_OrderBookModel_setLotSize(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, int32_t lotSize);
dxfg_observable_list_model_order_t*           dxfg_OrderBookModel_getBuyOrders(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
dxfg_observable_list_model_order_t*           dxfg_OrderBookModel_getSellOrders(graal_isolatethread_t *thread, dxfg_order_book_model_t *model);
dxfg_observable_list_model_listener_order_t*  dxfg_ObservableListModelListener_new(graal_isolatethread_t *thread, dxfg_observable_list_model_listener_function user_func, void *user_data);
int32_t                                       dxfg_ObservableListModelOrder_addListener(graal_isolatethread_t *thread, dxfg_observable_list_model_order_t *list, dxfg_observable_list_model_listener_order_t *listener);
int32_t                                       dxfg_ObservableListModelOrder_removeListener(graal_isolatethread_t *thread, dxfg_observable_list_model_order_t *list, dxfg_observable_list_model_listener_order_t *listener);
dxfg_order_book_model_listener_t*             dxfg_OrderBookModelListener_new(graal_isolatethread_t *thread, dxfg_order_book_model_listener_function user_func, void *user_data);
int32_t                                       dxfg_OrderBookModel_addListener(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_order_book_model_listener_t *listener);
int32_t                                       dxfg_OrderBookModel_removeListener(graal_isolatethread_t *thread, dxfg_order_book_model_t *model, dxfg_order_book_model_listener_t *listener);
dxfg_order_list_t*                            dxfg_ObservableListModelOrder_toArray(graal_isolatethread_t *thread, dxfg_observable_list_model_order_t *list); // use dxfg_CList_Order_release to release the allocated memory

/** @} */ // end of OrderBookModel

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_ORDER_BOOK_MODEL_H_