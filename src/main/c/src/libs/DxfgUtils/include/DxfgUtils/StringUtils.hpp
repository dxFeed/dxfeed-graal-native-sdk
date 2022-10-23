// SPDX-License-Identifier: MPL-2.0

#pragma once
#include <chrono>
#include <date/date.h>
#include <string>
#include <vector>

namespace dxfg {

class StringUtils {
  public:
    /**
     * @brief Returns a vector string that contains the substrings in this instance
     * that are delimited by elements of a specified char.
     * @param[in] string The input string.
     * @param[in] delimiter The delimeter.
     * @return Returns vector substrings.
     */
    static std::vector<std::string> splitString(const std::string &string, char delimiter);

    /**
     * @brief Converts UTF16 char to the string representation.
     * @param[in] c The UTF-16 char.
     * @return Returns a string representations UTF-16 char.
     */
    static std::string encodeUtf16Char(int16_t c);

    /**
     * @brief Encodes string.
     * @param[in] string The input string.
     * @return Returns 'null' if inputs string is nullptr, otherwise returns string.
     */
    static std::string encodeString(const char *string);

    /**
     * @brief Converts boolean val to string representations.
     * @tparam T The input value type.
     * @param[in] val The input val.
     * @return Returns 'true' if val not equals 0, otherwise returns true.
     */
    template <class T> inline static std::string encodeBool(T val) { return val != 0 ? "true" : "false"; }

    /**
     * @brief Converts the timepoint to the string representation.
     * @tparam TDuration The time dimension (std::chrono::milliseconds, std::chrono::seconds, etc.).
     * @param[in] timePoint The timepoint.
     * @return Returns a string representation timepoint.
     */
    template <class TDuration> inline static std::string encodeTime(int64_t timePoint) {
        if (timePoint == 0) {
            return "0";
        }
        return date::format("%Y%m%d-%H%m%S%z", (date::sys_time<TDuration>{TDuration{timePoint}}));
    }

    /**
     * @brief Converts enum value to the string representation.
     * @tparam TEnum The enum type.
     * @tparam TMap The map type.
     * @param[in] val The enum value.
     * @param[in] map A map containing an enumeration value as a key and its string representation as a value.
     * @return Returns a string representation of the input enum.
     */
    template <class TEnum, class TMap> inline static std::string enumToString(TEnum val, TMap map) {
        auto it = map.find(val);
        if (it != map.end()) {
            return it->second;
        } else {
            throw std::invalid_argument("Unknown events type: " + std::to_string(val));
        }
    }
};

} // namespace dxfg