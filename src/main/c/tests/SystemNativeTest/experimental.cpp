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

    dxfg_v4_connection_error_t sentinel{};
    dxfg_v4_connection_error_t *error = &sentinel;
    CHECK(dxfg_v4_DXEndpoint_getLastConnectionError(isolate.thread, nullptr, &error) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(isolate.thread).empty());
    CHECK(error == &sentinel);
}

} // namespace test
} // namespace dxfg
