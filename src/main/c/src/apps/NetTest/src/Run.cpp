// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Run.hpp>
#include <NetTest/Histogram.hpp>
#include <NetTest/RateLimiter.hpp>
#include <NetTest/Sdk.hpp>

#include <algorithm>
#include <array>
#include <csignal>
#include <cstdlib>
#include <mutex>
#include <optional>
#include <string_view>
#include <unordered_map>

namespace dxfg {
namespace nettest {

namespace {

using Steady = std::chrono::steady_clock;

std::int64_t steadyNanos() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(Steady::now().time_since_epoch()).count();
}

std::int64_t systemNanos() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
               std::chrono::system_clock::now().time_since_epoch())
        .count();
}

// Releases a message connector list with its connectors.
struct ConnectorList {
    graal_isolatethread_t *thread;
    dxfg_v4_message_connector_list *list = nullptr;

    ConnectorList(graal_isolatethread_t *thread, dxfg_v4_endpoint_t *endpoint) : thread(thread) {
        if (dxfg_v4_DXEndpoint_getConnectors(thread, endpoint, &list) != DXFG_EXECUTE_SUCCESSFULLY) {
            takeThreadException(thread);
            list = nullptr;
        }
    }

    ConnectorList(const ConnectorList &) = delete;
    ConnectorList &operator=(const ConnectorList &) = delete;

    ~ConnectorList() {
        if (list != nullptr) {
            dxfg_v4_message_connector_list_release(thread, list);
        }
    }

    std::int32_t size() const {
        return list == nullptr ? 0 : list->size;
    }

    dxfg_v4_message_connector_t *operator[](std::int32_t index) const {
        return list->elements[index];
    }
};

// One endpoint of the tool with its counters. The endpoint is closed and its handle is released at the destruction, on
// the thread that created the instance, after the handles of the subclass.
class Endpoint {
  protected:
    graal_isolatethread_t *thread_;
    dxfg_v4_endpoint_t *endpoint_ = nullptr;

  public:
    // The counters of the instance in the live counters of the run.
    InstanceCounters &counters;

    Endpoint(graal_isolatethread_t *thread, InstanceCounters &counters) : thread_(thread), counters(counters) {
    }

    Endpoint(const Endpoint &) = delete;
    Endpoint &operator=(const Endpoint &) = delete;

    virtual ~Endpoint() {
        if (endpoint_ != nullptr) {
            dxfg_v4_DXEndpoint_closeAndAwaitTermination(thread_, endpoint_);
            dxfg_v4_JavaObjectHandler_release(thread_, &endpoint_->handler);
        }
    }

    // Builds the endpoint of the role and the name with the value of dxscheme.nanoTime on its builder; returns false
    // and fills the error on a failure.
    bool build(dxfg_v4_endpoint_role_t role, const std::string &name, const std::string &nanoTime,
               std::string &error) {
        dxfg_v4_endpoint_builder_t *builder = nullptr;

        if (dxfg_v4_DXEndpoint_newBuilder(thread_, &builder) != DXFG_EXECUTE_SUCCESSFULLY) {
            error = "cannot create the endpoint " + name + ": " + takeThreadException(thread_);

            return false;
        }

        const bool built =
            dxfg_v4_DXEndpoint_Builder_withRole(thread_, builder, role) == DXFG_EXECUTE_SUCCESSFULLY &&
            dxfg_v4_DXEndpoint_Builder_withName(thread_, builder, name.c_str()) == DXFG_EXECUTE_SUCCESSFULLY &&
            dxfg_v4_DXEndpoint_Builder_withProperty(thread_, builder, NANO_TIME_PROPERTY, nanoTime.c_str()) ==
                DXFG_EXECUTE_SUCCESSFULLY &&
            dxfg_v4_DXEndpoint_Builder_build(thread_, builder, &endpoint_) == DXFG_EXECUTE_SUCCESSFULLY;

        if (!built) {
            endpoint_ = nullptr;
            error = "cannot create the endpoint " + name + ": " + takeThreadException(thread_);
        }

        dxfg_v4_JavaObjectHandler_release(thread_, &builder->handler);

        return built;
    }

    // Connects the endpoint. A malformed address is a failure. A server socket must bind: the connector of a bound
    // server socket reports itself as connected within milliseconds, one that cannot bind (the port is taken) stays
    // connecting (QD logs the BindException, the SDK records no connection error for it), so the start waits until
    // every server socket connector is connected, at most SERVER_START_TIMEOUT. A client address never waits: with no
    // peer the connector stays connecting, which is a normal state.
    bool connect(const std::string &name, const std::string &address, std::string &error) {
        if (dxfg_v4_DXEndpoint_connect(thread_, endpoint_, address.c_str()) != DXFG_EXECUTE_SUCCESSFULLY) {
            error = "cannot connect " + name + " to '" + address + "': " + takeThreadException(thread_);

            return false;
        }

        const auto deadline = Steady::now() + SERVER_START_TIMEOUT;

        for (;;) {
            bool pending = false;
            const ConnectorList connectors(thread_, endpoint_);

            for (std::int32_t index = 0; index < connectors.size(); ++index) {
                std::int32_t kind = DXFG_V4_MESSAGE_CONNECTOR_KIND_OTHER;
                std::int32_t state = DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTING;

                if (dxfg_v4_MessageConnector_getKind(thread_, connectors[index], &kind) != DXFG_EXECUTE_SUCCESSFULLY ||
                    kind != DXFG_V4_MESSAGE_CONNECTOR_KIND_SERVER_SOCKET) {
                    takeThreadException(thread_);

                    continue;
                }

                if (dxfg_v4_MessageConnector_getState(thread_, connectors[index], &state) ==
                        DXFG_EXECUTE_SUCCESSFULLY &&
                    state != DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED) {
                    pending = true;
                }
            }

            if (!pending) {
                return true;
            }

            if (Steady::now() >= deadline) {
                error = "the server socket '" + address + "' (" + name + ") did not start in " +
                        std::to_string(SERVER_START_TIMEOUT.count()) + " ms (is the port already in use?)";

                return false;
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(5));
        }
    }

