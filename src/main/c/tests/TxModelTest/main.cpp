// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// IndexedTxModel, TimeSeriesTxModel and TxModelListener through the C API: the models get the events published with
// dxfg_DXPublisher_publishEvents to a LOCAL_HUB endpoint.

#include <doctest.h>
#include <dxfg_api.h>

#include <algorithm>
#include <chrono>
#include <condition_variable>
#include <cstdint>
#include <mutex>
#include <string>
#include <vector>

namespace dxfg {
namespace test {

namespace {

constexpr int32_t REMOVE_EVENT = 0x02;
constexpr int32_t SNAPSHOT_BEGIN = 0x04;
constexpr int32_t SNAPSHOT_END = 0x08;

// Side.BUY in the flags of OrderBase (bits 2-3): only empty orders may have an undefined side.
constexpr int32_t ORDER_SIDE_BUY = 1 << 2;

constexpr auto CALL_TIMEOUT = std::chrono::seconds(10);
constexpr auto NO_CALL_TIMEOUT = std::chrono::milliseconds(500);

std::string text(const char *value) {
    return value == nullptr ? "<NULL>" : value;
}

// The Java exception of the failed call, as text.
std::string takeException(graal_isolatethread_t *thread) {
    dxfg_exception_t *exception = dxfg_get_and_clear_thread_exception_t(thread);

    if (exception == nullptr) {
        return "<no exception>";
    }

    std::string result = text(exception->class_name) + ": " + text(exception->message);

    dxfg_Exception_release(thread, exception);

    return result;
}

// One call of the listener: what the C code got (the SDK frees it after the call).
struct Call {
    std::string sourceName;
    int32_t sourceId = 0;
    bool isSnapshot = false;
    std::vector<int64_t> indices;
    std::vector<double> sizes;
    void *userData = nullptr;
};

struct Calls {
    std::mutex mutex;
    std::condition_variable changed;
    std::vector<Call> calls;

    // Waits for the given number of calls; returns whether they came.
    bool waitFor(std::size_t count, std::chrono::milliseconds timeout = CALL_TIMEOUT) {
        std::unique_lock<std::mutex> lock(mutex);

        return changed.wait_for(lock, timeout, [&] {
            return calls.size() >= count;
        });
    }

    std::vector<Call> get() {
        std::lock_guard<std::mutex> lock(mutex);

        return calls;
    }
};

void onEventsReceived(graal_isolatethread_t * /* thread */, dxfg_indexed_event_source_t *source,
                      dxfg_event_type_list *events, int32_t isSnapshot, void *userData) {
    Call call{};

    if (source != nullptr) {
        call.sourceName = text(source->name);
        call.sourceId = source->id;
    }

    call.isSnapshot = isSnapshot != 0;
    call.userData = userData;

    for (int32_t i = 0; events != nullptr && i < events->size; ++i) {
        dxfg_event_type_t *event = events->elements[i];

        if (event->clazz == DXFG_EVENT_ORDER) {
            auto *order = reinterpret_cast<dxfg_order_t *>(event);

            call.indices.push_back(order->order_base.index);
            call.sizes.push_back(order->order_base.size);
        } else if (event->clazz == DXFG_EVENT_TIME_AND_SALE) {
            auto *timeAndSale = reinterpret_cast<dxfg_time_and_sale_t *>(event);

            call.indices.push_back(timeAndSale->index);
            call.sizes.push_back(timeAndSale->size);
        }
    }

    auto *calls = static_cast<Calls *>(userData);

    {
        std::lock_guard<std::mutex> lock(calls->mutex);

        calls->calls.push_back(call);
    }

    calls->changed.notify_all();
}

graal_isolatethread_t *isolateThread() {
    static graal_isolatethread_t *thread = [] {
        graal_isolate_t *isolate = nullptr;
        graal_isolatethread_t *result = nullptr;

        return graal_create_isolate(nullptr, &isolate, &result) == 0 ? result : nullptr;
    }();

    return thread;
}

// Releases the handle of the result of a call that returns a handle (every builder function returns a new one).
template <typename Handle> void release(graal_isolatethread_t *thread, Handle *handle) {
    REQUIRE(handle != nullptr);
    dxfg_JavaObjectHandler_release(thread, &handle->handler);
}

// A LOCAL_HUB endpoint: its feed for the models and its publisher for the events.
struct Hub {
    graal_isolatethread_t *thread;
    dxfg_endpoint_t *endpoint;
    dxfg_feed_t *feed;
    dxfg_publisher_t *publisher;

