// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The small exports of the Java API (MDAPI-489): the configurationChanged callback of a subscription change listener
// (dxfg_v4_ObservableSubscriptionChangeListener_newWithConfigurationChanged()), the time of a time series subscription
// (dxfg_v4_DXFeedTimeSeriesSubscription_getFromTime()), an AuthOrderSource from a list of entries
// (dxfg_v4_AuthOrderSource_create()) and the property change listener of the on-demand service
// (dxfg_v4_OnDemandPropertyChangeListener_new(), dxfg_v4_OnDemandService_addPropertyChangeListener(),
// dxfg_v4_OnDemandService_removePropertyChangeListener()).

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdint>
#include <map>
#include <mutex>
#include <set>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace test {

namespace {

constexpr auto EVENT_TIMEOUT = std::chrono::seconds(10);
constexpr auto NO_EVENT_TIMEOUT = std::chrono::milliseconds(500);

const Isolate &isolate() {
    static const Isolate instance{};

    return instance;
}

// The calls of the callbacks of a subscription change listener.
struct ChangeCalls {
    std::atomic<int32_t> symbolsAdded{0};
    std::atomic<int32_t> symbolsRemoved{0};
    std::atomic<int32_t> subscriptionClosed{0};
    std::atomic<int32_t> configurationChanged{0};
    // The aggregation period that the configurationChanged callback read from the subscription.
    std::atomic<int64_t> periodInCallback{-1};
    dxfg_v4_subscription_t *subscription = nullptr;

    static void onSymbolsAdded(graal_isolatethread_t * /* thread */, dxfg_symbol_list * /* symbols */,
                               void *userData) {
        ++static_cast<ChangeCalls *>(userData)->symbolsAdded;
    }

    static void onSymbolsRemoved(graal_isolatethread_t * /* thread */, dxfg_symbol_list * /* symbols */,
                                 void *userData) {
        ++static_cast<ChangeCalls *>(userData)->symbolsRemoved;
    }

    static void onSubscriptionClosed(graal_isolatethread_t * /* thread */, void *userData) {
        ++static_cast<ChangeCalls *>(userData)->subscriptionClosed;
    }

