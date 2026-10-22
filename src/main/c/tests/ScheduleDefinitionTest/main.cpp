// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// dxfg_Schedule_getDefinition returns the string that defines the schedule (QDS 3.326).

#include <catch.hpp>
#include <dxfg_api.h>

#include <string>

namespace dxfg {
namespace test {

namespace {

constexpr const char *DEFINITION = "(tz=America/New_York;0=p04000930r09301600a16002000)";

graal_isolatethread_t *isolateThread() {
    static graal_isolatethread_t *thread = [] {
        graal_isolate_t *isolate = nullptr;
        graal_isolatethread_t *result = nullptr;

        return graal_create_isolate(nullptr, &isolate, &result) == 0 ? result : nullptr;
    }();

    return thread;
}

} // namespace

TEST_CASE("The definition of a schedule", "[Schedule]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_schedule_t *schedule = dxfg_Schedule_getInstance2(thread, DEFINITION);

    REQUIRE(schedule != nullptr);

    SECTION("Is the string given to dxfg_Schedule_getInstance2") {
        char *definition = nullptr;

        REQUIRE(dxfg_Schedule_getDefinition(thread, schedule, &definition) == 0);
        REQUIRE(definition != nullptr);
        CHECK(std::string(definition) == DEFINITION);
        dxfg_String_release(thread, definition);
    }

    SECTION("A NULL out pointer is an error") {
        CHECK(dxfg_Schedule_getDefinition(thread, schedule, nullptr) == -1);

        dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

        CHECK(exception != nullptr);
        dxfg_Exception_release(thread, exception);
    }

    dxfg_JavaObjectHandler_release(thread, &schedule->handler);
}

} // namespace test
} // namespace dxfg
