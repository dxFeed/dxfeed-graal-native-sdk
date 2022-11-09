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
 * In the Java implementation, all events are lined up in a hierarchy
 * from the underlying EventType interface. In the C implementation,
 * we specify the type in the event in a separate field because we
 * cannot get it from the class name.
 */
typedef enum dxfg_event_kind_t {
    DXFG_EVENT_TYPE_QUOTE = 0,    // LASTING
    DXFG_EVENT_TYPE_PROFILE,      // LASTING
    DXFG_EVENT_TYPE_SUMMARY,      // LASTING
    DXFG_EVENT_TYPE_GREEKS,       // LASTING + INDEXED -> TIME_SERIES
    DXFG_EVENT_TYPE_CANDLE,       // LASTING + INDEXED -> TIME_SERIES
    DXFG_EVENT_TYPE_DAILY_CANDLE, // LASTING + INDEXED -> TIME_SERIES -> CANDLE
    DXFG_EVENT_TYPE_UNDERLYING,   // LASTING + INDEXED -> TIME_SERIES
    DXFG_EVENT_TYPE_THEO_PRICE,   // LASTING + INDEXED -> TIME_SERIES
    // abstract DXFG_EVENT_TYPE_TRADE_BASE,     // LASTING
    DXFG_EVENT_TYPE_TRADE,          // LASTING -> TRADE_BASE
    DXFG_EVENT_TYPE_TRADE_ETH,      // LASTING -> TRADE_BASE
    DXFG_EVENT_TYPE_CONFIGURATION,  // LASTING
    DXFG_EVENT_TYPE_MESSAGE,        //
    DXFG_EVENT_TYPE_TIME_AND_SALE,  // INDEXED -> TIME_SERIES
    DXFG_EVENT_TYPE_ORDER_BASE,     // INDEXED
    DXFG_EVENT_TYPE_ORDER,          // INDEXED -> ORDER_BASE
    DXFG_EVENT_TYPE_ANALYTIC_ORDER, // INDEXED -> ORDER_BASE -> ORDER
    DXFG_EVENT_TYPE_SPREAD_ORDER,   // INDEXED -> ORDER_BASE
    DXFG_EVENT_TYPE_SERIES,         // INDEXED
} dxfg_event_kind_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandleExchange.html">Javadoc</a>
 */
typedef struct dxfg_candle_exchange_t {
    int16_t exchange_code;
} dxfg_candle_exchange_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandlePrice.html">Javadoc</a>
 */
typedef enum dxfg_candle_price_t {
    LAST = 0,
    BID,
    ASK,
    MARK,
    SETTLEMENT,
} dxfg_candle_price_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandleSession.html">Javadoc</a>
 */
typedef enum dxfg_candle_session_t {
    ANY,
    REGULAR,
} dxfg_candle_session_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandleType.html">Javadoc</a>
 */
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

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandlePeriod.html">Javadoc</a>
 */
typedef struct dxfg_candle_period_t {
    int64_t period_interval_millis;
    double value;
    dxfg_candle_type_t type;
    const char *string;
} dxfg_candle_period_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandleAlignment.html">Javadoc</a>
 */
typedef enum dxfg_candle_alignment_t {
    MIDNIGHT,
    SESSION,
} dxfg_candle_alignment_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandlePriceLevel.html">Javadoc</a>
 */
typedef struct dxfg_candle_price_level_t {
    double value;
} dxfg_candle_price_level_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/CandleSymbol.html">Javadoc</a>
 */
typedef struct dxfg_candle_symbol_t {
    const char *symbol;
    // the following transient fields
    const char *base_symbol;
    dxfg_candle_exchange_t *exchange;
    dxfg_candle_price_t price;
    dxfg_candle_session_t session;
    dxfg_candle_period_t *period;
    dxfg_candle_alignment_t alignment;
    dxfg_candle_price_level_t *price_level;
} dxfg_candle_symbol_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/EventType.html">Javadoc</a>
 */
