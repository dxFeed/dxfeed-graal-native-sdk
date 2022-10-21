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
typedef enum dxfg_event_type_t {
    DXFG_EVENT_TYPE_QUOTE = 0,
    DXFG_EVENT_TYPE_TIME_AND_SALE,
} dxfg_event_type_t;

/**
 * @brief Side of an order or a trade.
 */
typedef enum dxfg_event_side {
    /**
     * @brief Buy side (bid).
     */
    DXFG_EVENT_SIDE_BUY = 0,
    /**
     * @brief Sell side (ask or offer).
     */
    DXFG_EVENT_SIDE_SELL,
    /**
     * @brief Side is undefined, unknown or inapplicable.
     */
    DXFG_EVENT_SIDE_UNDEFINED,
} dxfg_event_side;

typedef struct dxfg_symbol_t {
    const char *symbol_name;
} dxfg_symbol_t;

typedef struct dxfg_candle_exchange_t {
    int16_t bid_exchange_code;
} dxfg_candle_exchange_t;

typedef enum dxfg_candle_price_t {
    LAST,
    BID,
    ASK,
    MARK,
    SETTLEMENT,
} dxfg_candle_price_t;

typedef enum dxfg_candle_session_t {
    ANY,
    REGULAR,
} dxfg_candle_session_t;

typedef enum dxfg_candle_type_t {
    TICK,
    SECOND,
    MINUTE,
    HOUR,
    DAY,
    WEEK,
    MONTH,
    OPTEXP,
    YEAR,
    VOLUME,
    PRICE,
    PRICE_MOMENTUM,
    PRICE_RENKO,
} dxfg_candle_type_t;

typedef struct dxfg_candle_period_t {
    double value;
    dxfg_candle_type_t type;
    const char *string;
} dxfg_candle_period_t;

typedef enum dxfg_candle_alignment_t {
    MIDNIGHT,
    SESSION,
} dxfg_candle_alignment_t;

typedef struct dxfg_candle_price_level_t {
    double value;
} dxfg_candle_price_level_t;

typedef struct dxfg_symbol_candle_t {
    dxfg_symbol_t baseSymbol;
    const char *base_symbol;
    dxfg_candle_exchange_t exchange;
    dxfg_candle_price_t price;
    dxfg_candle_period_t period;
    dxfg_candle_alignment_t alignment;
    dxfg_candle_price_level_t priceLevel;
} dxfg_symbol_candle_t;

/**
 * @brief Base event type.
 */
typedef struct dxfg_event_t {
    dxfg_event_type_t event_type;
    int64_t event_time;
    dxfg_symbol_t *symbol;
} dxfg_event_t;

/**
 * @brief Represents Quote market event.
 */
typedef struct dxfg_event_quote_t {
    dxfg_event_t event;
    int32_t time_millis_sequence;
    int32_t time_nano_part;
    int64_t bid_time;
    int16_t bid_exchange_code;
    double bid_price;
    double bid_size;
    int64_t ask_time;
    int16_t ask_exchange_code;
    double ask_price;
    double ask_size;
} dxfg_event_quote_t;

/**
 * @brief Type of a TimeAndSale event.
 */
typedef enum dxfg_event_time_and_sale_type_t {
    /**
     * @brief Represents new time and sale event.
     */
    DXFG_EVENT_TIME_AND_SALE_TYPE_NEW,
    /**
     * @brief Represents correction time and sale event.
     */
    DXFG_EVENT_TIME_AND_SALE_TYPE_CORRECTION,
    /**
     * @brief Represents cancel time and sale event.
     */
    DXFG_EVENT_TIME_AND_SALE_TYPE_CANCEL,
    /**
     * @brief Unknown type.
     */
    DXFG_EVENT_TIME_AND_SALE_TYPE_UNKNOWN,
} dxfg_event_time_and_sale_type_t;

/**
 * @brief Represents TimeAndSale market event.
 */
typedef struct dxfg_event_time_and_sale_t {
    dxfg_event_t event;
    int32_t event_flags;
    int64_t index;
    int32_t time_nano_part;
    int16_t exchange_code;
    double price;
    double size;
    double bid_price;
    double ask_price;
    const char *exchange_sale_condition;
    int32_t flags;
    const char *buyer;
    const char *seller;
} dxfg_event_time_and_sale_t;

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_EVENTS_H_
