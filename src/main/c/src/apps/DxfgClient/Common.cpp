// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "Common.hpp"

#include <cinttypes>
#include <cstdio>
#include <cstdint>

namespace dxfg {

void printException(dxfg_exception_t *exception) { // NOLINT(*-no-recursion)
    if (!exception) {
        return;
    }

    printf("  exception %s %s\n", exception->class_name, exception->message);
    printf("  PrintStackTrace %s\n", exception->print_stack_trace);

    for (int i = 0; i < exception->stack_trace->size; ++i) {
        dxfg_stack_trace_element_t *stackTraceElement = exception->stack_trace->elements[i];
        printf("  %s %d %s\n", stackTraceElement->class_name, stackTraceElement->line_number,
               stackTraceElement->method_name);
    }

    printException(exception->cause);
}

void getException(graal_isolatethread_t *isolateThread) {
    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(isolateThread);

    printException(exception);
    dxfg_Exception_release(isolateThread, exception);
}

void printEvent(graal_isolatethread_t *isolateThread, const dxfg_event_type_t *event) {
    static constexpr size_t DXFG_ORDER_FLAGS_SIDE_SHIFT = 2;
    static constexpr uint32_t DXFG_ORDER_FLAGS_SIDE_MASK = 0b11;

    auto getOrderSide = [](const int32_t flags) {
        switch (static_cast<uint32_t>(flags) >> DXFG_ORDER_FLAGS_SIDE_SHIFT & DXFG_ORDER_FLAGS_SIDE_MASK) {
        case 0:
            return "UNDEFINED(0)";
        case 1:
            return "BUY(1)";
        case 2:
            return "SELL(2)";
        default:
            return "UNKNOWN";
        }
    };

    if (!event) {
        return;
    }

    switch (event->clazz) {
    case DXFG_EVENT_QUOTE: {
        auto *quote = (dxfg_quote_t *)event;

        printf("  QUOTE{event_symbol=%s, event_time=%" PRId64
               " , time_millis_sequence=%d, time_nano_part=%d, bid_time=%" PRId64 ", "
               "bid_exchange_code=%d, bid_price=%E, bid_size=%E, ask_time=%" PRId64
               ", ask_exchange_code=%hd, ask_price=%f, "
               "ask_size=%E}\n",
               quote->market_event.event_symbol, quote->market_event.event_time, quote->time_millis_sequence,
               quote->time_nano_part, quote->bid_time, quote->bid_exchange_code, quote->bid_price, quote->bid_size,
               quote->ask_time, quote->ask_exchange_code, quote->ask_price, quote->ask_size);
    } break;
    case DXFG_EVENT_PROFILE: {
        auto *profile = (dxfg_profile_t *)event;

        printf("  PROFILE{description=%s, status_reason=%s, high_52_week_price=%f}\n", profile->description,
               profile->status_reason, profile->high_52_week_price);
    } break;
    case DXFG_EVENT_SUMMARY: {
        auto *summary = (dxfg_summary_t *)event;

        printf("  SUMMARY{day_id=%d, day_open_price=%f}\n", summary->day_id, summary->day_open_price);
    } break;
    case DXFG_EVENT_GREEKS: {
        auto *greeks = (dxfg_greeks_t *)event;

        printf("  GREEKS{index=%lld, volatility=%f}\n", greeks->index, greeks->volatility);
    } break;
    case DXFG_EVENT_CANDLE: {
        auto *candle = (dxfg_candle_t *)event;
        const auto time = (candle->index >> 32ULL) * 1000 + (int64_t)(((uint64_t)(candle->index >> 22ULL) & 0x3ffULL));

        printf("  CANDLE{symbol=%s, index=%" PRId64 ", count=%" PRId64 ", time=%" PRId64
               ", high=%f, low=%f, open=%f, close=%f, volume=%f, vwap=%f, bid_volume=%f, ask_volume=%f, "
               "imp_volatility=%f, open_interest=%f, event_time=%" PRId64 "}\n",
               candle->event_symbol, candle->index, candle->count, time, candle->high, candle->low, candle->open,
               candle->close, candle->volume, candle->vwap, candle->bid_volume, candle->ask_volume,
               candle->imp_volatility, candle->open_interest, candle->event_time);
    } break;
    case DXFG_EVENT_DAILY_CANDLE: {
        auto *daily_candle = (dxfg_daily_candle_t *)event;

        printf("  DAILY_CANDLE{symbol=%s, index=%" PRId64 ", ask_volume=%E}\n", daily_candle->candle.event_symbol,
               daily_candle->candle.index, daily_candle->candle.ask_volume);
    } break;
    case DXFG_EVENT_UNDERLYING: {
        auto *underlying = (dxfg_underlying_t *)event;

        printf("  UNDERLYING{index=%" PRId64 ", volatility=%f}\n", underlying->index, underlying->volatility);
    } break;
    case DXFG_EVENT_THEO_PRICE: {
        auto *theo_price = (dxfg_theo_price_t *)event;

        printf("  THEO_PRICE{index=%" PRId64 ", price=%f}\n", theo_price->index, theo_price->price);
    } break;
    case DXFG_EVENT_TRADE: {
        auto *trade = (dxfg_trade_t *)event;

        printf("  TRADE{trade_base.size=%f, trade_base.price=%f}\n", trade->trade_base.size, trade->trade_base.price);
    } break;
    case DXFG_EVENT_TRADE_ETH: {
        auto *trade_eth = (dxfg_trade_eth_t *)event;

        printf("  TRADE_ETH{trade_base.size=%f, trade_base.price=%f}\n", trade_eth->trade_base.size,
               trade_eth->trade_base.price);
    } break;
    case DXFG_EVENT_CONFIGURATION: {
        auto *configuration = (dxfg_configuration_t *)event;

        printf("  CONFIGURATION{event_symbol=%s, version=%d}\n", configuration->event_symbol, configuration->version);
    } break;
    case DXFG_EVENT_MESSAGE: {
        auto *message = (dxfg_message_t *)event;

        printf("  MESSAGE{event_symbol=%s, event_time=%" PRId64 "}\n", message->event_symbol, message->event_time);
    } break;
    case DXFG_EVENT_TIME_AND_SALE: {
        auto *time_and_sale = (dxfg_time_and_sale_t *)event;

        printf("  TIME_AND_SALE{event_symbol=%s, bid_price=%f, exchange_sale_conditions=%s, buyer=%s, seller=%s}\n",
               time_and_sale->market_event.event_symbol, time_and_sale->bid_price,
               time_and_sale->exchange_sale_conditions, time_and_sale->buyer, time_and_sale->seller);
    } break;
    // case DXFG_EVENT_ORDER_BASE:
    //     break;
    case DXFG_EVENT_ORDER: {
        auto *order = (dxfg_order_t *)event;
        dxfg_indexed_event_source_t *source =
            dxfg_IndexedEvent_getSource(isolateThread, &order->order_base.market_event.event_type);

        printf("  Order{%s, src=%s, side=%s, price=%f, size=%f, mm='%s', flags=0x%X, e.Flags=0x%X}\n",
               order->order_base.market_event.event_symbol, source->name, getOrderSide(order->order_base.flags),
               order->order_base.price, order->order_base.size, order->market_maker, order->order_base.flags,
               order->order_base.event_flags);
        dxfg_IndexedEventSource_release(isolateThread, source);
    } break;
    case DXFG_EVENT_ANALYTIC_ORDER: {
        auto *analytic_order = (dxfg_analytic_order_t *)event;

        printf("  ANALYTIC_ORDER{order_base.count=%" PRId64 ", iceberg_peak_size=%f}\n",
               analytic_order->order_base.order_base.count, analytic_order->iceberg_peak_size);
    } break;
    case DXFG_EVENT_OTC_MARKETS_ORDER: {
        auto *otc_markets_order = (dxfg_otc_markets_order_t *)event;

        printf("  OTC_MARKETS_ORDER{order_base.count=%" PRId64 ", quote_access_payment=%d}\n",
               otc_markets_order->order_base.order_base.count, otc_markets_order->quote_access_payment);
    } break;
    case DXFG_EVENT_SPREAD_ORDER: {
        auto *spread_order = (dxfg_spread_order_t *)event;

        printf("  SPREAD_ORDER{order_base.count=%" PRId64 ", spread_symbol=%s}\n", spread_order->order_base.count,
               spread_order->spread_symbol);
    } break;
    case DXFG_EVENT_SERIES: {
        auto *series = (dxfg_series_t *)event;

        printf("  SERIES{event_symbol=%s, index=%" PRId64 ", volatility=%f}\n", series->market_event.event_symbol,
               series->index, series->volatility);
    } break;
    case DXFG_EVENT_OPTION_SALE: {
        auto *option_sale = (dxfg_option_sale_t *)event;

        printf("  OPTION_SALE{event_symbol=%s, index=%" PRId64 ", volatility=%f, option_symbol=%s}\n",
               option_sale->market_event.event_symbol, option_sale->index, option_sale->volatility,
               option_sale->option_symbol);
    } break;
    case DXFG_EVENT_TEXT_MESSAGE: {
        auto *text_message = (dxfg_text_message_t *)event;

        printf("  TEXT_MESSAGE{event_symbol=%s, event_time=%" PRId64 ", time_sequence=%" PRId64 ", text=%s}\n",
               text_message->event_symbol, text_message->event_time, text_message->time_sequence, text_message->text);
    } break;
    case DXFG_EVENT_MARKET_MAKER: {
        auto *market_maker = (dxfg_market_maker_t *)event;

        printf("  MARKET_MAKER{event_symbol=%s, index=%" PRId64 ", bid_time=%" PRId64
               ", bid_price=%f, bid_size=%f, bid_count=%" PRId64 ", ask_time=%" PRId64
               ", ask_price=%f, ask_size=%f, ask_count=%" PRId64 "}\n",
               market_maker->market_event.event_symbol, market_maker->index, market_maker->bid_time,
               market_maker->bid_price, market_maker->bid_size, market_maker->bid_count, market_maker->ask_time,
               market_maker->ask_price, market_maker->ask_size, market_maker->ask_count);
    } break;
    case DXFG_EVENT_TEXT_CONFIGURATION: {
        auto *text_configuration = (dxfg_text_configuration_t *)event;

        printf("  TEXT_CONFIGURATION{event_symbol=%s, event_time=%" PRId64 ", time_sequence=%" PRId64
               ", version=%d, text=%s}\n",
               text_configuration->event_symbol, text_configuration->event_time, text_configuration->time_sequence,
               text_configuration->version, text_configuration->text);
    } break;

    default:
        printf("  %u{}\n", event->clazz);
    }
}

void printEvents(graal_isolatethread_t *isolateThread, dxfg_event_type_list *events, void * /* user_data */) {
    for (int i = 0; i < events->size; ++i) {
        printEvent(isolateThread, events->elements[i]);
    }
}

void observableListModelListenerCallback(graal_isolatethread_t *isolateThread, dxfg_event_type_list *orders,
                                         void * /* user_data */) {
    printf("  observableListModelListenerCallback:\n");
    for (int i = 0; i < orders->size; ++i) {
        printEvent(isolateThread, reinterpret_cast<dxfg_event_type_t *>(orders->elements[0]));
    }
}

void endpointStateChangeListener(graal_isolatethread_t * /* thread */, dxfg_endpoint_state_t old_state,
                                 dxfg_endpoint_state_t new_state, void * /* user_data */) {
    printf("  State %d -> %d\n", old_state, new_state);
}

void stateChangeListener(graal_isolatethread_t * /* thread */, dxfg_ipf_connection_state_t old_state,
                         dxfg_ipf_connection_state_t new_state, void * /* user_data */) {
    printf("  IPF stateChangeListener %d -> %d\n", old_state, new_state);
}

void finalize(graal_isolatethread_t * /* thread */, void * /* user_data */) {
    puts("  finalize");
}

}