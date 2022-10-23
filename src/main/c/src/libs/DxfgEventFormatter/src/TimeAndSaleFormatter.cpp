#include "DxfgUtils/BitUtils.hpp"
#include <DxfgEventFormatter/TimeAndSaleFormatter.hpp>
#include <DxfgUtils/StringUtils.hpp>
#include <cmath>

namespace dxfg {

const std::unordered_map<dxfg_event_time_and_sale_type_t, std::string> TimeAndSaleFormatter::typesMap_ = {
    {DXFG_EVENT_TIME_AND_SALE_TYPE_NEW, "NEW"},
    {DXFG_EVENT_TIME_AND_SALE_TYPE_CORRECTION, "CORRECTION"},
    {DXFG_EVENT_TIME_AND_SALE_TYPE_CANCEL, "CANCEL"},
    {DXFG_EVENT_TIME_AND_SALE_TYPE_UNKNOWN, "UNKNOWN"},
};

const std::unordered_map<dxfg_event_side_t, std::string> TimeAndSaleFormatter::sidesMap_ = {
    {DXFG_EVENT_SIDE_UNDEFINED, "UNDEFINED"},
    {DXFG_EVENT_SIDE_BUY, "BUY"},
    {DXFG_EVENT_SIDE_SELL, "SELL"},
};

int64_t TimeAndSaleFormatter::getTime(const dxfg_event_time_and_sale_t *e) {
    return (e->index >> 32) * 1000 + ((e->index >> 22) & 0x3ff);
}

int32_t TimeAndSaleFormatter::getSequence(const dxfg_event_time_and_sale_t *e) {
    return static_cast<int32_t>(e->index) & MAX_SEQUENCE;
}

int16_t TimeAndSaleFormatter::getTradeThroughExempt(const dxfg_event_time_and_sale_t *e) {
    return static_cast<int16_t>(BitUtils::getBits(e->flags, TTE_MASK, TTE_SHIFT));
}

dxfg_event_side_t TimeAndSaleFormatter::getAggressorSide(const dxfg_event_time_and_sale_t *e) {
    return static_cast<dxfg_event_side_t>(BitUtils::getBits(e->flags, SIDE_MASK, SIDE_SHIFT));
}

bool TimeAndSaleFormatter::isSpreadLeg(const dxfg_event_time_and_sale_t *e) {
    return (e->flags & SPREAD_LEG) != 0;
}

bool TimeAndSaleFormatter::isExtendedTradingHours(const dxfg_event_time_and_sale_t *e) {
    return (e->flags & ETH) != 0;
}

bool TimeAndSaleFormatter::isValidTick(const dxfg_event_time_and_sale_t *e) {
    return (e->flags & VALID_TICK) != 0;
}

dxfg_event_time_and_sale_type_t TimeAndSaleFormatter::getType(const dxfg_event_time_and_sale_t *e) {
    return static_cast<dxfg_event_time_and_sale_type_t>(BitUtils::getBits(e->flags, TYPE_MASK, TYPE_MASK));
}

std::string TimeAndSaleFormatter::toString(const dxfg_event_time_and_sale_t *e) {
    std::stringstream ss{};
    ss.precision(9);
    // clang-format off
    ss << "TimeAndSale{" <<  StringUtils::encodeString(e->base_event.symbol->symbol_name) <<
       ", eventTime=" << StringUtils::encodeTime<std::chrono::milliseconds>(e->base_event.event_time) <<
       ", eventFlags=0x" << std::hex << e->event_flags << std::dec <<
       ", time=" << StringUtils::encodeTime<std::chrono::milliseconds>(getTime(e)) <<
       ", timeNanoPart=" << e->time_nano_part <<
       ", sequence=" << getSequence(e) <<
       ", exchange=" << StringUtils::encodeUtf16Char(e->exchange_code) <<
       ", price=" << e->price <<
       ", size=" << e->size <<
       ", bid=" << e->bid_price <<
       ", ask=" << e->ask_price <<
       ", ESC='" << StringUtils::encodeString(e->exchange_sale_condition) << "'" <<
       ", TTE=" << StringUtils::encodeUtf16Char(getTradeThroughExempt(e)) <<
       ", side=" << StringUtils::enumToString(getAggressorSide(e), sidesMap_) <<
       ", spread=" << StringUtils::encodeBool(isSpreadLeg(e)) <<
       ", ETH=" << StringUtils::encodeBool(isExtendedTradingHours(e)) <<
       ", validTick=" << StringUtils::encodeBool(isValidTick(e)) <<
       ", type=" << StringUtils::enumToString(getType(e), typesMap_) <<
       (e->buyer == nullptr ? "" : (", byuer='" + std::string(e->buyer) + "'")) <<
       (e->seller == nullptr ? "" : (", seller='" + std::string(e->seller) + "'")) <<
       '}';
    // clang-format on
    return ss.str();
}

} // namespace dxfg
