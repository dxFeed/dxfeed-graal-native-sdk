#pragma once
#include <cstdint>
#include <dxfg_events.h>
#include <string>
#include <unordered_map>

namespace dxfg {

class TimeAndSaleFormatter {
  private:
    static const int32_t MAX_SEQUENCE = (1 << 22) - 1;

    /*
     * Flags property has several significant bits that are packed into an integer in the following way:
     *   31..16   15...8    7    6    5    4    3    2    1    0
     * +--------+--------+----+----+----+----+----+----+----+----+
     * |        |   TTE  |    |  Side   | SL | ETH| VT |  Type   |
     * +--------+--------+----+----+----+----+----+----+----+----+
     */

    // TTE (TradeThroughExempt) values are ASCII chars in [0, 255].
    static const int32_t TTE_MASK = 0xff;
    static const int32_t TTE_SHIFT = 8;

    // SIDE values are taken from Side enum.
    static const int32_t SIDE_MASK = 3;
    static const int32_t SIDE_SHIFT = 5;

    static const int32_t SPREAD_LEG = 1 << 4;
    static const int32_t ETH = 1 << 3;
    static const int32_t VALID_TICK = 1 << 2;

    // TYPE values are taken from TimeAndSaleType enum.
    static const int32_t TYPE_MASK = 3;
    static const int32_t TYPE_SHIFT = 0;

    static const std::unordered_map<dxfg_event_time_and_sale_type_t, std::string> typesMap_;
    static const std::unordered_map<dxfg_event_side_t, std::string> sidesMap_;

  public:
    static int64_t getTime(const dxfg_event_time_and_sale_t *e);
    static int32_t getSequence(const dxfg_event_time_and_sale_t *e);
    static int16_t getTradeThroughExempt(const dxfg_event_time_and_sale_t *e);
    static dxfg_event_side_t getAggressorSide(const dxfg_event_time_and_sale_t *e);
    static bool isSpreadLeg(const dxfg_event_time_and_sale_t *e);
    static bool isExtendedTradingHours(const dxfg_event_time_and_sale_t *e);
    static bool isValidTick(const dxfg_event_time_and_sale_t *e);
    static dxfg_event_time_and_sale_type_t getType(const dxfg_event_time_and_sale_t *e);
    static std::string toString(const dxfg_event_time_and_sale_t *e);
};

} // namespace dxfg