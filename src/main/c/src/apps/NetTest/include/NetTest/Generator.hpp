// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The generator loop of one NetTest instance: take the tokens of a batch, build the batch, publish it, count it.

#ifndef DXFG_NETTEST_GENERATOR_HPP
#define DXFG_NETTEST_GENERATOR_HPP

#include <NetTest/EventPool.hpp>
#include <NetTest/RateLimiter.hpp>
#include <NetTest/StopControl.hpp>

#include <dxfg_api.h>

#include <atomic>
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

struct GeneratorSettings {
    // The number of the events of a batch.
    std::size_t batch = 100;
    // Republish the pool events in place.
    bool reuse = false;
    // Publish through the deprecated dxfg_DXPublisher_publishEvents().
    bool legacy = false;
};

struct GeneratorResult {
    bool ok = true;
    // The text of the SDK exception of the failed publishing call.
    std::string error{};
};

// Runs the loop on the calling thread, which must be attached to the isolate, until the stop is requested or a
// publishing call fails. The limiter is shared by all generators; null means flat out.
GeneratorResult runGenerator(graal_isolatethread_t *thread, dxfg_v4_publisher_t *publisher, EventPool &pool,
                             const GeneratorSettings &settings, RateLimiter *limiter, const StopControl &stop,
                             InstanceCounters &counters);

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_GENERATOR_HPP
