// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Run.hpp>
#include <NetTest/RateLimiter.hpp>
#include <NetTest/Sdk.hpp>

#include <algorithm>
#include <csignal>
#include <cstdlib>
#include <mutex>
#include <optional>

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
    InstanceCounters counters{};

    explicit Endpoint(graal_isolatethread_t *thread) : thread_(thread) {
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
};

// One publisher endpoint with its event pool.
class Instance final : public Endpoint {
    dxfg_v4_publisher_t *publisher_ = nullptr;

  public:
    EventPool pool;

    Instance(graal_isolatethread_t *thread, EventKind kind, const Symbols &symbols, std::uint64_t seed)
        : Endpoint(thread), pool(kind, symbols, seed) {
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

// What the consumer instances share: the check of the nanosecond part over the first events of all instances and the
// time of the last record for the watchdog. The listeners write it on the threads of the SDK.
class ConsumerState {
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
    ConsumerState(StopControl &stop, bool nanoCheck) : stop_(stop), nanoCheck_(nanoCheck), nanoPending_(nanoCheck) {
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

// One feed endpoint with its subscription to a slice of the symbols. The listener counts the records and sums their
// e2e lags; it runs on the threads of the SDK.
class Consumer final : public Endpoint {
    EventKind kind_;
    ConsumerState &state_;
    dxfg_v4_feed_t *feed_ = nullptr;
    dxfg_v4_subscription_t *subscription_ = nullptr;
    dxfg_v4_feed_event_listener_t *listener_ = nullptr;

    static void onEvents(graal_isolatethread_t *, dxfg_event_type_list *events, void *userData) {
        static_cast<Consumer *>(userData)->receive(events);
    }

    void receive(const dxfg_event_type_list *events) {
        // One clock read per call of the listener.
        const auto now = systemNanos();
        const bool isTrade = kind_ == EventKind::TRADE;
        const auto expectedClass = isTrade ? DXFG_EVENT_TRADE : DXFG_EVENT_TIME_AND_SALE;
        const bool nanoPending = state_.nanoPending();
        std::vector<std::int32_t> nanoParts{};
        std::int64_t records = 0;
        std::int64_t lagSum = 0;

        for (std::int32_t index = 0; index < events->size; ++index) {
            const dxfg_event_type_t *event = events->elements[index];

            if (event == nullptr || event->clazz != expectedClass) {
                continue;
            }

            std::int64_t packed = 0;
            std::int32_t nanoPart = 0;

            if (isTrade) {
                const auto *trade = reinterpret_cast<const dxfg_trade_t *>(event);

                packed = trade->trade_base.time_sequence;
                nanoPart = trade->trade_base.time_nano_part;
            } else {
                const auto *sale = reinterpret_cast<const dxfg_time_and_sale_t *>(event);

                packed = sale->index;
                nanoPart = sale->time_nano_part;
            }

            // A negative lag (a clock step or skew between the hosts) enters the sum with its sign.
            ++records;
            lagSum += eventLagNanos(now, packed, nanoPart);

            if (nanoPending) {
                nanoParts.push_back(nanoPart);
            }
        }

        if (records == 0) {
            return;
        }

        counters.records.fetch_add(records, std::memory_order_relaxed);
        counters.weightedNanos.fetch_add(lagSum, std::memory_order_relaxed);
        state_.recordsArrived();

        if (nanoPending) {
            state_.checkNanoParts(nanoParts);
        }
    }

  public:
    Consumer(graal_isolatethread_t *thread, EventKind kind, ConsumerState &state)
        : Endpoint(thread), kind_(kind), state_(state) {
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
                     std::ostream &out, const RunHooks &hooks) {
    RunResult result;
    graal_isolate_t *isolate = graal_get_isolate(thread);
    const bool producer = options.mode == Mode::PRODUCER;
    const auto role = endpointRole(options);
    const auto nanoTime = endpointNanoTime(options);
    ConsumerState consumerState(stop, isNanoTimeOn(options));
    std::vector<std::unique_ptr<Instance>> instances;
    std::vector<std::unique_ptr<Consumer>> consumers;
    std::vector<Endpoint *> endpoints;
    std::string error;

    const auto failStart = [&result](const std::string &message) {
        result.exitCode = EXIT_FAILURE;
        result.status = RunStatus::FAILED;
        result.error = message;

        return result;
    };

    // An instance that failed to start is destroyed when this function returns, with the endpoints created before it.
    for (std::int32_t index = 1; index <= options.instances; ++index) {
        const auto name = options.name + "." + std::to_string(index);

        if (producer) {
            auto instance = std::make_unique<Instance>(thread, options.event, symbols,
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
            auto consumer = std::make_unique<Consumer>(thread, options.event, consumerState);
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

    // The reporter reads the counters and the connections of the instances and the runtime values of the isolate; it
    // stops after the generators.
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

                for (const auto *endpoint : endpoints) {
                    sample.instances.push_back({endpoint->counters.records.load(std::memory_order_relaxed),
                                                endpoint->counters.weightedNanos.load(std::memory_order_relaxed)});
                    sample.connections += endpoint->connections(reporterThread);
                }

                sample.runtime = readRuntime(reporterThread);

                return sample;
            },
            [&](const std::string &text) {
                out << text << std::flush;
            },
            producer ? Side::PRODUCER : Side::CONSUMER);
    });

    std::vector<std::thread> generators;

    for (const auto &instance : instances) {
        generators.push_back(attachedThread(isolate, [&, current = instance.get()](graal_isolatethread_t *generatorThread) {
            if (generatorThread == nullptr) {
                fail("cannot attach a generator thread to the isolate");

                return;
            }

            const auto generated = runGenerator(generatorThread, current->publisher(), current->pool, settings,
                                                limiter.get(), stop, current->counters);

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

SignalWatcher::~SignalWatcher() {
    done_.store(true);
    thread_.join();

    for (const auto &[signalNumber, handler] : previous_) {
        std::signal(signalNumber, handler == SIG_ERR ? SIG_DFL : handler);
    }
}

} // namespace nettest
} // namespace dxfg
