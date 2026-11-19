// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The run of NetTest: the instances (a publisher endpoint with a generator thread each on `p`, a feed endpoint with a
// listener each on `c`), the reporter thread and the run control (the stop flag, the duration, the signals and, on
// `c`, the starvation watchdog and the check of the nanosecond part).

#ifndef DXFG_NETTEST_RUN_HPP
#define DXFG_NETTEST_RUN_HPP

#include <NetTest/Args.hpp>
#include <NetTest/Generator.hpp>
#include <NetTest/Metrics.hpp>
#include <NetTest/Statistics.hpp>
#include <NetTest/StopControl.hpp>
#include <NetTest/Summary.hpp>
#include <NetTest/SymbolPool.hpp>

#include <dxfg_api.h>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <memory>
#include <ostream>
#include <string>
#include <thread>
#include <utility>
#include <vector>

namespace dxfg {
namespace nettest {

// How long the start waits for a server socket to bind: the connector of a bound one reports itself as connected within
// milliseconds; the connector of one that could not bind (the port is taken) stays connecting.
constexpr std::chrono::milliseconds SERVER_START_TIMEOUT{3000};

// The starvation watchdog of the consumer: with open connections, no records for this long after the last record...
constexpr std::chrono::milliseconds NO_DATA_AFTER_RECORD{30'000};
// ...or for this long after the connection when no record came at all, stops the run with RunStatus::STARVED.
constexpr std::chrono::milliseconds NO_DATA_AFTER_CONNECT{60'000};

// The consumer stops with RunStatus::NANO_MISSING when the first this many events all have the nanosecond part 0.
constexpr std::int64_t NANO_CHECK_EVENTS = 1'000;

// How a run ended, as the `status` of the summary file of the specification names it (without the signal, which only
// main() knows). The names avoid the macros ERROR (wingdi.h) and NO_DATA (winsock.h).
enum class RunStatus {
    // The duration passed or the stop was requested.
    OK,
    // An endpoint failed to start or a publishing call failed.
    FAILED,
    // The starvation watchdog of the consumer.
    STARVED,
    // The first NANO_CHECK_EVENTS events of the consumer had no nanosecond part.
    NANO_MISSING,
};

// The name of the status in the summary file: `ok`, `error`, `no_data`, `nano_missing`.
std::string runStatusName(RunStatus status);

struct RunResult {
    // 0 after a clean stop, 1 after a failed start, a failed publishing call, the watchdog or the nanosecond check.
    int exitCode = 0;
    RunStatus status = RunStatus::OK;
    // One message for stderr when the exit code is not 0.
    std::string error{};
    // The records handed to the publishing call (on `p`) or received (on `c`), in total and per instance.
    std::int64_t records = 0;
    std::vector<std::int64_t> instanceRecords{};
    // What the summary file writes about the run.
    RunReport report{};
};

// The seams of the tests; the defaults are the behaviour of the tool.
struct RunHooks {
    // The time that each generator waits between the fill of a batch (its clock read) and the publishing call: an
    // injected delay that the consumer's e2e lag must show.
    std::chrono::nanoseconds publishDelay{0};
    // The timeouts of the starvation watchdog.
    std::chrono::milliseconds noDataAfterRecord = NO_DATA_AFTER_RECORD;
    std::chrono::milliseconds noDataAfterConnect = NO_DATA_AFTER_CONNECT;
    // false reads the connector statistics as an SDK without DXFG_V4_FEATURE_MESSAGE_CONNECTOR_IO_STATISTICS does: v1
    // only.
    bool connectorStatisticsV2 = true;
};

// Starts the instances of the mode of the options and runs them with the reporter until the stop is requested, the
// duration of the options passes or the run fails (a publishing call on `p`; the watchdog or the nanosecond check on
// `c`), then stops and joins the threads, prints the last statistics and closes the endpoints. The statistics go to
// `out`, flushed. The thread is the attached thread of the caller; the properties of the options are not applied here
// (applySystemProperties()). The live counters, which /metrics reads, must have the instances of the options; without
// them the run keeps its own.
RunResult runNetTest(graal_isolatethread_t *thread, const Options &options, const Symbols &symbols, StopControl &stop,
                     std::ostream &out, const RunHooks &hooks = {}, LiveCounters *live = nullptr);

// The symbols of consumer instance `instance` (from 1) of `instances`: those with the index j (from 0) where
// j % instances == instance - 1, in the order of the set.
std::vector<std::string> consumerSlice(const std::vector<std::string> &symbols, std::int32_t instance,
                                       std::int32_t instances);

// The e2e lag of an event in nanoseconds: `nowNanos - (time_ms * 1000000 + nanoPart)`, where time_ms is the time packed
// in Trade.time_sequence or TimeAndSale.index (packTimeSequence()).
std::int64_t eventLagNanos(std::int64_t nowNanos, std::int64_t packedTime, std::int32_t nanoPart);

// Requests the stop of the control when the process receives SIGINT or SIGTERM (on Windows also SIGBREAK, which Ctrl+Break
// sends). The handler only sets a flag; a thread of the watcher sees it and requests the stop. The previous handlers
// come back at the destruction. One watcher at a time.
class SignalWatcher {
    StopControl &stop_;
    std::atomic<bool> done_{false};
    std::thread thread_{};
    std::vector<std::pair<int, void (*)(int)>> previous_{};

  public:
    explicit SignalWatcher(StopControl &stop);
    SignalWatcher(const SignalWatcher &) = delete;
    SignalWatcher &operator=(const SignalWatcher &) = delete;
    ~SignalWatcher();

    // Whether a signal came since the construction.
    bool received() const;
};

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_RUN_HPP