    explicit Hub(graal_isolatethread_t *thread)
        : thread(thread), endpoint(dxfg_DXEndpoint_create2(thread, DXFG_ENDPOINT_ROLE_LOCAL_HUB)),
          feed(dxfg_DXEndpoint_getFeed(thread, endpoint)), publisher(dxfg_DXEndpoint_getPublisher(thread, endpoint)) {
    }

    ~Hub() {
        dxfg_DXEndpoint_close(thread, endpoint);
        dxfg_JavaObjectHandler_release(thread, &publisher->handler);
        dxfg_JavaObjectHandler_release(thread, &feed->handler);
        dxfg_JavaObjectHandler_release(thread, &endpoint->handler);
    }

    template <typename Event> void publish(std::vector<Event> &events) {
        std::vector<dxfg_event_type_t *> elements{};

        for (auto &event : events) {
            elements.push_back(reinterpret_cast<dxfg_event_type_t *>(&event));
        }

        dxfg_event_type_list list{static_cast<int32_t>(elements.size()), elements.data()};

        if (dxfg_DXPublisher_publishEvents(thread, publisher, &list) != 0) {
            FAIL(takeException(thread));
        }
    }
};

constexpr const char *SYMBOL = "TX-MODEL-TEST";

// The index of an order of a regular (not special) source: the source ID is in its high 32 bits (as in Java).
int64_t orderIndex(int32_t sourceId, int64_t index) {
    return (static_cast<int64_t>(sourceId) << 32) | index;
}

dxfg_order_t order(int32_t sourceId, int64_t index, double size, int32_t eventFlags) {
    dxfg_order_t result{};

    result.order_base.market_event.event_type.clazz = DXFG_EVENT_ORDER;
    result.order_base.market_event.event_symbol = SYMBOL;
    result.order_base.event_flags = eventFlags;
    result.order_base.index = orderIndex(sourceId, index);
    result.order_base.size = size;
    result.order_base.price = 10.0 + static_cast<double>(index);
    result.order_base.flags = ORDER_SIDE_BUY;

    return result;
}

dxfg_time_and_sale_t timeAndSale(int64_t index, double size, int32_t eventFlags) {
    dxfg_time_and_sale_t result{};

    result.market_event.event_type.clazz = DXFG_EVENT_TIME_AND_SALE;
    result.market_event.event_symbol = SYMBOL;
    result.event_flags = eventFlags;
    result.index = index;
    result.size = size;
    result.price = 10.0;

    return result;
}

// The source of the name, created by the SDK (release it with dxfg_IndexedEventSource_release).
dxfg_indexed_event_source_t *newSource(graal_isolatethread_t *thread, const char *name) {
    dxfg_indexed_event_source_t *source = dxfg_IndexedEventSource_new(thread, name);

    REQUIRE(source != nullptr);

    return source;
}

std::vector<std::string> sourceNames(dxfg_indexed_event_source_list *sources) {
    std::vector<std::string> names{};

    for (int32_t i = 0; i < sources->size; ++i) {
        names.push_back(text(sources->elements[i]->name));
    }

    std::sort(names.begin(), names.end());

    return names;
}

} // namespace

TEST_CASE("IndexedTxModel delivers the snapshot and the updates of its source to the listener") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    // Calls outlives the hub, so a callback still in flight after a failed REQUIRE does not write into a destroyed one.
    Calls calls{};
    Hub hub(thread);
    dxfg_indexed_event_source_t *ntv = newSource(thread, "ntv");
    dxfg_indexed_event_source_t *dex = newSource(thread, "DEX");
    dxfg_string_symbol_t symbol{{STRING}, SYMBOL};

    dxfg_tx_model_listener_t *listener = dxfg_TxModelListener_new(thread, &onEventsReceived, &calls);

    REQUIRE(listener != nullptr);

