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
                "C: QUOTE{event_symbol=%s, event_time=%lld, time_millis_sequence=%d, time_nano_part=%d, bid_time=%lld, bid_exchange_code=%d, bid_price=%E, ask_time=%f, ask_exchange_code=%lld, ask_price=%hd, ask_size=%E}\n",
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
        } else if (pEvent->kind == DXFG_EVENT_TYPE_TIME_AND_SALE) {
            auto *time_and_sale = (dxfg_time_and_sale_t *)(*(events + i));
            printf(
                "C: TIME_AND_SALE{event_symbol=%s, bid_price=%f, buyer=%s, seller=%s}\n",
                time_and_sale->market_event.event_symbol,
                time_and_sale->bid_price,
                time_and_sale->buyer,
                time_and_sale->seller
                );
        } else if (pEvent->kind == DXFG_EVENT_TYPE_CANDLE) {
            dxfg_candle_t *candle = (dxfg_candle_t *)(*(events + i));
            printf(
                "C: CANDLE{symbol=%s, base_symbol=%s, exchange_code=%d, index=%lld, ask_volume=%E}\n",
                   candle->event_symbol->symbol,
                candle->event_symbol->base_symbol,
                candle->event_symbol->exchange->exchange_code,
                candle->index,
                candle->ask_volume
                );
        }
    }

}

void print_exception(graal_isolatethread_t *thread) {
    dxfg_exception_t exception;
    dxfg_get_and_clear_thread_exception_t(thread, &exception);
    printf("C: %s\n", exception.stackTrace);
}

void endpoint_state_change_listener(graal_isolatethread_t *thread, dxfg_endpoint_state_t old_state, dxfg_endpoint_state_t new_state, void *user_data) {
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

    dxfg_endpoint_state_change_listener stateListener = &endpoint_state_change_listener;
    dxfg_endpoint_add_state_change_listener(thread, &endpoint, stateListener, nullptr);


    // Connects to an address.
    res = dxfg_endpoint_connect(thread, &endpoint, address.c_str());
    if (res != 0) {
        print_exception(thread);
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

    // Adds symbols.
    for (const auto &symbol : symbols) {
        res = dxfg_subscription_add_symbol(thread, &sub, symbol.c_str());
        if (res != 0) {
            print_exception(thread);
        }
    }

    // Waiting for input to exit.
    std::cin.get();

    res = dxfg_endpoint_close_and_await_termination(thread, &endpoint);
    if (res != 0) {
        print_exception(thread);
    }
}