    // The open connections of all connectors of the endpoint.
    std::int64_t connections(graal_isolatethread_t *readingThread) const {
        std::int64_t total = 0;
        const ConnectorList connectors(readingThread, endpoint_);

        for (std::int32_t index = 0; index < connectors.size(); ++index) {
            std::int32_t count = 0;

            if (dxfg_v4_MessageConnector_getConnectionCount(readingThread, connectors[index], &count) ==
                DXFG_EXECUTE_SUCCESSFULLY) {
                total += count;
            } else {
                takeThreadException(readingThread);
            }
        }

        return total;
    }

    // The statistics of the connectors of the endpoint: v2 (one call that has v1 too) when asked, else v1; with their
    // addresses when asked. A connector whose statistics cannot be read is left out.
    std::vector<ConnectorSummary> connectorStatistics(graal_isolatethread_t *readingThread, bool v2,
                                                      bool withAddress) const {
        std::vector<ConnectorSummary> summaries;
        const ConnectorList connectors(readingThread, endpoint_);

        for (std::int32_t index = 0; index < connectors.size(); ++index) {
            ConnectorSummary summary;
            // Both structs name the values of v1 alike.
            const auto copyV1 = [&summary](const auto &stats) {
                summary.readBytes = stats.read_bytes;
                summary.writtenBytes = stats.written_bytes;
                summary.readDataRecords = stats.read_data_records;
                summary.writtenDataRecords = stats.written_data_records;
                summary.activeConnections = stats.active_connection_count;
                summary.closedConnections = stats.closed_connection_count;
            };

            if (v2) {
                dxfg_v4_message_connector_io_stats_t stats{};

                if (dxfg_v4_MessageConnector_getIoStatistics(readingThread, connectors[index], &stats) !=
                    DXFG_EXECUTE_SUCCESSFULLY) {
                    takeThreadException(readingThread);

                    continue;
                }

                copyV1(stats);
                summary.v2 = ConnectorSummaryV2{stats.read_sub_records,     stats.written_sub_records,
                                                stats.read_data_lag_sum_us, stats.written_data_lag_sum_us,
                                                stats.read_rtt_sum_us,      stats.written_rtt_sum_us};
            } else {
                dxfg_v4_message_connector_stats_t stats{};

                if (dxfg_v4_MessageConnector_getStatistics(readingThread, connectors[index], &stats) !=
                    DXFG_EXECUTE_SUCCESSFULLY) {
                    takeThreadException(readingThread);

                    continue;
                }

                copyV1(stats);
            }

            if (withAddress) {
                const char *address = nullptr;

                if (dxfg_v4_MessageConnector_getAddress(readingThread, connectors[index], &address) ==
                    DXFG_EXECUTE_SUCCESSFULLY) {
                    if (address != nullptr) {
                        summary.address = address;
                        dxfg_v4_String_release(readingThread, address);
                    }
                } else {
                    takeThreadException(readingThread);
                }
            }

            summaries.push_back(std::move(summary));
        }

        return summaries;
    }
};

// One publisher endpoint with its event pool.
class Instance final : public Endpoint {
    dxfg_v4_publisher_t *publisher_ = nullptr;

  public:
    EventPool pool;

    Instance(graal_isolatethread_t *thread, InstanceCounters &counters, EventKind kind, const Symbols &symbols,
             std::uint64_t seed)
        : Endpoint(thread, counters), pool(kind, symbols, seed) {
    }

    ~Instance() override {
        if (publisher_ != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread_, &publisher_->handler);
        }
    }

    dxfg_v4_publisher_t *publisher() const {
        return publisher_;
    }

    // Creates the endpoint of the role and its publisher; returns false and fills the error on a failure.
    bool create(dxfg_v4_endpoint_role_t role, const std::string &name, const std::string &nanoTime,
                std::string &error) {
        if (!build(role, name, nanoTime, error)) {
            return false;
        }

        if (dxfg_v4_DXEndpoint_getPublisher(thread_, endpoint_, &publisher_) != DXFG_EXECUTE_SUCCESSFULLY) {
            publisher_ = nullptr;
            error = "cannot get the publisher of the endpoint " + name + ": " + takeThreadException(thread_);

            return false;
        }

        return true;
    }
};

// The e2e lags of one interval of all consumers, with the signs; the maximum is valid when the count is not 0.
struct IntervalLags {
    std::int64_t count = 0;
    std::int64_t negative = 0;
    std::int64_t sum = 0;
    std::int64_t max = 0;
};

