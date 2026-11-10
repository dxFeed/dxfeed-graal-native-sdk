// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Generator.hpp>
#include <NetTest/Sdk.hpp>

#include <chrono>

namespace dxfg {
namespace nettest {

GeneratorResult runGenerator(graal_isolatethread_t *thread, dxfg_v4_publisher_t *publisher, EventPool &pool,
                             const GeneratorSettings &settings, RateLimiter *limiter, const StopControl &stop,
                             InstanceCounters &counters) {
    using Clock = std::chrono::steady_clock;

    GeneratorResult result;
    EventBatch batch;
    const auto records = static_cast<std::int64_t>(settings.batch);

    while (!stop.stopped()) {
        if (limiter != nullptr && !limiter->acquire(records)) {
            break;
        }

        const auto start = Clock::now();

        pool.fill(batch, settings.batch, settings.reuse);

        dxfg_v4_event_type_list *list = batch.list();
        const auto code = settings.legacy ? dxfg_DXPublisher_publishEvents(thread, publisher, list)
                                          : dxfg_v4_DXPublisher_publishEvents(thread, publisher, list);
        const auto elapsed = std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now() - start).count();

        if (code != DXFG_EXECUTE_SUCCESSFULLY) {
            result.ok = false;
            result.error = takeThreadException(thread);

            break;
        }

        const auto published = static_cast<std::int64_t>(batch.size());

        counters.records.fetch_add(published, std::memory_order_relaxed);
        counters.weightedNanos.fetch_add(published * elapsed, std::memory_order_relaxed);
        batch.clear();
    }

    return result;
}

} // namespace nettest
} // namespace dxfg
