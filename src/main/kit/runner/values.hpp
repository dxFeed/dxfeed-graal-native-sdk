// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <optional>
#include <string>
#include <utility>
#include <vector>

namespace kit {

/// Splits a line into the tokens separated by single spaces; empty tokens (double spaces) are kept as empty strings.
std::vector<std::string> splitTokens(const std::string &line);

/// Joins tokens with single spaces.
std::string joinTokens(const std::vector<std::string> &tokens, std::size_t first = 0);

/// Decodes a percent-encoded string. Returns nothing when an escape is malformed.
std::optional<std::string> percentDecode(const std::string &text);

/// One `<field>=<value>` token of an event line (protocol.md, "Values").
struct Field {
    std::string name;
    std::string value; // as written: still percent-encoded, "~" for NULL
};

/// An event line: `event <Type> <field>=<value>...`.
struct Event {
    std::string type;
    std::vector<Field> fields;

    /// The value of the field as written, or nothing when the event has no such field.
    const std::string *find(const std::string &name) const;
};

/// Parses an event line. Returns nothing and sets `error` when the line is not an event line.
std::optional<Event> parseEvent(const std::string &line, std::string &error);

/// Tells whether two values of a field are equal (protocol.md, "Comparison"): NULL equals only NULL; two integers are
/// compared as integers; two numbers of which one is not an integer are compared as doubles, by their bits, and any
/// NaN equals any NaN; anything else is compared as decoded strings.
bool valuesEqual(const std::string &expected, const std::string &actual);

/// Parses a number value (an integer or a double). Returns nothing when the value is not a number.
std::optional<double> parseNumber(const std::string &value);

} // namespace kit
