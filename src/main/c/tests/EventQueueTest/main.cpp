// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The event queue through the C API (dxfg_v4_event_queue.h): the results of a poll, the close of a queue by
// dxfg_v4_system_shutdown() and dxfg_v4_NativeListener_deactivateAll(), the executor guard and the lifetime of a
// returned list; for a shared queue also the tags, the models on its executor, the misuse of an attach and the checks
// of the executors. Shutdown cannot be undone for an isolate, so every test case creates its own isolate.

#include <catch.hpp>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <chrono>
#include <initializer_list>
#include <memory>
#include <set>
#include <string>
#include <thread>
#include <utility>
#include <vector>

namespace dxfg {
namespace test {

namespace {

constexpr int64_t POLL_TIMEOUT_MS = 10'000;
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

// Polls until the queue has no events within 200 ms; returns the symbols of the events.
std::vector<std::string> pollAll(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue) {
    std::vector<std::string> symbols{};
    dxfg_v4_event_type_list *events = nullptr;

    while (dxfg_v4_EventQueue_poll(thread, queue, 200, 0, &events) == DXFG_EXECUTE_SUCCESSFULLY) {
        const auto batch = symbolsOf(events);

        symbols.insert(symbols.end(), batch.begin(), batch.end());
        dxfg_v4_event_type_list_release(thread, events);
    }

    return symbols;
}

// Polls with an infinite timeout on an attached thread; the result of the poll or -2.
struct WaitingPoll {
    dxfg_v4_event_queue_t *queue;
    Attached attached;

    WaitingPoll(graal_isolate_t *isolate, dxfg_v4_event_queue_t *queue)
        : queue(queue), attached(isolate, [queue](graal_isolatethread_t *thread) {
              dxfg_v4_event_type_list *events = nullptr;
              const int32_t result = dxfg_v4_EventQueue_poll(thread, queue, -1, 0, &events);

              if (result == DXFG_EXECUTE_SUCCESSFULLY) {
                  dxfg_v4_event_type_list_release(thread, events);
              }

              // The closed outcome is not an error: no exception may be pending.
              return result == DXFG_EVENT_QUEUE_CLOSED && !takeExceptionClassName(thread).empty() ? -3 : result;
          }) {
    }

    // The poll does not return by itself while there are no events.
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

// A Quote subscription of the feed, or a detached one; closed and released at the end.
struct Subscription {
    graal_isolatethread_t *thread;
    dxfg_v4_subscription_t *subscription = nullptr;

    explicit Subscription(Hub &hub, bool attachedToFeed = true) : thread(hub.thread) {
        if (attachedToFeed) {
            dxfg_v4_DXFeed_createSubscription(thread, hub.feed, DXFG_EVENT_QUOTE, &subscription);
        } else {
            dxfg_v4_DXFeedSubscription_new(thread, DXFG_EVENT_QUOTE, &subscription);
        }
    }

    Subscription(const Subscription &) = delete;
    Subscription &operator=(const Subscription &) = delete;

    ~Subscription() {
        if (subscription != nullptr) {
            dxfg_v4_DXFeedSubscription_close(thread, subscription);
            dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
        }
    }

    bool addSymbols(std::initializer_list<const char *> symbols) {
        bool added = subscription != nullptr;

        for (const char *symbol : symbols) {
            dxfg_v4_string_symbol_t stringSymbol{{STRING}, symbol};

            added = added && dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &stringSymbol.supper) == 0;
        }

        return added;
    }
};

// A shared event queue and the Quote subscriptions attached to it.
struct Shared {
    graal_isolatethread_t *thread;
    dxfg_v4_event_queue_t *queue = nullptr;
    std::vector<std::unique_ptr<Subscription>> subscriptions{};
    bool ready = false;

    explicit Shared(Hub &hub) : thread(hub.thread) {
        ready = dxfg_v4_EventQueue_create(thread, &queue) == 0;
    }

    Shared(const Shared &) = delete;
    Shared &operator=(const Shared &) = delete;

    ~Shared() {
        subscriptions.clear();

        if (queue != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &queue->handler);
        }
    }

