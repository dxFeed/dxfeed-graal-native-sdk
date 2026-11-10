// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The token bucket that all generators of a run share: the total rate in records per second.

#ifndef DXFG_NETTEST_RATE_LIMITER_HPP
#define DXFG_NETTEST_RATE_LIMITER_HPP

#include <NetTest/StopControl.hpp>

#include <chrono>
#include <cstdint>
#include <mutex>

namespace dxfg {
namespace nettest {

// The tokens accrue continuously from the start of the run at the rate, up to the capacity of one second of tokens or
// one batch, whichever is larger (so a rate below the batch size still releases a batch every batch/rate seconds). A
// generator takes the tokens of a whole batch before it builds the batch, so the budget never goes into debt. The bucket
// starts empty. Thread-safe.
class RateLimiter {
  public:
    using Clock = std::chrono::steady_clock;

  private:
    StopControl &stop_;
    const double recordsPerSecond_;
    double capacity_;
    std::mutex mutex_{};
    double tokens_ = 0;
    Clock::time_point lastRefill_;

    // Adds the tokens accrued since the last refill; the caller holds the mutex.
    void refill(Clock::time_point now, double limit);

  public:
    // The batch is the number of the records that one acquire() takes; the start is the moment from which the tokens
    // accrue.
    RateLimiter(std::int64_t recordsPerSecond, std::int64_t batch, StopControl &stop,
                Clock::time_point start = Clock::now());

    RateLimiter(const RateLimiter &) = delete;
    RateLimiter &operator=(const RateLimiter &) = delete;

    // The most tokens the bucket holds.
    double capacity() const {
        return capacity_;
    }

    // Takes the tokens if the bucket has them now; never waits.
    bool tryAcquire(std::int64_t tokens);

    // Waits until the bucket has the tokens and takes them; returns false, taking nothing, when the stop is requested
    // before that. A wait shorter than about half a millisecond yields the thread instead of sleeping.
    bool acquire(std::int64_t tokens);
};

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_RATE_LIMITER_HPP
