// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Generator.hpp>
#include <NetTest/Sdk.hpp>

#include <algorithm>
#include <chrono>
#include <optional>
#include <thread>

namespace dxfg {
namespace nettest {

GeneratorResult runGenerator(graal_isolatethread_t *thread, dxfg_v4_publisher_t *publisher, EventPool &pool,
                             const GeneratorSettings &settings, RateLimiter *limiter, const StopControl &stop,
                             InstanceCounters &counters, PublishCounters &publish) {
    using Clock = std::chrono::steady_clock;

    GeneratorResult result;
    EventBatch batch;

    while (!stop.stopped()) {
        // (1) The wait for the batch of the schedule.
        std::optional<RateLimiter::Clock::time_point> due{};

        if (limiter != nullptr) {
            due = limiter->acquire();

            if (!due.has_value()) {
                break;
            }
        }

        // (2) One clock read: the steady clock for the slip and the elapsed time, the wall clock for the events.
        const auto start = Clock::now();
        const auto epochNanos =
            std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::system_clock::now().time_since_epoch())
                .count();

        if (due.has_value()) {
            const auto slip =
                std::max<std::int64_t>(0, std::chrono::duration_cast<std::chrono::nanoseconds>(start - *due).count());

            publish.slipLastNanos.store(slip, std::memory_order_relaxed);
            raiseTo(publish.slipIntervalMaxNanos, slip);
        }

        std::int64_t unset = 0;

        publish.firstBatchEpochNanos.compare_exchange_strong(unset, epochNanos, std::memory_order_relaxed);

        // (3) The fill and (4) the publishing call.
        pool.fillAt(batch, settings.batch, settings.reuse, epochNanos);

        if (settings.publishDelay.count() > 0) {
            std::this_thread::sleep_for(settings.publishDelay);
        }

        dxfg_v4_event_type_list *list = batch.list();
        const auto code = settings.legacy ? dxfg_DXPublisher_publishEvents(thread, publisher, list)
                                          : dxfg_v4_DXPublisher_publishEvents(thread, publisher, list);
        // (5) The elapsed time of (3) and (4).
        const auto elapsed = std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now() - start).count();

        if (code != DXFG_EXECUTE_SUCCESSFULLY) {
            result.ok = false;
            result.error = takeThreadException(thread);

            break;
        }

        const auto published = static_cast<std::int64_t>(batch.size());

        counters.records.fetch_add(published, std::memory_order_relaxed);
        counters.weightedNanos.fetch_add(published * elapsed, std::memory_order_relaxed);
        publish.nanos.fetch_add(elapsed, std::memory_order_relaxed);
        publish.calls.fetch_add(1, std::memory_order_relaxed);
        batch.clear();
    }

    return result;
}

} // namespace nettest
} // namespace dxfg
