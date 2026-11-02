// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The feed filter through the C API (dxfg_v4_feed_filter.h): a filter of categories with prefix, list and range
// expressions accepts and classifies the subscriptions and the events; the builder refuses bad categories and stays
// usable; a dynamic filter (an `ipf[...]` expression with an update period, whose file the test rewrites) calls its
// listener on a thread of the SDK, the listener can add and remove listeners from its call, and it is silent after its
// release and after dxfg_v4_NativeListener_deactivateAll().

#include <doctest.h>
#include <dxfg_api.h>

#include "Hub.hpp"

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <functional>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace test {

namespace {

// The wait for a listener call that has to come: the IPF is checked every second.
constexpr auto CALL_TIMEOUT = std::chrono::seconds(20);
// The wait for a listener call that must not come: longer than two update periods.
constexpr auto QUIET_TIME = std::chrono::milliseconds(2500);

// The isolate of the cases, except the one of deactivateAll(), which has an isolate of its own.
Isolate &isolate() {
    static Isolate instance;

    return instance;
}

// Releases a handle at the end of the scope.
template <typename Handle> struct Owned {
    graal_isolatethread_t *thread;
    Handle *handle = nullptr;

    explicit Owned(graal_isolatethread_t *thread) : thread(thread) {
    }

    Owned(const Owned &) = delete;
    Owned &operator=(const Owned &) = delete;

    ~Owned() {
        reset();
    }

    void reset() {
        if (handle != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &handle->handler);
            handle = nullptr;
        }
    }
};

using Builder = Owned<dxfg_v4_feed_filter_builder_t>;
using Filter = Owned<dxfg_v4_feed_filter_t>;
using Tracker = Owned<dxfg_v4_feed_filter_tracker_t>;
using Listener = Owned<dxfg_v4_feed_filter_listener_t>;

// A list of the C API over the strings (the pointers stay valid while the vector does).
struct StringList {
    std::vector<const char *> elements;
    dxfg_v4_string_list list{};

    explicit StringList(std::vector<const char *> strings) : elements(std::move(strings)) {
        list.size = static_cast<int32_t>(elements.size());
        list.elements = elements.data();
    }
};

// 1 if the subscription of the event type and the symbol passes the filter, 0 if not, -1 on error.
int32_t accepts(graal_isolatethread_t *thread, dxfg_v4_feed_filter_t *filter, dxfg_v4_event_clazz_t eventType,
                dxfg_v4_symbol_t *symbol) {
    int32_t accepted = -1;

    return dxfg_v4_DXFeedFilter_accept(thread, filter, eventType, symbol, &accepted) == DXFG_EXECUTE_SUCCESSFULLY
               ? accepted
               : -1;
}

int32_t accepts(graal_isolatethread_t *thread, dxfg_v4_feed_filter_t *filter, const char *symbol) {
    dxfg_v4_string_symbol_t stringSymbol{{STRING}, symbol};

    return accepts(thread, filter, DXFG_EVENT_QUOTE, &stringSymbol.supper);
}

// The category of the subscription, "<none>" if it passes no category, "<error>" on error.
std::string categoryOf(graal_isolatethread_t *thread, dxfg_v4_feed_filter_t *filter, dxfg_v4_event_clazz_t eventType,
                       dxfg_v4_symbol_t *symbol) {
    const char *category = "<unset>";

    if (dxfg_v4_DXFeedFilter_getCategory(thread, filter, eventType, symbol, &category) != DXFG_EXECUTE_SUCCESSFULLY) {
        return "<error>";
    }
    if (category == nullptr) {
        return "<none>";
    }

    std::string result = category;
    dxfg_v4_String_release(thread, category);

    return result;
}

std::string categoryOf(graal_isolatethread_t *thread, dxfg_v4_feed_filter_t *filter, const char *symbol) {
    dxfg_v4_string_symbol_t stringSymbol{{STRING}, symbol};

    return categoryOf(thread, filter, DXFG_EVENT_QUOTE, &stringSymbol.supper);
}

std::string eventCategoryOf(graal_isolatethread_t *thread, dxfg_v4_feed_filter_t *filter, const char *symbol) {
    dxfg_v4_quote_t quote{};
    quote.market_event.event_type.clazz = DXFG_EVENT_QUOTE;
    quote.market_event.event_symbol = symbol;
    const char *category = "<unset>";

    if (dxfg_v4_DXFeedFilter_getEventCategory(thread, filter, &quote.market_event.event_type, &category) !=
        DXFG_EXECUTE_SUCCESSFULLY) {
        return "<error>";
    }
    if (category == nullptr) {
        return "<none>";
    }

    std::string result = category;
    dxfg_v4_String_release(thread, category);

    return result;
}

int32_t acceptsEvent(graal_isolatethread_t *thread, dxfg_v4_feed_filter_t *filter, const char *symbol) {
    dxfg_v4_quote_t quote{};
    quote.market_event.event_type.clazz = DXFG_EVENT_QUOTE;
    quote.market_event.event_symbol = symbol;
    int32_t accepted = -1;

    return dxfg_v4_DXFeedFilter_acceptEvent(thread, filter, &quote.market_event.event_type, &accepted) ==
                   DXFG_EXECUTE_SUCCESSFULLY
               ? accepted
               : -1;
}

int32_t isDynamic(graal_isolatethread_t *thread, dxfg_v4_feed_filter_t *filter) {
    int32_t dynamic = -1;

    return dxfg_v4_DXFeedFilter_isDynamic(thread, filter, &dynamic) == DXFG_EXECUTE_SUCCESSFULLY ? dynamic : -1;
}

// The calls of a listener: counted and waited for; the action runs in each call, on the thread of the SDK.
struct Calls {
    std::mutex mutex{};
    std::condition_variable changed{};
    int count = 0;
    std::vector<std::thread::id> threads{};
    std::function<void(graal_isolatethread_t *)> action{};

    static void onUpdated(graal_isolatethread_t *thread, void *userData) {
        auto *calls = static_cast<Calls *>(userData);

        if (calls->action) {
            calls->action(thread);
        }

        std::lock_guard<std::mutex> lock(calls->mutex);
        ++calls->count;
        calls->threads.push_back(std::this_thread::get_id());
        calls->changed.notify_all();
    }

    bool waitFor(int expected) {
        std::unique_lock<std::mutex> lock(mutex);

        return changed.wait_for(lock, CALL_TIMEOUT, [this, expected] { return count >= expected; });
    }

    int get() {
        std::lock_guard<std::mutex> lock(mutex);

        return count;
    }
};

// An IPF file of the stocks in the current directory, deleted at the end. Each write moves the time of the file
// forward: the updater of the filter compares the last modified times.
struct IpfFile {
    std::filesystem::path path;

    explicit IpfFile(const std::string &name) : path(name) {
    }

    IpfFile(const IpfFile &) = delete;
    IpfFile &operator=(const IpfFile &) = delete;

    ~IpfFile() {
        std::error_code ignored;
        std::filesystem::remove(path, ignored);
    }

    bool write(const std::vector<std::string> &symbols) {
        {
            std::ofstream file(path, std::ios::binary | std::ios::trunc);
            file << "#STOCK::=TYPE,SYMBOL\n";

            for (const auto &symbol : symbols) {
                file << "STOCK," << symbol << "\n";
            }

            file << "##COMPLETE\n";

            if (!file) {
                return false;
            }
        }

        static int shift = 0;
        std::error_code error;
        std::filesystem::last_write_time(
            path, std::filesystem::file_time_type::clock::now() + std::chrono::seconds(2 * ++shift), error);

        return !error;
    }

    // The expression of a dynamic filter of the file, reloaded every second.
    std::string expression() const {
        return "ipf[" + path.generic_string() + ",update=1s]";
    }
};

// A dynamic filter of one category "ipf" of a new IPF file of IBM.
struct DynamicFilter {
    IpfFile file;
    Builder builder;
    Filter filter;
    Tracker tracker;
    bool ready = false;

    DynamicFilter(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint, const std::string &name)
        : file(name), builder(thread), filter(thread), tracker(thread) {
        const auto expression = file.expression();

        ready = file.write({"IBM"}) &&
                dxfg_v4_DXFeedFilter_newBuilder(thread, endpoint, &builder.handle) == DXFG_EXECUTE_SUCCESSFULLY &&
                dxfg_v4_DXFeedFilter_Builder_withCategory(thread, builder.handle, "ipf", expression.c_str()) ==
                    DXFG_EXECUTE_SUCCESSFULLY &&
                dxfg_v4_DXFeedFilter_Builder_build(thread, builder.handle, &filter.handle) ==
                    DXFG_EXECUTE_SUCCESSFULLY &&
                dxfg_v4_DXFeedFilter_getTracker(thread, filter.handle, &tracker.handle) == DXFG_EXECUTE_SUCCESSFULLY;
    }
};

std::string uniqueName(const char *prefix) {
    return std::string("FeedFilterTest-") + prefix + "-" +
           std::to_string(std::chrono::steady_clock::now().time_since_epoch().count()) + ".ipf";
}

} // namespace

