// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The teardown of an isolate while a thread polls an event queue with an infinite timeout: dxfg_v4_system_shutdown()
// ends the poll with DXFG_EVENT_QUEUE_CLOSED, the thread detaches and graal_tear_down_isolate() returns. A hang is
// the failure, so each case is a process of its own with a CTest timeout: an isolate is torn down once per process,
// and CTest runs the cases one by one by their tags.

#include <catch.hpp>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <chrono>
#include <thread>

namespace dxfg {
namespace test {

TEST_CASE("The isolate is torn down after a shutdown ends an infinite poll", "[EventQueue][Single]") {
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

        Attached poll(isolate.isolate, [queue](graal_isolatethread_t *pollThread) {
            dxfg_v4_event_type_list *events = nullptr;

            return dxfg_v4_EventQueue_poll(pollThread, queue, -1, 0, &events);
        });

        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        REQUIRE_FALSE(poll.returned());

        CHECK(dxfg_v4_system_shutdown(thread) == DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(poll.join() == DXFG_EVENT_QUEUE_CLOSED);

        dxfg_v4_JavaObjectHandler_release(thread, &queue->handler);
        dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
    } // Releases the publisher, the feed and the endpoint.

    CHECK(graal_tear_down_isolate(isolate.thread) == 0);
}

TEST_CASE("The isolate is torn down after a shutdown ends an infinite poll of a shared queue", "[EventQueue][Shared]") {
    Isolate isolate{};

    REQUIRE(isolate.thread != nullptr);

    // The calls outlive the hub, so a callback still in flight after a failed REQUIRE does not write into a destroyed one.
    ListenerCalls calls{};

    {
        Hub hub(isolate.thread);

        REQUIRE(hub.ready);

        auto *thread = isolate.thread;
        dxfg_v4_event_queue_t *queue = nullptr;
        dxfg_v4_executor_t *executor = nullptr;
        dxfg_v4_subscription_t *subscriptions[2] = {nullptr, nullptr};
        dxfg_v4_string_symbol_t symbols[2] = {{{STRING}, "A"}, {{STRING}, "B"}};

        REQUIRE(dxfg_v4_EventQueue_create(thread, &queue) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(dxfg_v4_EventQueue_getExecutor(thread, queue, &executor) == DXFG_EXECUTE_SUCCESSFULLY);

        for (int i = 0; i < 2; ++i) {
            REQUIRE(dxfg_v4_DXFeed_createSubscription(thread, hub.feed, DXFG_EVENT_QUOTE, &subscriptions[i]) ==
                    DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(dxfg_v4_EventQueue_attach(thread, queue, subscriptions[i], i + 1) == DXFG_EXECUTE_SUCCESSFULLY);
            REQUIRE(dxfg_v4_DXFeedSubscription_addSymbol(thread, subscriptions[i], &symbols[i].supper) ==
                    DXFG_EXECUTE_SUCCESSFULLY);
        }

        dxfg_v4_string_symbol_t modelSymbol{{STRING}, "T"};
        TimeAndSaleTxModel model(hub, executor, &modelSymbol.supper, calls);

        REQUIRE(model.ready);

        Attached poll(isolate.isolate, [queue](graal_isolatethread_t *pollThread) {
            dxfg_v4_tagged_event_list *events = nullptr;

            return dxfg_v4_EventQueue_pollTagged(pollThread, queue, -1, 0, &events);
        });

        std::this_thread::sleep_for(std::chrono::milliseconds(500));
        REQUIRE_FALSE(poll.returned());

        CHECK(dxfg_v4_system_shutdown(thread) == DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(poll.join() == DXFG_EVENT_QUEUE_CLOSED);

        for (auto *subscription : subscriptions) {
            dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
        }

        dxfg_v4_JavaObjectHandler_release(thread, &executor->handler);
        dxfg_v4_JavaObjectHandler_release(thread, &queue->handler);
    } // Closes and releases the model, releases the publisher, the feed and the endpoint.

    CHECK(graal_tear_down_isolate(isolate.thread) == 0);
}

} // namespace test
} // namespace dxfg
