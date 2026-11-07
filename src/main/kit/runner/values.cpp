// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "values.hpp"

#include <cerrno>
#include <cmath>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <limits>

namespace kit {

namespace {

int hexDigit(char c) {
    if (c >= '0' && c <= '9') {
        return c - '0';
    }

    if (c >= 'A' && c <= 'F') {
        return c - 'A' + 10;
    }

    if (c >= 'a' && c <= 'f') {
        return c - 'a' + 10;
    }

    return -1;
}

bool isFieldName(const std::string &name) {
    if (name.empty() || !(name[0] == '_' || (name[0] >= 'a' && name[0] <= 'z'))) {
        return false;
    }

    for (char c : name) {
        if (!(c == '_' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) {
            return false;
        }
    }

    return true;
}

// A decimal integer in the range of int64_t: an optional minus and digits, without a leading zero, and not "-0",
// which only a double prints.
std::optional<std::int64_t> parseInteger(const std::string &value) {
    std::size_t start = !value.empty() && value[0] == '-' ? 1 : 0;

    if (value.size() == start || value.size() - start > 20) {
        return std::nullopt;
    }

    for (std::size_t i = start; i < value.size(); ++i) {
        if (value[i] < '0' || value[i] > '9') {
            return std::nullopt;
        }
    }

    if (value[start] == '0' && (value.size() - start > 1 || start == 1)) {
        return std::nullopt;
    }

    errno = 0;
    char *end = nullptr;
    long long result = std::strtoll(value.c_str(), &end, 10);

    if (errno == ERANGE || end != value.c_str() + value.size()) {
        return std::nullopt;
    }

    return static_cast<std::int64_t>(result);
}

std::uint64_t bitsOf(double value) {
    std::uint64_t bits;
    std::memcpy(&bits, &value, sizeof(bits));

    return bits;
}

} // namespace

std::vector<std::string> splitTokens(const std::string &line) {
    std::vector<std::string> tokens;
    std::size_t start = 0;

    while (start <= line.size()) {
        std::size_t end = line.find(' ', start);

        if (end == std::string::npos) {
            end = line.size();
        }

        tokens.emplace_back(line, start, end - start);
        start = end + 1;
    }

    if (tokens.size() == 1 && tokens[0].empty()) {
        tokens.clear();
    }

    return tokens;
}

std::string joinTokens(const std::vector<std::string> &tokens, std::size_t first) {
    std::string result;

    for (std::size_t i = first; i < tokens.size(); ++i) {
        if (i > first) {
            result += ' ';
        }

        result += tokens[i];
    }

    return result;
}

std::optional<std::string> percentDecode(const std::string &text) {
    std::string result;
    result.reserve(text.size());

    for (std::size_t i = 0; i < text.size(); ++i) {
        if (text[i] != '%') {
            result += text[i];

            continue;
        }

        int high = i + 1 < text.size() ? hexDigit(text[i + 1]) : -1;
        int low = i + 2 < text.size() ? hexDigit(text[i + 2]) : -1;

        if (high < 0 || low < 0) {
            return std::nullopt;
        }

        result += static_cast<char>(high * 16 + low);
        i += 2;
    }

    return result;
}

const std::string *Event::find(const std::string &name) const {
    for (const auto &field : fields) {
        if (field.name == name) {
            return &field.value;
        }
    }

    return nullptr;
}

std::optional<Event> parseEvent(const std::string &line, std::string &error) {
    auto tokens = splitTokens(line);

    if (tokens.size() < 2 || tokens[0] != "event" || tokens[1].empty()) {
        error = "not an event line: " + line;

        return std::nullopt;
    }

    Event event;
    event.type = tokens[1];

    for (std::size_t i = 2; i < tokens.size(); ++i) {
        auto equals = tokens[i].find('=');

        if (equals == std::string::npos || !isFieldName(tokens[i].substr(0, equals))) {
            error = "not a <field>=<value> token: '" + tokens[i] + "'";

            return std::nullopt;
        }

        std::string value = tokens[i].substr(equals + 1);

        if (value != "~" && !percentDecode(value)) {
            error = "a malformed percent escape in '" + tokens[i] + "'";

            return std::nullopt;
        }

        event.fields.push_back({tokens[i].substr(0, equals), value});
    }

    return event;
}

std::optional<double> parseNumber(const std::string &value) {
    if (auto integer = parseInteger(value)) {
        return static_cast<double>(*integer);
    }

    if (value == "nan") {
        return std::numeric_limits<double>::quiet_NaN();
    }

    if (value == "inf") {
        return std::numeric_limits<double>::infinity();
    }

    if (value == "-inf") {
        return -std::numeric_limits<double>::infinity();
    }

    // A decimal: [-]digits[.digits][e[+-]digits]. strtod alone would also accept hex, "infinity" and spaces.
    std::size_t i = 0;
    std::size_t digits = 0;

    if (i < value.size() && value[i] == '-') {
        ++i;
    }

    std::size_t integerStart = i;

    while (i < value.size() && value[i] >= '0' && value[i] <= '9') {
        ++i;
        ++digits;
    }

    // No leading zero, so that a string such as "007" is not taken for a number.
    if (digits > 1 && value[integerStart] == '0') {
        return std::nullopt;
    }

    if (i < value.size() && value[i] == '.') {
        ++i;

        while (i < value.size() && value[i] >= '0' && value[i] <= '9') {
            ++i;
            ++digits;
        }
    }

    if (digits == 0) {
        return std::nullopt;
    }

    if (i < value.size() && (value[i] == 'e' || value[i] == 'E')) {
        ++i;

        if (i < value.size() && (value[i] == '+' || value[i] == '-')) {
            ++i;
        }

        std::size_t exponentDigits = 0;

        while (i < value.size() && value[i] >= '0' && value[i] <= '9') {
            ++i;
            ++exponentDigits;
        }

        if (exponentDigits == 0) {
            return std::nullopt;
        }
    }

    if (i != value.size()) {
        return std::nullopt;
    }

    return std::strtod(value.c_str(), nullptr);
}

bool valuesEqual(const std::string &expected, const std::string &actual) {
    if (expected == "~" || actual == "~") {
        return expected == actual;
    }

    auto expectedInteger = parseInteger(expected);
    auto actualInteger = parseInteger(actual);

    if (expectedInteger && actualInteger) {
        return *expectedInteger == *actualInteger;
    }

    auto expectedNumber = parseNumber(expected);
    auto actualNumber = parseNumber(actual);

    if (expectedNumber && actualNumber) {
        if (std::isnan(*expectedNumber) || std::isnan(*actualNumber)) {
            return std::isnan(*expectedNumber) && std::isnan(*actualNumber);
        }

        return bitsOf(*expectedNumber) == bitsOf(*actualNumber);
    }

    auto expectedText = percentDecode(expected);
    auto actualText = percentDecode(actual);

    return expectedText && actualText && *expectedText == *actualText;
}

} // namespace kit
