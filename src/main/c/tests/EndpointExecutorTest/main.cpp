// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// An endpoint built with dxfg_DXEndpoint_Builder_withExecutor() closes without an error. QDS 3.356 throws
// NullPointerException from the last step of closing such an endpoint (QD-1801); the SDK ignores it.

#include <catch.hpp>
#include <dxfg_api.h>

namespace dxfg {
namespace test {

namespace {

graal_isolatethread_t *isolateThread() {
    static graal_isolatethread_t *thread = [] {
        graal_isolate_t *isolate = nullptr;
        graal_isolatethread_t *result = nullptr;

        return graal_create_isolate(nullptr, &isolate, &result) == 0 ? result : nullptr;
    }();

    return thread;
}

// Builds an endpoint with the executor (NULL for the default one), returns NULL on a failure.
dxfg_endpoint_t *buildEndpoint(graal_isolatethread_t *thread, dxfg_executor_t *executor) {
    dxfg_endpoint_builder_t *builder = dxfg_DXEndpoint_newBuilder(thread);

    if (builder == nullptr) {
        return nullptr;
    }

    dxfg_endpoint_t *endpoint = dxfg_DXEndpoint_Builder_withExecutor(thread, builder, executor) == 0
                                    ? dxfg_DXEndpoint_Builder_build(thread, builder)
                                    : nullptr;

    dxfg_JavaObjectHandler_release(thread, &builder->handler);

    return endpoint;
}

} // namespace

TEST_CASE("An endpoint with an executor closes without an error", "[EndpointExecutor]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    // A pool runs the tasks itself: closeAndAwaitTermination() waits for them.
    dxfg_executor_t *executor = dxfg_Executors_newFixedThreadPool(thread, 1, "EndpointExecutorTest");

    REQUIRE(executor != nullptr);

    SECTION("dxfg_DXEndpoint_close") {
        dxfg_endpoint_t *endpoint = buildEndpoint(thread, executor);

        REQUIRE(endpoint != nullptr);
        CHECK(dxfg_DXEndpoint_close(thread, endpoint) == 0);
        CHECK(dxfg_DXEndpoint_getState(thread, endpoint) == DXFG_ENDPOINT_STATE_CLOSED);
        dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
    }

    SECTION("dxfg_DXEndpoint_closeAndAwaitTermination") {
        dxfg_endpoint_t *endpoint = buildEndpoint(thread, executor);

        REQUIRE(endpoint != nullptr);
        CHECK(dxfg_DXEndpoint_closeAndAwaitTermination(thread, endpoint) == 0);
        CHECK(dxfg_DXEndpoint_getState(thread, endpoint) == DXFG_ENDPOINT_STATE_CLOSED);
        dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
    }

    SECTION("dxfg_system_close_all_and_await_termination closes the other endpoints too") {
        dxfg_endpoint_t *withExecutor = buildEndpoint(thread, executor);
        dxfg_endpoint_t *withDefault = buildEndpoint(thread, nullptr);

        REQUIRE(withExecutor != nullptr);
        REQUIRE(withDefault != nullptr);
        CHECK(dxfg_system_close_all_and_await_termination(thread) == 0);
        CHECK(dxfg_DXEndpoint_getState(thread, withExecutor) == DXFG_ENDPOINT_STATE_CLOSED);
        CHECK(dxfg_DXEndpoint_getState(thread, withDefault) == DXFG_ENDPOINT_STATE_CLOSED);
        dxfg_JavaObjectHandler_release(thread, &withExecutor->handler);
        dxfg_JavaObjectHandler_release(thread, &withDefault->handler);
    }

    dxfg_JavaObjectHandler_release(thread, &executor->handler);
}

} // namespace test
} // namespace dxfg