    dxfg_indexed_tx_model_builder_t *builder = dxfg_IndexedTxModel_newBuilder(thread, DXFG_EVENT_ORDER);

    REQUIRE(builder != nullptr);

    {
        dxfg_indexed_event_source_t *elements[] = {ntv};
        dxfg_indexed_event_source_list sources{1, elements};

        release(thread, dxfg_IndexedTxModel_Builder_withSources(thread, builder, &sources));
    }

    release(thread, dxfg_IndexedTxModel_Builder_withBatchProcessing(thread, builder, 0));
    release(thread, dxfg_IndexedTxModel_Builder_withSnapshotProcessing(thread, builder, 1));
    release(thread, dxfg_IndexedTxModel_Builder_withFeed(thread, builder, hub.feed));
    release(thread, dxfg_IndexedTxModel_Builder_withSymbol(thread, builder, &symbol.supper));
    release(thread, dxfg_IndexedTxModel_Builder_withListener(thread, builder, listener));

    dxfg_indexed_tx_model_t *model = dxfg_IndexedTxModel_Builder_build(thread, builder);

    REQUIRE(model != nullptr);
    CHECK(dxfg_IndexedTxModel_isBatchProcessing(thread, model) == 0);
    CHECK(dxfg_IndexedTxModel_isSnapshotProcessing(thread, model) == 1);

    // The snapshot of three orders comes in one call (QD requires the snapshot to end with index 0).
    std::vector<dxfg_order_t> snapshot{order(ntv->id, 2, 30.0, SNAPSHOT_BEGIN), order(ntv->id, 1, 20.0, 0),
                                       order(ntv->id, 0, 10.0, SNAPSHOT_END)};

    hub.publish(snapshot);

    REQUIRE(calls.waitFor(1));

    {
        Call call = calls.get()[0];

        CHECK(call.isSnapshot);
        CHECK(call.sourceName == "ntv");
        CHECK(call.sourceId == ntv->id);
        CHECK(call.userData == &calls);
        REQUIRE(call.indices.size() == 3);
        CHECK(std::is_permutation(call.sizes.begin(), call.sizes.end(), std::vector<double>{10.0, 20.0, 30.0}.begin()));
    }

    // An update without flags comes alone and is not a snapshot.
    std::vector<dxfg_order_t> update{order(ntv->id, 1, 25.0, 0)};

    hub.publish(update);

    REQUIRE(calls.waitFor(2));

    {
        Call call = calls.get()[1];

        CHECK_FALSE(call.isSnapshot);
        CHECK(call.sourceName == "ntv");
        CHECK(call.userData == &calls);
        REQUIRE(call.indices.size() == 1);
        CHECK(call.indices[0] == orderIndex(ntv->id, 1));
        CHECK(call.sizes[0] == 25.0);
    }

    // The sources are replaced.
    {
        dxfg_indexed_event_source_list *sources = dxfg_IndexedTxModel_getSources(thread, model);

        REQUIRE(sources != nullptr);
        CHECK(sourceNames(sources) == std::vector<std::string>{"ntv"});
        dxfg_CList_IndexedEventSource_release(thread, sources);

        dxfg_indexed_event_source_t *elements[] = {ntv, dex};
        dxfg_indexed_event_source_list newSources{2, elements};

        CHECK(dxfg_IndexedTxModel_setSources(thread, model, &newSources) == 0);

        sources = dxfg_IndexedTxModel_getSources(thread, model);

        REQUIRE(sources != nullptr);
        CHECK(sourceNames(sources) == std::vector<std::string>{"DEX", "ntv"});
        dxfg_CList_IndexedEventSource_release(thread, sources);
    }

    // The events of the new source come with that source.
    std::vector<dxfg_order_t> dexSnapshot{order(dex->id, 0, 5.0, SNAPSHOT_BEGIN | SNAPSHOT_END)};

    hub.publish(dexSnapshot);

    REQUIRE(calls.waitFor(3));

    {
        Call call = calls.get()[2];

        CHECK(call.isSnapshot);
        CHECK(call.sourceName == "DEX");
        CHECK(call.sourceId == dex->id);
        REQUIRE(call.sizes.size() == 1);
        CHECK(call.sizes[0] == 5.0);
    }

