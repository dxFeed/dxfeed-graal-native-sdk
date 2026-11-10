// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Run.hpp>
#include <NetTest/RateLimiter.hpp>
#include <NetTest/Sdk.hpp>

#include <algorithm>
#include <csignal>
#include <mutex>

namespace dxfg {
namespace nettest {

namespace {

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

// One publisher endpoint with its event pool and its counters. The endpoint is closed and its handles are released
// at the destruction, on the thread that created the instance.
class Instance {
    graal_isolatethread_t *thread_;
    dxfg_v4_endpoint_t *endpoint_ = nullptr;
    dxfg_v4_publisher_t *publisher_ = nullptr;

  public:
    EventPool pool;
    InstanceCounters counters{};

    Instance(graal_isolatethread_t *thread, EventKind kind, const Symbols &symbols, std::uint64_t seed)
        : thread_(thread), pool(kind, symbols, seed) {
    }

    Instance(const Instance &) = delete;
    Instance &operator=(const Instance &) = delete;

    ~Instance() {
        if (publisher_ != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread_, &publisher_->handler);
        }

        if (endpoint_ != nullptr) {
            dxfg_v4_DXEndpoint_closeAndAwaitTermination(thread_, endpoint_);
            dxfg_v4_JavaObjectHandler_release(thread_, &endpoint_->handler);
        }
    }

    dxfg_v4_endpoint_t *endpoint() const {
        return endpoint_;
    }

    dxfg_v4_publisher_t *publisher() const {
        return publisher_;
    }

    // Creates the PUBLISHER endpoint of the name and its publisher; returns false and fills the error on a failure.
    bool create(const std::string &name, std::string &error) {
        dxfg_v4_endpoint_builder_t *builder = nullptr;

        if (dxfg_v4_DXEndpoint_newBuilder(thread_, &builder) != DXFG_EXECUTE_SUCCESSFULLY) {
            error = "cannot create the endpoint " + name + ": " + takeThreadException(thread_);

            return false;
        }

        const bool built = dxfg_v4_DXEndpoint_Builder_withRole(thread_, builder, DXFG_ENDPOINT_ROLE_PUBLISHER) ==
                               DXFG_EXECUTE_SUCCESSFULLY &&
                           dxfg_v4_DXEndpoint_Builder_withName(thread_, builder, name.c_str()) ==
                               DXFG_EXECUTE_SUCCESSFULLY &&
                           dxfg_v4_DXEndpoint_Builder_build(thread_, builder, &endpoint_) ==
                               DXFG_EXECUTE_SUCCESSFULLY;

        if (!built) {
            endpoint_ = nullptr;
            error = "cannot create the endpoint " + name + ": " + takeThreadException(thread_);
        }

        dxfg_v4_JavaObjectHandler_release(thread_, &builder->handler);

        if (!built) {
            return false;
        }

        if (dxfg_v4_DXEndpoint_getPublisher(thread_, endpoint_, &publisher_) != DXFG_EXECUTE_SUCCESSFULLY) {
            publisher_ = nullptr;
            error = "cannot get the publisher of the endpoint " + name + ": " + takeThreadException(thread_);

            return false;
        }

        return true;
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

        const auto deadline = std::chrono::steady_clock::now() + SERVER_START_TIMEOUT;

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

            if (std::chrono::steady_clock::now() >= deadline) {
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
GeneratorSettings generatorSettings(const Options &options) {
    GeneratorSettings settings;

    settings.batch = static_cast<std::size_t>(options.batch);
    settings.reuse = options.reuse;
    settings.legacy = options.legacy;

    return settings;
}

} // namespace

RunResult runNetTest(graal_isolatethread_t *thread, const Options &options, const Symbols &symbols, StopControl &stop,
                     std::ostream &out) {
    RunResult result;
    graal_isolate_t *isolate = graal_get_isolate(thread);
    std::vector<std::unique_ptr<Instance>> instances;
    std::string error;

    for (std::int32_t index = 1; index <= options.instances; ++index) {
        const auto name = options.name + "." + std::to_string(index);
        auto instance = std::make_unique<Instance>(thread, options.event, symbols,
                                                   0x9E3779B97F4A7C15ULL * static_cast<std::uint64_t>(index));

        // An instance that failed to start is destroyed here, so the endpoints created before it close below.
        if (!instance->create(name, error) || !instance->connect(name, options.address, error)) {
            result.exitCode = EXIT_FAILURE;
            result.error = error;

            return result;
        }

        instances.push_back(std::move(instance));
    }

    const auto settings = generatorSettings(options);
    std::unique_ptr<RateLimiter> limiter;

    if (options.recordsPerSecond.has_value()) {
        limiter = std::make_unique<RateLimiter>(*options.recordsPerSecond, options.batch, stop);
    }

    std::mutex failureMutex;
    std::string failure;
    auto fail = [&](const std::string &message) {
        {
            std::lock_guard<std::mutex> lock(failureMutex);

            if (failure.empty()) {
                failure = message;
            }
        }

        stop.request();
    };

    // The reporter reads the counters and the connections of the instances; it stops after the generators.
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

                for (const auto &instance : instances) {
                    sample.instances.push_back({instance->counters.records.load(std::memory_order_relaxed),
                                                instance->counters.weightedNanos.load(std::memory_order_relaxed)});
                    sample.connections += instance->connections(reporterThread);
                }

                return sample;
            },
            [&](const std::string &text) {
                out << text << std::flush;
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
                                                limiter.get(), stop, current->counters);

            if (!generated.ok) {
                fail("publishing failed: " + generated.error);
            }
        }));
    }

    if (options.durationMillis.has_value()) {
        stop.waitFor(std::chrono::milliseconds(*options.durationMillis));
    } else {
        stop.wait();
    }

    // The generators first, then the last statistics, then the endpoints.
    stop.request();

    for (auto &generator : generators) {
        generator.join();
    }

    reporterStop.request();
    reporter.join();

    for (const auto &instance : instances) {
        const auto records = instance->counters.records.load();

        result.instanceRecords.push_back(records);
        result.records += records;
    }

    if (!failure.empty()) {
        result.exitCode = EXIT_FAILURE;
        result.error = failure;
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