// The e2e lags of the current interval: the listeners record them, the reporter takes them at the end of the interval.
class LagRecorder {
    std::mutex mutex_{};
    std::unique_ptr<Histogram> current_ = std::make_unique<Histogram>();
    IntervalLags lags_{};
    // The histogram of the interval taken last; only the reporter touches it.
    std::unique_ptr<Histogram> taken_ = std::make_unique<Histogram>();

  public:
    void record(const std::vector<std::int64_t> &lags) {
        std::lock_guard<std::mutex> lock(mutex_);

        for (const auto lag : lags) {
            // The histogram takes a negative lag as 0.
            current_->record(lag);
            lags_.max = lags_.count == 0 ? lag : std::max(lags_.max, lag);
            lags_.sum += lag;
            lags_.negative += lag < 0 ? 1 : 0;
            ++lags_.count;
        }
    }

    // Takes the lags of the interval and starts the next one; the histogram stays valid until the next call.
    std::pair<IntervalLags, const Histogram *> take() {
        taken_->reset();

        std::lock_guard<std::mutex> lock(mutex_);
        const auto lags = lags_;

        std::swap(current_, taken_);
        lags_ = {};

        return {lags, taken_.get()};
    }
};

// What the consumer instances share: the check of the nanosecond part over the first events of all instances, the time
// of the last record for the watchdog, the lags and the symbols seen. The listeners write it on the threads of the SDK.
class ConsumerState {
  public:
    LiveCounters &live;
    LagRecorder lags{};
    // The wall-clock time of the first listener call with records, 0 before it: the start of the warm-up on `c`.
    std::atomic<std::int64_t> firstRecordEpochNanos{0};
    // The distinct symbols received by all instances.
    std::atomic<std::int64_t> symbolsSeen{0};

  private:
    StopControl &stop_;
    const bool nanoCheck_;
    std::mutex nanoMutex_{};
    std::int64_t nanoChecked_ = 0;
    bool nanoSeen_ = false;
    std::atomic<bool> nanoPending_;
    std::atomic<bool> nanoMissing_{false};
    // The steady-clock time of the last listener call with records, 0 before the first one.
    std::atomic<std::int64_t> lastRecordNanos_{0};

  public:
    ConsumerState(LiveCounters &live, StopControl &stop, bool nanoCheck)
        : live(live), stop_(stop), nanoCheck_(nanoCheck), nanoPending_(nanoCheck) {
    }

    bool nanoPending() const {
        return nanoPending_.load(std::memory_order_relaxed);
    }

    bool nanoMissing() const {
        return nanoMissing_.load();
    }

    std::int64_t lastRecordNanos() const {
        return lastRecordNanos_.load(std::memory_order_relaxed);
    }

    void recordsArrived() {
        lastRecordNanos_.store(steadyNanos(), std::memory_order_relaxed);
    }

    // Counts the nanosecond parts of the events of one listener call, in their order, up to NANO_CHECK_EVENTS events
    // in all; when they are all 0, stops the run.
    void checkNanoParts(const std::vector<std::int32_t> &nanoParts) {
        std::lock_guard<std::mutex> lock(nanoMutex_);

        if (!nanoCheck_ || !nanoPending_.load()) {
            return;
        }

        for (const auto nanoPart : nanoParts) {
            if (nanoChecked_ >= NANO_CHECK_EVENTS) {
                break;
            }

            ++nanoChecked_;
            nanoSeen_ = nanoSeen_ || nanoPart != 0;
        }

        if (nanoSeen_) {
            nanoPending_.store(false);
        } else if (nanoChecked_ >= NANO_CHECK_EVENTS) {
            nanoPending_.store(false);
            nanoMissing_.store(true);
            stop_.request();
        }
    }
};

// One feed endpoint with its subscription to a slice of the symbols. The listener counts the records, sums their e2e
// lags, records them and marks their symbols; it runs on the threads of the SDK.
class Consumer final : public Endpoint {
    EventKind kind_;
    ConsumerState &state_;
    dxfg_v4_feed_t *feed_ = nullptr;
    dxfg_v4_subscription_t *subscription_ = nullptr;
    dxfg_v4_feed_event_listener_t *listener_ = nullptr;
    // The slice of the symbols and which of them came.
    std::vector<std::string> symbols_{};
    std::unordered_map<std::string_view, std::size_t> symbolIndex_{};
    std::mutex seenMutex_{};
    std::vector<bool> seen_{};
    std::size_t seenCount_ = 0;
    std::atomic<bool> allSeen_{false};

    static void onEvents(graal_isolatethread_t *, dxfg_event_type_list *events, void *userData) {
        static_cast<Consumer *>(userData)->receive(events);
    }

    // Marks a received symbol; the caller holds seenMutex_.
    void markSeen(const char *symbol) {
        if (symbol == nullptr) {
            return;
        }

        const auto found = symbolIndex_.find(std::string_view(symbol));

        if (found == symbolIndex_.end() || seen_[found->second]) {
            return;
        }

        seen_[found->second] = true;
        ++seenCount_;
        state_.symbolsSeen.fetch_add(1, std::memory_order_relaxed);

        if (seenCount_ == seen_.size()) {
            allSeen_.store(true, std::memory_order_relaxed);
        }
    }

