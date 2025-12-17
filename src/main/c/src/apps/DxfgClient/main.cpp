// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <chrono>
#include <dxfg_api.h>
#include <functional>
#include <iostream>
#include <thread>

#include "CommandLineParser.hpp"
#include "CommandsContext.hpp"
#include "CommandsRegistry.hpp"
#include "Common.hpp"
#include <DxfgEventFormatter/QuoteFormatter.hpp>
#include <DxfgEventFormatter/TimeAndSaleFormatter.hpp>

#include "AdditionalUnderlyingsCase.hpp"
#include "CfiCase.hpp"
#include "DxEndpointMonitoringCase.hpp"
#include "DxEndpointSubscriptionCase.hpp"
#include "DxEndpointTimeSeriesSubscriptionCase.hpp"
#include "DxLinkCase.hpp"
#include "ExceptionCase.hpp"
#include "ExecutorBaseOnConcurrentLinkedQueueCase.hpp"
#include "FinalizeListenerCase.hpp"
#include "GetLastEventCase.hpp"
#include "HistoryEndpointCase.hpp"
#include "IndexedEventModelCase.hpp"
#include "IndexedEventTxModelCase.hpp"
#include "IndexedEventsPromiseCase.hpp"
#include "InstrumentProfileCustomFieldsCase.hpp"
#include "InstrumentProfileFieldCase.hpp"
#include "InstrumentProfileReaderBench.hpp"
#include "LastEventIfSubscribedCase.hpp"
#include "LiveIpfCase.hpp"
#include "LoggingCase.hpp"
#include "OnDemandServiceCase.hpp"
#include "OrcsCase.hpp"
#include "OrderBookModelCase.hpp"
#include "PriceIncrementsCase.hpp"
#include "PromiseCase.hpp"
#include "PromisesAllOfCase.hpp"
#include "ReaderIpfCase.hpp"
#include "ScheduleCase.hpp"
#include "SystemPropertiesCase.hpp"
#include "TxIndexedEventModelCase.hpp"

#include <cinttypes>

using namespace dxfg;
using namespace std::literals;

static graal_isolate_t *maintIsolate = nullptr;
static graal_isolatethread_t *mainIsolateThread = nullptr;

void printUsage() {
    auto usage =
        R"(Usage:
DxfgClient [<help|Help|-h|-?|--help>]
  Shows help on how to use the program.

DxfgClient <list|--list|-l>
  Shows a list of all cases.

DxfgClient <case> [<properties>] [<options>]
Where: <case> is integration case name.
       [<properties>] is optional list space-separated properties and starting with -D (e.g. -Ddxfeed.aggregationPeriod=5s).
       [<options>] is the case options (address, symbols etc).

DxfgClient Qds [<properties>] <options>
Where: [<properties>] is optional list space-separated properties and starting with -D (e.g. -Ddxfeed.aggregationPeriod=5s).
       <options> is the qds-tools options.

Examples:
    DxfgClient Qds help
    DxfgClient qds connect demo.dxfeed.com:7300 Quote AAPL
    DxfgClient qds -Ddxfeed.aggregationPeriod=1s connect demo.dxfeed.com:7300 Quote,TimeAndSale AAPL,IBM,/BTCUSD:CXBINA
)";

    std::cout << usage << std::endl;
}