typedef struct dxfg_event_type_t {
    dxfg_event_kind_t kind;
} dxfg_event_type_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/MarketEvent.html">Javadoc</a>
 */
typedef struct dxfg_market_event_t {
    dxfg_event_type_t event_type;
    const char *event_symbol;
    int64_t event_time;
} dxfg_market_event_t;

///**
// * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/LastingEvent.html">Javadoc</a>
// */
// typedef struct dxfg_lasting_event_t {
//} dxfg_lasting_event_t;
//
///**
// * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/IndexedEvent.html">Javadoc</a>
// */
// typedef struct dxfg_indexed_event_t {
//    //    dxfg_indexed_event_source_t source;
//    //    int32_t event_flags;
//    //    int64_t index;
//} dxfg_indexed_event_t;
//
///**
// * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/TimeSeriesEvent.html">Javadoc</a>
// */
// typedef struct dxfg_time_series_event_t {
//    dxfg_indexed_event_t indexed_event;
//    //    int64_t time;
//} dxfg_time_series_event_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Quote.html">Javadoc</a>
 */
typedef struct dxfg_quote_t {
    dxfg_market_event_t market_event;
    //    dxfg_lasting_event_t lasting_event;
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
} dxfg_quote_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Profile.html">Javadoc</a>
 */
typedef struct dxfg_profile_t {
    dxfg_market_event_t market_event;
    //    dxfg_lasting_event_t lasting_event;
    const char *description;
    const char *status_reason;
    int64_t halt_start_time;
    int64_t halt_end_time;
    double high_limit_price;
    double low_limit_price;
    double high_52_week_price;
    double low_52_week_price;
    int32_t flags;
} dxfg_profile_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Summary.html">Javadoc</a>
 */
typedef struct dxfg_summary_t {
    dxfg_market_event_t market_event;
    //    dxfg_lasting_event_t lasting_event;
    int32_t day_id;
    double day_open_price;
    double day_high_price;
    double day_low_price;
    double day_close_price;
    int32_t prev_day_id;
    double prev_day_close_price;
    double prev_day_volume;
    int64_t open_interest;
    int32_t flags;
} dxfg_summary_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/option/Greeks.html">Javadoc</a>
 */
typedef struct dxfg_greeks_t {
    dxfg_market_event_t market_event;
    //    dxfg_time_series_event_t time_series_event;
    //    dxfg_lasting_event_t lasting_event;
    int32_t eventFlags;

    int64_t index;
    double price;
    double volatility;
    double delta;
    double gamma;
    double theta;
    double rho;
    double vega;
} dxfg_greeks_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/Candle.html">Javadoc</a>
 */
typedef struct dxfg_candle_t {
    dxfg_event_type_t event_type;
    //    dxfg_time_series_event_t time_series_event;
    //    dxfg_lasting_event_t lasting_event;
    dxfg_candle_symbol_t *event_symbol;
    int32_t event_flags;
    int64_t event_time;
    int64_t index;
    int64_t count;
    double open;
    double high;
    double low;
    double close;
    double volume;
    double vwap;
    double bid_volume;
    double ask_volume;
    double imp_volatility;
    double open_interest;
} dxfg_candle_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/candle/DailyCandle.html">Javadoc</a>
 */
typedef struct dxfg_daily_candle_t {
    dxfg_candle_t candle;
} dxfg_daily_candle_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/option/Underlying.html">Javadoc</a>
 */
typedef struct dxfg_underlying_t {
    dxfg_market_event_t market_event;
    //    dxfg_time_series_event_t time_series_event;
    //    dxfg_lasting_event_t lasting_event;
    int32_t eventFlags;

    int64_t index;
    double volatility;
    double frontVolatility;
    double backVolatility;
    double callVolume;
    double putVolume;
    double putCallRatio;
} dxfg_underlying_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/option/TheoPrice.html">Javadoc</a>
 */
