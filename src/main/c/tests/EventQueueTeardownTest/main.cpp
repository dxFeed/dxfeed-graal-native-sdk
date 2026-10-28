// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The teardown of an isolate while a thread drains an event queue with an infinite timeout: dxfg_v4_system_shutdown()
// ends the drain with DXFG_EVENT_QUEUE_CLOSED, the thread detaches and graal_tear_down_isolate() returns. A hang is
// the failure, so the test is a process of its own with a CTest timeout.

#include <catch.hpp>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <chrono>
#include <thread>

namespace dxfg {
namespace test {

TEST_CASE("The isolate is torn down after a shutdown ends an infinite drain", "[EventQueue]") {
    Isolate isolate{};

    REQUIRE(isolate.thread != nullptr);

    {
        Hub hub(isolate.thread);

        REQUIRE(hub.ready);

        auto *thread = isolate.thread;
        dxfg_v4_subscription_t *subscription = nullptr;
        dxfg_v4_event_queue_t *queue = nullptr;
        dxfg_v4_string_symbol_t symbol{{STRING}, "A"};

        REQUIRE(dxfg_v4_DXFeed_createSubscription(thread, hub.feed, DXFG_EVENT_QUOTE, &subscription) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(dxfg_v4_EventQueue_new(thread, subscription, &queue) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) ==
                DXFG_EXECUTE_SUCCESSFULLY);

        Attached drain(isolate.isolate, [queue](graal_isolatethread_t *drainThread) {
            dxfg_v4_event_type_list *events = nullptr;

            return dxfg_v4_EventQueue_drain(drainThread, queue, -1, 0, &events);
        });

        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        REQUIRE_FALSE(drain.returned());

        CHECK(dxfg_v4_system_shutdown(thread) == DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(drain.join() == DXFG_EVENT_QUEUE_CLOSED);

        dxfg_v4_JavaObjectHandler_release(thread, &queue->handler);
        dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
    } // Releases the publisher, the feed and the endpoint.

    CHECK(graal_tear_down_isolate(isolate.thread) == 0);
}

} // namespace test
} // namespace dxfg
