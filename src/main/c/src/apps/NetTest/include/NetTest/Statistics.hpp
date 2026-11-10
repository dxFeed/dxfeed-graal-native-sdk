// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The statistics of NetTest: the formatter of the QD NetTest producer-side lines and the reporter that prints them on
// the ticks aligned to the wall clock.

#ifndef DXFG_NETTEST_STATISTICS_HPP
#define DXFG_NETTEST_STATISTICS_HPP

#include <NetTest/StopControl.hpp>

#include <cstdint>
#include <functional>
#include <string>
#include <vector>

namespace dxfg {
namespace nettest {

// The summary line of a statistics block as a regular expression that ECMAScript and CMake both read (no escapes, only
// bracket expressions): `{*NetTestProducerSide-<name>*} data N rps, lag N us, instances K, connections C`.
constexpr const char *SUMMARY_LINE_REGEX =
    "[{][*]NetTestProducerSide-[^*]+[*][}] data [0-9,]+ rps, lag [0-9,]+ us, instances [0-9]+, connections [0-9]+";

// The cumulative counters of one instance.
struct InstanceTotals {
    std::int64_t records = 0;
    // The sum of records * (the time to build and publish the batch of those records) in nanoseconds.
    std::int64_t weightedNanos = 0;
};

// The cumulative counters of all instances and the connections open now, as the reporter reads them.
struct StatisticsSample {
    std::vector<InstanceTotals> instances{};
    std::int64_t connections = 0;
};

// What a statistics block prints: the deltas of the instances over a period of the real length and the connections.
struct StatisticsSnapshot {
    std::string name = "NetTest";
    std::vector<InstanceTotals> deltas{};
    std::int64_t periodNanos = 0;
    std::int64_t connections = 0;
};

// The number with `,` grouping of the thousands, as NumberFormat.getIntegerInstance(Locale.US) prints it.
std::string groupDigits(std::int64_t value);

// Records per second over the period, integer division; 0 for an empty period.
std::int64_t recordsPerSecond(std::int64_t records, std::int64_t periodNanos);

// The record-weighted mean time to build and publish a batch in microseconds, integer division; 0 without records.
std::int64_t lagMicros(std::int64_t records, std::int64_t weightedNanos);

// The text of a statistics block without the line break at the end: the summary line, then one line per instance.
std::string formatStatistics(const StatisticsSnapshot &snapshot);

// The snapshot of the period between two samples of the same instances.
StatisticsSnapshot makeSnapshot(const std::string &name, const StatisticsSample &previous,
                                const StatisticsSample &current, std::int64_t periodNanos);

// The clock of the reporter, replaceable in tests.
struct ReporterClock {
    // The wall clock in nanoseconds since the Unix epoch.
    std::function<std::int64_t()> nowNanos;
    // Sleeps until the wall-clock time; returns false when the reporter must stop.
    std::function<bool(std::int64_t)> sleepUntilNanos;
};

// The clock of the system: the system clock, a sleep that ends at the stop of the control.
ReporterClock systemReporterClock(StopControl &stop);

// Reads the cumulative counters and the connections now.
using StatisticsSource = std::function<StatisticsSample()>;

// Receives the text of a block (with the line break at the end).
using StatisticsSink = std::function<void(const std::string &)>;

// Runs on the calling thread until the clock stops it. Sleeps to the next wall-clock multiple of the period and prints
// the deltas against the previous tick; the first tick after the start only primes the counters and prints nothing. At
// the stop it prints the partial period since the last tick (or since the start when no tick came) at its real length,
// unless that period is shorter than one second.
void runReporter(const std::string &name, std::int64_t periodMillis, const ReporterClock &clock,
                 const StatisticsSource &source, const StatisticsSink &sink);

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_STATISTICS_HPP
