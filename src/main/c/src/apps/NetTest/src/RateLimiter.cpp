// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/RateLimiter.hpp>

#include <algorithm>
#include <thread>

namespace dxfg {
namespace nettest {

namespace {

constexpr std::chrono::microseconds SPIN_LIMIT{500};

} // namespace

RateLimiter::RateLimiter(std::int64_t recordsPerSecond, std::int64_t batch, StopControl &stop, Clock::time_point start)
    : stop_(stop), recordsPerSecond_(static_cast<double>(recordsPerSecond)),
      capacity_(static_cast<double>(std::max(recordsPerSecond, batch))), lastRefill_(start) {
}

void RateLimiter::refill(Clock::time_point now, double limit) {
    if (now <= lastRefill_) {
        return;
    }

    const double seconds = std::chrono::duration<double>(now - lastRefill_).count();

    tokens_ = std::min(limit, tokens_ + seconds * recordsPerSecond_);
    lastRefill_ = now;
}

bool RateLimiter::tryAcquire(std::int64_t tokens) {
    std::lock_guard<std::mutex> lock(mutex_);
    const double needed = static_cast<double>(tokens);

    refill(Clock::now(), std::max(capacity_, needed));

    if (tokens_ < needed) {
        return false;
    }

    tokens_ -= needed;

    return true;
}

bool RateLimiter::acquire(std::int64_t tokens) {
    const double needed = static_cast<double>(tokens);

    while (!stop_.stopped()) {
        double missing = 0;

        {
            std::lock_guard<std::mutex> lock(mutex_);

            refill(Clock::now(), std::max(capacity_, needed));

            if (tokens_ >= needed) {
                tokens_ -= needed;

                return true;
            }

            missing = needed - tokens_;
        }

        const auto delay = std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::duration<double>(missing / recordsPerSecond_));

        if (delay < SPIN_LIMIT) {
            std::this_thread::yield();
        } else if (stop_.waitFor(delay)) {
            return false;
        }
    }

    return false;
}

} // namespace nettest
} // namespace dxfg