    void receive(const dxfg_event_type_list *events) {
        // One clock read per call of the listener.
        const auto now = systemNanos();
        const bool isTrade = kind_ == EventKind::TRADE;
        const auto expectedClass = isTrade ? DXFG_EVENT_TRADE : DXFG_EVENT_TIME_AND_SALE;
        const bool nanoPending = state_.nanoPending();
        std::vector<std::int32_t> nanoParts{};
        std::vector<std::int64_t> lags{};
        std::array<std::int64_t, LAG_BUCKET_BOUNDS_NANOS.size() + 1> buckets{};
        std::int64_t records = 0;
        std::int64_t lagSum = 0;
        std::int64_t positiveSum = 0;
        std::int64_t negative = 0;
        // Once every symbol of the slice came, the symbols are not looked up anymore.
        std::unique_lock<std::mutex> seenLock(seenMutex_, std::defer_lock);
        const bool trackSymbols = !allSeen_.load(std::memory_order_relaxed);

        if (trackSymbols) {
            seenLock.lock();
        }

        lags.reserve(static_cast<std::size_t>(std::max(events->size, 0)));

        for (std::int32_t index = 0; index < events->size; ++index) {
            const dxfg_event_type_t *event = events->elements[index];

            if (event == nullptr || event->clazz != expectedClass) {
                continue;
            }

            std::int64_t packed = 0;
            std::int32_t nanoPart = 0;
            const char *symbol = nullptr;

            if (isTrade) {
                const auto *trade = reinterpret_cast<const dxfg_trade_t *>(event);

                packed = trade->trade_base.time_sequence;
                nanoPart = trade->trade_base.time_nano_part;
                symbol = trade->trade_base.market_event.event_symbol;
            } else {
                const auto *sale = reinterpret_cast<const dxfg_time_and_sale_t *>(event);

                packed = sale->index;
                nanoPart = sale->time_nano_part;
                symbol = sale->market_event.event_symbol;
            }

            // A negative lag (a clock step or skew between the hosts) enters the sum with its sign, the buckets and the
            // histogram as 0.
            const auto lag = eventLagNanos(now, packed, nanoPart);

            ++records;
            lagSum += lag;
            lags.push_back(lag);
            ++buckets[lagBucketIndex(lag)];

            if (lag < 0) {
                ++negative;
            } else {
                positiveSum += lag;
            }

            if (trackSymbols) {
                markSeen(symbol);
            }

            if (nanoPending) {
                nanoParts.push_back(nanoPart);
            }
        }

        if (trackSymbols) {
            seenLock.unlock();
        }

        if (records == 0) {
            return;
        }

        std::int64_t unset = 0;

        state_.firstRecordEpochNanos.compare_exchange_strong(unset, now, std::memory_order_relaxed);
        counters.records.fetch_add(records, std::memory_order_relaxed);
        counters.weightedNanos.fetch_add(lagSum, std::memory_order_relaxed);
        state_.live.addLags(buckets, records, positiveSum, negative);
        state_.lags.record(lags);
        state_.recordsArrived();

        if (nanoPending) {
            state_.checkNanoParts(nanoParts);
        }
    }

  public:
    Consumer(graal_isolatethread_t *thread, InstanceCounters &counters, EventKind kind, ConsumerState &state)
        : Endpoint(thread, counters), kind_(kind), state_(state) {
    }

    ~Consumer() override {
        // The subscription first, then the listener: its release waits for the calls in progress, after which the
        // function is not called anymore.
        if (subscription_ != nullptr) {
            dxfg_v4_DXFeedSubscription_close(thread_, subscription_);
        }

        if (listener_ != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread_, &listener_->handler);
        }

        if (subscription_ != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread_, &subscription_->handler);
        }

        if (feed_ != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread_, &feed_->handler);
        }
    }

    // Creates the endpoint of the role, the subscription for the event, its listener and then its symbols; returns
    // false and fills the error on a failure. The endpoint connects later (Endpoint::connect()).
    bool create(dxfg_v4_endpoint_role_t role, const std::string &name, const std::string &nanoTime,
                const std::vector<std::string> &symbols, std::string &error) {
        // The index of the slice before the listener, whose calls read it.
        symbols_ = symbols;
        seen_.assign(symbols_.size(), false);
        allSeen_.store(symbols_.empty());

        for (std::size_t index = 0; index < symbols_.size(); ++index) {
            symbolIndex_.emplace(std::string_view(symbols_[index]), index);
        }

        if (!build(role, name, nanoTime, error)) {
            return false;
        }

        const auto eventClass = kind_ == EventKind::TRADE ? DXFG_EVENT_TRADE : DXFG_EVENT_TIME_AND_SALE;
        const bool subscribed =
            dxfg_v4_DXEndpoint_getFeed(thread_, endpoint_, &feed_) == DXFG_EXECUTE_SUCCESSFULLY &&
            dxfg_v4_DXFeed_createSubscription(thread_, feed_, eventClass, &subscription_) ==
                DXFG_EXECUTE_SUCCESSFULLY &&
            dxfg_v4_DXFeedEventListener_new(thread_, &Consumer::onEvents, this, &listener_) ==
                DXFG_EXECUTE_SUCCESSFULLY &&
            dxfg_v4_DXFeedSubscription_addEventListener(thread_, subscription_, listener_) ==
                DXFG_EXECUTE_SUCCESSFULLY;

        if (!subscribed) {
            error = "cannot subscribe the endpoint " + name + ": " + takeThreadException(thread_);

            return false;
        }

        std::vector<dxfg_v4_string_symbol_t> strings;
        std::vector<dxfg_v4_symbol_t *> elements;

        strings.reserve(symbols.size());
        elements.reserve(symbols.size());

        for (const auto &symbol : symbols) {
            strings.push_back(dxfg_v4_string_symbol_t{{STRING}, symbol.c_str()});
        }

        for (auto &string : strings) {
            elements.push_back(&string.supper);
        }

        dxfg_v4_symbol_list list{static_cast<std::int32_t>(elements.size()), elements.data()};

        if (dxfg_v4_DXFeedSubscription_addSymbols(thread_, subscription_, &list) != DXFG_EXECUTE_SUCCESSFULLY) {
            error = "cannot add the symbols of the endpoint " + name + ": " + takeThreadException(thread_);

            return false;
        }

        return true;
    }
};

