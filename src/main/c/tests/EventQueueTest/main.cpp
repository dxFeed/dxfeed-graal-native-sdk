// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The event queue through the C API (dxfg_v4_event_queue.h): the results of a drain, the close of a queue by
// dxfg_v4_system_shutdown() and dxfg_v4_NativeListener_deactivateAll(), the executor guard and the lifetime of a
// returned list. Shutdown cannot be undone for an isolate, so every test case creates its own isolate.

#include <catch.hpp>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <chrono>
#include <initializer_list>
#include <set>
#include <string>
#include <thread>
#include <utility>
#include <vector>

namespace dxfg {
namespace test {

namespace {

constexpr int64_t DRAIN_TIMEOUT_MS = 10'000;
constexpr auto RETURN_TIMEOUT = std::chrono::seconds(10);
constexpr auto NO_RETURN_TIMEOUT = std::chrono::milliseconds(500);

// A Quote subscription with an event queue; the symbols are added after the queue.
struct Queued {
    graal_isolatethread_t *thread;
    dxfg_v4_subscription_t *subscription = nullptr;
    dxfg_v4_event_queue_t *queue = nullptr;
    bool ready = false;

    Queued(Hub &hub, std::initializer_list<const char *> symbols) : thread(hub.thread) {
        ready = dxfg_v4_DXFeed_createSubscription(thread, hub.feed, DXFG_EVENT_QUOTE, &subscription) == 0 &&
                dxfg_v4_EventQueue_new(thread, subscription, &queue) == 0;

        for (const char *symbol : symbols) {
            dxfg_v4_string_symbol_t stringSymbol{{STRING}, symbol};

            ready = ready && dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &stringSymbol.supper) == 0;
        }
    }

    Queued(const Queued &) = delete;
    Queued &operator=(const Queued &) = delete;

    ~Queued() {
        if (queue != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &queue->handler);
        }

        if (subscription != nullptr) {
            dxfg_v4_DXFeedSubscription_close(thread, subscription);
            dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
        }
    }
};

bool publishQuotes(Hub &hub, const std::vector<std::pair<const char *, double>> &quotes) {
    std::vector<dxfg_v4_quote_t> events(quotes.size());
    std::vector<dxfg_v4_event_type_t *> pointers{};

    for (std::size_t i = 0; i < quotes.size(); ++i) {
        events[i].market_event.event_type.clazz = DXFG_EVENT_QUOTE;
        events[i].market_event.event_symbol = quotes[i].first;
        events[i].bid_price = quotes[i].second;
        pointers.push_back(&events[i].market_event.event_type);
    }

    return hub.publish(pointers.data(), static_cast<int32_t>(pointers.size()));
}

std::vector<std::string> symbolsOf(const dxfg_v4_event_type_list *events) {
    std::vector<std::string> symbols{};

    for (int32_t i = 0; i < events->size; ++i) {
        symbols.emplace_back(reinterpret_cast<dxfg_v4_quote_t *>(events->elements[i])->market_event.event_symbol);
    }

    return symbols;
}

// Drains until the queue has no events within 200 ms; returns the symbols of the events.
std::vector<std::string> drainAll(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue) {
    std::vector<std::string> symbols{};
    dxfg_v4_event_type_list *events = nullptr;

    while (dxfg_v4_EventQueue_drain(thread, queue, 200, 0, &events) == DXFG_EXECUTE_SUCCESSFULLY) {
        const auto batch = symbolsOf(events);

        symbols.insert(symbols.end(), batch.begin(), batch.end());
        dxfg_v4_event_type_list_release(thread, events);
    }

    return symbols;
}

// Drains with an infinite timeout on an attached thread; the result of the drain or -2.
struct WaitingDrain {
    dxfg_v4_event_queue_t *queue;
    Attached attached;

    WaitingDrain(graal_isolate_t *isolate, dxfg_v4_event_queue_t *queue)
        : queue(queue), attached(isolate, [queue](graal_isolatethread_t *thread) {
              dxfg_v4_event_type_list *events = nullptr;
              const int32_t result = dxfg_v4_EventQueue_drain(thread, queue, -1, 0, &events);

              if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                  dxfg_v4_event_type_list_release(thread, events);
              }

              // The closed outcome is not an error: no exception may be pending.
              return result == DXFG_EVENT_QUEUE_CLOSED && !takeExceptionClassName(thread).empty() ? -3 : result;
          }) {
    }

