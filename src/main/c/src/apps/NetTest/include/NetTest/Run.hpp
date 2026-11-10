// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The run of NetTest: the instances (a publisher endpoint each), their generator threads, the reporter thread and the
// run control (the stop flag, the duration and the signals).

#ifndef DXFG_NETTEST_RUN_HPP
#define DXFG_NETTEST_RUN_HPP

#include <NetTest/Args.hpp>
#include <NetTest/Generator.hpp>
#include <NetTest/Statistics.hpp>
#include <NetTest/StopControl.hpp>
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

struct RunResult {
    // 0 after a clean stop, non-zero after a failed start or a failed publishing call.
    int exitCode = 0;
    // One message for stderr when the exit code is not 0.
    std::string error{};
    // The records handed to the publishing call, in total and per instance.
    std::int64_t records = 0;
    std::vector<std::int64_t> instanceRecords{};
};

// Starts the instances, runs the generators and the reporter until the stop is requested, the duration of the options
// passes or a publishing call fails, then stops and joins the threads, prints the last statistics and closes the
// endpoints. The statistics go to `out`, flushed. The thread is the attached thread of the caller; the properties of
// the options are not applied here (applySystemProperties()).
RunResult runNetTest(graal_isolatethread_t *thread, const Options &options, const Symbols &symbols, StopControl &stop,
                     std::ostream &out);

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
};

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_RUN_HPP
