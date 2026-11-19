// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The statistics of NetTest: the formatter of the statistics blocks of both sides (the lines of QD NetTest) and the
// reporter that prints them on the ticks aligned to the wall clock.

#ifndef DXFG_NETTEST_STATISTICS_HPP
#define DXFG_NETTEST_STATISTICS_HPP

#include <NetTest/StopControl.hpp>

#include <cstdint>
#include <functional>
#include <optional>
#include <string>
#include <vector>

namespace dxfg {
namespace nettest {

// The summary line of a statistics block of either side as a regular expression that ECMAScript and CMake both read
// (no escapes, only bracket expressions): `{*NetTestProducerSide-<name>*} data N rps, lag N us, instances K,
// connections C`, with `NetTestConsumerSide` on `c`, whose lag may be negative. Two adjacent string literals: the
// process tests read them from this file and join them.
constexpr const char *SUMMARY_LINE_REGEX = "[{][*]NetTest(Producer|Consumer)Side-[^*]+[*][}] data [0-9,]+ rps, "
                                           "lag -?[0-9,]+ us, instances [0-9]+, connections [0-9]+";

// The runtime line printed after each statistics block, in the same form:
// `{*NetTestRuntime-<name>*} heap U MB of C MB, gc N (T ms), cpu P%`, where each value may be `n/a`.
constexpr const char *RUNTIME_LINE_REGEX =
    "[{][*]NetTestRuntime-[^*]+[*][}] heap ([0-9,]+ MB|n/a) of ([0-9,]+ MB|n/a), gc ([0-9,]+ [(][0-9,]+ ms[)]|n/a), "
    "cpu ([0-9,]+%|n/a)";

// A runtime value that the SDK does not give, as in dxfg_v4_runtime_stats_t.
constexpr std::int64_t RUNTIME_VALUE_ABSENT = -1;

// The side of the statistics: the label of the summary line and the meaning of its lag.
enum class Side {
    // `p`: NetTestProducerSide, the lag is the time to build and publish a batch, weighted by its records.
    PRODUCER,
    // `c`: NetTestConsumerSide, the lag is the mean e2e lag of the records, which may be negative.
    CONSUMER,
};

// The cumulative counters of one instance.
struct InstanceTotals {
    std::int64_t records = 0;
    // On `p`, the sum of records * (the time to build and publish the batch of those records) in nanoseconds; on `c`,
    // the sum of the e2e lags of the records in nanoseconds, with their signs.
    std::int64_t weightedNanos = 0;
};

// The runtime values of the isolate that the runtime line prints, as dxfg_v4_system_get_runtime_statistics() reads
// them: the heap now and the cumulative GC and CPU counters. A negative value is absent (RUNTIME_VALUE_ABSENT).
struct RuntimeSample {
    std::int64_t heapUsedBytes = RUNTIME_VALUE_ABSENT;
    std::int64_t heapCommittedBytes = RUNTIME_VALUE_ABSENT;
    std::int64_t youngGcCount = RUNTIME_VALUE_ABSENT;
    std::int64_t youngGcTimeMs = RUNTIME_VALUE_ABSENT;
    std::int64_t fullGcCount = RUNTIME_VALUE_ABSENT;
    std::int64_t fullGcTimeMs = RUNTIME_VALUE_ABSENT;
    std::int64_t processCpuTimeNs = RUNTIME_VALUE_ABSENT;
};

// The cumulative counters of all instances and the connections open now, as the reporter reads them, and the runtime
// values of the isolate; without them the reporter prints no runtime line.
struct StatisticsSample {
    std::vector<InstanceTotals> instances{};
    std::int64_t connections = 0;
    std::optional<RuntimeSample> runtime{};
};

// What a statistics block prints: the deltas of the instances over a period of the real length and the connections.
struct StatisticsSnapshot {
    Side side = Side::PRODUCER;
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

// The mean e2e lag in microseconds, `lagSumNanos / records / 1000` divided toward zero, so it may be negative; 0 without
// records.
std::int64_t meanLagMicros(std::int64_t records, std::int64_t lagSumNanos);

// The text of a statistics block without the line break at the end: the summary line, then one line per instance.
std::string formatStatistics(const StatisticsSnapshot &snapshot);

// The snapshot of the period between two samples of the same instances.
StatisticsSnapshot makeSnapshot(const std::string &name, const StatisticsSample &previous,
                                const StatisticsSample &current, std::int64_t periodNanos, Side side = Side::PRODUCER);

// The runtime line of the period between two samples, without the line break: the heap of the current sample in MB
// (MiB, rounded down), the young plus full collections of the period with their time, and the process CPU time of the
// period as a share of its length (above 100% on several cores). A value absent in either sample is `n/a`.
std::string formatRuntime(const std::string &name, const RuntimeSample &previous, const RuntimeSample &current,
                          std::int64_t periodNanos);

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

// Receives every interval of the reporter, printed or not, so that the intervals cover the whole run: the sample at its
// start (with no instances for the first interval, whose counters start at 0), the sample at its end, and its bounds on
// the wall clock in nanoseconds since the Unix epoch.
using IntervalSink = std::function<void(const StatisticsSample &previous, const StatisticsSample &current,
                                        std::int64_t startNanos, std::int64_t endNanos)>;

// Runs on the calling thread until the clock stops it. Sleeps to the next wall-clock multiple of the period and prints
// the deltas against the previous tick; the first tick after the start only primes the counters and prints nothing. At
// the stop it prints the partial period since the last tick (or since the start when no tick came) at its real length,
// unless that period is shorter than one second. When both samples have runtime values, the runtime line follows the
// block in the same text. The side selects the label and the lag of the blocks. The interval sink, when given, gets the
// interval from the start to the first tick, each interval between two ticks and the one from the last tick to the stop.
void runReporter(const std::string &name, std::int64_t periodMillis, const ReporterClock &clock,
                 const StatisticsSource &source, const StatisticsSink &sink, Side side = Side::PRODUCER,
                 const IntervalSink &intervals = {});

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_STATISTICS_HPP
