// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "values.hpp"

#include <cstdint>
#include <optional>
#include <string>
#include <utility>
#include <vector>

namespace kit {

/// A check of an `until` matcher over all the events received until the sentinel (protocol.md, "Scenarios").
struct Check {
    enum class Kind {
        Count,      // count <symbol> <op> <n>
        Last,       // last <symbol> <field>=<value>
        Increasing, // increasing <symbol> <field>
        Accounted,  // accounted <symbol> <n>
        Dropped,    // dropped <op> <n>
        Monotonic,  // monotonic dropped
    };

    Kind kind = Kind::Count;
    int line = 0;
    std::string symbol;
    std::string field;
    std::string value;
    std::string op;
    std::int64_t number = 0;
};

struct Matcher {
    enum class Kind {
        Ok,      // ok
        Reply,   // reply <text>
        Error,   // error
        Outcome, // outcome <outcome>
        Count,   // count <op> <n>
        Events,  // events, then event lines, then end
        Until,   // until <symbol>, then checks, then end
    };

    Kind kind = Kind::Ok;
    std::string text; // the reply, the outcome or the sentinel symbol
    std::string op;
    std::int64_t number = 0;
    std::vector<Event> events;
    std::vector<Check> checks;
};

struct Step {
    enum class Kind {
        Send,   // send <request>: expects ok
        Expect, // expect <request> -> <matcher>
        Exit,   // exit <code>: the adapter exits with the code
    };

    Kind kind = Kind::Send;
    int line = 0;
    std::string request;
    Matcher matcher;
    int exitCode = 0;
};

struct Scenario {
    std::string name;
    std::string path;
    bool ownProcess = false;
    bool usesExperimental = false;
    std::vector<std::string> features;
    std::vector<std::string> capabilities;
    std::vector<std::pair<std::string, std::string>> config;
    std::vector<Step> steps;
};

/// Reads and parses a scenario file. Returns nothing and sets `error` (with the file and line) when the file cannot be
/// read or breaks the grammar of protocol.md.
std::optional<Scenario> parseScenario(const std::string &path, bool ownProcess, std::string &error);

/// Tells whether `a <op> b` holds for the operators `=`, `<=` and `>=`.
bool compare(std::int64_t a, const std::string &op, std::int64_t b);

} // namespace kit
