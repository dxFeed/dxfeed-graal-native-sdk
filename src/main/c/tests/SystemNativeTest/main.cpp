#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <atomic>
#include <chrono>
#include <cstddef>
#include <cstring>
#include <fstream>
#include <string>
#include <thread>
#include <utility>
#include <vector>

// ToDo Fix java daemon thread, appears in
// https://github.com/stdcion/dxfeed-graal-native-api/commit/525e2eb5191bd196d08f725baf866f739f2a12b4#diff-390547990893e97ec6012ce95f1b5d4447f855fdbe11c95e183e9386f94f9f59R115
// ToDo Issue https://github.com/oracle/graal/issues/4290 https://github.com/oracle/graal/issues/4024
// https://github.com/oracle/graal/commit/52b709f17b7384f0997fd76476437d9106652de4

namespace dxfg {
namespace test {

TEST_CASE("Check reads unset property") {
    // Setup graal.
    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;
    auto graal_result = graal_create_isolate(nullptr, &isolate, &thread);
    REQUIRE(graal_result == 0);

    // On getting an unset property, returns nullptr.
    auto prop = dxfg_system_get_property(thread, "first_prop");
    REQUIRE(prop == nullptr);
    // Frees nullptr does not result in an error.
    dxfg_system_release_property(thread, prop);

    // Tear down graal.
    // graal_result = graal_detach_all_threads_and_tear_down_isolate(thread);
    REQUIRE(graal_result == 0);
}

TEST_CASE("Check write/read property") {
    // Setup graal.
    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;
    auto graal_result = graal_create_isolate(nullptr, &isolate, &thread);
    REQUIRE(graal_result == 0);

    size_t count = 100;
    std::string key = "key_";
    std::string val = "val_";

    // Sets properties.
    for (size_t i = 0; i < count; ++i) {
        auto currentKey = std::string(key + std::to_string(i));
        auto currentVal = std::string(val + std::to_string(i));
        auto res = dxfg_system_set_property(thread, currentKey.c_str(), currentVal.c_str());
        REQUIRE(res == 0);
    }

    // Gets properties.
    for (size_t i = 0; i < count; ++i) {
        auto currentKey = std::string(key + std::to_string(i));
        auto expectedVal = std::string(val + std::to_string(i));
        auto currentVal = dxfg_system_get_property(thread, currentKey.c_str());
        REQUIRE(currentVal != nullptr);
        REQUIRE(std::string(currentVal) == expectedVal);
        dxfg_system_release_property(thread, currentVal);
    }

    // Tear down graal.
    // graal_result = graal_detach_all_threads_and_tear_down_isolate(thread);
    REQUIRE(graal_result == 0);
}

TEST_CASE("Check in different isolate") {
    // Setup graal.
    graal_isolate_t *isolateFirst = nullptr;
    graal_isolatethread_t *threadFirst = nullptr;
    graal_isolate_t *isolateSecond = nullptr;
    graal_isolatethread_t *threadSecond = nullptr;
    auto graal_result = graal_create_isolate(nullptr, &isolateFirst, &threadFirst);
    REQUIRE(graal_result == 0);
    graal_result = graal_create_isolate(nullptr, &isolateSecond, &threadSecond);
    REQUIRE(graal_result == 0);

    size_t count = 100;
    std::string key = "key_";
    std::string valFirst = "val_first_";
    std::string valSecond = "val_second_";

    // Sets properties.
    for (size_t i = 0; i < count; ++i) {
        auto currentKey = std::string(key + std::to_string(i));
        auto currentValFirst = std::string(valFirst + std::to_string(i));
        auto currentValSecond = std::string(valSecond + std::to_string(i));
        auto res = dxfg_system_set_property(threadFirst, currentKey.c_str(), currentValFirst.c_str());
        REQUIRE(res == 0);
        res = dxfg_system_set_property(threadSecond, currentKey.c_str(), currentValSecond.c_str());
        REQUIRE(res == 0);
    }

    // Gets properties.
    for (size_t i = 0; i < count; ++i) {
        auto currentKey = std::string(key + std::to_string(i));
        auto expectedValFirst = std::string(valFirst + std::to_string(i));
        auto expectedValSecond = std::string(valSecond + std::to_string(i));

        auto currentVal = dxfg_system_get_property(threadFirst, currentKey.c_str());
        REQUIRE(currentVal != nullptr);
        REQUIRE(std::string(currentVal) == expectedValFirst);
        dxfg_system_release_property(threadFirst, currentVal);

        currentVal = dxfg_system_get_property(threadSecond, currentKey.c_str());
        REQUIRE(currentVal != nullptr);
        REQUIRE(std::string(currentVal) == expectedValSecond);
        dxfg_system_release_property(threadFirst, currentVal);
    }

    // Tear down graal.
    // graal_result = graal_detach_all_threads_and_tear_down_isolate(threadFirst);
    REQUIRE(graal_result == 0);

    // graal_result = graal_detach_all_threads_and_tear_down_isolate(threadSecond);
    REQUIRE(graal_result == 0);
}

TEST_CASE("The version is the one of the build") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    const char *version = nullptr;
    REQUIRE(dxfg_v4_system_get_version(isolate.thread, &version) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(version != nullptr);
    CHECK(std::string(version) == DXFG_TEST_SDK_VERSION);
    REQUIRE(dxfg_v4_String_release(isolate.thread, version) == DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(dxfg_v4_system_get_version(isolate.thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("The advanced revision is the one of the headers") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    int32_t revision = -1;
    REQUIRE(dxfg_v4_system_get_advanced_revision(isolate.thread, &revision) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(revision == DXFG_V4_ADVANCED_REVISION);

    CHECK(dxfg_v4_system_get_advanced_revision(isolate.thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("The features of the headers are supported, unknown ones are not") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    const auto isSupported = [&isolate](const char *name) {
        int32_t supported = -1;
        REQUIRE(dxfg_v4_system_is_feature_supported(isolate.thread, name, &supported) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE((supported == 0 || supported == 1));

        return supported == 1;
    };

    CHECK(isSupported(DXFG_V4_FEATURE_EVENT_QUEUE));
    CHECK(isSupported(DXFG_V4_FEATURE_SHARED_EVENT_QUEUE));
    CHECK(isSupported(DXFG_V4_FEATURE_EVENT_QUEUE_READY_SIGNAL));
    CHECK(isSupported(DXFG_V4_FEATURE_FEED_FILTER));
    CHECK(isSupported(DXFG_V4_FEATURE_TIME_SERIES_TX_MODEL_EVENT_PEEK));
    CHECK(isSupported(DXFG_V4_FEATURE_MESSAGE_CONNECTORS));
    CHECK(isSupported(DXFG_V4_FEATURE_CONNECTION_ERRORS));
    CHECK(isSupported(DXFG_V4_FEATURE_RUNTIME_STATISTICS));
    CHECK_FALSE(isSupported("no_such_feature"));
    CHECK_FALSE(isSupported(""));
    CHECK(takeExceptionClassName(isolate.thread).empty());

    int32_t supported = -1;
    CHECK(dxfg_v4_system_is_feature_supported(isolate.thread, nullptr, &supported) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");
    CHECK(supported == -1);
    CHECK(dxfg_v4_system_is_feature_supported(isolate.thread, DXFG_V4_FEATURE_EVENT_QUEUE, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");
}

// The layout of the struct is frozen: 18 fields of int64_t, the last one available_processors.
static_assert(sizeof(dxfg_v4_runtime_stats_t) == 18 * sizeof(int64_t), "The layout of dxfg_v4_runtime_stats_t changed");
static_assert(offsetof(dxfg_v4_runtime_stats_t, heap_used_bytes) == 0, "The layout of dxfg_v4_runtime_stats_t changed");
static_assert(offsetof(dxfg_v4_runtime_stats_t, allocated_bytes) == 11 * sizeof(int64_t),
              "The layout of dxfg_v4_runtime_stats_t changed");
static_assert(offsetof(dxfg_v4_runtime_stats_t, available_processors) == 17 * sizeof(int64_t),
              "The layout of dxfg_v4_runtime_stats_t changed");

// Whether the tested library is built with the Serial GC, from its build-info file (the G1 variant has another GC).
static bool isSerialGc() {
    std::ifstream file(DXFG_TEST_BUILD_INFO);
    REQUIRE(file.is_open());

    const std::string key = "native-image.garbage-collector=";

    for (std::string line; std::getline(file, line);) {
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }

        if (line.compare(0, key.size(), key) == 0) {
            return line.substr(key.size()) == "Serial GC";
        }
    }

    FAIL("No " << key << " in " << DXFG_TEST_BUILD_INFO);

    return false;
}

// The fields of the runtime statistics with their names, in the order of the struct.
static std::vector<std::pair<const char *, int64_t>> fieldsOf(const dxfg_v4_runtime_stats_t &stats) {
    return {{"heap_used_bytes", stats.heap_used_bytes},
            {"heap_committed_bytes", stats.heap_committed_bytes},
            {"heap_max_bytes", stats.heap_max_bytes},
            {"young_gc_count", stats.young_gc_count},
            {"young_gc_time_ms", stats.young_gc_time_ms},
            {"full_gc_count", stats.full_gc_count},
            {"full_gc_time_ms", stats.full_gc_time_ms},
            {"thread_count", stats.thread_count},
            {"peak_thread_count", stats.peak_thread_count},
            {"process_cpu_time_ns", stats.process_cpu_time_ns},
            {"uptime_ms", stats.uptime_ms},
            {"allocated_bytes", stats.allocated_bytes},
            {"eden_used_bytes", stats.eden_used_bytes},
            {"survivor_used_bytes", stats.survivor_used_bytes},
            {"old_used_bytes", stats.old_used_bytes},
            {"daemon_thread_count", stats.daemon_thread_count},
            {"total_started_thread_count", stats.total_started_thread_count},
            {"available_processors", stats.available_processors}};
}

// Reads the runtime statistics into a struct filled with a value that no field may have (all bytes 0xFE, negative
// and not -1), so a field the SDK does not write fails the checks.
static dxfg_v4_runtime_stats_t readRuntimeStatistics(graal_isolatethread_t *thread) {
    dxfg_v4_runtime_stats_t stats;
    std::memset(&stats, 0xFE, sizeof(stats));

    REQUIRE(dxfg_v4_system_get_runtime_statistics(thread, &stats) == DXFG_EXECUTE_SUCCESSFULLY);

    return stats;
}

// Every field is written and is -1 or 0 or more, and the values agree with each other where both are given.
static void checkRuntimeStatistics(const dxfg_v4_runtime_stats_t &stats) {
    for (const auto &field : fieldsOf(stats)) {
        INFO(field.first << " = " << field.second);
        CHECK((field.second == -1 || field.second >= 0));
    }

    const auto given = [](int64_t value) {
        return value != -1;
    };

    if (given(stats.heap_used_bytes) && given(stats.heap_committed_bytes) && given(stats.heap_max_bytes)) {
        CHECK(stats.heap_used_bytes <= stats.heap_committed_bytes);
        CHECK(stats.heap_committed_bytes <= stats.heap_max_bytes);
    }

    for (const int64_t pool : {stats.eden_used_bytes, stats.survivor_used_bytes, stats.old_used_bytes}) {
        if (given(pool) && given(stats.heap_committed_bytes)) {
            CHECK(pool <= stats.heap_committed_bytes);
        }
    }

    // The values every build of the SDK gives on the desktop platforms of the tests.
    CHECK(stats.heap_used_bytes > 0);
    CHECK(stats.thread_count >= 1);
    CHECK(stats.peak_thread_count >= stats.thread_count);
    CHECK(stats.daemon_thread_count <= stats.thread_count);
    CHECK(stats.total_started_thread_count >= stats.thread_count);
    CHECK(stats.process_cpu_time_ns > 0);
    CHECK(stats.uptime_ms >= 0);
    CHECK(stats.available_processors >= 1);

    // The Serial GC gives the collections and the pools (by the names of its beans); another GC (G1) gives none.
    if (isSerialGc()) {
        CHECK(stats.young_gc_count >= 0);
        CHECK(stats.young_gc_time_ms >= 0);
        CHECK(stats.full_gc_count >= 0);
        CHECK(stats.full_gc_time_ms >= 0);
        CHECK(stats.eden_used_bytes >= 0);
        CHECK(stats.survivor_used_bytes >= 0);
        CHECK(stats.old_used_bytes >= 0);
    } else {
        CHECK(stats.young_gc_count == -1);
        CHECK(stats.full_gc_count == -1);
    }
}

// Allocates on the Java heap (system properties with 4 KiB values, overwritten) until the young GC count grows or,
// where the GC gives no count (G1), a fixed number of times; returns the statistics after it.
static dxfg_v4_runtime_stats_t allocateUntilYoungGc(graal_isolatethread_t *thread,
                                                    const dxfg_v4_runtime_stats_t &before) {
    const std::string value(4096, 'x');
    const auto deadline = std::chrono::steady_clock::now() + std::chrono::seconds(30);
    auto after = before;

    for (int round = 0; std::chrono::steady_clock::now() < deadline; ++round) {
        for (int i = 0; i < 10000; ++i) {
            REQUIRE(dxfg_system_set_property(thread, "runtime_statistics_test", value.c_str()) == 0);
        }

        after = readRuntimeStatistics(thread);

        if (after.young_gc_count == -1 ? round >= 20 : after.young_gc_count > before.young_gc_count) {
            break;
        }
    }

    return after;
}

TEST_CASE("The runtime statistics of a fresh isolate are given and consistent") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    const auto stats = readRuntimeStatistics(isolate.thread);

    checkRuntimeStatistics(stats);
    CHECK(takeExceptionClassName(isolate.thread).empty());
}

TEST_CASE("The runtime statistics count the collections, CPU time and uptime of an allocation") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    const auto before = readRuntimeStatistics(isolate.thread);
    const auto after = allocateUntilYoungGc(isolate.thread, before);

    checkRuntimeStatistics(after);
    INFO("young GC " << before.young_gc_count << " -> " << after.young_gc_count << ", CPU "
                     << before.process_cpu_time_ns << " -> " << after.process_cpu_time_ns);

    // The Serial GC counts the collections (checkRuntimeStatistics checks that another GC gives -1).
    if (isSerialGc()) {
        CHECK(after.young_gc_count > before.young_gc_count);
        CHECK(after.young_gc_time_ms >= before.young_gc_time_ms);
        CHECK(after.full_gc_count >= before.full_gc_count);
        CHECK(after.full_gc_time_ms >= before.full_gc_time_ms);
    }

    CHECK(after.uptime_ms > before.uptime_ms);
    CHECK(after.process_cpu_time_ns > before.process_cpu_time_ns);
    CHECK(after.total_started_thread_count >= before.total_started_thread_count);

    // GraalVM 25.5 has no total of the allocated bytes; a later release fills it.
    if (before.allocated_bytes != -1) {
        CHECK(after.allocated_bytes > before.allocated_bytes);
    }
}

TEST_CASE("The runtime statistics count an attached thread and read from it") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    const auto before = readRuntimeStatistics(isolate.thread);
    std::atomic<bool> attached{false};
    std::atomic<bool> release{false};
    int32_t attachedResult = -1;
    dxfg_v4_runtime_stats_t fromAttached{};

    Attached second(isolate.isolate, [&](graal_isolatethread_t *thread) {
        attachedResult = dxfg_v4_system_get_runtime_statistics(thread, &fromAttached);
        attached = true;

        while (!release) {
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
        }

        return 0;
    });

    while (!attached) {
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }

    const auto during = readRuntimeStatistics(isolate.thread);
    release = true;
    REQUIRE(second.join() == 0);

    REQUIRE(attachedResult == DXFG_EXECUTE_SUCCESSFULLY);
    checkRuntimeStatistics(fromAttached);
    checkRuntimeStatistics(during);
    INFO("threads " << before.thread_count << " -> " << during.thread_count);
    CHECK(during.thread_count >= before.thread_count + 1);
    CHECK(during.peak_thread_count >= during.thread_count);
    CHECK(during.total_started_thread_count >= before.total_started_thread_count + 1);
}

TEST_CASE("The runtime statistics fail on a NULL struct and work after closing everything") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    CHECK(dxfg_v4_system_get_runtime_statistics(isolate.thread, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(isolate.thread) == "java.lang.IllegalArgumentException");

    REQUIRE(dxfg_v4_system_close_all_and_await_termination(isolate.thread) == DXFG_EXECUTE_SUCCESSFULLY);

    checkRuntimeStatistics(readRuntimeStatistics(isolate.thread));
}

} // namespace test
} // namespace dxfg