// The runtime values of the isolate; all absent when the call fails.
RuntimeSample readRuntime(graal_isolatethread_t *thread) {
    dxfg_v4_runtime_stats_t stats{};
    RuntimeSample sample;

    if (dxfg_v4_system_get_runtime_statistics(thread, &stats) != DXFG_EXECUTE_SUCCESSFULLY) {
        takeThreadException(thread);

        return sample;
    }

    sample.heapUsedBytes = stats.heap_used_bytes;
    sample.heapCommittedBytes = stats.heap_committed_bytes;
    sample.youngGcCount = stats.young_gc_count;
    sample.youngGcTimeMs = stats.young_gc_time_ms;
    sample.fullGcCount = stats.full_gc_count;
    sample.fullGcTimeMs = stats.full_gc_time_ms;
    sample.processCpuTimeNs = stats.process_cpu_time_ns;

    return sample;
}

// A thread attached to the isolate for its whole body: attaches once, detaches at the exit.
template <typename Function> std::thread attachedThread(graal_isolate_t *isolate, Function function) {
    return std::thread([isolate, function = std::move(function)] {
        graal_isolatethread_t *thread = nullptr;

        if (graal_attach_thread(isolate, &thread) != 0) {
            function(static_cast<graal_isolatethread_t *>(nullptr));

            return;
        }

        function(thread);
        graal_detach_thread(thread);
    });
}

// The records of the options as the batch of a generator.
GeneratorSettings generatorSettings(const Options &options, const RunHooks &hooks) {
    GeneratorSettings settings;

    settings.batch = static_cast<std::size_t>(options.batch);
    settings.reuse = options.reuse;
    settings.legacy = options.legacy;
    settings.publishDelay = hooks.publishDelay;

    return settings;
}

// The starvation watchdog of the consumer, checked by the waiting thread of the run.
class Watchdog {
    const RunHooks &hooks_;
    // The steady-clock time since which the connections are open, unset while there are none.
    std::optional<std::int64_t> connectedSince_{};

  public:
    explicit Watchdog(const RunHooks &hooks) : hooks_(hooks) {
    }

    // Returns the message of the starvation, or an empty string.
    std::string check(std::int64_t connections, std::int64_t lastRecordNanos) {
        const auto now = steadyNanos();

        if (connections <= 0) {
            connectedSince_.reset();

            return {};
        }

        if (!connectedSince_.has_value()) {
            connectedSince_ = now;
        }

        const auto toNanos = [](std::chrono::milliseconds time) {
            return std::chrono::duration_cast<std::chrono::nanoseconds>(time).count();
        };

        if (lastRecordNanos == 0) {
            if (now - *connectedSince_ >= toNanos(hooks_.noDataAfterConnect)) {
                return "no records for " + std::to_string(hooks_.noDataAfterConnect.count()) +
                       " ms after the connection, with " + std::to_string(connections) + " open connections";
            }

            return {};
        }

        if (now - std::max(lastRecordNanos, *connectedSince_) >= toNanos(hooks_.noDataAfterRecord)) {
            return "no records for " + std::to_string(hooks_.noDataAfterRecord.count()) +
                   " ms after the last record, with " + std::to_string(connections) + " open connections";
        }

        return {};
    }
};

// How often the waiting thread of the run checks the watchdog.
constexpr std::chrono::milliseconds WATCHDOG_PERIOD{200};

// Whether the SDK has the connector statistics v2 (dxfg_v4_MessageConnector_getIoStatistics()); a failed check is no.
bool hasConnectorStatisticsV2(graal_isolatethread_t *thread) {
    std::int32_t supported = 0;

    if (dxfg_v4_system_is_feature_supported(thread, DXFG_V4_FEATURE_MESSAGE_CONNECTOR_IO_STATISTICS, &supported) !=
        DXFG_EXECUTE_SUCCESSFULLY) {
        takeThreadException(thread);

        return false;
    }

    return supported != 0;
}

} // namespace