    // Nothing comes after close; closing again does nothing.
    CHECK(dxfg_IndexedTxModel_close(thread, model) == 0);

    std::vector<dxfg_order_t> afterClose{order(ntv->id, 1, 15.0, 0)};

    hub.publish(afterClose);

    CHECK_FALSE(calls.waitFor(4, NO_CALL_TIMEOUT));
    CHECK(dxfg_IndexedTxModel_close(thread, model) == 0);

    release(thread, model);
    release(thread, builder);
    release(thread, listener);
    dxfg_IndexedEventSource_release(thread, dex);
    dxfg_IndexedEventSource_release(thread, ntv);
}

TEST_CASE("TimeSeriesTxModel delivers the snapshot from its time and the updates to the listener") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    // Calls outlives the hub, so a callback still in flight after a failed REQUIRE does not write into a destroyed one.
    Calls calls{};
    Hub hub(thread);
    dxfg_string_symbol_t symbol{{STRING}, SYMBOL};

    dxfg_tx_model_listener_t *listener = dxfg_TxModelListener_new(thread, &onEventsReceived, &calls);

    REQUIRE(listener != nullptr);

    dxfg_time_series_tx_model_builder_t *builder = dxfg_TimeSeriesTxModel_newBuilder(thread, DXFG_EVENT_TIME_AND_SALE);

    REQUIRE(builder != nullptr);

    release(thread, dxfg_TimeSeriesTxModel_Builder_withFromTime(thread, builder, 0));
    release(thread, dxfg_TimeSeriesTxModel_Builder_withSorting(thread, builder, ASCENDING));
    release(thread, dxfg_TimeSeriesTxModel_Builder_withSnapshotProcessing(thread, builder, 1));
    release(thread, dxfg_TimeSeriesTxModel_Builder_withEventPeek(thread, builder, nullptr));
    release(thread, dxfg_TimeSeriesTxModel_Builder_withFeed(thread, builder, hub.feed));
    release(thread, dxfg_TimeSeriesTxModel_Builder_withSymbol(thread, builder, &symbol.supper));
    release(thread, dxfg_TimeSeriesTxModel_Builder_withListener(thread, builder, listener));

    dxfg_time_series_tx_model_t *model = dxfg_TimeSeriesTxModel_Builder_build(thread, builder);

    REQUIRE(model != nullptr);
    CHECK(dxfg_TimeSeriesTxModel_getFromTime(thread, model) == 0);
    CHECK(dxfg_TimeSeriesTxModel_isBatchProcessing(thread, model) == 1);
    CHECK(dxfg_TimeSeriesTxModel_isSnapshotProcessing(thread, model) == 1);

    // The snapshot comes in one call, sorted by index; the removed event at the end of the snapshot is dropped.
    std::vector<dxfg_time_and_sale_t> snapshot{timeAndSale(3, 30.0, SNAPSHOT_BEGIN), timeAndSale(2, 20.0, 0),
                                               timeAndSale(1, 10.0, 0),
                                               timeAndSale(0, 0.0, REMOVE_EVENT | SNAPSHOT_END)};

    hub.publish(snapshot);

    REQUIRE(calls.waitFor(1));

    {
        Call call = calls.get()[0];

        CHECK(call.isSnapshot);
        CHECK(call.userData == &calls);
        CHECK(call.indices == std::vector<int64_t>{1, 2, 3});
        CHECK(call.sizes == std::vector<double>{10.0, 20.0, 30.0});
    }

    std::vector<dxfg_time_and_sale_t> update{timeAndSale(4, 40.0, 0)};

    hub.publish(update);

    REQUIRE(calls.waitFor(2));

    {
        Call call = calls.get()[1];

        CHECK_FALSE(call.isSnapshot);
        CHECK(call.indices == std::vector<int64_t>{4});
        CHECK(call.sizes == std::vector<double>{40.0});
    }

    // Nothing comes after close; closing again does nothing.
    CHECK(dxfg_TimeSeriesTxModel_close(thread, model) == 0);

    std::vector<dxfg_time_and_sale_t> afterClose{timeAndSale(5, 50.0, 0)};

    hub.publish(afterClose);

