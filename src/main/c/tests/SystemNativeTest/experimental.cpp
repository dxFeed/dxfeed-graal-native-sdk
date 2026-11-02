// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// An application may define DXFG_EXPERIMENTAL before the headers are included, e.g. as a deprecation attribute: the
// headers still compile and the experimental functions still work (the deprecation warnings are off for the tests;
// SystemNativeTest_ExperimentalMarker checks that the compiler warns).
#define DXFG_EXPERIMENTAL [[deprecated("experimental")]]

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

namespace dxfg {
namespace test {

TEST_CASE("DXFG_EXPERIMENTAL defined by the application as a deprecation attribute") {
    const Isolate isolate;
    REQUIRE(isolate.thread != nullptr);

    int64_t droppedEvents = -1;
    CHECK(dxfg_v4_DXFeedSubscription_getTotalDroppedEvents(isolate.thread, nullptr, &droppedEvents) ==
          DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(isolate.thread).empty());
    CHECK(droppedEvents == -1);
}

} // namespace test
} // namespace dxfg