TEST_CASE("A filter of prefix, list and range categories accepts and classifies symbols and events") {
    Hub hub(isolate().thread);
    REQUIRE(hub.ready);
    const auto thread = hub.thread;

    Builder builder(thread);
    REQUIRE(dxfg_v4_DXFeedFilter_newBuilder(thread, hub.endpoint, &builder.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(builder.handle != nullptr);
    REQUIRE(dxfg_v4_DXFeedFilter_Builder_withCategory(thread, builder.handle, "list", "IBM,MSFT") ==
            DXFG_EXECUTE_SUCCESSFULLY);

    StringList names({"prefix", "range"});
    StringList expressions({"A*", "range-N-P-"});
    REQUIRE(dxfg_v4_DXFeedFilter_Builder_withCategories(thread, builder.handle, &names.list, &expressions.list) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    Filter filter(thread);
    REQUIRE(dxfg_v4_DXFeedFilter_Builder_build(thread, builder.handle, &filter.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(filter.handle != nullptr);
    CHECK(takeExceptionClassName(thread).empty());

    // The subscriptions: the first category that matches, in the order of the builder.
    CHECK(accepts(thread, filter.handle, "IBM") == 1);
    CHECK(categoryOf(thread, filter.handle, "IBM") == "list");
    CHECK(categoryOf(thread, filter.handle, "MSFT") == "list");
    CHECK(categoryOf(thread, filter.handle, "AAPL") == "prefix");
    CHECK(categoryOf(thread, filter.handle, "NVDA") == "range");
    CHECK(categoryOf(thread, filter.handle, "ORCL") == "range");

    // Other symbols: no category is not an error.
    CHECK(accepts(thread, filter.handle, "ZZZ") == 0);
    CHECK(categoryOf(thread, filter.handle, "ZZZ") == "<none>");
    CHECK(accepts(thread, filter.handle, "GOOG") == 0);
    CHECK(categoryOf(thread, filter.handle, "QQQ") == "<none>");

    // A time series subscription symbol is classified by its event symbol.
    dxfg_v4_string_symbol_t aapl{{STRING}, "AAPL"};
    dxfg_v4_time_series_subscription_symbol_t series{{TIME_SERIES_SUBSCRIPTION}, &aapl.supper, 0};
    CHECK(accepts(thread, filter.handle, DXFG_EVENT_TIME_AND_SALE, &series.supper) == 1);
    CHECK(categoryOf(thread, filter.handle, DXFG_EVENT_TIME_AND_SALE, &series.supper) == "prefix");
    dxfg_v4_string_symbol_t zzz{{STRING}, "ZZZ"};
    dxfg_v4_time_series_subscription_symbol_t otherSeries{{TIME_SERIES_SUBSCRIPTION}, &zzz.supper, 0};
    CHECK(accepts(thread, filter.handle, DXFG_EVENT_TIME_AND_SALE, &otherSeries.supper) == 0);
    CHECK(categoryOf(thread, filter.handle, DXFG_EVENT_TIME_AND_SALE, &otherSeries.supper) == "<none>");

    // The events.
    CHECK(acceptsEvent(thread, filter.handle, "MSFT") == 1);
    CHECK(eventCategoryOf(thread, filter.handle, "MSFT") == "list");
    CHECK(eventCategoryOf(thread, filter.handle, "AMZN") == "prefix");
    CHECK(eventCategoryOf(thread, filter.handle, "ORCL") == "range");
    CHECK(acceptsEvent(thread, filter.handle, "ZZZ") == 0);
    CHECK(eventCategoryOf(thread, filter.handle, "ZZZ") == "<none>");
    CHECK(takeExceptionClassName(thread).empty());

    // A static filter: its tracker returns the same filter.
    CHECK(isDynamic(thread, filter.handle) == 0);
    Tracker tracker(thread);
    REQUIRE(dxfg_v4_DXFeedFilter_getTracker(thread, filter.handle, &tracker.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    Filter current(thread);
    REQUIRE(dxfg_v4_DXFeedFilterTracker_getCurrentFilter(thread, tracker.handle, &current.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    int32_t equal = -1;
    REQUIRE(dxfg_v4_Object_equals(thread, &filter.handle->handler, &current.handle->handler, &equal) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);

    // Each call gives a handle of its own.
    Tracker sameTracker(thread);
    REQUIRE(dxfg_v4_DXFeedFilter_getTracker(thread, current.handle, &sameTracker.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(sameTracker.handle != tracker.handle);
    REQUIRE(dxfg_v4_Object_equals(thread, &tracker.handle->handler, &sameTracker.handle->handler, &equal) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(equal == 1);

    // The listener of a static filter is ignored: no calls, and it can be removed.
    Calls calls;
    Listener listener(thread);
    REQUIRE(dxfg_v4_DXFeedFilterListener_new(thread, &Calls::onUpdated, &calls, &listener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXFeedFilterTracker_addListener(thread, tracker.handle, listener.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(dxfg_v4_DXFeedFilterTracker_removeListener(thread, tracker.handle, listener.handle) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(calls.get() == 0);
}

TEST_CASE("The builder refuses bad categories and stays usable") {
    Hub hub(isolate().thread);
    REQUIRE(hub.ready);
    const auto thread = hub.thread;

    Builder builder(thread);
    REQUIRE(dxfg_v4_DXFeedFilter_newBuilder(thread, hub.endpoint, &builder.handle) == DXFG_EXECUTE_SUCCESSFULLY);

    // No categories yet.
    Filter filter(thread);
    CHECK(dxfg_v4_DXFeedFilter_Builder_build(thread, builder.handle, &filter.handle) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(filter.handle == nullptr);

    const auto withCategory = [&](const char *name, const char *expression) {
        return dxfg_v4_DXFeedFilter_Builder_withCategory(thread, builder.handle, name, expression);
    };

    REQUIRE(withCategory("a", "A*") == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(withCategory(nullptr, "B*") == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategory("", "B*") == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategory("b", nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategory("b", "") == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    // A duplicate name keeps the first expression.
    CHECK(withCategory("a", "B*") == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    const auto withCategories = [&](const dxfg_v4_string_list *names, const dxfg_v4_string_list *expressions) {
        return dxfg_v4_DXFeedFilter_Builder_withCategories(thread, builder.handle, names, expressions);
    };

    // The bad element is the last one: the first ones must not be added either.
    StringList twoNames({"b", "c"});
    StringList oneExpression({"B*"});
    StringList twoExpressions({"B*", "C*"});
    StringList nullName({"b", nullptr});
    StringList emptyName({"b", ""});
    StringList nullExpression({"B*", nullptr});
    StringList repeatedName({"b", "b"});
    StringList existingName({"b", "a"});

    CHECK(withCategories(nullptr, &twoExpressions.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&twoNames.list, nullptr) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&twoNames.list, &oneExpression.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&nullName.list, &twoExpressions.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&emptyName.list, &twoExpressions.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&twoNames.list, &nullExpression.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&repeatedName.list, &twoExpressions.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&existingName.list, &twoExpressions.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // Malformed lists: elements missing, a negative size.
    dxfg_v4_string_list noElements{2, nullptr};
    dxfg_v4_string_list negativeSize{-1, twoNames.elements.data()};
    CHECK(withCategories(&noElements, &twoExpressions.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&twoNames.list, &noElements) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&negativeSize, &twoExpressions.list) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");
    CHECK(withCategories(&twoNames.list, &negativeSize) == DXFG_EXECUTE_FAIL);
    CHECK(takeExceptionClassName(thread) == "java.lang.IllegalArgumentException");

    // The builder has only "a" = "A*": BBB and CCC pass no category.
    REQUIRE(dxfg_v4_DXFeedFilter_Builder_build(thread, builder.handle, &filter.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(categoryOf(thread, filter.handle, "AAA") == "a");
    CHECK(categoryOf(thread, filter.handle, "BBB") == "<none>");
    CHECK(categoryOf(thread, filter.handle, "CCC") == "<none>");

    // Still usable: more categories and another filter; empty lists add nothing.
    StringList noStrings({});
    CHECK(withCategories(&noStrings.list, &noStrings.list) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(withCategories(&twoNames.list, &twoExpressions.list) == DXFG_EXECUTE_SUCCESSFULLY);
    Filter second(thread);
    REQUIRE(dxfg_v4_DXFeedFilter_Builder_build(thread, builder.handle, &second.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(categoryOf(thread, second.handle, "BBB") == "b");
    CHECK(categoryOf(thread, second.handle, "CCC") == "c");
    // The first filter does not change.
    CHECK(categoryOf(thread, filter.handle, "BBB") == "<none>");

    // A bad expression fails at build.
    REQUIRE(withCategory("bad", "range-") == DXFG_EXECUTE_SUCCESSFULLY);
    Filter bad(thread);
    CHECK(dxfg_v4_DXFeedFilter_Builder_build(thread, builder.handle, &bad.handle) == DXFG_EXECUTE_FAIL);
    CHECK_FALSE(takeExceptionClassName(thread).empty());
    CHECK(bad.handle == nullptr);
}

TEST_CASE("The functions refuse NULL handles and NULL out pointers") {
    Hub hub(isolate().thread);
    REQUIRE(hub.ready);
    const auto thread = hub.thread;
    const std::string illegalArgument = "java.lang.IllegalArgumentException";

    Builder builder(thread);
    Filter filter(thread);
    Tracker tracker(thread);
    Calls calls;
    Listener listener(thread);
    REQUIRE(dxfg_v4_DXFeedFilter_newBuilder(thread, hub.endpoint, &builder.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedFilter_Builder_withCategory(thread, builder.handle, "a", "A*") == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedFilter_Builder_build(thread, builder.handle, &filter.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedFilter_getTracker(thread, filter.handle, &tracker.handle) == DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedFilterListener_new(thread, &Calls::onUpdated, &calls, &listener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    dxfg_v4_feed_filter_builder_t *newBuilder = nullptr;
    dxfg_v4_feed_filter_t *newFilter = nullptr;
    dxfg_v4_feed_filter_tracker_t *newTracker = nullptr;
    dxfg_v4_feed_filter_listener_t *newListener = nullptr;
    dxfg_v4_string_symbol_t symbol{{STRING}, "AAPL"};
    dxfg_v4_quote_t quote{};
    quote.market_event.event_type.clazz = DXFG_EVENT_QUOTE;
    quote.market_event.event_symbol = "AAPL";
    StringList names({"b"});
    StringList expressions({"B*"});
    int32_t value = -1;
    const char *category = "<unset>";

    const auto fails = [&](int32_t result) {
        return result == DXFG_EXECUTE_FAIL && takeExceptionClassName(thread) == illegalArgument;
    };

    CHECK(fails(dxfg_v4_DXFeedFilter_newBuilder(thread, nullptr, &newBuilder)));
    CHECK(fails(dxfg_v4_DXFeedFilter_newBuilder(thread, hub.endpoint, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilter_Builder_withCategory(thread, nullptr, "b", "B*")));
    CHECK(fails(dxfg_v4_DXFeedFilter_Builder_withCategories(thread, nullptr, &names.list, &expressions.list)));
    CHECK(fails(dxfg_v4_DXFeedFilter_Builder_build(thread, nullptr, &newFilter)));
    CHECK(fails(dxfg_v4_DXFeedFilter_Builder_build(thread, builder.handle, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilter_accept(thread, nullptr, DXFG_EVENT_QUOTE, &symbol.supper, &value)));
    CHECK(fails(dxfg_v4_DXFeedFilter_accept(thread, filter.handle, DXFG_EVENT_QUOTE, nullptr, &value)));
    CHECK(fails(dxfg_v4_DXFeedFilter_accept(thread, filter.handle, DXFG_EVENT_QUOTE, &symbol.supper, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilter_acceptEvent(thread, nullptr, &quote.market_event.event_type, &value)));
    CHECK(fails(dxfg_v4_DXFeedFilter_acceptEvent(thread, filter.handle, nullptr, &value)));
    CHECK(fails(dxfg_v4_DXFeedFilter_acceptEvent(thread, filter.handle, &quote.market_event.event_type, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilter_getCategory(thread, nullptr, DXFG_EVENT_QUOTE, &symbol.supper, &category)));
    CHECK(fails(dxfg_v4_DXFeedFilter_getCategory(thread, filter.handle, DXFG_EVENT_QUOTE, nullptr, &category)));
    CHECK(fails(dxfg_v4_DXFeedFilter_getCategory(thread, filter.handle, DXFG_EVENT_QUOTE, &symbol.supper, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilter_getEventCategory(thread, nullptr, &quote.market_event.event_type, &category)));
    CHECK(fails(dxfg_v4_DXFeedFilter_getEventCategory(thread, filter.handle, nullptr, &category)));
    CHECK(fails(dxfg_v4_DXFeedFilter_getEventCategory(thread, filter.handle, &quote.market_event.event_type, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilter_isDynamic(thread, nullptr, &value)));
    CHECK(fails(dxfg_v4_DXFeedFilter_isDynamic(thread, filter.handle, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilter_getTracker(thread, nullptr, &newTracker)));
    CHECK(fails(dxfg_v4_DXFeedFilter_getTracker(thread, filter.handle, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilterTracker_getCurrentFilter(thread, nullptr, &newFilter)));
    CHECK(fails(dxfg_v4_DXFeedFilterTracker_getCurrentFilter(thread, tracker.handle, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilterTracker_addListener(thread, nullptr, listener.handle)));
    CHECK(fails(dxfg_v4_DXFeedFilterTracker_addListener(thread, tracker.handle, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilterTracker_removeListener(thread, nullptr, listener.handle)));
    CHECK(fails(dxfg_v4_DXFeedFilterTracker_removeListener(thread, tracker.handle, nullptr)));
    CHECK(fails(dxfg_v4_DXFeedFilterListener_new(thread, nullptr, &calls, &newListener)));
    CHECK(fails(dxfg_v4_DXFeedFilterListener_new(thread, &Calls::onUpdated, &calls, nullptr)));

    // Nothing was written.
    CHECK(newBuilder == nullptr);
    CHECK(newFilter == nullptr);
    CHECK(newTracker == nullptr);
    CHECK(newListener == nullptr);
    CHECK(value == -1);
    CHECK(std::string(category) == "<unset>");
}

TEST_CASE("A dynamic filter calls its listener after an update until the listener is released") {
    Hub hub(isolate().thread);
    REQUIRE(hub.ready);
    const auto thread = hub.thread;

    DynamicFilter dynamic(thread, hub.endpoint, uniqueName("update"));
    REQUIRE(dynamic.ready);
    CHECK(isDynamic(thread, dynamic.filter.handle) == 1);
    CHECK(categoryOf(thread, dynamic.filter.handle, "IBM") == "ipf");
    CHECK(categoryOf(thread, dynamic.filter.handle, "MSFT") == "<none>");

    Calls calls;
    Listener listener(thread);
    REQUIRE(dxfg_v4_DXFeedFilterListener_new(thread, &Calls::onUpdated, &calls, &listener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedFilterTracker_addListener(thread, dynamic.tracker.handle, listener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dynamic.file.write({"MSFT"}));
    REQUIRE(calls.waitFor(1));
    {
        std::lock_guard<std::mutex> lock(calls.mutex);
        CHECK(calls.threads.front() != std::this_thread::get_id());
    }

    // The tracker has the new version; the old one does not change.
    Filter current(thread);
    REQUIRE(dxfg_v4_DXFeedFilterTracker_getCurrentFilter(thread, dynamic.tracker.handle, &current.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(categoryOf(thread, current.handle, "MSFT") == "ipf");
    CHECK(accepts(thread, current.handle, "IBM") == 0);
    CHECK(isDynamic(thread, current.handle) == 1);
    CHECK(categoryOf(thread, dynamic.filter.handle, "IBM") == "ipf");
    CHECK(categoryOf(thread, dynamic.filter.handle, "MSFT") == "<none>");

    // After the release of the listener (still added) its function is not called.
    listener.reset();
    const int before = calls.get();
    REQUIRE(dynamic.file.write({"AAPL"}));
    std::this_thread::sleep_for(QUIET_TIME);
    CHECK(calls.get() == before);
    CHECK(takeExceptionClassName(thread).empty());
}

TEST_CASE("A listener adds and removes listeners and releases itself from its call") {
    Hub hub(isolate().thread);
    REQUIRE(hub.ready);
    const auto thread = hub.thread;

    DynamicFilter dynamic(thread, hub.endpoint, uniqueName("reentrant"));
    REQUIRE(dynamic.ready);

    Calls first;
    Calls second;
    Listener firstListener(thread);
    Listener secondListener(thread);
    REQUIRE(dxfg_v4_DXFeedFilterListener_new(thread, &Calls::onUpdated, &first, &firstListener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedFilterListener_new(thread, &Calls::onUpdated, &second, &secondListener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    // The first call of the first listener adds the second one, removes itself and releases its own handle; the
    // calls run on the thread of the SDK, with the thread it gives.
    std::atomic<bool> callbackSucceeded{false};
    auto *tracker = dynamic.tracker.handle;
    auto *firstHandle = firstListener.handle;
    auto *secondHandle = secondListener.handle;
    first.action = [&callbackSucceeded, tracker, firstHandle, secondHandle](graal_isolatethread_t *callThread) {
        callbackSucceeded =
            dxfg_v4_DXFeedFilterTracker_addListener(callThread, tracker, secondHandle) == DXFG_EXECUTE_SUCCESSFULLY &&
            dxfg_v4_DXFeedFilterTracker_removeListener(callThread, tracker, firstHandle) == DXFG_EXECUTE_SUCCESSFULLY &&
            dxfg_v4_JavaObjectHandler_release(callThread, &firstHandle->handler) == DXFG_EXECUTE_SUCCESSFULLY;
    };
    REQUIRE(dxfg_v4_DXFeedFilterTracker_addListener(thread, dynamic.tracker.handle, firstListener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dynamic.file.write({"MSFT"}));
    REQUIRE(first.waitFor(1));
    firstListener.handle = nullptr; // released by its own call
    CHECK(callbackSucceeded);

    REQUIRE(dynamic.file.write({"AAPL"}));
    REQUIRE(second.waitFor(1));
    CHECK(first.get() == 1);

    CHECK(dxfg_v4_DXFeedFilterTracker_removeListener(thread, dynamic.tracker.handle, secondListener.handle) ==
          DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(takeExceptionClassName(thread).empty());
}

// The last case: deactivateAll() cannot be undone, so it runs in an isolate of its own.
TEST_CASE("No listener is called after deactivateAll") {
    Isolate own;
    REQUIRE(own.thread != nullptr);
    Hub hub(own.thread);
    REQUIRE(hub.ready);
    const auto thread = hub.thread;

    DynamicFilter dynamic(thread, hub.endpoint, uniqueName("deactivate"));
    REQUIRE(dynamic.ready);

    Calls calls;
    Listener listener(thread);
    REQUIRE(dxfg_v4_DXFeedFilterListener_new(thread, &Calls::onUpdated, &calls, &listener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    REQUIRE(dxfg_v4_DXFeedFilterTracker_addListener(thread, dynamic.tracker.handle, listener.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);

    REQUIRE(dynamic.file.write({"MSFT"}));
    REQUIRE(calls.waitFor(1));

    REQUIRE(dxfg_v4_NativeListener_deactivateAll(thread) == DXFG_EXECUTE_SUCCESSFULLY);
    const int before = calls.get();
    REQUIRE(dynamic.file.write({"AAPL"}));
    std::this_thread::sleep_for(QUIET_TIME);
    CHECK(calls.get() == before);

    // The tracker still follows the updates.
    Filter current(thread);
    REQUIRE(dxfg_v4_DXFeedFilterTracker_getCurrentFilter(thread, dynamic.tracker.handle, &current.handle) ==
            DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(categoryOf(thread, current.handle, "AAPL") == "ipf");
}

} // namespace test
} // namespace dxfg