    CHECK_FALSE(calls.waitFor(3, NO_CALL_TIMEOUT));
    CHECK(dxfg_TimeSeriesTxModel_close(thread, model) == 0);

    // The time is changed after close: a new time of an open model would subscribe again and bring a new snapshot.
    CHECK(dxfg_TimeSeriesTxModel_setFromTime(thread, model, 1000) == 0);
    CHECK(dxfg_TimeSeriesTxModel_getFromTime(thread, model) == 1000);

    release(thread, model);
    release(thread, builder);
    release(thread, listener);
}

namespace {

// What the event peek and the listener saw, in the order of the calls: "peek <index>" and "list <index>,<index>...".
struct PeekLog {
    std::mutex mutex;
    std::condition_variable changed;
    std::vector<std::string> entries;
    std::vector<void *> userData;

    void add(std::string entry, void *data) {
        {
            std::lock_guard<std::mutex> lock(mutex);

            entries.push_back(std::move(entry));
            userData.push_back(data);
        }

        changed.notify_all();
    }

    // Waits for the given number of the entries of the listener; returns whether they came.
    bool waitForLists(std::size_t count, std::chrono::milliseconds timeout = CALL_TIMEOUT) {
        std::unique_lock<std::mutex> lock(mutex);

        return changed.wait_for(lock, timeout, [&] {
            return static_cast<std::size_t>(std::count_if(entries.begin(), entries.end(), [](const std::string &entry) {
                       return entry.rfind("list", 0) == 0;
                   })) >= count;
        });
    }

    std::vector<std::string> get() {
        std::lock_guard<std::mutex> lock(mutex);

        return entries;
    }

    std::vector<void *> getUserData() {
        std::lock_guard<std::mutex> lock(mutex);

        return userData;
    }
};

int64_t timeAndSaleIndex(dxfg_event_type_t *event) {
    return event->clazz == DXFG_EVENT_TIME_AND_SALE ? reinterpret_cast<dxfg_time_and_sale_t *>(event)->index : -1;
}

void onEventPeek(graal_isolatethread_t * /* thread */, dxfg_v4_event_type_t *event, void *userData) {
    static_cast<PeekLog *>(userData)->add("peek " + std::to_string(timeAndSaleIndex(event)), userData);
}

void onEventsLogged(graal_isolatethread_t * /* thread */, dxfg_indexed_event_source_t * /* source */,
                    dxfg_event_type_list *events, int32_t /* isSnapshot */, void *userData) {
    std::string entry = "list ";

    for (int32_t i = 0; events != nullptr && i < events->size; ++i) {
        entry += (i == 0 ? "" : ",") + std::to_string(timeAndSaleIndex(events->elements[i]));
    }

    static_cast<PeekLog *>(userData)->add(entry, userData);
}

// Sets the event peek with dxfg_v4_TimeSeriesTxModel_Builder_withEventPeekFunction() and releases the returned handle.
void withEventPeekFunction(graal_isolatethread_t *thread, dxfg_time_series_tx_model_builder_t *builder,
                           dxfg_v4_time_series_tx_model_event_peek_function eventPeek, void *userData) {
    dxfg_v4_time_series_tx_model_builder_t *result = nullptr;

    REQUIRE(dxfg_v4_TimeSeriesTxModel_Builder_withEventPeekFunction(thread, builder, eventPeek, userData, &result) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    release(thread, result);
}

// A time series model of the time and sales of SYMBOL from the time 0 with the listener that logs to the log; the
// event peek is set by `configure`.
struct PeekModel {
    graal_isolatethread_t *thread;
    dxfg_tx_model_listener_t *listener = nullptr;
    dxfg_time_series_tx_model_builder_t *builder = nullptr;
    dxfg_time_series_tx_model_t *model = nullptr;
    dxfg_string_symbol_t symbol{{STRING}, SYMBOL};

    template <typename Configure>
    PeekModel(Hub &hub, PeekLog &log, dxfg_executor_t *executor, Configure configure) : thread(hub.thread) {
        listener = dxfg_TxModelListener_new(thread, &onEventsLogged, &log);
        REQUIRE(listener != nullptr);
        builder = dxfg_TimeSeriesTxModel_newBuilder(thread, DXFG_EVENT_TIME_AND_SALE);
        REQUIRE(builder != nullptr);
        release(thread, dxfg_TimeSeriesTxModel_Builder_withFromTime(thread, builder, 0));
        release(thread, dxfg_TimeSeriesTxModel_Builder_withSorting(thread, builder, ASCENDING));
        release(thread, dxfg_TimeSeriesTxModel_Builder_withSnapshotProcessing(thread, builder, 1));
        release(thread, dxfg_TimeSeriesTxModel_Builder_withFeed(thread, builder, hub.feed));
        release(thread, dxfg_TimeSeriesTxModel_Builder_withSymbol(thread, builder, &symbol.supper));
        release(thread, dxfg_TimeSeriesTxModel_Builder_withListener(thread, builder, listener));

        if (executor != nullptr) {
            release(thread, dxfg_TimeSeriesTxModel_Builder_withExecutor(thread, builder, executor));
        }

        configure(builder);
        model = dxfg_TimeSeriesTxModel_Builder_build(thread, builder);
        REQUIRE(model != nullptr);
    }

    PeekModel(const PeekModel &) = delete;
    PeekModel &operator=(const PeekModel &) = delete;

    ~PeekModel() {
        if (model != nullptr) {
            dxfg_TimeSeriesTxModel_close(thread, model);
            dxfg_JavaObjectHandler_release(thread, &model->handler);
        }

        if (builder != nullptr) {
            dxfg_JavaObjectHandler_release(thread, &builder->handler);
        }

        if (listener != nullptr) {
            dxfg_JavaObjectHandler_release(thread, &listener->handler);
        }
    }
};

} // namespace

TEST_CASE("The event peek of a TimeSeriesTxModel sees each event before the listener gets the list") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    int32_t supported = 0;

