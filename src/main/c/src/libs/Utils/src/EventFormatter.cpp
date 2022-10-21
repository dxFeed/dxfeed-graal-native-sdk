// SPDX-License-Identifier: MPL-2.0

#include <Utils/EventFormatter.hpp>
#include <Utils/StringUtils.hpp>
#include <chrono>
#include <sstream>

namespace dxfg {

std::string EventFormatter::Quote::toString(const dxfg_event_quote_t *event) {
    std::stringstream ss{};
    ss.precision(9);
    // clang-format off
    ss << "Quote{" <<  StringUtils::encodeString(event->base_event.symbol->symbol_name) <<
       ", eventTime=" << StringUtils::encodeTime<std::chrono::milliseconds>(event->base_event.event_time) <<
//       ", time=" << StringUtils::encodeTime<std::chrono::milliseconds>(event->time) <<
       ", timeNanoPart=" << event->time_nano_part <<
//       ", sequence=" << event->sequence <<
       ", bidTime=" << StringUtils::encodeTime<std::chrono::milliseconds>(event->bid_time) <<
       ", bidExchange=" << StringUtils::encodeUtf16Char(event->bid_exchange_code) <<
       ", bidPrice=" << event->bid_price <<
       ", bidSize=" << event->bid_size <<
       ", bidTime=" << StringUtils::encodeTime<std::chrono::milliseconds>(event->ask_time) <<
       ", askExchange=" << StringUtils::encodeUtf16Char(event->ask_exchange_code) <<
       ", askPrice=" << event->ask_price <<
       ", askSize=" << event->ask_size <<
       '}';
    // clang-format on
    return ss.str();
}

const std::unordered_map<dxfg_event_time_and_sale_type_t, std::string> EventFormatter::TimeAndSale::typesMap_ = {
    {DXFG_EVENT_TIME_AND_SALE_TYPE_NEW, "NEW"},
    {DXFG_EVENT_TIME_AND_SALE_TYPE_CORRECTION, "CORRECTION"},
    {DXFG_EVENT_TIME_AND_SALE_TYPE_CANCEL, "CANCEL"},
    {DXFG_EVENT_TIME_AND_SALE_TYPE_UNKNOWN, "UNKNOWN"},
};

const std::unordered_map<dxfg_event_side_t, std::string> EventFormatter::TimeAndSale::sidesMap_ = {
    {DXFG_EVENT_SIDE_UNDEFINED, "UNDEFINED"},
    {DXFG_EVENT_SIDE_BUY, "BUY"},
    {DXFG_EVENT_SIDE_SELL, "SELL"},
};

std::string EventFormatter::TimeAndSale::toString(const dxfg_event_time_and_sale_t *event) {
    std::stringstream ss{};
    ss.precision(9);
    // clang-format off
    ss << "TimeAndSale{" <<  StringUtils::encodeString(event->base_event.symbol->symbol_name) <<
       ", eventTime=" << StringUtils::encodeTime<std::chrono::milliseconds>(event->base_event.event_time) <<
       ", eventFlags=0x" << std::hex << event->event_flags << std::dec <<
//       ", time=" << StringUtils::encodeTime<std::chrono::milliseconds>(event->time) <<
       ", timeNanoPart=" << event->time_nano_part <<
//       ", sequence=" << event->sequence <<
       ", exchange=" << StringUtils::encodeUtf16Char(event->exchange_code) <<
       ", price=" << event->price <<
       ", size=" << event->size <<
       ", bid=" << event->bid_price <<
       ", ask=" << event->ask_price <<
//       ", ESC='" << StringUtils::encodeString(event->exchange_sale_condition) << "'" <<
//       ", TTE=" << StringUtils::encodeUtf16Char(event->trade_through_exempt) <<
//       ", side=" << StringUtils::enumToString(event->aggressor_side, sidesMap_) <<
//       ", spread=" << StringUtils::encodeBool(event->is_spread_leg) <<
//       ", ETH=" << StringUtils::encodeBool(event->is_extended_trading_hours) <<
//       ", validTick=" << StringUtils::encodeBool(event->is_valid_tick) <<
//       ", type=" << StringUtils::enumToString(event->type, typesMap_) <<
//       (event->buyer == nullptr ? "" : (", byuer='" + std::string(event->buyer) + "'")) <<
//       (event->seller == nullptr ? "" : (", seller='" + std::string(event->seller) + "'")) <<
       '}';
    // clang-format on
    return ss.str();
}

} // namespace dxfg