std::string runStatusName(RunStatus status) {
    switch (status) {
    case RunStatus::OK:
        return "ok";
    case RunStatus::FAILED:
        return "error";
    case RunStatus::STARVED:
        return "no_data";
    case RunStatus::NANO_MISSING:
        return "nano_missing";
    }

    return "error";
}

std::vector<std::string> consumerSlice(const std::vector<std::string> &symbols, std::int32_t instance,
                                       std::int32_t instances) {
    std::vector<std::string> slice;

    if (instances < 1 || instance < 1 || instance > instances) {
        return slice;
    }

    for (std::size_t index = static_cast<std::size_t>(instance - 1); index < symbols.size();
         index += static_cast<std::size_t>(instances)) {
        slice.push_back(symbols[index]);
    }

    return slice;
}

std::int64_t eventLagNanos(std::int64_t nowNanos, std::int64_t packedTime, std::int32_t nanoPart) {
    const auto timeMillis = (packedTime >> 32) * 1000 + ((packedTime >> 22) & 0x3FF);

    return nowNanos - (timeMillis * 1'000'000 + nanoPart);
}

RunResult runNetTest(graal_isolatethread_t *thread, const Options &options, const Symbols &symbols, StopControl &stop,
                     std::ostream &out, const RunHooks &hooks, LiveCounters *live) {
    RunResult result;
    result.report.startEpochNs = systemNanos();
    graal_isolate_t *isolate = graal_get_isolate(thread);
    const bool producer = options.mode == Mode::PRODUCER;
    const auto role = endpointRole(options);
    const auto nanoTime = endpointNanoTime(options);
    const bool connectorStatisticsV2 = hooks.connectorStatisticsV2 && hasConnectorStatisticsV2(thread);
    std::unique_ptr<LiveCounters> ownCounters;

    if (live == nullptr || live->instanceCount < options.instances) {
        ownCounters = std::make_unique<LiveCounters>(options.instances);
        live = ownCounters.get();
    }

    LiveCounters &counters = *live;
    ConsumerState consumerState(counters, stop, isNanoTimeOn(options));
    std::vector<std::unique_ptr<Instance>> instances;
    std::vector<std::unique_ptr<Consumer>> consumers;
    std::vector<Endpoint *> endpoints;
    std::string error;

    const auto failStart = [&result](const std::string &message) {
        result.exitCode = EXIT_FAILURE;
        result.status = RunStatus::FAILED;
        result.error = message;
        result.report.endEpochNs = systemNanos();

        return result;
    };

    // An instance that failed to start is destroyed when this function returns, with the endpoints created before it.
    for (std::int32_t index = 1; index <= options.instances; ++index) {
        const auto name = options.name + "." + std::to_string(index);
        auto &instanceCounters = counters.instances[static_cast<std::size_t>(index - 1)];

        if (producer) {
            auto instance = std::make_unique<Instance>(thread, instanceCounters, options.event, symbols,
                                                       0x9E3779B97F4A7C15ULL * static_cast<std::uint64_t>(index));
            const bool started =
                instance->create(role, name, nanoTime, error) && instance->connect(name, options.address, error);

            endpoints.push_back(instance.get());
            instances.push_back(std::move(instance));

            if (!started) {
                return failStart(error);
            }
        } else {
            // The listener comes before the symbols, and the endpoint connects last.
            auto consumer = std::make_unique<Consumer>(thread, instanceCounters, options.event, consumerState);
            const bool started =
                consumer->create(role, name, nanoTime, consumerSlice(*symbols, index, options.instances), error) &&
                consumer->connect(name, options.address, error);

            endpoints.push_back(consumer.get());
            consumers.push_back(std::move(consumer));

            if (!started) {
                return failStart(error);
            }
        }
    }

    const auto settings = generatorSettings(options, hooks);
    std::unique_ptr<RateLimiter> limiter;

    if (producer && options.recordsPerSecond.has_value()) {
        limiter = std::make_unique<RateLimiter>(*options.recordsPerSecond, options.batch, stop);
    }

    std::mutex failureMutex;
    std::string failure;
    RunStatus failureStatus = RunStatus::FAILED;
    auto fail = [&](const std::string &message, RunStatus status = RunStatus::FAILED) {
        {
            std::lock_guard<std::mutex> lock(failureMutex);

            if (failure.empty()) {
                failure = message;
                failureStatus = status;
            }
        }

        stop.request();
    };

    // What the reporter collects for the summary; read after it is joined.
    const Side side = producer ? Side::PRODUCER : Side::CONSUMER;
    const bool scheduled = limiter != nullptr;
    const auto warmupNanos = options.warmupMillis.value_or(0) * 1'000'000;
    std::vector<IntervalRecord> intervals;
    // The lags of the measured intervals.
    const auto lagTotal = std::make_unique<Histogram>();
    IntervalLags lagTotals{};
    std::optional<RuntimeSample> firstRuntime;
    std::optional<RuntimeSample> lastRuntime;
    std::int64_t firstRuntimeNanos = 0;
    std::int64_t lastRuntimeNanos = 0;
    std::int64_t lastPublishNanos = 0;

    // The warm-up starts at the first batch (`p`) or the first record (`c`).
    const auto isWarmup = [&](std::int64_t startNanos) {
        if (!options.warmupMillis.has_value()) {
            return false;
        }

        const auto warmupStart = producer ? counters.publish.firstBatchEpochNanos.load(std::memory_order_relaxed)
                                          : consumerState.firstRecordEpochNanos.load(std::memory_order_relaxed);

        return isWarmupInterval(warmupNanos, warmupStart, startNanos);
    };

    // The reporter reads the counters and the connections of the instances, the statistics of their connectors and the
    // runtime values of the isolate, keeps them in the live counters and collects the intervals; it stops after the
    // generators.
    StopControl reporterStop;
    std::thread reporter = attachedThread(isolate, [&](graal_isolatethread_t *reporterThread) {
        if (reporterThread == nullptr) {
            fail("cannot attach the statistics thread to the isolate");

            return;
        }

        runReporter(
            options.name, options.periodMillis, systemReporterClock(reporterStop),
            [&] {
                StatisticsSample sample;
                ConnectorTotals totals;

                if (connectorStatisticsV2) {
                    totals.subRecords = 0;
                    totals.readLagMicros = 0;
                    totals.rttMicros = 0;
                }

                for (std::size_t index = 0; index < endpoints.size(); ++index) {
                    const auto *endpoint = endpoints[index];
                    const auto connections = endpoint->connections(reporterThread);

                    sample.instances.push_back({endpoint->counters.records.load(std::memory_order_relaxed),
                                                endpoint->counters.weightedNanos.load(std::memory_order_relaxed)});
                    sample.connections += connections;
                    counters.connections[index].store(connections, std::memory_order_relaxed);

                    for (const auto &connector :
                         endpoint->connectorStatistics(reporterThread, connectorStatisticsV2, false)) {
                        totals.readBytes += connector.readBytes;
                        totals.writtenBytes += connector.writtenBytes;
                        totals.readRecords += connector.readDataRecords;
                        totals.writtenRecords += connector.writtenDataRecords;

                        if (connector.v2.has_value()) {
                            totals.subRecords += connector.v2->readSubRecords + connector.v2->writtenSubRecords;
                            totals.readLagMicros += connector.v2->readDataLagMicros;
                            totals.rttMicros += connector.v2->readRttMicros + connector.v2->writtenRttMicros;
                        }
                    }
                }

                const auto runtime = readRuntime(reporterThread);
                const auto now = systemNanos();

                counters.setConnectors(totals);
                counters.setRuntime(runtime);
                sample.runtime = runtime;

                if (!firstRuntime.has_value()) {
                    firstRuntime = runtime;
                    firstRuntimeNanos = now;
                }

                lastRuntime = runtime;
                lastRuntimeNanos = now;

                return sample;
            },
            [&](const std::string &text) {
                out << text << std::flush;
            },
            side,
            [&](const StatisticsSample &previous, const StatisticsSample &current, std::int64_t startNanos,
                std::int64_t endNanos) {
                IntervalRecord interval;

                interval.startEpochMs = startNanos / 1'000'000;
                interval.endEpochMs = endNanos / 1'000'000;
                interval.warmup = isWarmup(startNanos);
                interval.connections = current.connections;

                for (const auto &delta : makeSnapshot(options.name, previous, current, endNanos - startNanos, side).deltas) {
                    interval.records += delta.records;
                }

                interval.rate = recordsPerSecond(interval.records, endNanos - startNanos);

                if (producer) {
                    const auto publishNanos = counters.publish.nanos.load(std::memory_order_relaxed);

                    if (interval.records > 0) {
                        interval.publishNsPerEvent = (publishNanos - lastPublishNanos) / interval.records;
                    }

                    lastPublishNanos = publishNanos;

                    if (scheduled) {
                        interval.slipMaxNs = counters.publish.slipIntervalMaxNanos.exchange(0, std::memory_order_relaxed);
                        interval.slipEndNs = counters.publish.slipLastNanos.load(std::memory_order_relaxed);
                    }
                } else {
                    const auto [lags, histogram] = consumerState.lags.take();

                    if (lags.count > 0) {
                        // C++ divides toward zero, as the specification wants for a negative sum.
                        interval.lagMeanNs = lags.sum / lags.count;
                        interval.lagP50Ns = histogram->valueAtPercentile(50.0);
                        interval.lagP99Ns = histogram->valueAtPercentile(99.0);
                        interval.lagMaxNs = lags.max;

                        if (!interval.warmup) {
                            lagTotal->add(*histogram);
                            lagTotals.max = lagTotals.count == 0 ? lags.max : std::max(lagTotals.max, lags.max);
                            lagTotals.count += lags.count;
                            lagTotals.sum += lags.sum;
                            lagTotals.negative += lags.negative;
                        }
                    }
                }

                intervals.push_back(interval);
            });
    });

    std::vector<std::thread> generators;

    for (const auto &instance : instances) {
        generators.push_back(attachedThread(isolate, [&, current = instance.get()](graal_isolatethread_t *generatorThread) {
            if (generatorThread == nullptr) {
                fail("cannot attach a generator thread to the isolate");

                return;
            }

            const auto generated = runGenerator(generatorThread, current->publisher(), current->pool, settings,
                                                limiter.get(), stop, current->counters, counters.publish);

            if (!generated.ok) {
                fail("publishing failed: " + generated.error);
            }
        }));
    }

    // The thread of the caller waits for the stop or the duration. `p` keeps the one wait it had before the mode `c`;
    // `c` wakes every WATCHDOG_PERIOD to count the connections for the watchdog.
    if (producer) {
        if (options.durationMillis.has_value()) {
            stop.waitFor(std::chrono::milliseconds(*options.durationMillis));
        } else {
            stop.wait();
        }
    } else {
        const auto deadline =
            options.durationMillis.has_value()
                ? std::optional<Steady::time_point>(Steady::now() + std::chrono::milliseconds(*options.durationMillis))
                : std::nullopt;
        Watchdog watchdog(hooks);

        while (!stop.stopped()) {
            auto wait = WATCHDOG_PERIOD;

            if (deadline.has_value()) {
                const auto left = std::chrono::duration_cast<std::chrono::milliseconds>(*deadline - Steady::now());

                if (left.count() <= 0) {
                    break;
                }

                wait = std::min(wait, left);
            }

            if (stop.waitFor(wait)) {
                break;
            }

            std::int64_t connections = 0;

            for (const auto &consumer : consumers) {
                connections += consumer->connections(thread);
            }

            if (const auto starved = watchdog.check(connections, consumerState.lastRecordNanos()); !starved.empty()) {
                fail(starved, RunStatus::STARVED);
            }
        }
    }

    // The generators first, then the last statistics, then the endpoints.
    stop.request();

    for (auto &generator : generators) {
        generator.join();
    }

    reporterStop.request();
    reporter.join();

    for (const auto *endpoint : endpoints) {
        const auto records = endpoint->counters.records.load();

        result.instanceRecords.push_back(records);
        result.records += records;
    }

    // The report of the summary, before the endpoints close.
    auto &report = result.report;

    report.endEpochNs = systemNanos();
    report.intervals = std::move(intervals);

    for (const auto &interval : report.intervals) {
        report.records += interval.records;
    }

    for (const auto *endpoint : endpoints) {
        for (auto &connector : endpoint->connectorStatistics(thread, connectorStatisticsV2, true)) {
            report.connectors.push_back(std::move(connector));
        }
    }

    if (producer) {
        report.publish = PublishSummary{counters.publish.calls.load(), result.records, counters.publish.nanos.load()};

        if (scheduled) {
            report.slip = slipSummary(report.intervals, counters.publish.slipLastNanos.load());
        }
    } else {
        LagSummary lag;

        lag.count = lagTotals.count;
        lag.negative = lagTotals.negative;

        if (lag.count > 0) {
            lag.p50Ns = lagTotal->valueAtPercentile(50.0);
            lag.p90Ns = lagTotal->valueAtPercentile(90.0);
            lag.p99Ns = lagTotal->valueAtPercentile(99.0);
            lag.p999Ns = lagTotal->valueAtPercentile(99.9);
            lag.p9999Ns = lagTotal->valueAtPercentile(99.99);
            lag.maxNs = lagTotals.max;
            lag.meanNs = lagTotals.sum / lagTotals.count;
        }

        report.e2eLag = lag;
        report.symbolsSeen = consumerState.symbolsSeen.load();
    }

    if (firstRuntime.has_value() && lastRuntime.has_value()) {
        report.runtime = runtimeSummary(*firstRuntime, firstRuntimeNanos, *lastRuntime, lastRuntimeNanos);
    }

    if (consumerState.nanoMissing() && failure.empty()) {
        failure = "the first " + std::to_string(NANO_CHECK_EVENTS) +
                  " events have the nanosecond part 0: a hop of the path lacks dxscheme.nanoTime=true "
                  "(run with -Ddxscheme.nanoTime=false to receive without it)";
        failureStatus = RunStatus::NANO_MISSING;
    }

    if (!failure.empty()) {
        result.exitCode = EXIT_FAILURE;
        result.status = failureStatus;
        // The statuses of the consumer's checks lead the line; other failures keep the text `p` printed before.
        result.error =
            failureStatus == RunStatus::FAILED ? failure : runStatusName(failureStatus) + ": " + failure;
    }

    return result;
}

namespace {

std::atomic<bool> signalReceived{false};

extern "C" void onSignal(int) {
    signalReceived.store(true);
}

std::vector<int> watchedSignals() {
    std::vector<int> signals{SIGINT, SIGTERM};

#ifdef SIGBREAK
    signals.push_back(SIGBREAK);
#endif

    return signals;
}

} // namespace

SignalWatcher::SignalWatcher(StopControl &stop) : stop_(stop) {
    signalReceived.store(false);

    for (const int signalNumber : watchedSignals()) {
        previous_.emplace_back(signalNumber, std::signal(signalNumber, onSignal));
    }

    thread_ = std::thread([this] {
        while (!done_.load() && !stop_.stopped()) {
            if (signalReceived.load()) {
                stop_.request();

                break;
            }

            std::this_thread::sleep_for(std::chrono::milliseconds(20));
        }
    });
}

bool SignalWatcher::received() const {
    return signalReceived.load();
}

SignalWatcher::~SignalWatcher() {
    done_.store(true);
    thread_.join();

    for (const auto &[signalNumber, handler] : previous_) {
        std::signal(signalNumber, handler == SIG_ERR ? SIG_DFL : handler);
    }
}

} // namespace nettest
} // namespace dxfg
