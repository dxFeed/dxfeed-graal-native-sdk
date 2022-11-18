// SPDX-License-Identifier: MPL-2.0

#include "CommandLineParser.hpp"
#include <DxfgEventFormatter/QuoteFormatter.hpp>
#include <DxfgEventFormatter/TimeAndSaleFormatter.hpp>
#include <dxfg_api.h>
#include <iostream>

using namespace dxfg;

static graal_isolate_t *isolate = nullptr;
static graal_isolatethread_t *thread = nullptr;
static std::unordered_map<std::string, std::string> properties{};
static std::string address{};
static std::vector<dxfg_event_kind_t> eventTypes{};
static std::vector<std::string> symbols{};
static bool isQuiteMode = false;

inline static void printEvent(const dxfg_event_type_t *event) {
    if (event->kind == DXFG_EVENT_TYPE_QUOTE) {
        const auto quote = reinterpret_cast<const dxfg_quote_t *>(event);
        std::clog << QuoteFormatter::toString(quote) << std::endl;
    } else if (event->kind == DXFG_EVENT_TYPE_TIME_AND_SALE) {
        const auto tns = reinterpret_cast<const dxfg_time_and_sale_t *>(event);
        std::clog << TimeAndSaleFormatter::toString(tns) << std::endl;
    } else {
        std::cerr << "UnknownEventType{}" << std::endl;
    }
}

static void listener(const dxfg_event_type_t **events, int32_t size) {
    if (!isQuiteMode) {
        for (int32_t i = 0; i < size; ++i) {
            printEvent(events[i]);
        }
    }
}

static void printUsage() {
    // clang-format off
    std::cout << "usage: DxfgClient <properties> <address> <types> <symbols> <options>" << std::endl;
    std::cout << "where: <properties> is optional list space-separated properties and starting with -D (e.g. -Ddxfeed.aggregationPeriod=5s)." << std::endl;
    std::cout << "       <address>    is address to connect for data (remote host or local file):" << std::endl;
    std::cout << "                        <tcp:hostname:port> - for remote host.\r\n"
                 "                        <file:path_to_file> - for local file.\r\n"
                 "                        If you don't specify a prefix (tcp: or file:),\r\n"
                 "                        it will try to automatically determine the connector (by the presence of a colon char)." << std::endl;
    std::cout << "       <types>      is comma-separated list of dxfeed event type (Quote,TimeAndSale)." << std::endl;
    std::cout << "       <symbols>    is comma-separated list of event_symbol names to get events for (e.g. \"IBM,AAPL,MSFT\")." << std::endl;
    std::cout << "options:" << std::endl;
    std::cout << "       -q           is quiet mode, event printing disabled." << std::endl;
    std::cout << "examples:" << std::endl;
    std::cout << "       DxfgClient demo.dxfeed.com:7300 Quote,TimeAndSale AAPL,IBM,/BTCUSD:CXBINA" << std::endl;
    std::cout << "       DxfgClient -Ddxfeed.aggregationPeriod=1s demo.dxfeed.com:7300 Quote,TimeAndSale AAPL,IBM,/BTCUSD:CXBINA" << std::endl;
    // clang-format on
}

static void parseArgs(int argc, char *argv[]) {
    if (argc < 4) {
        std::cerr << "Argument parsing error!" << std::endl;
        printUsage();
        std::exit(-1);
    }

    int currentArg = 1;
    try {
        properties = CommandLineParser::parseSystemProperties(argv, currentArg);
        address = CommandLineParser::parseAddress(argv, currentArg);
        eventTypes = CommandLineParser::parseEventTypes(argv, currentArg);
        symbols = CommandLineParser::parseSymbols(argv, currentArg);
        while (currentArg != argc) {
            if (std::string(argv[currentArg]) == "-q") {
                isQuiteMode = true;
            }
            ++currentArg;
        }
    } catch (std::exception &ex) {
        std::cerr << "Argument parsing error!" << std::endl;
        std::cerr << ex.what() << std::endl;
        printUsage();
        std::exit(-1);
    }
}