    // Attaches a new subscription with the tag, then adds the symbols; NULL on failure.
    dxfg_v4_subscription_t *attach(Hub &hub, int64_t tag, std::initializer_list<const char *> symbols) {
        auto &added = subscriptions.emplace_back(std::make_unique<Subscription>(hub));

        return added->subscription != nullptr &&
                       dxfg_v4_EventQueue_attach(thread, queue, added->subscription, tag) == 0 &&
                       added->addSymbols(symbols)
                   ? added->subscription
                   : nullptr;
    }
};

using TaggedSymbols = std::vector<std::pair<std::string, int64_t>>;

TaggedSymbols taggedSymbolsOf(const dxfg_v4_tagged_event_list *events) {
    TaggedSymbols symbols{};

    REQUIRE(events->tags != nullptr);

    for (int32_t i = 0; i < events->size; ++i) {
        symbols.emplace_back(reinterpret_cast<dxfg_v4_quote_t *>(events->elements[i])->market_event.event_symbol,
                             events->tags[i]);
    }

    return symbols;
}

// Polls with tags until the number of events is reached, the queue is closed or the time is out.
TaggedSymbols pollTagged(graal_isolatethread_t *thread, dxfg_v4_event_queue_t *queue, std::size_t count) {
    TaggedSymbols symbols{};
    dxfg_v4_tagged_event_list *events = nullptr;

    for (auto end = std::chrono::steady_clock::now() + RETURN_TIMEOUT;
         symbols.size() < count && std::chrono::steady_clock::now() < end;) {
        const int32_t result = dxfg_v4_EventQueue_pollTagged(thread, queue, 200, 0, &events);

        if (result == DXFG_EXECUTE_SUCCESSFULLY) {
            const auto batch = taggedSymbolsOf(events);

            symbols.insert(symbols.end(), batch.begin(), batch.end());
            dxfg_v4_tagged_event_list_release(thread, events);
        } else if (result != DXFG_EVENT_QUEUE_NO_EVENTS) {
            break;
        }
    }

    return symbols;
}

std::vector<std::string> symbolsWithTag(const TaggedSymbols &symbols, int64_t tag) {
    std::vector<std::string> result{};

    for (const auto &[symbol, symbolTag] : symbols) {
        if (symbolTag == tag) {
            result.push_back(symbol);
        }
    }

    return result;
}

// A snapshot of two TimeAndSale events of the symbol down to index 0 (time 0), the last one removed.
bool publishTimeAndSales(Hub &hub, const char *symbol) {
    constexpr int32_t REMOVE_EVENT = 0x02;
    constexpr int32_t SNAPSHOT_BEGIN = 0x04;
    constexpr int32_t SNAPSHOT_END = 0x08;

    std::vector<dxfg_v4_time_and_sale_t> events(2);
    std::vector<dxfg_v4_event_type_t *> pointers{};

    for (std::size_t i = 0; i < events.size(); ++i) {
        events[i].market_event.event_type.clazz = DXFG_EVENT_TIME_AND_SALE;
        events[i].market_event.event_symbol = symbol;
        events[i].index = static_cast<int64_t>(events.size() - 1 - i);
        events[i].price = 10.0;
        events[i].size = 1.0;
        pointers.push_back(&events[i].market_event.event_type);
    }

    events.front().event_flags = SNAPSHOT_BEGIN;
    events.back().event_flags = REMOVE_EVENT | SNAPSHOT_END;

    return hub.publish(pointers.data(), static_cast<int32_t>(pointers.size()));
}

void onListChanged(graal_isolatethread_t * /* thread */, dxfg_v4_event_type_list * /* events */, void *userData) {
    static_cast<ListenerCalls *>(userData)->add();
}

// A TimeSeriesEventModel of TimeAndSale from time 0 on the executor whose list listener records its calls; closed and
// released at the end.
struct TimeAndSaleEventModel {
    graal_isolatethread_t *thread;
    dxfg_v4_time_series_event_model_t *model = nullptr;
    dxfg_v4_observable_list_model_t *list = nullptr;
    dxfg_v4_observable_list_model_listener_t *listener = nullptr;
    bool ready = false;

