// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The generator loop of one NetTest instance: wait for the batch of the schedule, build the batch, publish it, count it.

#ifndef DXFG_NETTEST_GENERATOR_HPP
#define DXFG_NETTEST_GENERATOR_HPP

#include <NetTest/EventPool.hpp>
#include <NetTest/RateLimiter.hpp>
#include <NetTest/StopControl.hpp>

#include <dxfg_api.h>

#include <atomic>
#include <chrono>
#include <cstddef>
#include <cstdint>
#include <string>

namespace dxfg {
namespace nettest {

// The cumulative counters of an instance: written by its generator, read by the reporter. The nanoseconds are weighted
// by the records: each batch adds records * (the steady-clock time from the start of its build to the return of the
// publishing call).
struct InstanceCounters {
    std::atomic<std::int64_t> records{0};
    std::atomic<std::int64_t> weightedNanos{0};
};

// The counters of the publishing calls of all generators of a run.
struct PublishCounters {
    // The publishing calls and the nanoseconds of the fills and the calls (steps 3 and 4).
    std::atomic<std::int64_t> calls{0};
    std::atomic<std::int64_t> nanos{0};
    // With a schedule: the slip of the last batch and the largest slip since the reporter last took it.
    std::atomic<std::int64_t> slipLastNanos{0};
    std::atomic<std::int64_t> slipIntervalMaxNanos{0};
    // The wall-clock time of the clock read of the first batch, 0 before it: the start of the warm-up on `p`.
    std::atomic<std::int64_t> firstBatchEpochNanos{0};
};

// Raises the atomic to the value when the value is larger.
inline void raiseTo(std::atomic<std::int64_t> &target, std::int64_t value) {
    auto current = target.load(std::memory_order_relaxed);

    while (value > current && !target.compare_exchange_weak(current, value, std::memory_order_relaxed)) {
    }
}

struct GeneratorSettings {
    // The number of the events of a batch.
    std::size_t batch = 100;
    // Republish the pool events in place.
    bool reuse = false;
    // Publish through the deprecated dxfg_DXPublisher_publishEvents().
    bool legacy = false;
    // A wait between the fill of a batch and its publishing call, for the tests (RunHooks::publishDelay).
    std::chrono::nanoseconds publishDelay{0};
};

struct GeneratorResult {
    bool ok = true;
    // The text of the SDK exception of the failed publishing call.
    std::string error{};
};

// Runs the loop on the calling thread, which must be attached to the isolate, until the stop is requested or a
// publishing call fails. Each batch: (1) the wait for its time of the schedule, (2) one read of the clock, whose time
// stamps the events of the batch and gives the slip, (3) the fill, (4) the publishing call, (5) the elapsed time of (3)
// and (4). The limiter is shared by all generators; null means flat out, without a schedule.
GeneratorResult runGenerator(graal_isolatethread_t *thread, dxfg_v4_publisher_t *publisher, EventPool &pool,
                             const GeneratorSettings &settings, RateLimiter *limiter, const StopControl &stop,
                             InstanceCounters &counters, PublishCounters &publish);

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_GENERATOR_HPP