void c_print(graal_isolatethread_t *thread, const dxfg_event_type_t **events, int32_t size, void *user_data) {
    for (int i = 0; i < size; ++i) {
        auto *pEvent = (dxfg_event_type_t *)(*(events + i));
        if (pEvent->kind == DXFG_EVENT_TYPE_QUOTE) {
            auto *quote = (dxfg_quote_t *)(*(events + i));
            printf(
                "C: QUOTE{event_symbol=%s, event_time=%lld, time_millis_sequence=%d, time_nano_part=%d, bid_time=%lld, bid_exchange_code=%d, bid_price=%E, bid_size=%E, ask_time=%lld, ask_exchange_code=%hd, ask_price=%f, ask_size=%E}\n",
                quote->market_event.event_symbol,
                quote->market_event.event_time,
                quote->time_millis_sequence,
                quote->time_nano_part,
                quote->bid_time,
                quote->bid_exchange_code,
                quote->bid_price,
                quote->bid_size,
                quote->ask_time,
                quote->ask_exchange_code,
                quote->ask_price,
                quote->ask_size
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_SERIES) {
            auto *event = (dxfg_series_t *)(*(events + i));
            printf(
                "C: SERIES{event_symbol=%s, index=%lld, volatility=%f}\n",
                event->market_event.event_symbol,
                event->index,
                event->volatility
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_TIME_AND_SALE) {
            auto *time_and_sale = (dxfg_time_and_sale_t *)(*(events + i));
            printf(
                "C: TIME_AND_SALE{event_symbol=%s, bid_price=%f, exchange_sale_conditions=%s, buyer=%s, seller=%s}\n",
                time_and_sale->market_event.event_symbol,
                time_and_sale->bid_price,
                time_and_sale->exchange_sale_conditions,
                time_and_sale->buyer,
                time_and_sale->seller
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_SPREAD_ORDER) {
            auto *event = (dxfg_spread_order_t *)(*(events + i));
            printf(
                "C: SPREAD_ORDER{order_base.count=%lld, spread_symbol=%s}\n",
                event->order_base.count,
                event->spread_symbol
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_ORDER) {
            auto *event = (dxfg_order_t *)(*(events + i));
            printf(
                "C: ORDER{order_base.count=%lld, market_maker=%s}\n",
                event->order_base.count,
                event->market_maker
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_ANALYTIC_ORDER) {
            auto *event = (dxfg_analytic_order_t *)(*(events + i));
            printf(
                "C: ANALYTIC_ORDER{order_base.count=%lld, iceberg_peak_size=%f}\n",
                event->order_base.count,
                event->iceberg_peak_size
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_MESSAGE) {
            auto *event = (dxfg_message_t *)(*(events + i));
            printf(
                "C: MESSAGE{event_symbol=%s, event_time=%lld}\n",
                event->event_symbol,
                event->event_time
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_ORDER_BASE) {
            auto *event = (dxfg_order_base_t *)(*(events + i));
            printf(
                "C: ORDER_BASE{size=%f, index=%lld}\n",
                event->size,
                event->index
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_CONFIGURATION) {
            auto *event = (dxfg_configuration_t *)(*(events + i));
            printf(
                "C: CONFIGURATION{event_symbol=%s, version=%d}\n",
                event->event_symbol,
                event->version
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_TRADE) {
            auto *event = (dxfg_trade_t *)(*(events + i));
            printf(
                "C: TRADE{trade_base.size=%f, trade_base.price=%f}\n",
                event->trade_base.size,
                event->trade_base.price
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_TRADE_ETH) {
            auto *event = (dxfg_trade_eth_t *)(*(events + i));
            printf(
                "C: TRADE_ETH{trade_base.size=%f, trade_base.price=%f}\n",
                event->trade_base.size,
                event->trade_base.price
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_THEO_PRICE) {
            auto *event = (dxfg_theo_price_t *)(*(events + i));
            printf(
                "C: THEO_PRICE{index=%lld, price=%f}\n",
                event->index,
                event->price
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_UNDERLYING) {
            auto *event = (dxfg_underlying_t *)(*(events + i));
            printf(
                "C: UNDERLYING{index=%lld, volatility=%f}\n",
                event->index,
                event->volatility
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_GREEKS) {
            auto *event = (dxfg_greeks_t *)(*(events + i));
            printf(
                "C: GREEKS{index=%lld, volatility=%f}\n",
                event->index,
                event->volatility
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_SUMMARY) {
            auto *event = (dxfg_summary_t *)(*(events + i));
            printf(
                "C: SUMMARY{day_id=%d, day_open_price=%f}\n",
                event->day_id,
                event->day_open_price
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_PROFILE) {
            auto *event = (dxfg_profile_t *)(*(events + i));
            printf(
                "C: PROFILE{description=%s, status_reason=%s, high_52_week_price=%f}\n",
                event->description,
                event->status_reason,
                event->high_52_week_price
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_CANDLE) {
            auto *event = (dxfg_candle_t *)(*(events + i));
            printf(
                "C: CANDLE{symbol=%s, base_symbol=%s, exchange_code=%d, index=%lld, ask_volume=%E}\n",
                event->event_symbol->symbol,
                event->event_symbol->base_symbol,
                event->event_symbol->exchange->exchange_code,
                event->index,
                event->ask_volume
            );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_DAILY_CANDLE) {
            auto *event = (dxfg_daily_candle_t *)(*(events + i));
            printf(
                "C: DAILY_CANDLE{symbol=%s, base_symbol=%s, exchange_code=%d, index=%lld, ask_volume=%E}\n",
                event->candle.event_symbol->symbol,
                event->candle.event_symbol->base_symbol,
                event->candle.event_symbol->exchange->exchange_code,
                event->candle.index,
                event->candle.ask_volume
            );
        } else {
            printf(
                "C: %u{}\n",
                pEvent->kind
            );
        }
    }

}

void print_exception(graal_isolatethread_t *thread) {
    dxfg_exception_t* exception = dxfg_get_and_clear_thread_exception_t(thread);
    if (exception) {
        printf("C: %s\n", exception->stackTrace);
        dxfg_release_exception_t(thread, exception);
    }
}

void endpoint_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_state_t old_state,
                                    dxfg_endpoint_state_t new_state, void *user_data) {
    printf("C: state %d -> %d\n", old_state, new_state);
}

int main(int argc, char *argv[]) {
    // Parses input args.
    parseArgs(argc, argv);

    // Creates a graal isolate.
    auto graal_result = graal_create_isolate(nullptr, &isolate, &thread);
    if (graal_result != 0) {
        std::cerr << "Can't create graal isolate!"
                  << "Error code: " << graal_result << std::endl;
        exit(graal_result);
    }

    // Sets system properties.
    for (const auto &property : properties) {
        if (dxfg_system_set_property(thread, property.first.c_str(), property.second.c_str()) != 0) {
            std::cout << "Can't set property: [" << property.first << ":" << property.second << "]" << std::endl;
        }
    }

    const char* d = dxfg_system_get_property(thread, "dsds");
    if (!d) {
        print_exception(thread);
    }


    // Creates an endpoint object.
    dxfg_endpoint_t endpoint;
    int32_t res = dxfg_endpoint_create(thread, &endpoint);
    if (res != 0) {
        print_exception(thread);
    }


    dxfg_executor_t executor;
    dxfg_executor_new_fixed_thread_pool(thread, 2, "thread-processing-events", &executor);
    dxfg_endpoint_set_executor(thread, &endpoint, &executor);

    dxfg_endpoint_state_change_listener_t stateListener;
    dxfg_endpoint_create_state_change_listener(thread, endpoint_state_change_listener, nullptr, &stateListener);
    dxfg_endpoint_add_state_change_listener(thread, &endpoint, &stateListener);
    dxfg_endpoint_add_state_change_listener(thread, &endpoint, &stateListener);

    // Connects to an address.
    res = dxfg_endpoint_connect(thread, &endpoint, address.c_str());
    if (res != 0) {
        print_exception(thread);
    }

    int size = dxfg_endpoint_get_event_types_size(thread, &endpoint);
    int* array = (int*)malloc(sizeof(int) * size);;
    dxfg_endpoint_get_event_types(thread, &endpoint, array);
    for (int i = 0; i < size; ++i) {
        printf("%d \n", array[i]);
    }

    dxfg_feed_t feed;
    res = dxfg_endpoint_get_feed(thread, &endpoint, &feed);
    if (res != 0) {
        print_exception(thread);
    }

    dxfg_endpoint_state_t state;
    res = dxfg_endpoint_get_state(thread, &endpoint, &state);
    if (res != 0) {
        print_exception(thread);
    } else {
        printf("C: get_state = %d\n", state);
    }


    // Creates a subscription with the given types.
    auto types = eventTypes.data();
    auto typesSize = static_cast<int32_t>(eventTypes.size());
    dxfg_subscription_t sub;
    res = dxfg_feed_create_subscription(thread, &feed, types, typesSize, &sub);
    if (res != 0) {
        print_exception(thread);
    }

    // Adds an event listener, before symbols are added.
    dxfg_subscription_event_listener listener = &c_print;



    res = dxfg_subscription_add_event_listener(thread, &sub, listener, nullptr);
    if (res != 0) {
        print_exception(thread);
    }

    dxfg_symbol_t symbol;
    symbol.symbol_type = WILDCARD;
    symbol.symbol = nullptr;

    // Adds symbols.
//    for (const auto &symbol : symbols) {
        res = dxfg_subscription_add_symbol(thread, &sub, &symbol);
        if (res != 0) {
            print_exception(thread);
        }
//    }


    dxfg_quote_t* quote = nullptr;
    dxfg_symbol_t symbol1;
    symbol1.symbol_type = STRING;
    symbol1.symbol = "AAPL";
    dxfg_feed_get_last_event_if_subscribed(thread, &feed, DXFG_EVENT_TYPE_QUOTE, &symbol1, (dxfg_event_type_t**)&quote);

    // Waiting for input to exit.
    std::cin.get();

    dxfg_feed_get_last_event_if_subscribed(thread, &feed, DXFG_EVENT_TYPE_QUOTE, &symbol1, (dxfg_event_type_t**)&quote);


    res = dxfg_endpoint_close_and_await_termination(thread, &endpoint);
    if (res != 0) {
        print_exception(thread);
    }
}
