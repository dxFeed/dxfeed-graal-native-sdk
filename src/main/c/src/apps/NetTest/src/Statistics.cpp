// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Statistics.hpp>

#include <algorithm>
#include <chrono>
#include <limits>

namespace dxfg {
namespace nettest {

namespace {

constexpr std::int64_t NANOS_PER_SECOND = 1'000'000'000;
constexpr std::int64_t NANOS_PER_MICRO = 1'000;
constexpr std::int64_t NANOS_PER_MILLI = 1'000'000;

// The shortest last block that the reporter prints at the stop.
constexpr std::int64_t MIN_LAST_BLOCK_NANOS = NANOS_PER_SECOND;

} // namespace

std::string groupDigits(std::int64_t value) {
    const bool negative = value < 0;
    // The magnitude in an unsigned type, so that the lowest int64 value works as well.
    const auto magnitude = negative ? 0 - static_cast<std::uint64_t>(value) : static_cast<std::uint64_t>(value);
    const std::string digits = std::to_string(magnitude);
    std::string result;

    for (std::size_t index = 0; index < digits.size(); ++index) {
        if (index != 0 && (digits.size() - index) % 3 == 0) {
            result += ',';
        }

        result += digits[index];
    }

    return negative ? "-" + result : result;
}

std::int64_t recordsPerSecond(std::int64_t records, std::int64_t periodNanos) {
    if (periodNanos <= 0 || records <= 0) {
        return 0;
    }

    if (records <= std::numeric_limits<std::int64_t>::max() / NANOS_PER_SECOND) {
        return records * NANOS_PER_SECOND / periodNanos;
    }

    return static_cast<std::int64_t>(static_cast<long double>(records) * NANOS_PER_SECOND / periodNanos);
}

std::int64_t lagMicros(std::int64_t records, std::int64_t weightedNanos) {
    if (records <= 0 || weightedNanos <= 0) {
        return 0;
    }

    return weightedNanos / records / NANOS_PER_MICRO;
}

std::string formatStatistics(const StatisticsSnapshot &snapshot) {
    std::int64_t records = 0;
    std::int64_t weightedNanos = 0;

    for (const auto &delta : snapshot.deltas) {
        records += delta.records;
        weightedNanos += delta.weightedNanos;
    }

    std::string text = "{*NetTestProducerSide-" + snapshot.name + "*} data " +
                       groupDigits(recordsPerSecond(records, snapshot.periodNanos)) + " rps, lag " +
                       groupDigits(lagMicros(records, weightedNanos)) + " us, instances " +
                       std::to_string(snapshot.deltas.size()) + ", connections " +
                       std::to_string(snapshot.connections);

    for (std::size_t index = 0; index < snapshot.deltas.size(); ++index) {
        const auto &delta = snapshot.deltas[index];

        text += "\n    " + snapshot.name + "." + std::to_string(index + 1) + " data " +
                groupDigits(recordsPerSecond(delta.records, snapshot.periodNanos)) + " rps, lag " +
                groupDigits(lagMicros(delta.records, delta.weightedNanos)) + " us";
    }

    return text;
}

StatisticsSnapshot makeSnapshot(const std::string &name, const StatisticsSample &previous,
                                const StatisticsSample &current, std::int64_t periodNanos) {
    StatisticsSnapshot snapshot;

    snapshot.name = name;
    snapshot.periodNanos = periodNanos;
    snapshot.connections = current.connections;

    for (std::size_t index = 0; index < current.instances.size(); ++index) {
        InstanceTotals delta = current.instances[index];

        if (index < previous.instances.size()) {
            delta.records -= previous.instances[index].records;
            delta.weightedNanos -= previous.instances[index].weightedNanos;
        }

        snapshot.deltas.push_back(delta);
    }

    return snapshot;
}

ReporterClock systemReporterClock(StopControl &stop) {
    using Clock = std::chrono::system_clock;

    ReporterClock clock;

    clock.nowNanos = [] {
        return std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now().time_since_epoch()).count();
    };
    clock.sleepUntilNanos = [&stop](std::int64_t nanos) {
        const Clock::time_point time{std::chrono::duration_cast<Clock::duration>(std::chrono::nanoseconds(nanos))};

        return !stop.waitUntil(time);
    };

    return clock;
}

void runReporter(const std::string &name, std::int64_t periodMillis, const ReporterClock &clock,
                 const StatisticsSource &source, const StatisticsSink &sink) {
    const std::int64_t periodNanos = std::max<std::int64_t>(periodMillis, 1) * NANOS_PER_MILLI;
    auto previous = source();
    auto previousTime = clock.nowNanos();
    bool primed = false;

    // The ticks are the multiples of the period of the wall clock; the next one is always after the current time, so a
    // late wake-up never moves the later ticks.
    while (clock.sleepUntilNanos((clock.nowNanos() / periodNanos + 1) * periodNanos)) {
        const auto current = source();
        const auto currentTime = clock.nowNanos();

        if (primed) {
            sink(formatStatistics(makeSnapshot(name, previous, current, currentTime - previousTime)) + "\n");
        }

        primed = true;
        previous = current;
        previousTime = currentTime;
    }

    const auto current = source();
    const auto currentTime = clock.nowNanos();

    // A shorter last block is mostly the noise of its few batches, so it is not printed.
    if (currentTime - previousTime >= MIN_LAST_BLOCK_NANOS) {
        sink(formatStatistics(makeSnapshot(name, previous, current, currentTime - previousTime)) + "\n");
    }
}

} // namespace nettest
} // namespace dxfg
