// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <chrono>
#include <date/date.h>
#include <string>
#include <vector>

namespace dxfg {

class StringUtils {
  public:
    static std::vector<std::string> splitString(const std::string &string, char delimiter);
    static std::string encodeBool(int8_t val);
    static std::string encodeUtf16Char(int16_t c);
    static std::string encodeString(const char *string);
    template <class TEnum, class TMap> static std::string enumToString(TEnum val, TMap map) {
        auto it = map.find(val);
        if (it != map.end()) {
            return it->second;
        } else {
            throw std::invalid_argument("Unknown events type: " + std::to_string(val));
        }
    }
    template <class TDuration> static std::string encodeTime(int64_t time) {
        if (time == 0) {
            return "0";
        }
        return date::format("%Y%m%d-%H%m%S%z", (date::sys_time<TDuration>{TDuration{time}}));
    }
};

} // namespace dxfg