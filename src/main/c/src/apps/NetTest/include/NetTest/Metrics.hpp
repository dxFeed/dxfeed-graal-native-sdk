// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The live counters of a run and their /metrics: the text exposition format of Prometheus (section "Metrics" of the
// NetTest specification) and a minimal HTTP/1.0 responder on one thread that serves it.

#ifndef DXFG_NETTEST_METRICS_HPP
#define DXFG_NETTEST_METRICS_HPP

#include <NetTest/Generator.hpp>
#include <NetTest/Statistics.hpp>

#include <array>
#include <atomic>
#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <thread>

namespace dxfg {
namespace nettest {

// The content type of /metrics.
inline constexpr const char *METRICS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

// The upper bounds of the buckets of nettest_e2e_lag_seconds in nanoseconds; the last bucket, +Inf, follows them.
inline constexpr std::array<std::int64_t, 13> LAG_BUCKET_BOUNDS_NANOS{
    50'000,     100'000,    250'000,     500'000,     1'000'000,   2'500'000,    5'000'000,
    10'000'000, 25'000'000, 50'000'000, 100'000'000, 250'000'000, 1'000'000'000};

// The value of a connector statistic v2 when the SDK does not have them
// (DXFG_V4_FEATURE_MESSAGE_CONNECTOR_IO_STATISTICS).
constexpr std::int64_t CONNECTOR_V2_ABSENT = -1;

// The connector statistics summed over the connectors of all instances: v1, and v2 or CONNECTOR_V2_ABSENT.
struct ConnectorTotals {
    std::int64_t readBytes = 0;
    std::int64_t writtenBytes = 0;
    std::int64_t readRecords = 0;
    std::int64_t writtenRecords = 0;
    // The subscription records read and written.
    std::int64_t subRecords = CONNECTOR_V2_ABSENT;
    // The sum of the lags of the data records read, in microseconds.
    std::int64_t readLagMicros = CONNECTOR_V2_ABSENT;
    // The sum of the round-trip times of the records read and written, in microseconds.
    std::int64_t rttMicros = CONNECTOR_V2_ABSENT;
};

// The counters of a run. The generators, the listeners and the reporter write them; /metrics and the summary read them.
// All are atomics, so that a scrape never calls into the isolate.
struct LiveCounters {
    const std::int32_t instanceCount;
    // Per instance: the records and the weighted nanoseconds of the statistics lines.
    std::unique_ptr<InstanceCounters[]> instances;
    // Per instance: the open connections at the last tick of the reporter.
    std::unique_ptr<std::atomic<std::int64_t>[]> connections;

    // `p`: the publishing calls of all generators.
    PublishCounters publish{};

    // `c`: the e2e lags of all records (the warm-up included): the count per bucket (not cumulative, +Inf last), the sum
    // of the lags with the negative ones as 0, and the negative lags.
    std::array<std::atomic<std::int64_t>, LAG_BUCKET_BOUNDS_NANOS.size() + 1> lagBuckets{};
    std::atomic<std::int64_t> lagCount{0};
    std::atomic<std::int64_t> lagSumNanos{0};
    std::atomic<std::int64_t> lagNegative{0};

    // The connector statistics v1 at the last tick of the reporter.
    std::atomic<std::int64_t> connectorReadBytes{0};
    std::atomic<std::int64_t> connectorWrittenBytes{0};
    std::atomic<std::int64_t> connectorReadRecords{0};
    std::atomic<std::int64_t> connectorWrittenRecords{0};
    // The connector statistics v2 at the last tick of the reporter; CONNECTOR_V2_ABSENT without them.
    std::atomic<std::int64_t> connectorSubRecords{CONNECTOR_V2_ABSENT};
    std::atomic<std::int64_t> connectorReadLagMicros{CONNECTOR_V2_ABSENT};
    std::atomic<std::int64_t> connectorRttMicros{CONNECTOR_V2_ABSENT};

    // The runtime values at the last tick of the reporter; RUNTIME_VALUE_ABSENT when the isolate does not give them.
    std::atomic<std::int64_t> heapUsedBytes{RUNTIME_VALUE_ABSENT};
    std::atomic<std::int64_t> heapCommittedBytes{RUNTIME_VALUE_ABSENT};
    std::atomic<std::int64_t> gcCount{RUNTIME_VALUE_ABSENT};
    std::atomic<std::int64_t> gcTimeMs{RUNTIME_VALUE_ABSENT};
    std::atomic<std::int64_t> cpuNanos{RUNTIME_VALUE_ABSENT};

    explicit LiveCounters(std::int32_t instances);

    LiveCounters(const LiveCounters &) = delete;
    LiveCounters &operator=(const LiveCounters &) = delete;

    // Counts the lags of the records of one listener call: the count per bucket, the count, the sum of the lags with the
    // negative ones as 0, and the negative lags.
    void addLags(const std::array<std::int64_t, LAG_BUCKET_BOUNDS_NANOS.size() + 1> &buckets, std::int64_t count,
                 std::int64_t positiveSum, std::int64_t negative);

    // Keeps the runtime values of a sample (young and full collections together).
    void setRuntime(const RuntimeSample &sample);

    void setConnectors(const ConnectorTotals &totals);
};

// The bucket of a lag (nanoseconds, negative as 0) in LiveCounters::lagBuckets.
std::size_t lagBucketIndex(std::int64_t lagNanos);

// The labels and the configuration of the series.
struct MetricsInfo {
    // `producer` or `consumer`.
    std::string side = "producer";
    std::string name = "NetTest";
    // The SDK version, empty when unknown.
    std::string version{};
    std::string role{};
    std::string event{};
    // Empty without -c.
    std::string collector{};
    // `p` with -r: the slip series is exported.
    bool scheduled = false;
};

// Nanoseconds as seconds in decimal without exponent or trailing zeros: 86100000000 -> 86.1, 12000 -> 0.000012.
std::string formatSeconds(std::int64_t nanos);

// The text of /metrics: the metrics of the side with their HELP and TYPE lines, the series of the tool `c-api`.
std::string formatMetrics(const MetricsInfo &info, const LiveCounters &counters);

// The HTTP/1.0 responder: binds at the construction, serves GET /metrics on its thread after start(). Other paths get
// 404, other methods 405. One request per connection.
class MetricsServer {
    std::string host_;
    std::int32_t port_ = 0;
    std::string error_{};
    // The listening socket as an integer (SOCKET or int); -1 when not bound.
    std::intptr_t socket_ = -1;
    std::atomic<bool> stopping_{false};
    std::thread thread_{};
    [[maybe_unused]] bool winsock_ = false;

    void serve(const std::function<std::string()> &body);

  public:
    // Binds the host and the port (0: a free one) and listens; error() tells why it failed.
    MetricsServer(const std::string &host, std::int32_t port);
    MetricsServer(const MetricsServer &) = delete;
    MetricsServer &operator=(const MetricsServer &) = delete;
    ~MetricsServer();

    bool ok() const {
        return socket_ != -1;
    }

    const std::string &error() const {
        return error_;
    }

    // The bound port, the one taken for port 0.
    std::int32_t port() const {
        return port_;
    }

    // `http://<host>:<port>/metrics`.
    std::string url() const;

    // Serves the text that `body` gives for each request, on the thread of the server.
    void start(std::function<std::string()> body);

    // Stops the thread and closes the socket; the destructor calls it.
    void stop();
};

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_METRICS_HPP