    TimeAndSaleEventModel(Hub &hub, dxfg_v4_executor_t *executor, dxfg_v4_symbol_t *symbol, ListenerCalls &calls)
        : thread(hub.thread) {
        ready = dxfg_v4_TimeSeriesEventModel_new(thread, DXFG_EVENT_TIME_AND_SALE, &model) == 0 &&
                dxfg_v4_TimeSeriesEventModel_setExecutor(thread, model, executor) == 0 &&
                dxfg_v4_TimeSeriesEventModel_setFromTime(thread, model, 0) == 0 &&
                dxfg_v4_TimeSeriesEventModel_setSymbol(thread, model, symbol) == 0 &&
                dxfg_v4_TimeSeriesEventModel_getEventsList(thread, model, &list) == 0 &&
                dxfg_v4_ObservableListModelListener_new(thread, &onListChanged, &calls, &listener) == 0 &&
                dxfg_v4_ObservableListModel_addListener(thread, list, listener) == 0 &&
                dxfg_v4_TimeSeriesEventModel_attach(thread, model, hub.feed) == 0;
    }

    TimeAndSaleEventModel(const TimeAndSaleEventModel &) = delete;
    TimeAndSaleEventModel &operator=(const TimeAndSaleEventModel &) = delete;

    ~TimeAndSaleEventModel() {
        if (model != nullptr) {
            dxfg_v4_TimeSeriesEventModel_close(thread, model);
        }

        if (listener != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &listener->handler);
        }

        if (list != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &list->handler);
        }

        if (model != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &model->handler);
        }
    }
};

} // namespace