    REQUIRE(dxfg_v4_system_is_feature_supported(thread, DXFG_V4_FEATURE_TIME_SERIES_TX_MODEL_EVENT_PEEK, &supported) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(supported == 1);

    // The log outlives the hub, so a callback still in flight after a failed REQUIRE does not write into a destroyed one.
    PeekLog log{};
    Hub hub(thread);
    PeekModel model(hub, log, nullptr, [&](dxfg_time_series_tx_model_builder_t *builder) {
        withEventPeekFunction(thread, builder, &onEventPeek, &log);
    });

    // The removed event at the end of the snapshot is dropped before the peek, as before the listener.
    std::vector<dxfg_time_and_sale_t> snapshot{timeAndSale(3, 30.0, SNAPSHOT_BEGIN), timeAndSale(2, 20.0, 0),
                                               timeAndSale(1, 10.0, 0),
                                               timeAndSale(0, 0.0, REMOVE_EVENT | SNAPSHOT_END)};

    hub.publish(snapshot);
    REQUIRE(log.waitForLists(1));

    std::vector<dxfg_time_and_sale_t> update{timeAndSale(4, 40.0, 0)};

    hub.publish(update);
    REQUIRE(log.waitForLists(2));
    CHECK(log.get() == std::vector<std::string>{"peek 1", "peek 2", "peek 3", "list 1,2,3", "peek 4", "list 4"});

    for (void *userData : log.getUserData()) {
        CHECK(userData == &log);
    }
}

TEST_CASE("NULL and the older no-op function remove the event peek of a TimeSeriesTxModel") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    for (int removeWithOlderFunction = 0; removeWithOlderFunction <= 1; ++removeWithOlderFunction) {
        INFO("removed with the older function: " << removeWithOlderFunction);

        PeekLog log{};
        Hub hub(thread);
        PeekModel model(hub, log, nullptr, [&](dxfg_time_series_tx_model_builder_t *builder) {
            withEventPeekFunction(thread, builder, &onEventPeek, &log);

            if (removeWithOlderFunction == 1) {
                dxfg_v4_time_series_tx_model_builder_t *result = nullptr;

                REQUIRE(dxfg_v4_TimeSeriesTxModel_Builder_withEventPeek(thread, builder, nullptr, &result) ==
                        DXFG_EXECUTE_SUCCESSFULLY);
                release(thread, result);
            } else {
                withEventPeekFunction(thread, builder, nullptr, &log);
            }
        });

        std::vector<dxfg_time_and_sale_t> snapshot{timeAndSale(2, 20.0, SNAPSHOT_BEGIN),
                                                   timeAndSale(1, 10.0, 0),
                                                   timeAndSale(0, 0.0, REMOVE_EVENT | SNAPSHOT_END)};

        hub.publish(snapshot);
        REQUIRE(log.waitForLists(1));
        CHECK(log.get() == std::vector<std::string>{"list 1,2"});
    }
}

