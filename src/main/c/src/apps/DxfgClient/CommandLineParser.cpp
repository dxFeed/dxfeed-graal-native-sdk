// SPDX-License-Identifier: MPL-2.0

#include "CommandLineParser.hpp"
#include <DxfgUtils/StringUtils.hpp>
#include <stdexcept>

namespace dxfg {

const std::string CommandLineParser::propertyPattern_ = "-D";
const std::unordered_map<std::string, dxfg_event_clazz_t> CommandLineParser::eventTypesMap_ = {
    {"Quote", DXFG_EVENT_QUOTE},
    {"Series", DXFG_EVENT_SERIES},
    {"OptionSale", DXFG_EVENT_OPTION_SALE},
    {"TimeAndSale", DXFG_EVENT_TIME_AND_SALE},
    {"SpreadOrder", DXFG_EVENT_SPREAD_ORDER},
    {"Order", DXFG_EVENT_ORDER},
    {"AnalyticOrder", DXFG_EVENT_ANALYTIC_ORDER},
    {"OtcMarketsOrder", DXFG_EVENT_OTC_MARKETS_ORDER},
    {"Message", DXFG_EVENT_MESSAGE},
    {"OrderBase", DXFG_EVENT_ORDER_BASE},
    {"Configuration", DXFG_EVENT_CONFIGURATION},
    {"Trade", DXFG_EVENT_TRADE},
    {"TradeETH", DXFG_EVENT_TRADE_ETH},
    {"TheoPrice", DXFG_EVENT_THEO_PRICE},
    {"Underlying", DXFG_EVENT_UNDERLYING},
    {"Greeks", DXFG_EVENT_GREEKS},
    {"Summary", DXFG_EVENT_SUMMARY},
    {"Profile", DXFG_EVENT_PROFILE},
    {"DailyCandle", DXFG_EVENT_DAILY_CANDLE},
    {"Candle", DXFG_EVENT_CANDLE},
};

std::unordered_map<std::string, std::string> CommandLineParser::parseSystemProperties(char **argv, int &currentArg) {
    std::unordered_map<std::string, std::string> properties{};
    while (std::string(argv[currentArg]).rfind(propertyPattern_, 0) == 0) {
        auto strings = StringUtils::splitString(argv[currentArg], '=');
        if (strings.size() != 2) {
            throw std::invalid_argument("Incorrect property key/value pair: " + std::string(argv[currentArg]));
        }
        const auto key = strings[0].substr(propertyPattern_.length());
        const auto value = strings[1];
        properties.emplace(key, value);
        ++currentArg;
    }
    return properties;
}

std::string CommandLineParser::parseAddress(char **argv, int &currentArg) { return argv[currentArg++]; }

std::vector<dxfg_event_clazz_t> CommandLineParser::parseEventTypes(char **argv, int &currentArg) {
    std::vector<dxfg_event_clazz_t> eventTypes{};
    const auto stringListEventTypes = StringUtils::splitString(argv[currentArg++], ',');
    for (const auto &stringEventType : stringListEventTypes) {
        auto it = eventTypesMap_.find(stringEventType);
        if (it != eventTypesMap_.end()) {
            eventTypes.push_back(it->second);
        } else {
            throw std::invalid_argument("Unknown events type: " + stringEventType);
        }
    }
    return eventTypes;
}

std::vector<std::string> CommandLineParser::parseSymbols(char **argv, int &currentArg) {
    return StringUtils::splitString(argv[currentArg++], ',');
}

} // namespace dxfg
