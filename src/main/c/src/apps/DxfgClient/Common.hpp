// Copyright (c) 2024 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <algorithm>
#include <locale>
#include <string>
#include <vector>

namespace dxfg {

inline std::string toUpper(const std::string &s, std::locale locale = std::locale()) {
    std::string result(s.size(), ' ');

    std::transform(s.begin(), s.end(), result.begin(), [locale](auto c) {
        return std::toupper(c, locale);
    });

    return result;
}

struct CaseInsensitiveHash {
    std::size_t operator()(const std::string &s) const {
        return std::hash<std::string>()(toUpper(s));
    }
};

struct CaseInsensitiveEq {
    std::size_t operator()(const std::string &s1, const std::string &s2) const {
        return toUpper(s1) == toUpper(s2);
    }
};

} // namespace dxfg