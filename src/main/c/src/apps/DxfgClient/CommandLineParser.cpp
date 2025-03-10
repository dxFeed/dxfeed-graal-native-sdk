// Copyright (c) 2025 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "CommandLineParser.hpp"

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

} // namespace dxfg