    static void onConfigurationChanged(graal_isolatethread_t *thread, void *userData) {
        auto *calls = static_cast<ChangeCalls *>(userData);
        int64_t period = -1;

        if (calls->subscription != nullptr &&
            dxfg_v4_DXFeedSubscription_getAggregationPeriodMillis(thread, calls->subscription, &period) ==
                DXFG_EXECUTE_SUCCESSFULLY) {
            calls->periodInCallback = period;
        }

        ++calls->configurationChanged;
    }
};

dxfg_v4_observable_subscription_change_listener_t *newChangeListener(graal_isolatethread_t *thread,
                                                                     ChangeCalls &calls) {
    dxfg_v4_observable_subscription_change_listener_t *listener = nullptr;

    REQUIRE(dxfg_v4_ObservableSubscriptionChangeListener_newWithConfigurationChanged(
                thread, &ChangeCalls::onSymbolsAdded, &ChangeCalls::onSymbolsRemoved,
                &ChangeCalls::onSubscriptionClosed, &ChangeCalls::onConfigurationChanged, &calls,
                &listener) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(listener != nullptr);

    return listener;
}

dxfg_v4_subscription_t *newSubscription(graal_isolatethread_t *thread) {
    dxfg_v4_subscription_t *subscription = nullptr;

    REQUIRE(dxfg_v4_DXFeedSubscription_new(thread, DXFG_EVENT_QUOTE, &subscription) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(subscription != nullptr);

    return subscription;
}

void releaseSubscription(graal_isolatethread_t *thread, dxfg_v4_subscription_t *subscription) {
    CHECK(dxfg_v4_DXFeedSubscription_close(thread, subscription) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

int32_t setPeriod(graal_isolatethread_t *thread, dxfg_v4_subscription_t *subscription, int64_t period) {
    return dxfg_v4_DXFeedSubscription_setAggregationPeriodMillis(thread, subscription, period);
}

// An entry of the list given to dxfg_v4_AuthOrderSource_create(): the strings stay owned by the test.
struct Entry {
    int32_t id;
    std::vector<const char *> symbols;
};

// The native list of the entries; the structures live as long as the object.
class EntryList {
    std::vector<dxfg_v4_symbols_by_order_source_id_map_entry_t> entries_;
    std::vector<dxfg_v4_string_list> symbolLists_;
    std::vector<std::vector<const char *>> symbols_;
    std::vector<dxfg_v4_symbols_by_order_source_id_map_entry_t *> pointers_;

  public:
    dxfg_v4_symbols_by_order_source_id_map_entry_list list{};

    explicit EntryList(const std::vector<Entry> &entries)
        : entries_(entries.size()), symbolLists_(entries.size()), symbols_(entries.size()),
          pointers_(entries.size()) {
        for (std::size_t i = 0; i < entries.size(); ++i) {
            symbols_[i] = entries[i].symbols;
            symbolLists_[i].size = static_cast<int32_t>(symbols_[i].size());
            symbolLists_[i].elements = symbols_[i].empty() ? nullptr : symbols_[i].data();
            entries_[i].order_source_id = entries[i].id;
            entries_[i].symbols = &symbolLists_[i];
            pointers_[i] = &entries_[i];
        }

        list.size = static_cast<int32_t>(pointers_.size());
        list.elements = pointers_.empty() ? nullptr : pointers_.data();
    }

    EntryList(const EntryList &) = delete;
    EntryList &operator=(const EntryList &) = delete;

    dxfg_v4_symbols_by_order_source_id_map_entry_t &entry(std::size_t index) {
        return entries_[index];
    }

    dxfg_v4_string_list &symbols(std::size_t index) {
        return symbolLists_[index];
    }

    void setEntry(std::size_t index, dxfg_v4_symbols_by_order_source_id_map_entry_t *entry) {
        pointers_[index] = entry;
    }
};

using SymbolsById = std::map<int32_t, std::set<std::string>>;

SymbolsById byIds(graal_isolatethread_t *thread, dxfg_v4_auth_order_source_t *source) {
    dxfg_v4_symbols_by_order_source_id_map_entry_list *entries = nullptr;

    REQUIRE(dxfg_v4_AuthOrderSource_getByIds(thread, source, &entries) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(entries != nullptr);

    SymbolsById result{};

    for (int32_t i = 0; i < entries->size; ++i) {
        const dxfg_v4_symbols_by_order_source_id_map_entry_t *entry = entries->elements[i];
        std::set<std::string> symbols{};

        for (int32_t j = 0; entry->symbols != nullptr && j < entry->symbols->size; ++j) {
            symbols.insert(entry->symbols->elements[j]);
        }

        result.emplace(entry->order_source_id, std::move(symbols));
    }

    CHECK(dxfg_v4_symbols_by_order_source_id_map_entry_list_release(thread, entries) == DXFG_EXECUTE_SUCCESSFULLY);

    return result;
}

// Creates an AuthOrderSource that must fail with IllegalArgumentException, leaving the out variable untouched.
void requireCreateFails(graal_isolatethread_t *thread,
                        const dxfg_v4_symbols_by_order_source_id_map_entry_list *entries) {
    auto *untouched = reinterpret_cast<dxfg_v4_auth_order_source_t *>(static_cast<std::uintptr_t>(0x1));
    dxfg_v4_auth_order_source_t *source = untouched;

    CHECK(dxfg_v4_AuthOrderSource_create(thread, entries, &source) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(source == untouched);
}

// A property change that the on-demand listener received.
struct PropertyChange {
    std::string name;
    dxfg_v4_on_demand_property_value_t oldValue;
    dxfg_v4_on_demand_property_value_t newValue;
};

// The property changes of an on-demand service, as the listener receives them on the executor thread.
class PropertyChanges {
    std::mutex mutex_{};
    std::condition_variable changed_{};
    std::vector<PropertyChange> changes_{};

  public:
    static void onPropertyChange(graal_isolatethread_t * /* thread */, const char *propertyName,
                                 const dxfg_v4_on_demand_property_value_t *oldValue,
                                 const dxfg_v4_on_demand_property_value_t *newValue, void *userData) {
        auto *changes = static_cast<PropertyChanges *>(userData);

        {
            std::lock_guard<std::mutex> lock(changes->mutex_);

            changes->changes_.push_back(
                PropertyChange{propertyName != nullptr ? propertyName : "<null>", *oldValue, *newValue});
        }

        changes->changed_.notify_all();
    }

    // Waits for a change of the property whose new value satisfies the predicate; returns it or nothing on timeout.
    template <typename Predicate>
    bool waitFor(const std::string &name, Predicate predicate, PropertyChange &found) {
        std::unique_lock<std::mutex> lock(mutex_);

        return changed_.wait_for(lock, EVENT_TIMEOUT, [&] {
            for (const auto &change : changes_) {
                if (change.name == name && predicate(change)) {
                    found = change;

                    return true;
                }
            }

            return false;
        });
    }

    std::size_t count() {
        std::lock_guard<std::mutex> lock(mutex_);

        return changes_.size();
    }
};

dxfg_v4_on_demand_property_change_listener_t *newPropertyListener(graal_isolatethread_t *thread,
                                                                  PropertyChanges &changes) {
    dxfg_v4_on_demand_property_change_listener_t *listener = nullptr;

    REQUIRE(dxfg_v4_OnDemandPropertyChangeListener_new(thread, &PropertyChanges::onPropertyChange, &changes,
                                                       &listener) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(listener != nullptr);

    return listener;
}

// An endpoint of the on-demand role and its service; closed and released at the end.
struct OnDemand {
    graal_isolatethread_t *thread;
    dxfg_v4_endpoint_t *endpoint = nullptr;
    dxfg_v4_on_demand_service_t *service = nullptr;

    explicit OnDemand(graal_isolatethread_t *thread) : thread(thread) {
        REQUIRE(dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_ON_DEMAND_FEED, &endpoint) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(dxfg_v4_OnDemandService_getInstanceByEndpoint(thread, endpoint, &service) ==
                DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(service != nullptr);
    }

    OnDemand(const OnDemand &) = delete;
    OnDemand &operator=(const OnDemand &) = delete;

    ~OnDemand() {
        if (service != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &service->handler);
        }

        if (endpoint != nullptr) {
            dxfg_v4_DXEndpoint_close(thread, endpoint);
            dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);
        }
    }

    // An on-demand connector makes the replay supported; the address is not reachable, the replay does not need it.
    void connect() {
        REQUIRE(dxfg_v4_DXEndpoint_connect(thread, endpoint, "ondemand:127.0.0.1:1") == DXFG_EXECUTE_SUCCESSFULLY);
    }
};

// 2020-01-01 00:00:00 UTC.
constexpr int64_t REPLAY_TIME = 1'577'836'800'000;

} // namespace

TEST_CASE("configurationChanged is called once per changed aggregation period, not for the same one") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    ChangeCalls calls{};
    dxfg_v4_subscription_t *subscription = newSubscription(thread);
    calls.subscription = subscription;
    dxfg_v4_observable_subscription_change_listener_t *listener = newChangeListener(thread, calls);

    REQUIRE(dxfg_v4_DXFeedSubscription_addChangeListener(thread, subscription, listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(setPeriod(thread, subscription, 100) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.configurationChanged == 1);
    CHECK(calls.periodInCallback == 100);

    REQUIRE(setPeriod(thread, subscription, 100) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.configurationChanged == 1);

    REQUIRE(setPeriod(thread, subscription, 250) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.configurationChanged == 2);
    CHECK(calls.periodInCallback == 250);

    // The other callbacks keep working.
    dxfg_v4_string_symbol_t symbol{{STRING}, "AAPL"};

    REQUIRE(dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.symbolsAdded == 1);
    REQUIRE(dxfg_v4_DXFeedSubscription_removeSymbol(thread, subscription, &symbol.supper) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.symbolsRemoved == 1);
    REQUIRE(dxfg_v4_DXFeedSubscription_removeChangeListener(thread, subscription, listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.subscriptionClosed == 1);

    // Removed: no more calls.
    REQUIRE(setPeriod(thread, subscription, 500) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.configurationChanged == 2);

    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    releaseSubscription(thread, subscription);
}

TEST_CASE("Each NULL callback of the listener is skipped, the others are called") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    for (int mask = 0; mask < 16; ++mask) {
        CAPTURE(mask);

        const bool withAdded = (mask & 1) != 0;
        const bool withRemoved = (mask & 2) != 0;
        const bool withClosed = (mask & 4) != 0;
        const bool withConfiguration = (mask & 8) != 0;

        ChangeCalls calls{};
        dxfg_v4_subscription_t *subscription = newSubscription(thread);
        dxfg_v4_observable_subscription_change_listener_t *listener = nullptr;

        REQUIRE(dxfg_v4_ObservableSubscriptionChangeListener_newWithConfigurationChanged(
                    thread, withAdded ? &ChangeCalls::onSymbolsAdded : nullptr,
                    withRemoved ? &ChangeCalls::onSymbolsRemoved : nullptr,
                    withClosed ? &ChangeCalls::onSubscriptionClosed : nullptr,
                    withConfiguration ? &ChangeCalls::onConfigurationChanged : nullptr, &calls,
                    &listener) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(listener != nullptr);
        REQUIRE(dxfg_v4_DXFeedSubscription_addChangeListener(thread, subscription, listener) ==
                DXFG_EXECUTE_SUCCESSFULLY);

        dxfg_v4_string_symbol_t symbol{{STRING}, "IBM"};

        CHECK(dxfg_v4_DXFeedSubscription_addSymbol(thread, subscription, &symbol.supper) ==
              DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(dxfg_v4_DXFeedSubscription_removeSymbol(thread, subscription, &symbol.supper) ==
              DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(setPeriod(thread, subscription, 100) == DXFG_EXECUTE_SUCCESSFULLY);
        CHECK(dxfg_v4_DXFeedSubscription_removeChangeListener(thread, subscription, listener) ==
              DXFG_EXECUTE_SUCCESSFULLY);

        CHECK(calls.symbolsAdded == (withAdded ? 1 : 0));
        CHECK(calls.symbolsRemoved == (withRemoved ? 1 : 0));
        CHECK(calls.subscriptionClosed == (withClosed ? 1 : 0));
        CHECK(calls.configurationChanged == (withConfiguration ? 1 : 0));

        CHECK(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
        releaseSubscription(thread, subscription);
    }
}

TEST_CASE("A released listener is not called for a changed configuration") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    ChangeCalls calls{};
    dxfg_v4_subscription_t *subscription = newSubscription(thread);
    dxfg_v4_observable_subscription_change_listener_t *listener = newChangeListener(thread, calls);

    REQUIRE(dxfg_v4_DXFeedSubscription_addChangeListener(thread, subscription, listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(setPeriod(thread, subscription, 100) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(calls.configurationChanged == 1);

    // The listener stays added to the subscription, but its last handle is released.
    REQUIRE(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(setPeriod(thread, subscription, 200) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.configurationChanged == 1);

    releaseSubscription(thread, subscription);
    CHECK(calls.subscriptionClosed == 0);
}

TEST_CASE("No listener of this ticket is called after dxfg_v4_NativeListener_deactivateAll") {
    // A separate isolate: the deactivation is for the whole isolate.
    Isolate own{};
    graal_isolatethread_t *thread = own.thread;
    REQUIRE(thread != nullptr);

    ChangeCalls calls{};
    dxfg_v4_subscription_t *subscription = newSubscription(thread);
    dxfg_v4_observable_subscription_change_listener_t *listener = newChangeListener(thread, calls);

    REQUIRE(dxfg_v4_DXFeedSubscription_addChangeListener(thread, subscription, listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    PropertyChanges changes{};
    OnDemand onDemand(thread);
    dxfg_v4_on_demand_property_change_listener_t *propertyListener = newPropertyListener(thread, changes);

    REQUIRE(dxfg_v4_OnDemandService_addPropertyChangeListener(thread, onDemand.service, propertyListener) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dxfg_v4_NativeListener_deactivateAll(thread) == DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(setPeriod(thread, subscription, 100) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.configurationChanged == 0);

    // The connector makes the replay supported, a change that the service reports on its executor.
    onDemand.connect();
    REQUIRE(dxfg_v4_OnDemandService_replayWithSpeed(thread, onDemand.service, REPLAY_TIME, 1.0) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    std::this_thread::sleep_for(NO_EVENT_TIMEOUT);
    CHECK(changes.count() == 0);

    CHECK(dxfg_v4_OnDemandService_removePropertyChangeListener(thread, onDemand.service, propertyListener) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &propertyListener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
    releaseSubscription(thread, subscription);
}

TEST_CASE("The listener with configurationChanged needs the out pointer") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    CHECK(dxfg_v4_ObservableSubscriptionChangeListener_newWithConfigurationChanged(thread, nullptr, nullptr, nullptr,
                                                                                    nullptr, nullptr, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
}

TEST_CASE("getFromTime returns INT64_MAX for a new time series subscription and then the time set") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    Hub hub(thread);
    REQUIRE(hub.ready);

    dxfg_v4_time_series_subscription_t *subscription = nullptr;

    REQUIRE(dxfg_v4_DXFeed_createTimeSeriesSubscription(thread, hub.feed, DXFG_EVENT_TIME_AND_SALE, &subscription) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(subscription != nullptr);

    int64_t fromTime = 0;

    CHECK(dxfg_v4_DXFeedTimeSeriesSubscription_getFromTime(thread, subscription, &fromTime) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(fromTime == INT64_MAX);

    REQUIRE(dxfg_v4_DXFeedTimeSeriesSubscription_setFromTime(thread, subscription, 1'234'567) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXFeedTimeSeriesSubscription_getFromTime(thread, subscription, &fromTime) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(fromTime == 1'234'567);

    REQUIRE(dxfg_v4_DXFeedTimeSeriesSubscription_setFromTime(thread, subscription, 0) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXFeedTimeSeriesSubscription_getFromTime(thread, subscription, &fromTime) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(fromTime == 0);

    // A NULL out pointer fails and the time stays.
    CHECK(dxfg_v4_DXFeedTimeSeriesSubscription_getFromTime(thread, subscription, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    CHECK(dxfg_v4_DXFeedSubscription_close(thread, &subscription->sub) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &subscription->sub.handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("getFromTime fails for a plain subscription and a NULL handle") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    dxfg_v4_subscription_t *plain = newSubscription(thread);
    int64_t fromTime = 42;

    // The handle of a plain subscription cast to the type: a clear error instead of a ClassCastException.
    CHECK(dxfg_v4_DXFeedTimeSeriesSubscription_getFromTime(
              thread, reinterpret_cast<dxfg_v4_time_series_subscription_t *>(plain), &fromTime) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(fromTime == 42);

    CHECK(dxfg_v4_DXFeedTimeSeriesSubscription_getFromTime(thread, nullptr, &fromTime) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(fromTime == 42);

    releaseSubscription(thread, plain);
}

TEST_CASE("An AuthOrderSource created from entries returns the same entries") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    EntryList entries({{1, {"AAPL", "IBM", "AAPL"}}, {7, {}}, {-3, {"MSFT"}}});
    dxfg_v4_auth_order_source_t *source = nullptr;

    REQUIRE(dxfg_v4_AuthOrderSource_create(thread, &entries.list, &source) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(source != nullptr);

    // The SDK copied the entries: the list may change after the call.
    entries.entry(0).order_source_id = 99;

    const SymbolsById expected{{1, {"AAPL", "IBM"}}, {7, {}}, {-3, {"MSFT"}}};

    CHECK(byIds(thread, source) == expected);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &source->handler) == DXFG_EXECUTE_SUCCESSFULLY);

    // An empty list gives an AuthOrderSource without order sources.
    EntryList empty({});

    REQUIRE(dxfg_v4_AuthOrderSource_create(thread, &empty.list, &source) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(source != nullptr);
    CHECK(byIds(thread, source).empty());
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &source->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("AuthOrderSource_create rejects a NULL list, entry, symbol list or symbol and a duplicate identifier") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    SUBCASE("a NULL list") {
        requireCreateFails(thread, nullptr);
    }

    SUBCASE("a NULL entry") {
        EntryList entries({{1, {"AAPL"}}, {2, {"IBM"}}});

        entries.setEntry(1, nullptr);
        requireCreateFails(thread, &entries.list);
    }

    SUBCASE("a NULL symbol list") {
        EntryList entries({{1, {"AAPL"}}, {2, {"IBM"}}});

        entries.entry(1).symbols = nullptr;
        requireCreateFails(thread, &entries.list);
    }

    SUBCASE("a NULL symbol") {
        EntryList entries({{1, {"AAPL", nullptr}}});

        requireCreateFails(thread, &entries.list);
    }

    SUBCASE("a duplicate identifier") {
        EntryList entries({{1, {"AAPL"}}, {2, {"IBM"}}, {1, {"MSFT"}}});

        requireCreateFails(thread, &entries.list);
    }

    SUBCASE("a negative size") {
        EntryList entries({{1, {"AAPL"}}});

        entries.list.size = -1;
        requireCreateFails(thread, &entries.list);
    }

    SUBCASE("a NULL out pointer") {
        EntryList entries({{1, {"AAPL"}}});

        CHECK(dxfg_v4_AuthOrderSource_create(thread, &entries.list, nullptr) == DXFG_EXECUTE_FAIL);
        CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    }
}

TEST_CASE("The on-demand property listener sees the replay and the speed with their old and new values") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    OnDemand onDemand(thread);
    PropertyChanges changes{};
    dxfg_v4_on_demand_property_change_listener_t *listener = newPropertyListener(thread, changes);

    REQUIRE(dxfg_v4_OnDemandService_addPropertyChangeListener(thread, onDemand.service, listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    PropertyChange change{};

    onDemand.connect();
    REQUIRE(changes.waitFor("replaySupported",
                            [](const PropertyChange &candidate) { return candidate.newValue.boolean_value == 1; },
                            change));
    CHECK(change.oldValue.kind == DXFG_V4_ON_DEMAND_PROPERTY_VALUE_BOOLEAN);
    CHECK(change.oldValue.boolean_value == 0);
    CHECK(change.newValue.kind == DXFG_V4_ON_DEMAND_PROPERTY_VALUE_BOOLEAN);

    REQUIRE(dxfg_v4_OnDemandService_replayWithSpeed(thread, onDemand.service, REPLAY_TIME, 2.0) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(changes.waitFor("replay",
                            [](const PropertyChange &candidate) { return candidate.newValue.boolean_value == 1; },
                            change));
    CHECK(change.oldValue.kind == DXFG_V4_ON_DEMAND_PROPERTY_VALUE_BOOLEAN);
    CHECK(change.oldValue.boolean_value == 0);
    REQUIRE(changes.waitFor("speed",
                            [](const PropertyChange &candidate) { return candidate.newValue.double_value == 2.0; },
                            change));
    CHECK(change.oldValue.kind == DXFG_V4_ON_DEMAND_PROPERTY_VALUE_DOUBLE);
    CHECK(change.oldValue.double_value == 0.0);
    CHECK(change.newValue.kind == DXFG_V4_ON_DEMAND_PROPERTY_VALUE_DOUBLE);
    REQUIRE(changes.waitFor("time",
                            [](const PropertyChange &candidate) {
                                return candidate.newValue.time_value >= REPLAY_TIME;
                            },
                            change));
    CHECK(change.oldValue.kind == DXFG_V4_ON_DEMAND_PROPERTY_VALUE_TIME);
    CHECK(change.newValue.kind == DXFG_V4_ON_DEMAND_PROPERTY_VALUE_TIME);

    REQUIRE(dxfg_v4_OnDemandService_setSpeed(thread, onDemand.service, 3.0) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(changes.waitFor("speed",
                            [](const PropertyChange &candidate) { return candidate.newValue.double_value == 3.0; },
                            change));
    CHECK(change.oldValue.double_value == 2.0);

    // Removed: a later change does not reach the listener. A change reported at the time of the removal may still
    // arrive, so the count is taken after a pause.
    REQUIRE(dxfg_v4_OnDemandService_removePropertyChangeListener(thread, onDemand.service, listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    std::this_thread::sleep_for(NO_EVENT_TIMEOUT);

    const std::size_t countAfterRemoval = changes.count();

    REQUIRE(dxfg_v4_OnDemandService_setSpeed(thread, onDemand.service, 4.0) == DXFG_EXECUTE_SUCCESSFULLY);
    std::this_thread::sleep_for(NO_EVENT_TIMEOUT);
    CHECK(changes.count() == countAfterRemoval);

    // Removing it again does nothing.
    CHECK(dxfg_v4_OnDemandService_removePropertyChangeListener(thread, onDemand.service, listener) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_OnDemandService_stopAndResume(thread, onDemand.service) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("A released on-demand property listener is not called while it stays added") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    OnDemand onDemand(thread);
    PropertyChanges changes{};
    dxfg_v4_on_demand_property_change_listener_t *listener = newPropertyListener(thread, changes);

    REQUIRE(dxfg_v4_OnDemandService_addPropertyChangeListener(thread, onDemand.service, listener) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);

    onDemand.connect();
    REQUIRE(dxfg_v4_OnDemandService_replayWithSpeed(thread, onDemand.service, REPLAY_TIME, 1.0) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    std::this_thread::sleep_for(NO_EVENT_TIMEOUT);
    CHECK(changes.count() == 0);
    CHECK(dxfg_v4_OnDemandService_stopAndResume(thread, onDemand.service) == DXFG_EXECUTE_SUCCESSFULLY);
}

TEST_CASE("The on-demand property listener functions check their arguments") {
    graal_isolatethread_t *thread = isolate().thread;
    REQUIRE(thread != nullptr);

    PropertyChanges changes{};
    dxfg_v4_on_demand_property_change_listener_t *listener = nullptr;

    CHECK(dxfg_v4_OnDemandPropertyChangeListener_new(thread, nullptr, &changes, &listener) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(listener == nullptr);
    CHECK(dxfg_v4_OnDemandPropertyChangeListener_new(thread, &PropertyChanges::onPropertyChange, &changes, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    OnDemand onDemand(thread);

    CHECK(dxfg_v4_OnDemandService_addPropertyChangeListener(thread, onDemand.service, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(dxfg_v4_OnDemandService_removePropertyChangeListener(thread, onDemand.service, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    listener = newPropertyListener(thread, changes);

    CHECK(dxfg_v4_OnDemandService_addPropertyChangeListener(thread, nullptr, listener) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
    CHECK(dxfg_v4_JavaObjectHandler_release(thread, &listener->handler) == DXFG_EXECUTE_SUCCESSFULLY);
}

} // namespace test
} // namespace dxfg
