// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The histogram, /metrics and the summary file of the C tool against the kit of the NetTest specification
// (docs/wrapper-guide/nettest.md): the percentiles of histogram-vectors.json, the samples metrics-sample-*.txt byte for
// byte, the HTTP responder, and summaries that nettest-summary.schema.json accepts.

#ifdef _WIN32
#    ifndef WIN32_LEAN_AND_MEAN
#        define WIN32_LEAN_AND_MEAN
#    endif
#    include <winsock2.h>
#    include <ws2tcpip.h>
#else
#    include <arpa/inet.h>
#    include <netinet/in.h>
#    include <sys/socket.h>
#    include <unistd.h>
#endif

#include <doctest.h>

#include <NetTest/Args.hpp>
#include <NetTest/Histogram.hpp>
#include <NetTest/Metrics.hpp>
#include <NetTest/Summary.hpp>

#include "KitJson.hpp"
#include "KitSchema.hpp"

#include <array>
#include <chrono>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace nettest {

namespace {

const std::string KIT_DIR = NETTEST_KIT_DIR;

kit::Json kitFile(const std::string &name) {
    return kit::readJson(KIT_DIR + "/" + name);
}

// A kit text without carriage returns, as a checkout with CRLF line ends may have them.
std::string kitText(const std::string &name) {
    auto text = kit::readText(KIT_DIR + "/" + name);
    std::string result;

    for (const char character : text) {
        if (character != '\r') {
            result += character;
        }
    }

    return result;
}

std::string replaceAll(std::string text, const std::string &from, const std::string &to) {
    for (auto at = text.find(from); at != std::string::npos; at = text.find(from, at + to.size())) {
        text.replace(at, from.size(), to);
    }

    return text;
}

// The first line where two texts differ, for the message of a failed comparison.
std::string firstDifference(const std::string &actual, const std::string &expected) {
    std::size_t start = 0;

    for (;;) {
        const auto actualEnd = actual.find('\n', start);
        const auto expectedEnd = expected.find('\n', start);
        const auto actualLine = actual.substr(start, actualEnd == std::string::npos ? std::string::npos : actualEnd - start);
        const auto expectedLine =
            expected.substr(start, expectedEnd == std::string::npos ? std::string::npos : expectedEnd - start);

        if (actualLine != expectedLine || actualEnd != expectedEnd) {
            return "actual:   " + actualLine + "\nexpected: " + expectedLine;
        }

        if (actualEnd == std::string::npos) {
            return "no difference";
        }

        start = actualEnd + 1;
    }
}

std::vector<std::string> schemaErrors(const std::string &text) {
    const auto schema = kitFile("nettest-summary.schema.json");
    const auto value = kit::JsonReader(text).read();
    std::vector<std::string> errors;

    kit::checkSchema(value, schema, "$", errors);

    return errors;
}

std::string joined(const std::vector<std::string> &lines) {
    std::string text;

    for (const auto &line : lines) {
        text += line + "\n";
    }

    return text;
}

Options parsedOptions(const std::vector<std::string> &args) {
    std::vector<std::string> full{"NetTest"};

    full.insert(full.end(), args.begin(), args.end());

    const auto parsed = parseArguments(full);

    REQUIRE_MESSAGE(parsed.ok, parsed.error);

    return parsed.options;
}

} // namespace

TEST_CASE("Kit: the histogram gives the percentiles of histogram-vectors.json") {
    const auto vectors = kitFile("histogram-vectors.json");
    const auto &settings = vectors["histogram"];

    CHECK(settings["lowest"].asInteger() == HISTOGRAM_LOWEST);
    CHECK(settings["highest"].asInteger() == HISTOGRAM_HIGHEST);
    CHECK(settings["digits"].asInteger() == HISTOGRAM_DIGITS);
    REQUIRE(!vectors["vectors"].array.empty());

    for (const auto &vector : vectors["vectors"].array) {
        Histogram histogram;

        if (vector.has("recorded_range")) {
            const auto &range = vector["recorded_range"];

            for (auto value = range["from"].asInteger(); value <= range["to"].asInteger(); ++value) {
                histogram.record(value, range["count_each"].asInteger());
            }
        } else {
            for (const auto &pair : vector["recorded"].array) {
                histogram.record(pair.array.at(0).asInteger(), pair.array.at(1).asInteger());
            }
        }

        INFO(vector["about"].asString());
        CHECK(histogram.totalCount() == vector["count"].asInteger());
        CHECK(histogram.valueAtPercentile(50.0) == vector["p50_ns"].asInteger());
        CHECK(histogram.valueAtPercentile(90.0) == vector["p90_ns"].asInteger());
        CHECK(histogram.valueAtPercentile(99.0) == vector["p99_ns"].asInteger());
        CHECK(histogram.valueAtPercentile(99.9) == vector["p999_ns"].asInteger());
        CHECK(histogram.valueAtPercentile(99.99) == vector["p9999_ns"].asInteger());
    }
}