TEST_CASE("dxfg_v4_EventQueue_poll reports each outcome", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A", "B", "C"});

    REQUIRE(queued.ready);

    auto *thread = isolate.thread;
    dxfg_v4_event_type_list *events = nullptr;

    // No events.
    events = reinterpret_cast<dxfg_v4_event_type_list *>(1);
    CHECK(dxfg_v4_EventQueue_poll(thread, queued.queue, 0, 0, &events) == DXFG_EVENT_QUEUE_NO_EVENTS);
    CHECK(events == nullptr);
    CHECK(takeExceptionClassName(thread).empty());

    // Events, in the order of publication.
    REQUIRE(publishQuotes(hub, {{"A", 1}, {"B", 2}, {"C", 3}}));
    REQUIRE(dxfg_v4_EventQueue_poll(thread, queued.queue, POLL_TIMEOUT_MS, 0, &events) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(events != nullptr);
    CHECK(symbolsOf(events) == std::vector<std::string>{"A", "B", "C"});
    CHECK(dxfg_v4_event_type_list_release(thread, events) == DXFG_EXECUTE_SUCCESSFULLY);

    int64_t dropped = -1;

    CHECK(dxfg_v4_EventQueue_getDroppedEvents(thread, queued.queue, &dropped) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dropped == 0);

    // An error: a negative limit.
    CHECK(dxfg_v4_EventQueue_poll(thread, queued.queue, 0, -1, &events) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // A wake-up before the poll.
    CHECK(dxfg_v4_EventQueue_wakeUp(thread, queued.queue) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_EventQueue_poll(thread, queued.queue, POLL_TIMEOUT_MS, 0, &events) == DXFG_EVENT_QUEUE_NO_EVENTS);

    // Closed, also for every later poll; the events of a closed queue are counted as dropped.
    CHECK(dxfg_v4_EventQueue_close(thread, queued.queue) == DXFG_EXECUTE_SUCCESSFULLY);
    events = reinterpret_cast<dxfg_v4_event_type_list *>(1);
    CHECK(dxfg_v4_EventQueue_poll(thread, queued.queue, POLL_TIMEOUT_MS, 0, &events) == DXFG_EVENT_QUEUE_CLOSED);
    CHECK(events == nullptr);
    CHECK(takeExceptionClassName(thread).empty());
    REQUIRE(publishQuotes(hub, {{"A", 4}}));
    CHECK(dxfg_v4_EventQueue_getDroppedEvents(thread, queued.queue, &dropped) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dropped == 1);
}

TEST_CASE("dxfg_v4_system_shutdown ends a waiting poll with closed", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A"});

    REQUIRE(queued.ready);

    WaitingPoll poll(isolate.isolate, queued.queue);

    REQUIRE(poll.stillWaiting());

    Attached shuttingDown(isolate.isolate, &dxfg_v4_system_shutdown);

    CHECK(shuttingDown.join() == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(poll.attached.join() == DXFG_EVENT_QUEUE_CLOSED);
}

TEST_CASE("dxfg_v4_NativeListener_deactivateAll ends a waiting poll with closed", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A"});

    REQUIRE(queued.ready);

    WaitingPoll poll(isolate.isolate, queued.queue);

    REQUIRE(poll.stillWaiting());
    CHECK(dxfg_v4_NativeListener_deactivateAll(isolate.thread) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(poll.attached.join() == DXFG_EVENT_QUEUE_CLOSED);

    // A queue created afterwards is closed from the start.
    Queued later(hub, {});

    REQUIRE(later.ready);

    dxfg_v4_event_type_list *events = nullptr;

    CHECK(dxfg_v4_EventQueue_poll(isolate.thread, later.queue, POLL_TIMEOUT_MS, 0, &events) ==
          DXFG_EVENT_QUEUE_CLOSED);
}

TEST_CASE("dxfg_v4_system_shutdown returns with pending events and no poll", "[EventQueue]") {
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

    // A second queue, and a queue on a subscription of the feed that has symbols (QD refuses its listener).
    dxfg_v4_event_queue_t *second = nullptr;

    CHECK(dxfg_v4_EventQueue_new(thread, queued.subscription, &second) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");

    Subscription withSymbols(hub);

    REQUIRE(withSymbols.addSymbols({"A"}));
    CHECK(dxfg_v4_EventQueue_new(thread, withSymbols.subscription, &second) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");

    dxfg_v4_JavaObjectHandler_release(thread, &controller->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &executor->handler);
}

TEST_CASE("An endpoint close does not end a poll, a queue close does", "[EventQueue]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Queued queued(hub, {"A"});

    REQUIRE(queued.ready);

    auto *thread = isolate.thread;
    dxfg_v4_event_type_list *events = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_close(thread, hub.endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_EventQueue_poll(thread, queued.queue, 200, 0, &events) == DXFG_EVENT_QUEUE_NO_EVENTS);
    CHECK(dxfg_v4_EventQueue_close(thread, queued.queue) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_EventQueue_poll(thread, queued.queue, 200, 0, &events) == DXFG_EVENT_QUEUE_CLOSED);
}

TEST_CASE("A list returned by a poll outlives the queue, the subscription and the endpoint", "[EventQueue]") {
    Isolate isolate{};
    auto *thread = isolate.thread;
    dxfg_v4_event_type_list *events = nullptr;

    {
        Hub hub(thread);

        REQUIRE(hub.ready);

        Queued queued(hub, {"A"});

        REQUIRE(queued.ready);
        REQUIRE(publishQuotes(hub, {{"A", 1}}));
        REQUIRE(dxfg_v4_EventQueue_poll(thread, queued.queue, POLL_TIMEOUT_MS, 0, &events) ==
                DXFG_EXECUTE_SUCCESSFULLY);
    } // Releases the queue, closes and releases the subscription and the endpoint.

    REQUIRE(events != nullptr);
    CHECK(symbolsOf(events) == std::vector<std::string>{"A"});
    CHECK(reinterpret_cast<dxfg_v4_quote_t *>(events->elements[0])->bid_price == 1);
    CHECK(dxfg_v4_event_type_list_release(thread, events) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The events of a removed symbol are not returned by later polls", "[EventQueue]") {
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

    const auto symbols = pollAll(thread, queued.queue);
    const std::set<std::string> distinct(symbols.begin(), symbols.end());

    CHECK(distinct == std::set<std::string>{"A"});
}

TEST_CASE("dxfg_v4_EventQueue_pollTagged tags the events of each attached subscription", "[EventQueue][Shared]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    Shared shared(hub);

    REQUIRE(shared.ready);
    REQUIRE(shared.attach(hub, 1, {"A", "B", "C"}) != nullptr);
    REQUIRE(shared.attach(hub, 2, {"A", "B", "C"}) != nullptr);
    REQUIRE(publishQuotes(hub, {{"A", 1}, {"B", 2}, {"C", 3}}));

    // Each event of a symbol reaches both subscriptions; each subscription keeps the order of publication.
    const auto symbols = pollTagged(isolate.thread, shared.queue, 6);

    CHECK(symbols.size() == 6);
    CHECK(symbolsWithTag(symbols, 1) == std::vector<std::string>{"A", "B", "C"});
    CHECK(symbolsWithTag(symbols, 2) == std::vector<std::string>{"A", "B", "C"});
}

TEST_CASE("A plain poll of a shared queue returns its events, a queue of a subscription tags them with 0",
          "[EventQueue][Shared]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    auto *thread = isolate.thread;
    Shared shared(hub);

    REQUIRE(shared.ready);
    REQUIRE(shared.attach(hub, 1, {"A"}) != nullptr);
    REQUIRE(shared.attach(hub, 2, {"B"}) != nullptr);
    REQUIRE(publishQuotes(hub, {{"A", 1}, {"B", 2}}));

    std::multiset<std::string> symbols{};
    dxfg_v4_event_type_list *events = nullptr;

    while (symbols.size() < 2 &&
           dxfg_v4_EventQueue_poll(thread, shared.queue, POLL_TIMEOUT_MS, 0, &events) == DXFG_EXECUTE_SUCCESSFULLY) {
        const auto batch = symbolsOf(events);

        symbols.insert(batch.begin(), batch.end());
        dxfg_v4_event_type_list_release(thread, events);
    }

    CHECK(symbols == std::multiset<std::string>{"A", "B"});

    Queued queued(hub, {"C"});

    REQUIRE(queued.ready);
    REQUIRE(publishQuotes(hub, {{"C", 3}}));
    CHECK(pollTagged(thread, queued.queue, 1) == TaggedSymbols{{"C", 0}});
}

TEST_CASE("A detached subscription with symbols can be given a queue, then attached to the feed",
          "[EventQueue][Shared]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    auto *thread = isolate.thread;
    Subscription forBound(hub, false);
    Subscription forShared(hub, false);

    REQUIRE(forBound.addSymbols({"A"}));
    REQUIRE(forShared.addSymbols({"B"}));

    dxfg_v4_event_queue_t *bound = nullptr;

    REQUIRE(dxfg_v4_EventQueue_new(thread, forBound.subscription, &bound) == DXFG_EXECUTE_SUCCESSFULLY);

    Shared shared(hub);

    REQUIRE(shared.ready);
    REQUIRE(dxfg_v4_EventQueue_attach(thread, shared.queue, forShared.subscription, 7) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeed_attachSubscription(thread, hub.feed, forBound.subscription) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeed_attachSubscription(thread, hub.feed, forShared.subscription) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(publishQuotes(hub, {{"A", 1}, {"B", 2}}));
    CHECK(pollTagged(thread, bound, 1) == TaggedSymbols{{"A", 0}});
    CHECK(pollTagged(thread, shared.queue, 1) == TaggedSymbols{{"B", 7}});

    dxfg_v4_JavaObjectHandler_release(thread, &bound->handler);
}

TEST_CASE("Models on the executor of a shared queue call their listeners in a poll, on the polling thread",
          "[EventQueue][Shared]") {
    Isolate isolate{};
    // The calls outlive the hub, so a callback still in flight after a failed REQUIRE does not write into a destroyed one.
    ListenerCalls txCalls{};
    ListenerCalls listCalls{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    auto *thread = isolate.thread;
    Shared shared(hub);
    dxfg_v4_executor_t *executor = nullptr;
    dxfg_v4_string_symbol_t symbol{{STRING}, "T"};

    REQUIRE(shared.ready);
    REQUIRE(dxfg_v4_EventQueue_getExecutor(thread, shared.queue, &executor) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(executor != nullptr);

    {
        TimeAndSaleTxModel txModel(hub, executor, &symbol.supper, txCalls);
        TimeAndSaleEventModel eventModel(hub, executor, &symbol.supper, listCalls);

        REQUIRE(txModel.ready);
        REQUIRE(eventModel.ready);
        REQUIRE(publishTimeAndSales(hub, "T"));

        // Nothing is called until a poll.
        std::this_thread::sleep_for(NO_RETURN_TIMEOUT);
        CHECK(txCalls.get().empty());
        CHECK(listCalls.get().empty());

        dxfg_v4_event_type_list *events = nullptr;

        for (auto end = std::chrono::steady_clock::now() + RETURN_TIMEOUT;
             (txCalls.get().empty() || listCalls.get().empty()) && std::chrono::steady_clock::now() < end;) {
            if (dxfg_v4_EventQueue_poll(thread, shared.queue, 200, 0, &events) == DXFG_EXECUTE_SUCCESSFULLY) {
                dxfg_v4_event_type_list_release(thread, events);
            }
        }

        const auto txThreads = txCalls.get();
        const auto listThreads = listCalls.get();

        REQUIRE_FALSE(txThreads.empty());
        REQUIRE_FALSE(listThreads.empty());

        for (const auto &threads : {txThreads, listThreads}) {
            for (const auto &id : threads) {
                CHECK(id == std::this_thread::get_id());
            }
        }
    }

    dxfg_v4_JavaObjectHandler_release(thread, &executor->handler);
}

TEST_CASE("A misused dxfg_v4_EventQueue_attach fails with IllegalStateException", "[EventQueue][Shared]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    auto *thread = isolate.thread;
    Shared shared(hub);
    Shared other(hub);

    REQUIRE(shared.ready);
    REQUIRE(other.ready);

    auto *attached = shared.attach(hub, 1, {});

    REQUIRE(attached != nullptr);

    // Attached already, to this queue or to another one.
    CHECK(dxfg_v4_EventQueue_attach(thread, shared.queue, attached, 2) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    CHECK(dxfg_v4_EventQueue_attach(thread, other.queue, attached, 2) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");

    // With symbols on a subscription of the feed (QD refuses its listener), or with an executor.
    Subscription withSymbols(hub);

    REQUIRE(withSymbols.addSymbols({"A"}));
    CHECK(dxfg_v4_EventQueue_attach(thread, shared.queue, withSymbols.subscription, 2) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");

    Subscription withExecutor(hub);
    dxfg_v4_executor_t *executor = nullptr;

    REQUIRE(withExecutor.subscription != nullptr);
    REQUIRE(dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_new(thread, &executor) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedSubscription_setExecutor(thread, withExecutor.subscription, executor) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_EventQueue_attach(thread, shared.queue, withExecutor.subscription, 2) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    dxfg_v4_JavaObjectHandler_release(thread, &executor->handler);

    // A queue of a subscription takes no other one and has no executor of its own.
    Queued queued(hub, {});
    Subscription plain(hub);

    REQUIRE(queued.ready);
    REQUIRE(plain.subscription != nullptr);
    CHECK(dxfg_v4_EventQueue_attach(thread, queued.queue, plain.subscription, 2) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");

    executor = nullptr;
    CHECK(dxfg_v4_EventQueue_getExecutor(thread, queued.queue, &executor) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    CHECK(executor == nullptr);
}

TEST_CASE("The executors of a shared queue are checked by kind", "[EventQueue][Shared]") {
    Isolate isolate{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    auto *thread = isolate.thread;
    Shared shared(hub);

    REQUIRE(shared.ready);

    auto *attached = shared.attach(hub, 1, {});
    dxfg_v4_executor_t *attachmentExecutor = nullptr;
    dxfg_v4_executor_t *queueExecutor = nullptr;

    REQUIRE(attached != nullptr);
    REQUIRE(dxfg_v4_DXFeedSubscription_getExecutor(thread, attached, &attachmentExecutor) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_EventQueue_getExecutor(thread, shared.queue, &queueExecutor) == DXFG_EXECUTE_SUCCESSFULLY);

    // A model takes the executor of the queue, not the one of an attached subscription.
    dxfg_v4_time_series_event_model_t *model = nullptr;

    REQUIRE(dxfg_v4_TimeSeriesEventModel_new(thread, DXFG_EVENT_TIME_AND_SALE, &model) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_TimeSeriesEventModel_setExecutor(thread, model, attachmentExecutor) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    CHECK(dxfg_v4_TimeSeriesEventModel_setExecutor(thread, model, queueExecutor) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_TimeSeriesEventModel_close(thread, model) == DXFG_EXECUTE_SUCCESSFULLY);
    dxfg_v4_JavaObjectHandler_release(thread, &model->handler);

    // So does a subscription that is not attached.
    Subscription plain(hub);

    REQUIRE(plain.subscription != nullptr);
    CHECK(dxfg_v4_DXFeedSubscription_setExecutor(thread, plain.subscription, attachmentExecutor) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalStateException");
    CHECK(dxfg_v4_DXFeedSubscription_setExecutor(thread, plain.subscription, queueExecutor) ==
          DXFG_EXECUTE_SUCCESSFULLY);

    // The endpoint builder takes the executor of the queue.
    dxfg_v4_endpoint_builder_t *builder = nullptr;
    dxfg_v4_endpoint_t *endpoint = nullptr;

    REQUIRE(dxfg_v4_DXEndpoint_newBuilder(thread, &builder) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_Builder_withRole(thread, builder, DXFG_ENDPOINT_ROLE_LOCAL_HUB) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_Builder_withExecutor(thread, builder, queueExecutor) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXEndpoint_Builder_build(thread, builder, &endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXEndpoint_close(thread, endpoint) == DXFG_EXECUTE_SUCCESSFULLY);
    dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &builder->handler);

    // Only the executors of dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_new() process their tasks on request.
    CHECK(dxfg_v4_ExecutorBaseOnConcurrentLinkedQueue_processAllPendingTasks(thread, queueExecutor) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    dxfg_v4_JavaObjectHandler_release(thread, &queueExecutor->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &attachmentExecutor->handler);
}

TEST_CASE("A subscription close keeps a shared poll open, the close of the queue ends it", "[EventQueue][Shared]") {
    Isolate isolate{};
    ListenerCalls calls{};
    Hub hub(isolate.thread);

    REQUIRE(hub.ready);

    auto *thread = isolate.thread;
    Shared shared(hub);
    dxfg_v4_executor_t *executor = nullptr;
    dxfg_v4_string_symbol_t symbol{{STRING}, "T"};

    REQUIRE(shared.ready);
    REQUIRE(dxfg_v4_EventQueue_getExecutor(thread, shared.queue, &executor) == DXFG_EXECUTE_SUCCESSFULLY);

    auto *first = shared.attach(hub, 1, {"A"});

    REQUIRE(first != nullptr);
    REQUIRE(shared.attach(hub, 2, {"B"}) != nullptr);

    TimeAndSaleTxModel model(hub, executor, &symbol.supper, calls);

    REQUIRE(model.ready);

    // The events of the closed subscription no longer arrive, those of the other one do.
    REQUIRE(dxfg_v4_DXFeedSubscription_close(thread, first) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(publishQuotes(hub, {{"A", 1}, {"B", 2}}));
    CHECK(pollTagged(thread, shared.queue, 1) == TaggedSymbols{{"B", 2}});

    WaitingPoll poll(isolate.isolate, shared.queue);

    REQUIRE(poll.stillWaiting());

    SECTION("by dxfg_v4_EventQueue_close") {
        CHECK(dxfg_v4_EventQueue_close(thread, shared.queue) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    SECTION("by dxfg_v4_system_shutdown") {
        CHECK(dxfg_v4_system_shutdown(thread) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    SECTION("by dxfg_v4_NativeListener_deactivateAll") {
        CHECK(dxfg_v4_NativeListener_deactivateAll(thread) == DXFG_EXECUTE_SUCCESSFULLY);
    }

    CHECK(poll.attached.join() == DXFG_EVENT_QUEUE_CLOSED);
    dxfg_v4_JavaObjectHandler_release(thread, &executor->handler);
}

TEST_CASE("A tagged list outlives the queue, the subscriptions and the endpoint", "[EventQueue][Shared]") {
    Isolate isolate{};
    auto *thread = isolate.thread;
    dxfg_v4_tagged_event_list *events = nullptr;

    {
        Hub hub(thread);

        REQUIRE(hub.ready);

        Shared shared(hub);

        REQUIRE(shared.ready);
        REQUIRE(shared.attach(hub, 7, {"A"}) != nullptr);
        REQUIRE(publishQuotes(hub, {{"A", 1}}));
        REQUIRE(dxfg_v4_EventQueue_pollTagged(thread, shared.queue, POLL_TIMEOUT_MS, 0, &events) ==
                DXFG_EXECUTE_SUCCESSFULLY);
    } // Releases the queue, closes and releases the subscription, releases the endpoint.

    REQUIRE(events != nullptr);
    CHECK(taggedSymbolsOf(events) == TaggedSymbols{{"A", 7}});
    CHECK(reinterpret_cast<dxfg_v4_quote_t *>(events->elements[0])->bid_price == 1);

    // Another attached thread releases it.
    Attached releasing(isolate.isolate,
                       [events](graal_isolatethread_t *other) { return dxfg_v4_tagged_event_list_release(other, events); });

    CHECK(releasing.join() == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_tagged_event_list_release(thread, nullptr) == DXFG_EXECUTE_SUCCESSFULLY);
}

} // namespace test
} // namespace dxfg