typedef struct dxfg_theo_price_t {
    dxfg_market_event_t market_event;
    //    dxfg_time_series_event_t time_series_event;
    //    dxfg_lasting_event_t lasting_event;
    int32_t eventFlags;

    int64_t index;
    double price;
    double underlyingPrice;
    double delta;
    double gamma;
    double dividend;
    double interest;
} dxfg_theo_price_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/TradeBase.html">Javadoc</a>
 */
typedef struct dxfg_trade_base_t {
    dxfg_market_event_t market_event;
    //    dxfg_lasting_event_t lasting_event;
    int64_t timeSequence;
    int32_t timeNanoPart;
    int16_t exchangeCode;
    double price;
    double change;
    double size;
    int32_t dayId;
    double dayVolume;
    double dayTurnover;
    int32_t flags;
} dxfg_trade_base_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Trade.html">Javadoc</a>
 */
typedef struct dxfg_trade_t {
    dxfg_trade_base_t trade_base;
} dxfg_trade_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/TradeETH.html">Javadoc</a>
 */
typedef struct dxfg_trade_eth_t {
    dxfg_trade_base_t trade_base;
} dxfg_trade_eth_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/misc/Configuration.html">Javadoc</a>
 */
typedef struct dxfg_configuration_t {
    //    dxfg_lasting_event_t lasting_event;
    const char *eventSymbol;
    int64_t eventTime;
    int32_t version;
    void *attachment;
} dxfg_configuration_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/misc/Message.html">Javadoc</a>
 */
typedef struct dxfg_message_t {
    dxfg_event_type_t event_type;
    const char *event_symbol;
    int64_t event_time;
    void *attachment;
} dxfg_message_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/TimeAndSale.html">Javadoc</a>
 */
typedef struct dxfg_time_and_sale_t {
    dxfg_market_event_t market_event;
    //    dxfg_time_series_event_t time_series_event;
    int32_t event_flags;
    int64_t index;
    int32_t time_nano_part;
    int16_t exchange_code;
    double price;
    double size;
    double bid_price;
    double ask_price;
    const char *exchange_sale_conditions;
    int32_t flags;
    const char *buyer;
    const char *seller;
} dxfg_time_and_sale_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/OrderBase.html">Javadoc</a>
 */
typedef struct dxfg_order_base_t {
    dxfg_market_event_t market_event;
    //    dxfg_indexed_event_t indexed_event;
    int32_t eventFlags;

    int64_t index;
    int64_t timeSequence;
    int32_t timeNanoPart;

    int64_t actionTime;
    int64_t orderId;
    int64_t auxOrderId;

    double price;
    double size;
    double executedSize;
    int64_t count;
    int32_t flags;

    int64_t tradeId;
    double tradePrice;
    double tradeSize;
} dxfg_order_base_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/Order.html">Javadoc</a>
 */
typedef struct dxfg_order_t {
    dxfg_order_base_t order_base;
    const char *marketMaker;
} dxfg_order_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/AnalyticOrder.html">Javadoc</a>
 */
typedef struct dxfg_analytic_order_t {
    dxfg_order_base_t order_base;
    double icebergPeakSize;
    double icebergHiddenSize;
    double icebergExecutedSize;
    int32_t icebergFlags;
} dxfg_analytic_order_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/market/SpreadOrder.html">Javadoc</a>
 */
typedef struct dxfg_spread_order_t {
    dxfg_order_base_t order_base;
    const char *spreadSymbol;
} dxfg_spread_order_t;

/**
 * <a href="https://docs.dxfeed.com/dxfeed/api/com/dxfeed/event/option/Series.html">Javadoc</a>
 */
typedef struct dxfg_series_t {
    dxfg_market_event_t market_event;
    //    dxfg_indexed_event_t indexed_event;

    int32_t eventFlags;

    int64_t index;
    int64_t timeSequence;
    int32_t expiration;
    double volatility;
    double callVolume;
    double putVolume;
    double putCallRatio;
    double forwardPrice;
    double dividend;
    double interest;
} dxfg_series_t;

#ifdef __cplusplus
}
#endif

#endif // DXFEED_GRAAL_NATIVE_API_EVENTS_H_