int main(int argc, char *argv[]) {
    std::vector<std::string> args{};

    if (argc > 1) {
        for (int argIndex = 1; argIndex < argc; argIndex++) {
            args.emplace_back(argv[argIndex]);
        }
    }

    std::vector<Command> cases{
        additionalUnderlyingsCase,
        cfiCase,
        dxEndpointMonitoringCase,
        dxEndpointSubscriptionCase,
        dxEndpointTimeSeriesSubscriptionCase,
        dxLinkCase,
        exceptionCase,
        executorBaseOnConcurrentLinkedQueueCase,
        finalizeListenerCase,
        getLastEventCase,
        indexedEventModelCase,
        indexedEventsPromiseCase,
        lastEventIfSubscribedCase,
        liveIpfCase,
        loggingCase,
        onDemandServiceCase,
        orcsCase,
        orderBookModelCase,
        priceIncrementsCase,
        promiseCase,
        promisesAllOfCase,
        readerIpfCase,
        scheduleCase,
        schedule2Case,
        systemPropertiesCase,
        txIndexedEventModelCase,
        instrumentProfileFieldCase,
        instrumentProfileCustomFieldsCase,
        instrumentProfileReaderBench,
        historyEndpointCase,
        indexedEventTxModelCase,
    };

    for (auto const &c : cases) {
        dxfg::CommandsRegistry::addCommand(c);
    }

    CommandsRegistry::addCommand({"AllCases",
                                  {"all"},
                                  "Runs all cases",
                                  "all",
                                  {},
                                  [&cases](const Command & /*self*/, graal_isolatethread_t *isolateThread,
                                           const std::vector<std::string> &args, const dxfg::CommandsContext &context) {
                                      for (auto const &c : cases) {
                                          c(isolateThread, args, context);
                                      }
                                  }});

    CommandsRegistry::addCommand(
        {"List",
         {"--list", "-l"},
         "Shows a list of all cases.",
         "list",
         {},
         [&cases](const Command & /*self*/, graal_isolatethread_t * /*isolateThread*/,
                  const std::vector<std::string> & /*args*/, const dxfg::CommandsContext &context) {
             for (auto const &c : cases) {
                 std::string info{};

                 info += c.name;

                 for (auto const &sn : c.shortNames) {
                     info += "|" + sn;
                 }

                 if (!c.description.empty()) {
                     info += " - " + c.description;
                 }

                 info += "\n";

                 if (!c.usage.empty()) {
                     info += "Usage:\n  DxfgClient " + c.usage + "\n";
                 }

                 if (!c.examples.empty()) {
                     info += "Examples:\n";
                     for (auto &example : c.examples) {
                         info += "  DxfgClient " + context.substituteDefaultValues(example) + "\n";
                     }
                 }

                 std::cout << info << std::endl;
             }
         }});

    CommandsRegistry::addCommand({"Qds",
                                  {},
                                  "",
                                  "Qds [<properties>] <options>",
                                  {"Qds help", "qds connect %defaultAddress% Quote AAPL",
                                   "qds -Ddxfeed.aggregationPeriod=1s connect %defaultAddress% Quote,TimeAndSale "
                                   "%defaultSymbols%,/BTCUSD:CXBINA"},
                                  [](const Command & /*self*/, graal_isolatethread_t * /*isolateThread*/,
                                     const std::vector<std::string> &args, const dxfg::CommandsContext & /*context*/) {
                                      dxfg_string_list toolsArgs{};
                                      std::vector<const char *> toolsArgsData{};

                                      toolsArgs.size = 0;

                                      for (const auto &arg : args) {
                                          toolsArgsData.emplace_back(arg.c_str());
                                          toolsArgs.size++;
                                      }

                                      toolsArgs.elements = toolsArgsData.data();

                                      dxfg_Tools_main(mainIsolateThread, &toolsArgs);
                                  }});

    CommandsRegistry::addCommand(
        {"Help",
         {"-h", "--help", "-?"},
         "Shows this help",
         "-?",
         {},
         [&cases](const Command & /*self*/, graal_isolatethread_t * /*isolateThread*/,
                  const std::vector<std::string> & /*args*/, const dxfg::CommandsContext & /*context*/) {
             printUsage();
         }});

    if (graal_create_isolate(nullptr, &maintIsolate, &mainIsolateThread) != 0) {
        getException(mainIsolateThread);
        return 1;
    }

    std::size_t argIndex = 1;
    const auto systemProperties = CommandLineParser::parseSystemProperties(args, argIndex);
    CommandsRegistry::COMMANDS_CONTEXT.setSystemProperties(systemProperties);

    for (const auto &systemPropertiesEntry : systemProperties) {
        if (dxfg_system_set_property(mainIsolateThread, systemPropertiesEntry.first.c_str(),
                                     systemPropertiesEntry.second.c_str()) != 0) {
            getException(mainIsolateThread);
            return 1;
        }
    }

    if (!systemProperties.empty() && args.size() > 1) {
        const size_t endIndex = argIndex == args.size() - 1 ? args.size() : argIndex;

        args.erase(args.begin() + 1, args.begin() + static_cast<std::ptrdiff_t>(endIndex));
    }

    CommandsRegistry::tryRunCommand(mainIsolateThread, args, [] {
        printUsage();
    });

    return 0;
}