TEST_CASE("The event peek of a TimeSeriesTxModel is not called in the tasks of a closed event queue") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    PeekLog log{};
    Hub hub(thread);
    dxfg_v4_event_queue_t *queue = nullptr;
    dxfg_v4_executor_t *executor = nullptr;

    REQUIRE(dxfg_v4_EventQueue_create(thread, &queue) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_EventQueue_getExecutor(thread, queue, &executor) == DXFG_EXECUTE_SUCCESSFULLY);

    {
        PeekModel model(hub, log, executor, [&](dxfg_time_series_tx_model_builder_t *builder) {
            withEventPeekFunction(thread, builder, &onEventPeek, &log);
        });

        // An open queue: the model runs in a poll, the peek is called.
        std::vector<dxfg_time_and_sale_t> snapshot{timeAndSale(2, 20.0, SNAPSHOT_BEGIN),
                                                   timeAndSale(1, 10.0, 0),
                                                   timeAndSale(0, 0.0, REMOVE_EVENT | SNAPSHOT_END)};

        hub.publish(snapshot);

        for (auto end = std::chrono::steady_clock::now() + CALL_TIMEOUT;
             log.get().empty() && std::chrono::steady_clock::now() < end;) {
            dxfg_v4_event_type_list *events = nullptr;

            if (dxfg_v4_EventQueue_poll(thread, queue, 200, 0, &events) == DXFG_EXECUTE_SUCCESSFULLY &&
                events != nullptr) {
                dxfg_v4_event_type_list_release(thread, events);
            }
        }

        REQUIRE(log.waitForLists(1));
        CHECK(log.get() == std::vector<std::string>{"peek 1", "peek 2", "list 1,2"});

        // A closed queue: its executor runs the tasks at once in the muted scope, without the peek and the listener.
        REQUIRE(dxfg_v4_EventQueue_close(thread, queue) == DXFG_EXECUTE_SUCCESSFULLY);

        std::vector<dxfg_time_and_sale_t> update{timeAndSale(3, 30.0, 0)};

        hub.publish(update);
        CHECK_FALSE(log.waitForLists(2, NO_CALL_TIMEOUT));
        CHECK(log.get() == std::vector<std::string>{"peek 1", "peek 2", "list 1,2"});
    }

    dxfg_v4_JavaObjectHandler_release(thread, &executor->handler);
    dxfg_v4_JavaObjectHandler_release(thread, &queue->handler);
}

TEST_CASE("dxfg_v4_TimeSeriesTxModel_Builder_withEventPeekFunction fails on NULL pointers") {
    graal_isolatethread_t *thread = isolateThread();

    REQUIRE(thread != nullptr);

    dxfg_time_series_tx_model_builder_t *builder = dxfg_TimeSeriesTxModel_newBuilder(thread, DXFG_EVENT_TIME_AND_SALE);
    dxfg_v4_time_series_tx_model_builder_t *result = nullptr;

    REQUIRE(builder != nullptr);
    CHECK(dxfg_v4_TimeSeriesTxModel_Builder_withEventPeekFunction(thread, builder, &onEventPeek, nullptr, nullptr) ==
          DXFG_EXECUTE_FAIL);
    CHECK(takeException(thread).rfind("java.lang.IllegalArgumentException", 0) == 0);
    CHECK(dxfg_v4_TimeSeriesTxModel_Builder_withEventPeekFunction(thread, nullptr, &onEventPeek, nullptr, &result) ==
          DXFG_EXECUTE_FAIL);
    CHECK(result == nullptr);
    CHECK(takeException(thread).rfind("java.lang.IllegalArgumentException", 0) == 0);

    release(thread, builder);
}

} // namespace test
} // namespace dxfg
