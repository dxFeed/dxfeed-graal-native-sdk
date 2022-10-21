// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_SUBSCRIPTION_H_
#define DXFEED_GRAAL_NATIVE_API_SUBSCRIPTION_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

#include "dxfg_endpoint.h"
#include "dxfg_error_codes.h"
#include "dxfg_events.h"
#include "graal_isolate.h"

/** @defgroup Subscription
 *  @{
 */

/**
 * @brief Subscription for a set of symbols and event types.
 * Used for subscribe to specific events and receive them via listener as they happen.
 * Represents Java class
 * com.dxfeed.api.DXFeedSubscription
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedSubscription.html">Javadoc</a>).
 * The opaque representation of a handle to a Java object given out to unmanaged code.
 * Clients must not interpret or dereference the value.
 */
typedef int64_t dxfg_sub_t;

/**
 * @brief Subscription event listener.
 */
typedef void (*dxfg_sub_event_listener)(const dxfg_event_t **events, int32_t size);

/**
 * @brief Creates new subscription for a events type that is attached to this endpoint.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] endpoint The endpoint.
 * @param[in] events The array of event types.
 * @param[in] size The events type count.
 * @param[out] sub The subscription.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_create_from_endpoint(graal_isolatethread_t *thread, dxfg_endpoint_t endpoint,
                                              dxfg_event_type_t *eventTypes, int32_t size, dxfg_sub_t *sub);

/**
 * @brief Clears the set of subscribed symbols and closes this subscription.
 * This functions clears lists of all installed event listeners and subscription change
 * listeners and makes sure that no more listeners can be added.
 * Once closed, the dxfg_sub_t descriptor cannot be used in other functions.
 * Removes all internal ref to DXFeedSubscription.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] sub The subscription.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_close(graal_isolatethread_t *thread, dxfg_sub_t sub);

/**
 * @brief Adds listener for events. Event lister can be added only when subscription is not producing any events.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] sub The subscription.
 * @param[in] listener The event listener.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_add_event_listener(graal_isolatethread_t *thread, dxfg_sub_t sub,
                                            dxfg_sub_event_listener listener);

/**
 * @brief Removes listener for events.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] sub The subscription.
 * @param[in] listener The event listener.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_remove_event_listener(graal_isolatethread_t *thread, dxfg_sub_t sub,
                                               dxfg_sub_event_listener listener);

/**
 * @brief Add the specified symbol to the set of subscribed symbols.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] sub The subscription.
 * @param[in] symbol The symbol to add.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_add_symbol(graal_isolatethread_t *thread, dxfg_sub_t sub, dxfg_symbol_t *symbol);

/**
 * @brief Adds the specified array of symbols to the set of subscribed symbols.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] sub The subscription.
 * @param[in] symbols An array of symbols to add.
 * @param[in] size The count of symbols to add.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_add_symbols(graal_isolatethread_t *thread, dxfg_sub_t sub, dxfg_symbol_t **symbol,
                                     int32_t size);

/**
 * @brief Removes the specified symbol from the set of subscribed symbols.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] sub The subscription.
 * @param[in] symbol The symbol to remove.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_remove_symbol(graal_isolatethread_t *thread, dxfg_sub_t sub, dxfg_symbol_t *symbol);

/**
 * @brief Removes the specified array of symbols from the set of subscribed symbols
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] sub The subscription.
 * @param[in] symbols An array of symbols to remove.
 * @param[in] size The count of remove symbols.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_remove_symbols(graal_isolatethread_t *thread, dxfg_sub_t sub, dxfg_symbol_t *symbol,
                                        int32_t size);

/**
 * @brief Clears the set of subscribed symbols.
 * @param[in] thread The pointer to the runtime data structure for a thread.
 * @param[in] sub The subscription.
 * @return 0 - if the operation was successful; otherwise, an error code.
 */
ERROR_CODE dxfg_sub_clear(graal_isolatethread_t *thread, dxfg_sub_t sub);

/** @} */ // end of Subscription

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_SUBSCRIPTION_H_
