// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The release functions free all the strings of the structures they release: the tests repeat a create/release cycle
// and check that the heap in use does not grow.
//
// They need the heap statistics of glibc (mallinfo2), so they run on Linux only. There the SDK allocates with the libc
// malloc, the same allocator as the test's strdup.

#include <catch.hpp>
#include <dxfg_api.h>

#if defined(__GLIBC__)
#include <cstring>
#include <malloc.h>
#endif

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

#if defined(__GLIBC__)
constexpr int WARM_UP_ITERATIONS = 1'000;
constexpr int ITERATIONS = 20'000;

// Less than one leaked string per iteration.
constexpr double MAX_GROWTH_PER_ITERATION = 8.0;

// Runs the action (it returns false on a failure) after warming up and returns the growth of the heap in use, in bytes
// per iteration. No assertions in the loop: they may allocate.
template <typename Action> double heapGrowthPerIteration(Action action) {
    bool succeeded = true;

    for (int i = 0; i < WARM_UP_ITERATIONS; ++i) {
        succeeded = action() && succeeded;
    }

    const auto before = mallinfo2().uordblks;

    for (int i = 0; i < ITERATIONS; ++i) {
        succeeded = action() && succeeded;
    }

    const auto after = mallinfo2().uordblks;

    REQUIRE(succeeded);

    return (static_cast<double>(after) - static_cast<double>(before)) / ITERATIONS;
}
#endif

} // namespace

TEST_CASE("dxfg_EventType_release frees the strings of a NuamOrder", "[MemoryRelease]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

#if defined(__GLIBC__)
    const auto growth = heapGrowthPerIteration([thread] {
        dxfg_event_type_t *event = dxfg_EventType_new(thread, "SYMBOL", DXFG_EVENT_NUAM_ORDER);

        if (event == nullptr) {
            return false;
        }

        auto *order = reinterpret_cast<dxfg_nuam_order_t *>(event);

        // The strings of an event created by the SDK are freed by the SDK.
        order->order_base.market_maker = strdup("MARKET-MAKER");
        order->client_order_id = strdup("CLIENT-ORDER-ID-0123456789");
        order->customer_account = strdup("CUSTOMER-ACCOUNT-0123456789");
        order->customer_info = strdup("CUSTOMER-INFO-0123456789");
        order->exchange_info = strdup("EXCHANGE-INFO-0123456789");

        return dxfg_EventType_release(thread, event) == 0;
    });

    INFO("Heap growth per released NuamOrder: " << growth << " bytes");
    CHECK(growth < MAX_GROWTH_PER_ITERATION);
#else
    WARN("Skipped: needs the heap statistics of glibc");
#endif
}

TEST_CASE("dxfg_Exception_release frees the strings of the stack trace", "[MemoryRelease]") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

#if defined(__GLIBC__)
    const auto growth = heapGrowthPerIteration([thread] {
        // A null key throws NullPointerException.
        if (dxfg_system_set_property(thread, nullptr, nullptr) == 0) {
            return false;
        }

        dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

        if (exception == nullptr) {
            return false;
        }

        dxfg_Exception_release(thread, exception);

        return true;
    });

    INFO("Heap growth per released exception: " << growth << " bytes");
    CHECK(growth < MAX_GROWTH_PER_ITERATION);
#else
    WARN("Skipped: needs the heap statistics of glibc");
#endif
}

} // namespace test
} // namespace dxfg
