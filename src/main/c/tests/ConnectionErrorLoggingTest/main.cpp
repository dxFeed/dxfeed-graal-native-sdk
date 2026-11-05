// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The capture of the connection errors (dxfg_v4_message_connector.h) with a logging listener (dxfg_v4_logging.h): the
// listener still gets QD's records of a failed connect, and once its level is OFF it gets none while the errors are
// still captured. Its own executable: a logging listener cannot be removed from the isolate.

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <chrono>
#include <cstdint>
#include <functional>
#include <mutex>
#include <string>
#include <thread>

namespace dxfg {
namespace test {

namespace {

constexpr auto TIMEOUT = std::chrono::seconds(20);
constexpr auto CONNECTOR_LOGGER = "com.devexperts.qd.qtp.MessageConnector";

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

// The records the logging listener got: all of them, and those of a connector logger with an exception.
struct LogRecords {
    std::mutex mutex;
    int count = 0;
    int connectorErrors = 0;

    static void call(graal_isolatethread_t * /* thread */, dxfg_logging_level_t /* level */, int64_t /* timestamp */,
                     const char * /* threadName */, int64_t /* threadId */, const char *loggerName,
                     const char * /* message */, dxfg_exception_t *exception, const char * /* formattedMessage */,
                     void *userData) {
        auto *self = static_cast<LogRecords *>(userData);
        std::lock_guard lock(self->mutex);

        ++self->count;

        if (loggerName != nullptr && std::string(loggerName).rfind(CONNECTOR_LOGGER, 0) == 0 && exception != nullptr) {
            ++self->connectorErrors;
        }
    }

    int total() {
        std::lock_guard lock(mutex);

        return count;
    }

    int ofConnectors() {
        std::lock_guard lock(mutex);

        return connectorErrors;
    }
};

// The address of the last connection error of the endpoint, empty when there is none; the category through
// `category`.
std::string lastErrorAddress(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint,
                             int32_t *category = nullptr) {
    dxfg_v4_connection_error_t *error = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_getLastConnectionError(thread, endpoint, &error) == DXFG_EXECUTE_SUCCESSFULLY);

    if (error == nullptr) {
        return {};
    }

    const std::string address = error->address != nullptr ? error->address : "";

    if (category != nullptr) {
        *category = error->category;
    }

    REQUIRE(dxfg_v4_ConnectionError_release(thread, error) == DXFG_EXECUTE_SUCCESSFULLY);

    return address;
}

} // namespace

TEST_CASE("The connection errors are captured with a logging listener, also when its level is OFF") {
    const Isolate isolate;
    const auto thread = isolate.thread;
    REQUIRE(thread != nullptr);

    REQUIRE(dxfg_v4_system_set_property(thread, "log.className", "com.devexperts.logging.InterceptableLogging") ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_system_set_property(thread, "log.level", "INFO") == DXFG_EXECUTE_SUCCESSFULLY);

    static LogRecords records{}; // Static: the listener is never removed.
    dxfg_v4_logging_listener_t *listener = nullptr;

    REQUIRE(dxfg_v4_logging_listener_new(thread, &LogRecords::call, &records, &listener) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_logging_set_listener(thread, listener) == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_endpoint_t *endpoint = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_FEED, &endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXEndpoint_connect(thread, endpoint, "127.0.0.1:1") == DXFG_EXECUTE_SUCCESSFULLY);

    // The listener gets QD's record of the failed connect, and the error is captured.
    CHECK(waitFor([&] { return lastErrorAddress(thread, endpoint) == "127.0.0.1:1"; }));
    CHECK(waitFor([] { return records.ofConnectors() > 0; }));

    int32_t category = -1;
    lastErrorAddress(thread, endpoint, &category);
    CHECK(category == DXFG_V4_CONNECTION_ERROR_CONNECT_FAILED);

    // Silenced: the listener gets nothing, the failed connect of a new connector is still captured (a new address:
    // its connector tries at once, while a reconnect of the old one waits for QD's reconnect delay).
    REQUIRE(dxfg_v4_logging_set_log_level(thread, DXFG_LOGGING_LEVEL_OFF) == DXFG_EXECUTE_SUCCESSFULLY);
    std::this_thread::sleep_for(std::chrono::milliseconds(200)); // a record being published finishes

    const auto silenced = records.total();

    REQUIRE(dxfg_v4_DXEndpoint_connect(thread, endpoint, "127.0.0.1:2") == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(waitFor([&] { return lastErrorAddress(thread, endpoint) == "127.0.0.1:2"; }));
    CHECK(records.total() == silenced);

    dxfg_v4_DXEndpoint_close(thread, endpoint);
    dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &listener->handler);
    CHECK(takeExceptionClassName(thread).empty());
}

} // namespace test
} // namespace dxfg
