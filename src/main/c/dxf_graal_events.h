// SPDX-License-Identifier: MPL-2.0

#ifndef DXFEED_GRAAL_NATIVE_API_EVENTS_H_
#define DXFEED_GRAAL_NATIVE_API_EVENTS_H_

#ifdef __cplusplus
extern "C" {
#    include <cstdint>
#else
#    include <stdint.h>
#endif

/**
 * @brief List of market events.
 */
typedef enum dxf_graal_event_type_t {
    DXF_GRAAL_EVENT_TYPE_QUOTE = 0,
    DXF_GRAAL_EVENT_TYPE_TIME_AND_SALE,
} dxf_graal_event_type_t;

/**
 * @brief Base event type.
 */
typedef struct dxf_graal_event_t {
    dxf_graal_event_type_t event_type;
    const char *symbol_name;
} dxf_graal_event_t;

/**
 * @brief Represents Quote market event.
 */
typedef struct dxf_graal_event_quote_t {
    dxf_graal_event_t event;
    int32_t sequence;
    int64_t time;
    int64_t time_nanos;
    int32_t time_nano_part;
    int64_t bid_time;
    int16_t bid_exchange_code;
    double bid_price;
    double bid_size;
    int64_t ask_time;
    int16_t ask_exchange_code;
    double ask_price;
    double ask_size;
} dxf_graal_event_quote_t;

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_EVENTS_H_
