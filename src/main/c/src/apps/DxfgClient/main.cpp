// SPDX-License-Identifier: MPL-2.0

#include "CommandLineParser.hpp"
#include <Utils/EventFormatter.hpp>
#include <dxfg_api.h>
#include <iostream>

using namespace dxfg;

static graal_isolate_t *isolate = nullptr;
static graal_isolatethread_t *thread = nullptr;
static std::unordered_map<std::string, std::string> properties{};
static std::string address{};
static std::vector<dxfg_event_type_t> eventTypes{};
static std::vector<std::string> symbols{};
static bool isQuiteMode = false;

inline static void printEvent(const dxfg_event_t *event) {
    if (event->event_type == DXFG_EVENT_TYPE_QUOTE) {
        const auto quote = reinterpret_cast<const dxfg_event_quote_t *>(event);
        std::clog << EventFormatter::Quote::toString(quote) << std::endl;
    } else if (event->event_type == DXFG_EVENT_TYPE_TIME_AND_SALE) {
        const auto tns = reinterpret_cast<const dxfg_event_time_and_sale_t *>(event);
        std::clog << EventFormatter::TimeAndSale::toString(tns) << std::endl;
    } else {
        std::cerr << "UnknownEventType{}" << std::endl;
    }
}

static void listener(const dxfg_event_t **events, int32_t size) {
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
    std::cout << "       <symbols>    is comma-separated list of symbol names to get events for (e.g. \"IBM,AAPL,MSFT\")." << std::endl;
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
        std::exit(DXFG_EC_ILLEGAL_ARGUMENT_EX);
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
        std::exit(DXFG_EC_ILLEGAL_ARGUMENT_EX);
    }
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
        if (dxfg_system_set_property(thread, property.first.c_str(), property.second.c_str()) != DXFG_EC_SUCCESS) {
            std::cout << "Can't set property: [" << property.first << ":" << property.second << "]" << std::endl;
        }
    }

    // Creates an endpoint object.
    dxfg_endpoint_t endpoint;
    ERROR_CODE res = dxfg_endpoint_create(thread, &endpoint);
    if (res != DXFG_EC_SUCCESS) {
        std::cerr << "Can't create endpoint! "
                  << "Error code: " << res << std::endl;
        exit(res);
    }

    // Connects to an address.
    res = dxfg_endpoint_connect(thread, endpoint, address.c_str());
    if (res != DXFG_EC_SUCCESS) {
        std::cerr << "Can't connect to endpoint! "
                  << "Error code: " << res << std::endl;
        exit(res);
    }

    // Creates a subscription with the given types.
    auto types = eventTypes.data();
    auto typesSize = static_cast<int32_t>(eventTypes.size());
    dxfg_sub_t sub;
    res = dxfg_sub_create_from_endpoint(thread, endpoint, types, typesSize, &sub);
    if (res != DXFG_EC_SUCCESS) {
        std::cerr << "Can't create subscription! "
                  << "Error code: " << res << std::endl;
        std::exit(res);
    }

    // Adds an event listener, before symbols are added.
    res = dxfg_sub_add_event_listener(thread, sub, listener);
    if (res != DXFG_EC_SUCCESS) {
        std::cerr << "Can't add event listener! "
                  << "Error code: " << res << std::endl;
        std::exit(res);
    }

    // Adds symbols.
    for (const auto &symbol : symbols) {
        dxfg_symbol_t s{};
        s.symbol_name = symbol.c_str();
        res = dxfg_sub_add_symbol(thread, sub, &s);
        if (res != DXFG_EC_SUCCESS) {
            std::cout << "Can't add symbol! "
                      << "Error code: " << res << std::endl;
        }
    }

    // Waiting for input to exit.
    std::cin.get();
}
