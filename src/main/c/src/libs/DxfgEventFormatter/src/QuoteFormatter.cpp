#include <DxfgEventFormatter/QuoteFormatter.hpp>
#include <DxfgUtils/StringUtils.hpp>
#include <cmath>

namespace dxfg {

int64_t QuoteFormatter::getTime(const dxfg_quote_t *e) {
    auto max = static_cast<double>(std::max(e->bid_time, e->ask_time)) / 1000.0;
    auto floorMax = static_cast<int64_t>(std::floor(max));
    return floorMax * 1000 + (e->time_millis_sequence >> 22);
}

int32_t QuoteFormatter::getSequence(const dxfg_quote_t *e) {
    return e->time_millis_sequence & MAX_SEQUENCE;
}

std::string QuoteFormatter::toString(const dxfg_quote_t *e) {
    std::stringstream ss{};
    ss.precision(9);
    // clang-format off
    ss << "Quote{" <<  StringUtils::encodeString(e->market_event.event_symbol) <<
       ", eventTime=" << StringUtils::encodeTime<std::chrono::milliseconds>(e->market_event.event_time) <<
       ", time=" << StringUtils::encodeTime<std::chrono::milliseconds>(getTime(e)) <<
       ", timeNanoPart=" << e->time_nano_part <<
       ", sequence=" << getSequence(e) <<
       ", bidTime=" << StringUtils::encodeTime<std::chrono::seconds>(e->bid_time / 1000) <<
       ", bidExchange=" << StringUtils::encodeUtf16Char(e->bid_exchange_code) <<
       ", bidPrice=" << e->bid_price <<
       ", bidSize=" << e->bid_size <<
       ", askTime=" << StringUtils::encodeTime<std::chrono::seconds>(e->ask_time / 1000) <<
       ", askExchange=" << StringUtils::encodeUtf16Char(e->ask_exchange_code) <<
       ", askPrice=" << e->ask_price <<
       ", askSize=" << e->ask_size <<
       '}';
    // clang-format on
    return ss.str();
}

} // namespace dxfg
