// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The command line of NetTest: the options, their parsing and validation, the usage text and the system properties.

#ifndef DXFG_NETTEST_ARGS_HPP
#define DXFG_NETTEST_ARGS_HPP

#include <dxfg_api.h>

#include <cstdint>
#include <optional>
#include <string>
#include <utility>
#include <vector>

namespace dxfg {
namespace nettest {

// The event type that the instances publish.
enum class EventKind {
    TRADE,
    TIME_AND_SALE,
};

// The parsed and validated command line of the publisher mode.
struct Options {
    // The address of the endpoints: `host:port` to connect, `:port` to listen.
    std::string address{};
    // The number of the symbols of the pool (the `<total>` of -S).
    std::int64_t totalSymbols = 0;
    // The IPF file of the symbols (the `@<file>` of -S), empty when the symbols are generated.
    std::string symbolsFile{};
    // The number of the instances (-C).
    std::int32_t instances = 1;
    // The total rate of all instances in records per second (-r */<rate>), unset when the generators run flat out.
    std::optional<std::int64_t> recordsPerSecond{};
    // The period of the statistics line in milliseconds (-s).
    std::int64_t periodMillis = 10'000;
    // The name in the statistics line and the base of the endpoint names (-n).
    std::string name = "NetTest";
    // The number of the events of a published batch (--batch).
    std::int32_t batch = 100;
    EventKind event = EventKind::TRADE;
    // Republish the pre-built events in place instead of copying them (--reuse).
    bool reuse = false;
    // Publish through the deprecated function (--legacy).
    bool legacy = false;
    // The length of the run in milliseconds (--duration), unset when it ends on a signal only.
    std::optional<std::int64_t> durationMillis{};
    // The system properties (-D<key>=<value>) in the command line order.
    std::vector<std::pair<std::string, std::string>> properties{};
    // The arguments of the isolate (-Xmx, -Xms, -Xmn, -XX:<option>) as they are, in the command line order.
    std::vector<std::string> isolateArguments{};
};

// The result of the parsing: the options, or the message of the first error.
struct ParseResult {
    bool ok = false;
    Options options{};
    std::string error{};
};

// Parses the command line; args[0] is the program name, as in argv. Never throws on bad input.
ParseResult parseArguments(const std::vector<std::string> &args);

// Parses a duration with the suffix `ms`, `s` or `m` (for example `500ms`, `10s`, `2m`) into milliseconds. Returns
// nullopt for a missing suffix, a non-positive or an overflowing value.
std::optional<std::int64_t> parseDuration(const std::string &text);

// Parses a rate with the optional suffix `k`, `M` or `G` (for example `500k`, `1M`, `2.5M`) into records per second.
// Returns nullopt for a value below 1 record per second or an overflowing value.
std::optional<std::int64_t> parseRate(const std::string &text);

// Returns true when the address makes an endpoint listen (it starts with `:`).
bool isServerAddress(const std::string &address);

// The usage text.
std::string usageText();

// Sets the -D properties through dxfg_v4_system_set_property(); call it before the first endpoint is created. Returns
// false and fills the error with the key of the first failure.
bool applySystemProperties(graal_isolatethread_t *thread, const Options &options, std::string &error);

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_ARGS_HPP
