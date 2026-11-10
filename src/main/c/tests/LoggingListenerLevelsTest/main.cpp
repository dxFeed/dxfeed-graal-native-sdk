// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// A logging listener (dxfg_v4_logging.h) at the level ALL: Java's records of every level reach it with a valid SDK
// level, and the calls that log keep working. A connect logs at FINER (the MBean of the new connector), a level the
// SDK has no constant for. Its own executable: a logging listener cannot be removed from the isolate.

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <array>
#include <chrono>
#include <cstdint>
#include <functional>
#include <mutex>
#include <thread>

namespace dxfg {
namespace test {

namespace {

constexpr auto TIMEOUT = std::chrono::seconds(20);

// Waits until the condition holds or the timeout passes; returns the last result of the condition.
bool waitFor(const std::function<bool()> &condition) {
    const auto deadline = std::chrono::steady_clock::now() + TIMEOUT;

    while (!condition()) {
        if (std::chrono::steady_clock::now() > deadline) {
            return false;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(20));
    }

    return true;
}

// The records the logging listener got, by level; a level outside the enum is counted apart.
struct LogRecords {
    std::mutex mutex;
    std::array<int, DXFG_LOGGING_LEVEL_OFF + 1> byLevel{};
    int invalid = 0;

    static void call(graal_isolatethread_t * /* thread */, dxfg_logging_level_t level, int64_t /* timestamp */,
                     const char * /* threadName */, int64_t /* threadId */, const char * /* loggerName */,
                     const char * /* message */, dxfg_exception_t * /* exception */,
                     const char * /* formattedMessage */, void *userData) {
        auto *self = static_cast<LogRecords *>(userData);
        std::lock_guard lock(self->mutex);

        if (level >= DXFG_LOGGING_LEVEL_ALL && level <= DXFG_LOGGING_LEVEL_OFF) {
            ++self->byLevel[level];
        } else {
            ++self->invalid;
        }
    }

    int of(dxfg_logging_level_t level) {
        std::lock_guard lock(mutex);

        return byLevel[level];
    }

    int outsideTheEnum() {
        std::lock_guard lock(mutex);

        return invalid;
    }
};

} // namespace

TEST_CASE("A logging listener at the level ALL gets the FINER records as TRACE, and a connect succeeds") {
    const Isolate isolate;
    const auto thread = isolate.thread;
    REQUIRE(thread != nullptr);

    REQUIRE(dxfg_v4_system_set_property(thread, "log.className", "com.devexperts.logging.InterceptableLogging") ==
            DXFG_EXECUTE_SUCCESSFULLY);

    static LogRecords records{}; // Static: the listener is never removed.
    dxfg_v4_logging_listener_t *listener = nullptr;

    REQUIRE(dxfg_v4_logging_listener_new(thread, &LogRecords::call, &records, &listener) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_logging_set_listener(thread, listener) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_logging_set_log_level(thread, DXFG_LOGGING_LEVEL_ALL) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_endpoint_t *endpoint = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_FEED, &endpoint) == DXFG_EXECUTE_SUCCESSFULLY);

    // Nothing listens on the port: the connect only creates the connector, which logs at FINER on the way.
    CHECK(dxfg_v4_DXEndpoint_connect(thread, endpoint, "127.0.0.1:1") == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());

    CHECK(waitFor([] { return records.of(DXFG_LOGGING_LEVEL_TRACE) > 0; }));
    CHECK(records.outsideTheEnum() == 0);

    dxfg_v4_DXEndpoint_close(thread, endpoint);
    dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &listener->handler);
    CHECK(takeExceptionClassName(thread).empty());
}

} // namespace test
} // namespace dxfg