TEST_CASE("Histogram: a negative value enters as 0, a value above the highest as the highest; add and reset") {
    Histogram histogram;

    histogram.record(-5);
    CHECK(histogram.valueAtPercentile(100.0) == 0);

    histogram.record(HISTOGRAM_HIGHEST * 2);
    CHECK(histogram.valueAtPercentile(100.0) == histogram.highestEquivalentValue(HISTOGRAM_HIGHEST));

    Histogram other;

    other.record(1000, 3);
    histogram.add(other);
    CHECK(histogram.totalCount() == 5);

    histogram.reset();
    CHECK(histogram.totalCount() == 0);
    CHECK(histogram.valueAtPercentile(50.0) == 0);
}

TEST_CASE("Kit: /metrics of the producer is metrics-sample-producer.txt byte for byte") {
    LiveCounters counters(1);

    counters.instances[0].records = 210'000'000;
    counters.publish.nanos = 86'100'000'000;
    counters.publish.calls = 2'100'000;
    counters.publish.slipLastNanos = 12'000;
    counters.connections[0] = 1;
    // v2: 100,000 subscription records read; a round-trip time of 200 us over the data and subscription records.
    counters.setConnectors({1200, 6'825'000'000, 0, 210'000'000, 100'000, 0, 42'020'000'000});
    counters.heapUsedBytes = 125'829'120;
    counters.heapCommittedBytes = 268'435'456;
    counters.gcCount = 312;
    counters.gcTimeMs = 1520;
    counters.cpuNanos = 402'700'000'000;

    const MetricsInfo info{"producer", "bench", "3.18.0", "STREAM_PUBLISHER", "Trade", "stream", true};
    const auto actual = formatMetrics(info, counters);
    const auto expected = kitText("metrics-sample-producer.txt");

    INFO(firstDifference(actual, expected));
    CHECK(actual == expected);
}

TEST_CASE("Kit: /metrics of the consumer is metrics-sample-consumer.txt byte for byte, with the tool c-api") {
    LiveCounters counters(2);
    // The cumulative counts of the sample per bucket, +Inf last.
    const std::vector<std::int64_t> cumulative{0,           1'200'000,   42'000'000,  150'000'000, 200'500'000,
                                               208'000'000, 209'500'000, 209'900'000, 209'990'000, 209'998'000,
                                               209'999'000, 209'999'000, 209'999'000, 209'999'000};
    std::array<std::int64_t, LAG_BUCKET_BOUNDS_NANOS.size() + 1> buckets{};

    REQUIRE(cumulative.size() == buckets.size());

    for (std::size_t index = 0; index < buckets.size(); ++index) {
        buckets[index] = cumulative[index] - (index == 0 ? 0 : cumulative[index - 1]);
    }

    counters.instances[0].records = 105'000'000;
    counters.instances[1].records = 104'999'000;
    counters.addLags(buckets, 209'999'000, 85'049'595'000'000, 0);
    counters.connections[0] = 1;
    counters.connections[1] = 1;
    // v2: 200,000 subscription records written; a connector lag of 150 us over the data records read and a round-trip
    // time of 200 us over the data and subscription records.
    counters.setConnectors({6'825'000'000, 1200, 209'999'000, 0, 200'000, 31'499'850'000, 42'039'800'000});
    counters.heapUsedBytes = 325'058'560;
    counters.heapCommittedBytes = 1'073'741'824;
    counters.gcCount = 41;
    counters.gcTimeMs = 380;
    counters.cpuNanos = 365'400'000'000;

    const MetricsInfo info{"consumer", "bench", "3.18.0", "STREAM_FEED", "Trade", "stream", false};
    const auto actual = formatMetrics(info, counters);
    const auto expected = replaceAll(kitText("metrics-sample-consumer.txt"), "tool=\"java-api\"", "tool=\"c-api\"");

    INFO(firstDifference(actual, expected));
    CHECK(actual == expected);
}

TEST_CASE("Metrics: a value that the tool does not have gets no sample line") {
    LiveCounters counters(1);
    const MetricsInfo info{"producer", "NetTest", "", "PUBLISHER", "Trade", "", false};
    const auto text = formatMetrics(info, counters);

    CHECK(text.find("nettest_records_total{") != std::string::npos);
    CHECK(text.find("nettest_schedule_slip_seconds{") == std::string::npos);
    CHECK(text.find("nettest_runtime_heap_used_bytes{") == std::string::npos);
    CHECK(text.find("nettest_runtime_cpu_seconds_total{") == std::string::npos);
}

TEST_CASE("Metrics: without the connector statistics v2 their families are absent and those of v1 stay") {
    for (const std::string side : {"producer", "consumer"}) {
        LiveCounters counters(1);

        counters.setConnectors({10, 20, 30, 40});

        const MetricsInfo info{side, "NetTest", "", "STREAM_FEED", "Trade", "stream", false};
        const auto text = formatMetrics(info, counters);

        INFO(text);
        CHECK(text.find("# TYPE nettest_connector_read_bytes_total counter\n") != std::string::npos);
        CHECK(text.find("nettest_connector_written_records_total{tool=\"c-api\",side=\"" + side +
                        "\",name=\"NetTest\"} 40\n") != std::string::npos);
        CHECK(text.find("nettest_connector_lag_seconds_total") == std::string::npos);
        CHECK(text.find("nettest_connector_rtt_seconds_total") == std::string::npos);
        CHECK(text.find("nettest_connector_sub_records_total") == std::string::npos);
    }
}

TEST_CASE("Metrics: the connector statistics v2 are counters in seconds and records, the lag on the consumer only") {
    for (const std::string side : {"producer", "consumer"}) {
        LiveCounters counters(1);

        counters.setConnectors({10, 20, 30, 40, 7, 1'500'001, 2'000'000});

        const MetricsInfo info{side, "NetTest", "", "STREAM_FEED", "Trade", "stream", false};
        const auto text = formatMetrics(info, counters);
        const auto labels = "{tool=\"c-api\",side=\"" + side + "\",name=\"NetTest\"}";

        INFO(text);
        CHECK(text.find("# TYPE nettest_connector_rtt_seconds_total counter\nnettest_connector_rtt_seconds_total" +
                        labels + " 2\n") != std::string::npos);
        CHECK(text.find("# TYPE nettest_connector_sub_records_total counter\nnettest_connector_sub_records_total" +
                        labels + " 7\n") != std::string::npos);

        if (side == "consumer") {
            CHECK(text.find("# TYPE nettest_connector_lag_seconds_total counter\nnettest_connector_lag_seconds_total" +
                            labels + " 1.500001\n") != std::string::npos);
        } else {
            CHECK(text.find("nettest_connector_lag_seconds_total") == std::string::npos);
        }
    }
}

TEST_CASE("Metrics: the lags fall into the buckets of nettest_e2e_lag_seconds, a negative one into the first") {
    CHECK(lagBucketIndex(-1) == 0);
    CHECK(lagBucketIndex(0) == 0);
    CHECK(lagBucketIndex(50'000) == 0);
    CHECK(lagBucketIndex(50'001) == 1);
    CHECK(lagBucketIndex(1'000'000'000) == LAG_BUCKET_BOUNDS_NANOS.size() - 1);
    CHECK(lagBucketIndex(1'000'000'001) == LAG_BUCKET_BOUNDS_NANOS.size());
    CHECK(formatSeconds(86'100'000'000) == "86.1");
    CHECK(formatSeconds(12'000) == "0.000012");
    CHECK(formatSeconds(0) == "0");
}

namespace {

#ifdef _WIN32
using NativeSocket = SOCKET;
const NativeSocket NO_SOCKET = INVALID_SOCKET;

void closeNative(NativeSocket socket) {
    closesocket(socket);
}
#else
using NativeSocket = int;
const NativeSocket NO_SOCKET = -1;

void closeNative(NativeSocket socket) {
    close(socket);
}
#endif

// Sends the request to the loopback port and reads the response until the server closes the connection.
std::string exchange(std::int32_t port, const std::string &request) {
#ifdef _WIN32
    WSADATA data{};

    REQUIRE(WSAStartup(MAKEWORD(2, 2), &data) == 0);
#endif

    std::string response;
    const NativeSocket client = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

    if (client != NO_SOCKET) {
        sockaddr_in address{};

        address.sin_family = AF_INET;
        address.sin_port = htons(static_cast<std::uint16_t>(port));
        address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

        if (connect(client, reinterpret_cast<const sockaddr *>(&address), sizeof(address)) == 0) {
            send(client, request.data(), static_cast<int>(request.size()), 0);

            char buffer[4096];

            for (;;) {
                const auto received = recv(client, buffer, sizeof(buffer), 0);

                if (received <= 0) {
                    break;
                }

                response.append(buffer, static_cast<std::size_t>(received));
            }
        }

        closeNative(client);
    }

#ifdef _WIN32
    WSACleanup();
#endif

    return response;
}

} // namespace

TEST_CASE("Metrics: the responder serves GET /metrics with the content type, 404 and 405 otherwise") {
    MetricsServer server("127.0.0.1", 0);

    REQUIRE_MESSAGE(server.ok(), server.error());
    REQUIRE(server.port() > 0);
    CHECK(server.url() == "http://127.0.0.1:" + std::to_string(server.port()) + "/metrics");

    server.start([] {
        return std::string("nettest_test 1\n");
    });

    const auto metrics = exchange(server.port(), "GET /metrics HTTP/1.0\r\nHost: localhost\r\n\r\n");

    CHECK(metrics.rfind("HTTP/1.0 200 OK\r\n", 0) == 0);
    CHECK(metrics.find("\r\nContent-Type: " + std::string(METRICS_CONTENT_TYPE) + "\r\n") != std::string::npos);
    CHECK(metrics.size() >= 15);
    CHECK(metrics.substr(metrics.size() - 15) == "nettest_test 1\n");

    const auto head = exchange(server.port(), "HEAD /metrics HTTP/1.0\r\n\r\n");

    CHECK(head.rfind("HTTP/1.0 200 OK\r\n", 0) == 0);
    CHECK(head.find("nettest_test") == std::string::npos);

    CHECK(exchange(server.port(), "GET /other HTTP/1.0\r\n\r\n").rfind("HTTP/1.0 404 ", 0) == 0);
    CHECK(exchange(server.port(), "POST /metrics HTTP/1.0\r\n\r\n").rfind("HTTP/1.0 405 ", 0) == 0);

    // A port in use: the second server cannot bind it.
    MetricsServer busy("127.0.0.1", server.port());

    CHECK_FALSE(busy.ok());
    CHECK(busy.error().find("is the port in use?") != std::string::npos);

    server.stop();
}

namespace {

// A loopback connection to the port; NO_SOCKET when it fails.
NativeSocket connectLoopback(std::int32_t port) {
    const NativeSocket client = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

    if (client == NO_SOCKET) {
        return NO_SOCKET;
    }

    sockaddr_in address{};

    address.sin_family = AF_INET;
    address.sin_port = htons(static_cast<std::uint16_t>(port));
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    if (connect(client, reinterpret_cast<const sockaddr *>(&address), sizeof(address)) != 0) {
        closeNative(client);

        return NO_SOCKET;
    }

    return client;
}

// True when the server closed the connection within the milliseconds (a read gives the end or an error).
bool closedWithin(NativeSocket client, int millis) {
    fd_set set;

    FD_ZERO(&set);
    FD_SET(client, &set);

    timeval timeout{};

    timeout.tv_sec = millis / 1000;
    timeout.tv_usec = (millis % 1000) * 1000;

#ifdef _WIN32
    const int nfds = 0;
#else
    const int nfds = client + 1;
#endif

    if (select(nfds, &set, nullptr, nullptr, &timeout) <= 0) {
        return false;
    }

    char buffer[256];

    return recv(client, buffer, sizeof(buffer), 0) <= 0;
}

} // namespace

TEST_CASE("Metrics: a client that sends its request a byte at a time is closed at the deadline of the whole request") {
    MetricsServer server("127.0.0.1", 0);

    REQUIRE_MESSAGE(server.ok(), server.error());

    server.start([] {
        return std::string("nettest_test 1\n");
    });

    const NativeSocket client = connectLoopback(server.port());

    REQUIRE(client != NO_SOCKET);

    // A byte every 500 ms keeps a timeout per read alive forever; the deadline of the request (2 s) closes it.
    const std::string request = "GET /metrics HTTP/1.0\r\n";
    const auto start = std::chrono::steady_clock::now();
    bool closed = false;

    for (std::size_t i = 0; i < request.size() && !closed; i++) {
        send(client, request.data() + i, 1, 0);
        closed = closedWithin(client, 500);
    }

    const auto elapsed = std::chrono::steady_clock::now() - start;

    closeNative(client);

    CHECK(closed);
    CHECK(elapsed < std::chrono::seconds(5));

    server.stop();
}

TEST_CASE("Metrics: the stop does not wait for a client that sends nothing") {
    MetricsServer server("127.0.0.1", 0);

    REQUIRE_MESSAGE(server.ok(), server.error());

    server.start([] {
        return std::string("nettest_test 1\n");
    });

    const NativeSocket client = connectLoopback(server.port());

    REQUIRE(client != NO_SOCKET);

    // The responder has accepted the connection and waits for its request.
    std::this_thread::sleep_for(std::chrono::milliseconds(300));

    const auto start = std::chrono::steady_clock::now();

    server.stop();

    const auto elapsed = std::chrono::steady_clock::now() - start;

    CHECK(closedWithin(client, 1000));
    closeNative(client);

    // The deadline of the request is 2 s; the stop is seen within a poll of 100 ms.
    CHECK(elapsed < std::chrono::milliseconds(1000));
}

namespace {

RunReport producerReport() {
    RunReport report;

    report.startEpochNs = 1'763'380'800'000'000'000;
    report.endEpochNs = report.startEpochNs + 20'000'000'000;

    for (int index = 0; index < 2; ++index) {
        IntervalRecord interval;

        interval.startEpochMs = report.startEpochNs / 1'000'000 + index * 10'000;
        interval.endEpochMs = interval.startEpochMs + 10'000;
        interval.warmup = index == 0;
        interval.records = 50'000;
        interval.rate = 5'000;
        interval.publishNsPerEvent = 120;
        interval.slipMaxNs = 30'000;
        interval.slipEndNs = 2'000;
        interval.connections = 1;
        report.records += interval.records;
        report.intervals.push_back(interval);
    }

    report.publish = PublishSummary{1000, 100'000, 12'000'000};
    report.slip = SlipSummary{30'000, 2'000, std::nullopt};

    ConnectorSummary connector;

    connector.address = ":7001";
    connector.writtenBytes = 3'000'000;
    connector.writtenDataRecords = 100'000;
    connector.activeConnections = 1;
    // Distinct values, so that a key on the wrong field shows.
    connector.v2 = ConnectorSummaryV2{1, 2, 3, 4, 5, 6};
    report.connectors.push_back(connector);

    // A connector of an SDK without the statistics v2.
    ConnectorSummary v1Only;

    v1Only.address = ":7002";
    report.connectors.push_back(v1Only);
    report.runtime.heapUsedMb = 40;
    report.runtime.heapCommittedMb = 256;
    report.runtime.gcCount = 2;
    report.runtime.gcTimeMs = 15;
    report.runtime.cpuPercentMean = 37;

    return report;
}

RunReport consumerReport() {
    RunReport report;

    report.startEpochNs = 1'763'380'800'000'000'000;
    report.endEpochNs = report.startEpochNs + 10'000'000'000;

    IntervalRecord interval;

    interval.startEpochMs = report.startEpochNs / 1'000'000;
    interval.endEpochMs = interval.startEpochMs + 10'000;
    interval.records = 1000;
    interval.rate = 100;
    interval.lagMeanNs = -12;
    interval.lagP50Ns = 0;
    interval.lagP99Ns = 381'183;
    interval.lagMaxNs = 400'000;
    report.records = interval.records;
    report.intervals.push_back(interval);

    LagSummary lag;

    lag.count = 1000;
    lag.negative = 3;
    lag.p50Ns = 0;
    lag.p90Ns = 1023;
    lag.p99Ns = 381'183;
    lag.p999Ns = 381'183;
    lag.p9999Ns = 381'183;
    lag.maxNs = 400'000;
    lag.meanNs = -12;
    report.e2eLag = lag;
    report.symbolsSeen = 100;

    return report;
}

} // namespace

TEST_CASE("Kit: the summary of a producer has every key that nettest-summary.schema.json requires") {
    const auto options =
        parsedOptions({"p", ":7001", "-S", "100", "-r", "*/5k", "--warmup", "10s", "--summary", "summary.json"});
    SummaryStatus status;

    status.version = "3.18.0";

    const auto text = formatSummary(options, producerReport(), status);
    const auto errors = schemaErrors(text);

    INFO(text);
    INFO(joined(errors));
    CHECK(errors.empty());

    const auto value = kit::JsonReader(text).read();

    CHECK(value["side"].asString() == "producer");
    CHECK(value["tool"]["implementation"].asString() == "c-api");
    CHECK(value["config"]["warmup_ms"].asInteger() == 10'000);
    CHECK(value["config"]["rate_limit"].asInteger() == 5'000);
    CHECK(value["e2e_lag"].isNull());
    CHECK(value["symbols_seen"].isNull());
    CHECK(value["schedule_slip"]["max_ns"].asInteger() == 30'000);
    CHECK(value["schedule_slip"]["growth_ns_per_s"].isNull());
    CHECK(value["intervals"].array.at(0)["phase"].asString() == "warmup");
    CHECK(value["intervals"].array.at(1)["phase"].asString() == "measure");

    std::int64_t records = 0;

    for (const auto &interval : value["intervals"].array) {
        records += interval["records"].asInteger();
    }

    CHECK(records == value["records"].asInteger());

    const auto &connectors = value["connectors"].array;

    REQUIRE(connectors.size() == 2);
    CHECK(connectors.at(0)["v1"]["written_data_records"].asInteger() == 100'000);
    CHECK(connectors.at(0)["v2"]["read_sub_records"].asInteger() == 1);
    CHECK(connectors.at(0)["v2"]["written_sub_records"].asInteger() == 2);
    CHECK(connectors.at(0)["v2"]["data_read_lag_us"].asInteger() == 3);
    CHECK(connectors.at(0)["v2"]["data_write_lag_us"].asInteger() == 4);
    CHECK(connectors.at(0)["v2"]["read_rtt_us"].asInteger() == 5);
    CHECK(connectors.at(0)["v2"]["write_rtt_us"].asInteger() == 6);
    CHECK(connectors.at(1)["v1"].has("read_bytes"));
    CHECK(connectors.at(1)["v2"].isNull());

    // The check of the schema is not empty: an empty name breaks its minLength.
    auto unnamed = options;

    unnamed.name.clear();

    const auto unnamedErrors = schemaErrors(formatSummary(unnamed, producerReport(), status));

    INFO(joined(unnamedErrors));
    CHECK(joined(unnamedErrors).find(".name: shorter than the minLength") != std::string::npos);
}

TEST_CASE("Kit: the summary of a consumer stopped by a signal is valid against nettest-summary.schema.json") {
    const auto options = parsedOptions({"c", "127.0.0.1:7001", "-S", "100", "-c", "stream"});
    SummaryStatus status;

    status.status = "stopped_by_signal";

    const auto text = formatSummary(options, consumerReport(), status);
    const auto errors = schemaErrors(text);

    INFO(text);
    INFO(joined(errors));
    CHECK(errors.empty());

    const auto value = kit::JsonReader(text).read();

    CHECK(value["status"].asString() == "stopped_by_signal");
    CHECK(value["tool"]["version"].isNull());
    CHECK(value["e2e_lag"]["negative"].asInteger() == 3);
    CHECK(value["e2e_lag"]["mean_ns"].asInteger() == -12);
    CHECK(value["publish_call"].isNull());
    CHECK(value["schedule_slip"].isNull());
    CHECK(value["symbols_seen"].asInteger() == 100);
    CHECK(value["connectors"].array.empty());
}

TEST_CASE("Kit: the summary of a failed run carries the error and is valid against the schema") {
    const auto options = parsedOptions({"p", ":7001", "-S", "100"});
    SummaryStatus status;

    status.status = "error";
    status.exitCode = 1;
    status.error = "cannot connect \"x\"\n\ttab";

    RunReport report;

    report.startEpochNs = 1'763'380'800'000'000'000;
    report.endEpochNs = report.startEpochNs;

    const auto text = formatSummary(options, report, status);
    const auto errors = schemaErrors(text);

    INFO(text);
    INFO(joined(errors));
    CHECK(errors.empty());
    CHECK(kit::JsonReader(text).read()["error"].asString() == status.error);
}

TEST_CASE("Summary: the file is written through <file>.tmp and an unwritable path is an error") {
    const auto directory = std::filesystem::temp_directory_path() / "nettest-summary-test";

    std::filesystem::create_directories(directory);

    const auto file = (directory / "summary.json").string();
    std::string error;

    REQUIRE(writeSummary(file, "{}\n", error));
    CHECK_FALSE(std::filesystem::exists(file + ".tmp"));

    std::ifstream stream(file, std::ios::binary);
    const std::string content((std::istreambuf_iterator<char>(stream)), std::istreambuf_iterator<char>());

    CHECK(content == "{}\n");
    stream.close();

    // An existing file is replaced.
    REQUIRE(writeSummary(file, "{\"a\":1}\n", error));

    CHECK_FALSE(writeSummary((directory / "no-such-dir" / "summary.json").string(), "{}\n", error));
    CHECK_FALSE(error.empty());
    CHECK(isWritableDirectoryOf(file));
    CHECK_FALSE(isWritableDirectoryOf((directory / "no-such-dir" / "summary.json").string()));
    CHECK_FALSE(isWritableDirectoryOf(directory.string()));

    std::filesystem::remove_all(directory);
}

TEST_CASE("Summary: the JSON strings escape the quotes, the backslash and the control characters") {
    CHECK(jsonString("a\"b\\c") == "\"a\\\"b\\\\c\"");
    CHECK(jsonString("\n\t\x01") == "\"\\n\\t\\u0001\"");
}

TEST_CASE("Summary: an interval is warm-up until the warm-up started and while it starts before its end") {
    constexpr std::int64_t warmup = 1'000'000'000;
    constexpr std::int64_t start = 5'000'000'000;

    // The warm-up has not started: no batch (`p`) or record (`c`) yet.
    CHECK(isWarmupInterval(warmup, 0, start));
    CHECK(isWarmupInterval(warmup, start, start - 1));
    CHECK(isWarmupInterval(warmup, start, start));
    CHECK(isWarmupInterval(warmup, start, start + warmup - 1));
    // The end of the warm-up belongs to the measurement.
    CHECK_FALSE(isWarmupInterval(warmup, start, start + warmup));
    CHECK_FALSE(isWarmupInterval(warmup, start, start + warmup + 1));
    // --warmup 0 measures from the start of the warm-up on.
    CHECK_FALSE(isWarmupInterval(0, start, start));
}

namespace {

IntervalRecord slipInterval(std::int64_t endEpochMs, bool warmup, std::int64_t maxNs, std::int64_t endNs) {
    IntervalRecord interval;

    interval.startEpochMs = endEpochMs - 1000;
    interval.endEpochMs = endEpochMs;
    interval.warmup = warmup;
    interval.slipMaxNs = maxNs;
    interval.slipEndNs = endNs;

    return interval;
}

} // namespace

TEST_CASE("Summary: the slip is the largest of the measured intervals and grows per second of their ends") {
    // A warm-up interval with a larger slip than any measured one, then three measured intervals over 2 s.
    const std::vector<IntervalRecord> intervals{
        slipInterval(1000, true, 90'000'000, 80'000'000),
        slipInterval(2000, false, 5'000'000, 1'000'000),
        slipInterval(3000, false, 7'000'000, 4'000'000),
        slipInterval(4000, false, 6'000'000, 9'000'000),
    };
    const auto slip = slipSummary(intervals, 9'500'000);

    CHECK(slip.maxNs == 7'000'000);
    CHECK(slip.endNs == 9'500'000);
    // (9 ms - 1 ms) over the 2 s between the ends of the first and the last measured interval.
    REQUIRE(slip.growthNsPerSecond.has_value());
    CHECK(*slip.growthNsPerSecond == 4'000'000);

    // A slip that shrinks grows negatively.
    const auto shrinking = slipSummary({slipInterval(1000, false, 8, 8), slipInterval(1500, false, 8, 2)}, 2);

    REQUIRE(shrinking.growthNsPerSecond.has_value());
    CHECK(*shrinking.growthNsPerSecond == -12);
}

TEST_CASE("Summary: without measured intervals the slip is the largest of all, and one measured has no growth") {
    const auto warmupOnly =
        slipSummary({slipInterval(1000, true, 3'000, 1'000), slipInterval(2000, true, 8'000, 2'000)}, 2'000);

    CHECK(warmupOnly.maxNs == 8'000);
    CHECK_FALSE(warmupOnly.growthNsPerSecond.has_value());

    const auto oneMeasured =
        slipSummary({slipInterval(1000, true, 3'000, 1'000), slipInterval(2000, false, 2'000, 2'000)}, 2'000);

    CHECK(oneMeasured.maxNs == 2'000);
    CHECK_FALSE(oneMeasured.growthNsPerSecond.has_value());

    CHECK(slipSummary({}, 0).maxNs == 0);
}

TEST_CASE("Summary: the runtime is the heap at the end and the collections and the CPU time between the samples") {
    constexpr std::int64_t MB = 1024 * 1024;
    RuntimeSample first;
    RuntimeSample last;

    first.heapUsedBytes = 10 * MB;
    first.heapCommittedBytes = 20 * MB;
    first.youngGcCount = 3;
    first.youngGcTimeMs = 30;
    first.fullGcCount = 1;
    first.fullGcTimeMs = 100;
    first.processCpuTimeNs = 1'000'000'000;

    last.heapUsedBytes = 50 * MB + MB / 2;
    last.heapCommittedBytes = 64 * MB;
    last.youngGcCount = 10;
    last.youngGcTimeMs = 75;
    last.fullGcCount = 2;
    last.fullGcTimeMs = 160;
    last.processCpuTimeNs = 4'000'000'000;

    // 3 s of CPU time over 2 s of the run: 150 %.
    const auto runtime = runtimeSummary(first, 1'000'000'000, last, 3'000'000'000);

    CHECK(runtime.heapUsedMb == 50);
    CHECK(runtime.heapCommittedMb == 64);
    CHECK(runtime.gcCount == 8);
    CHECK(runtime.gcTimeMs == 105);
    CHECK(runtime.cpuPercentMean == 150);

    // An absent value, a decreasing count and an empty period leave the values absent.
    last.heapUsedBytes = RUNTIME_VALUE_ABSENT;
    last.fullGcCount = 0;
    first.youngGcTimeMs = RUNTIME_VALUE_ABSENT;

    const auto partial = runtimeSummary(first, 1'000'000'000, last, 1'000'000'000);

    CHECK_FALSE(partial.heapUsedMb.has_value());
    CHECK(partial.heapCommittedMb == 64);
    CHECK_FALSE(partial.gcCount.has_value());
    CHECK_FALSE(partial.gcTimeMs.has_value());
    CHECK_FALSE(partial.cpuPercentMean.has_value());
}

} // namespace nettest
} // namespace dxfg
