// SPDX-License-Identifier: MPL-2.0

#include <Utils/StringUtils.hpp>
#include <iomanip>
#include <sstream>

namespace dxfg {

std::vector<std::string> StringUtils::splitString(const std::string &string, char delimiter) {
    std::vector<std::string> tokens{};
    std::string token{};
    std::istringstream tokenStream(string);
    while (std::getline(tokenStream, token, delimiter)) {
        tokens.push_back(token);
    }
    return tokens;
}

std::string StringUtils::encodeUtf16Char(int16_t c) {
    if (c >= 32 && c <= 126) {
        return {static_cast<char>(c)};
    }
    if (c == 0) {
        return "\\0";
    }
    std::stringstream ss{};
    ss << "\\u" << std::setfill('0') << std::setw(sizeof(c) * 2) << std::hex << c;
    return ss.str();
}

std::string StringUtils::encodeString(const char *string) { return string == nullptr ? "null" : std::string(string); }

std::string StringUtils::encodeBool(int8_t val) { return val == 0 ? "false" : "true"; }

} // namespace dxfg