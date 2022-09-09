#ifndef DXFEED_GRAAL_API_DFX_TYPES_H_
#define DXFEED_GRAAL_API_DFX_TYPES_H_

#ifdef __cplusplus
extern "C" {
#endif

#include "stdint.h"

typedef int ERROR_CODE;

/**
 * @brief Manages network connections to feed.
 * Used for for connect/disconnect from remote hosts, receive instances of other classes.
 * Represents Java class
 * com.dxfeed.api.DXEndpoint
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXEndpoint.html">Javadoc</a>).
 * The opaque representation of a handle to a Java object given out to unmanaged code.
 * Clients must not interpret or dereference the value.
 */
typedef int64_t dxf_endpoint_t;

/**
 * @brief Main entry class for dxFeed API.
 * Used for for retrieve events, create subscriptions.
 * Represents Java class
 * com.dxfeed.api.DXFeed
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeed.html">Javadoc</a>).
 * The opaque representation of a handle to a Java object given out to unmanaged code.
 * Clients must not interpret or dereference the value.
 */
typedef int64_t dxf_feed_t;

/**
 * @brief Subscription for a set of symbols and event types.
 * Used for subscribe to specific events and receive them via listener as they happen.
 * Represents Java class
 * com.dxfeed.api.DXFeedSubscription
 * (<a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/api/DXFeedSubscription.html">Javadoc</a>).
 * The opaque representation of a handle to a Java object given out to unmanaged code.
 * Clients must not interpret or dereference the value.
 */
typedef int64_t dxf_subscription_t;

/**
 * @brief Represents the current state of endpoint.
 */
typedef enum dxf_endpoint_state_t {
  /**
   * @brief Endpoint was created by is not connected to remote endpoints.
   */
  DXF_ENDPOINT_NOT_CONNECTED,

  /**
   * @brief The dxf_endpoint_connect method was called to establish connection to remove endpoint,
   * but connection is not actually established yet or was lost.
   */
  DXF_ENDPOINT_CONNECTING,

  /**
   * @brief The connection to remote endpoint is established.
   */
  DXF_ENDPOINT_CONNECTED,

  /**
   * @brief Endpoint was dxf_endpoint_close closed.
   */
  DXF_ENDPOINT_CLOSED
} dxf_endpoint_state_t;

/**
 * @brief Change endpoint state listener.
 */
typedef void (*dxf_endpoint_on_changing_state)(dxf_endpoint_state_t oldState, dxf_endpoint_state_t newState);

/**
 * @brief List of market events.
 */
typedef enum dxf_event_type_t {
  DXF_EVENT_TYPE_QUOTE,
  DXF_EVENT_TYPE_TIME_AND_SALE,
  DXF_EVENT_TYPE_CANDLES,
} dxf_event_type_t;

/**
 * @brief Base event type.
 */
typedef struct dxf_event_t {
  int32_t event_type;
  char *symbol_name;
} dxf_event_t;

/**
 * @brief The Quote.
 */
typedef struct dxf_event_quote_t {
  dxf_event_t event;
  double bid_price;
  double ask_price;
} dxf_event_quote_t;

/**
 * @brief The TimeAndSale
 */
typedef struct dxf_event_time_and_sale_t {
  dxf_event_t event;
  int32_t event_flag;
  int64_t index;
} dxf_event_time_and_sale_t;

/**
 * @brief Subscription listener.
 */
typedef void (*dxf_subscription_event_listener)(dxf_event_t **events, size_t count);


/**
 * @brief Represents indexed symbol.
 */
typedef struct dxf_indexed_symbol_t {
  /// Symbol name.
  const char *symbol;
  /// Order source.
  const char *source;
} dxf_indexed_symbol_t;

/**
 * @brief Represents time series symbol.
 */
typedef struct dxf_time_series_symbol_t {
  /// Symbol name.
  const char *symbol;
  /// From time in milliseconds.
  int64_t fromTime;
} dxf_time_series_symbol_t;

#ifdef __cplusplus
}
#endif

#endif //DXFEED_GRAAL_API_DFX_TYPES_H_