    // The drain does not return by itself while there are no events.
    bool stillWaiting() {
        std::this_thread::sleep_for(NO_RETURN_TIMEOUT);

        return !attached.returned();
    }
};

bool returnsWithin(Attached &attached) {
    for (auto end = std::chrono::steady_clock::now() + RETURN_TIMEOUT; std::chrono::steady_clock::now() < end;) {
        if (attached.returned()) {
            return true;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    return attached.returned();
}

} // namespace

TEST_CASE("dxfg_v4_EventQueue_drain reports each outcome", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A", "B", "C"});

    REQUIRE(queued.ready);

    auto *thread = isolate.thread;
    dxfg_v4_event_type_list *events = nullptr;

    // No events.
    events = reinterpret_cast<dxfg_v4_event_type_list *>(1);
    CHECK(dxfg_v4_EventQueue_drain(thread, queued.queue, 0, 0, &events) == DXFG_EVENT_QUEUE_NO_EVENTS);
    CHECK(events == nullptr);
    CHECK(takeExceptionClassName(thread).empty());

    // Events, in the order of publication.
    REQUIRE(publishQuotes(hub, {{"A", 1}, {"B", 2}, {"C", 3}}));
    REQUIRE(dxfg_v4_EventQueue_drain(thread, queued.queue, DRAIN_TIMEOUT_MS, 0, &events) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(events != nullptr);
    CHECK(symbolsOf(events) == std::vector<std::string>{"A", "B", "C"});
    CHECK(dxfg_v4_event_type_list_release(thread, events) == DXFG_EXECUTE_SUCCESSFULLY);

    int64_t dropped = -1;

    CHECK(dxfg_v4_EventQueue_getDroppedEvents(thread, queued.queue, &dropped) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dropped == 0);

    // An error: a negative limit.
    CHECK(dxfg_v4_EventQueue_drain(thread, queued.queue, 0, -1, &events) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // A wake-up before the drain.
    CHECK(dxfg_v4_EventQueue_wakeUp(thread, queued.queue) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_EventQueue_drain(thread, queued.queue, DRAIN_TIMEOUT_MS, 0, &events) == DXFG_EVENT_QUEUE_NO_EVENTS);

    // Closed, also for every later drain; the events of a closed queue are counted as dropped.
    CHECK(dxfg_v4_EventQueue_close(thread, queued.queue) == DXFG_EXECUTE_SUCCESSFULLY);
    events = reinterpret_cast<dxfg_v4_event_type_list *>(1);
    CHECK(dxfg_v4_EventQueue_drain(thread, queued.queue, DRAIN_TIMEOUT_MS, 0, &events) == DXFG_EVENT_QUEUE_CLOSED);
    CHECK(events == nullptr);
    CHECK(takeExceptionClassName(thread).empty());
    REQUIRE(publishQuotes(hub, {{"A", 4}}));
    CHECK(dxfg_v4_EventQueue_getDroppedEvents(thread, queued.queue, &dropped) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dropped == 1);
}

TEST_CASE("dxfg_v4_system_shutdown ends a waiting drain with closed", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A"});

    REQUIRE(queued.ready);

    WaitingDrain drain(isolate.isolate, queued.queue);

    REQUIRE(drain.stillWaiting());

    Attached shuttingDown(isolate.isolate, &dxfg_v4_system_shutdown);

    CHECK(shuttingDown.join() == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(drain.attached.join() == DXFG_EVENT_QUEUE_CLOSED);
}

TEST_CASE("dxfg_v4_NativeListener_deactivateAll ends a waiting drain with closed", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A"});

    REQUIRE(queued.ready);

    WaitingDrain drain(isolate.isolate, queued.queue);

    REQUIRE(drain.stillWaiting());
    CHECK(dxfg_v4_NativeListener_deactivateAll(isolate.thread) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(drain.attached.join() == DXFG_EVENT_QUEUE_CLOSED);

    // A queue created afterwards is closed from the start.
    Queued later(hub, {});

    REQUIRE(later.ready);

    dxfg_v4_event_type_list *events = nullptr;

    CHECK(dxfg_v4_EventQueue_drain(isolate.thread, later.queue, DRAIN_TIMEOUT_MS, 0, &events) ==
          DXFG_EVENT_QUEUE_CLOSED);
}

TEST_CASE("dxfg_v4_system_shutdown returns with undrained events and no drain", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A", "B"});

    REQUIRE(queued.ready);
    REQUIRE(publishQuotes(hub, {{"A", 1}, {"B", 2}}));

    Attached shuttingDown(isolate.isolate, &dxfg_v4_system_shutdown);

    REQUIRE(returnsWithin(shuttingDown));
    CHECK(shuttingDown.join() == DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_endpoint_state_t state{};

    REQUIRE(dxfg_v4_DXEndpoint_getState(isolate.thread, hub.endpoint, &state) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(state == DXFG_ENDPOINT_STATE_CLOSED);
}

TEST_CASE("The executor of a subscription with a queue cannot be changed", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {});

    REQUIRE(queued.ready);

    auto *thread = isolate.thread;
    dxfg_v4_executor_t *executor = nullptr;
    dxfg_v4_subscription_controller_t *controller = nullptr;

    REQUIRE(dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_new(thread, &executor) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedSubscription_getSubscriptionController(thread, queued.subscription, &controller) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    CHECK(dxfg_v4_DXFeedSubscription_setExecutor(thread, queued.subscription, executor) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    CHECK(dxfg_DXFeedSubscription_setExecutor(thread, queued.subscription, executor) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    CHECK(dxfg_v4_SubscriptionController_setExecutor(thread, controller, executor) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");

    // A second queue, and a queue on a subscription with symbols.
    dxfg_v4_event_queue_t *second = nullptr;

    CHECK(dxfg_v4_EventQueue_new(thread, queued.subscription, &second) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");

    Queued withSymbols(hub, {"A"});

    REQUIRE(withSymbols.ready);
    CHECK(dxfg_v4_EventQueue_new(thread, withSymbols.subscription, &second) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");

    dxfg_v4_JavaObjectHandler_release(thread, &controller->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &executor->handler);
}

TEST_CASE("An endpoint close does not end a drain, a queue close does", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A"});

    REQUIRE(queued.ready);

    auto *thread = isolate.thread;
    dxfg_v4_event_type_list *events = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_close(thread, hub.endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_EventQueue_drain(thread, queued.queue, 200, 0, &events) == DXFG_EVENT_QUEUE_NO_EVENTS);
    CHECK(dxfg_v4_EventQueue_close(thread, queued.queue) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_EventQueue_drain(thread, queued.queue, 200, 0, &events) == DXFG_EVENT_QUEUE_CLOSED);
}

TEST_CASE("A drained list outlives the queue, the subscription and the endpoint", "[EventQueue]") {
    Isolate isolate{};
    auto *thread = isolate.thread;
    dxfg_v4_event_type_list *events = nullptr;

    {
        Hub hub(thread);

        REQUIRE(hub.ready);

        Queued queued(hub, {"A"});

        REQUIRE(queued.ready);
        REQUIRE(publishQuotes(hub, {{"A", 1}}));
        REQUIRE(dxfg_v4_EventQueue_drain(thread, queued.queue, DRAIN_TIMEOUT_MS, 0, &events) ==
                DXFG_EXECUTE_SUCCESSFULLY);
    } // Releases the queue, closes and releases the subscription and the endpoint.

    REQUIRE(events != nullptr);
    CHECK(symbolsOf(events) == std::vector<std::string>{"A"});
    CHECK(reinterpret_cast<dxfg_v4_quote_t *>(events->elements[0])->bid_price == 1);
    CHECK(dxfg_v4_event_type_list_release(thread, events) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The events of a removed symbol are not drained afterwards", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A", "B"});

    REQUIRE(queued.ready);

    auto *thread = isolate.thread;
    dxfg_v4_string_symbol_t removed{{STRING}, "B"};

    REQUIRE(publishQuotes(hub, {{"A", 1}, {"B", 2}}));
    REQUIRE(dxfg_v4_DXFeedSubscription_removeSymbol(thread, queued.subscription, &removed.supper) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(publishQuotes(hub, {{"A", 3}, {"B", 4}}));

    const auto symbols = drainAll(thread, queued.queue);
    const std::set<std::string> distinct(symbols.begin(), symbols.end());

    CHECK(distinct == std::set<std::string>{"A"});
}

} // namespace test
} // namespace dxfg
