#include "DxfgUtils/BitUtils.hpp"
#include <DxfgEventFormatter/TimeAndSaleFormatter.hpp>
#include <DxfgUtils/StringUtils.hpp>
#include <cmath>

namespace dxfg {

int64_t TimeAndSaleFormatter::getTime(const dxfg_time_and_sale_t *e) {
    return (e->index >> 32) * 1000 + ((e->index >> 22) & 0x3ff);
}

int32_t TimeAndSaleFormatter::getSequence(const dxfg_time_and_sale_t *e) {
    return static_cast<int32_t>(e->index) & MAX_SEQUENCE;
}

int16_t TimeAndSaleFormatter::getTradeThroughExempt(const dxfg_time_and_sale_t *e) {
    return static_cast<int16_t>(BitUtils::getBits(e->flags, TTE_MASK, TTE_SHIFT));
}

bool TimeAndSaleFormatter::isSpreadLeg(const dxfg_time_and_sale_t *e) {
    return (e->flags & SPREAD_LEG) != 0;
}

bool TimeAndSaleFormatter::isExtendedTradingHours(const dxfg_time_and_sale_t *e) {
    return (e->flags & ETH) != 0;
}

bool TimeAndSaleFormatter::isValidTick(const dxfg_time_and_sale_t *e) {
    return (e->flags & VALID_TICK) != 0;
}

std::string TimeAndSaleFormatter::toString(const dxfg_time_and_sale_t *e) {
    std::stringstream ss{};
    ss.precision(9);
    // clang-format off
    ss << "TimeAndSale{" <<  StringUtils::encodeString(e->market_event.event_symbol) <<
       ", eventTime=" << StringUtils::encodeTime<std::chrono::milliseconds>(e->market_event.event_time) <<
       ", eventFlags=0x" << std::hex << e->event_flags << std::dec <<
       ", time=" << StringUtils::encodeTime<std::chrono::milliseconds>(getTime(e)) <<
       ", timeNanoPart=" << e->time_nano_part <<
       ", sequence=" << getSequence(e) <<
       ", exchange=" << StringUtils::encodeUtf16Char(e->exchange_code) <<
       ", price=" << e->price <<
       ", size=" << e->size <<
       ", bid=" << e->bid_price <<
       ", ask=" << e->ask_price <<
       ", TTE=" << StringUtils::encodeUtf16Char(getTradeThroughExempt(e)) <<
       ", spread=" << StringUtils::encodeBool(isSpreadLeg(e)) <<
       ", ETH=" << StringUtils::encodeBool(isExtendedTradingHours(e)) <<
       ", validTick=" << StringUtils::encodeBool(isValidTick(e)) <<
       (e->buyer == nullptr ? "" : (", byuer='" + std::string(e->buyer) + "'")) <<
       (e->seller == nullptr ? "" : (", seller='" + std::string(e->seller) + "'")) <<
       '}';
    // clang-format on
    return ss.str();
}

} // namespace dxfg
