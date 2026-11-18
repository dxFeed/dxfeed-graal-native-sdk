// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The C tool against the kit of the NetTest specification (docs/wrapper-guide/nettest.md): the expressions of
// Statistics.hpp and of regexes.json agree on the lines, the formatter prints the byte samples of lines.json, and the
// event pool and the symbol generator give the values of pool-vectors.json and symbols-generated-10.txt.

#include <doctest.h>
#include <dxfg_api.h>

#include <NetTest/EventPool.hpp>
#include <NetTest/Sdk.hpp>
#include <NetTest/Statistics.hpp>
#include <NetTest/SymbolPool.hpp>

#include "KitJson.hpp"

#include <cstdint>
#include <memory>
#include <regex>
#include <sstream>
#include <string>
#include <vector>

namespace dxfg {
namespace nettest {

namespace {

const std::string KIT_DIR = NETTEST_KIT_DIR;

kit::Json kitFile(const std::string &name) {
    return kit::readJson(KIT_DIR + "/" + name);
}

std::vector<std::string> splitLines(const std::string &text) {
    std::vector<std::string> lines;
    std::istringstream stream(text);
    std::string line;

    while (std::getline(stream, line)) {
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }

        lines.push_back(line);
    }

    return lines;
}

std::uint64_t hexValue(const kit::Json &value) {
    return std::stoull(value.asString(), nullptr, 16);
}

std::int64_t runtimeValue(const kit::Json &value) {
    return value.isNull() ? RUNTIME_VALUE_ABSENT : value.asInteger();
}

// The kit's runtime sample has one cumulative count and time of the collections: the C sample counts them as young.
RuntimeSample runtimeSample(const kit::Json &sample) {
    RuntimeSample result;

    result.heapUsedBytes = runtimeValue(sample["heap_used_bytes"]);
    result.heapCommittedBytes = runtimeValue(sample["heap_committed_bytes"]);
    result.youngGcCount = runtimeValue(sample["gc_count"]);
    result.youngGcTimeMs = runtimeValue(sample["gc_time_ms"]);
    result.fullGcCount = sample["gc_count"].isNull() ? RUNTIME_VALUE_ABSENT : 0;
    result.fullGcTimeMs = sample["gc_time_ms"].isNull() ? RUNTIME_VALUE_ABSENT : 0;
    result.processCpuTimeNs = runtimeValue(sample["cpu_ns"]);

    return result;
}

std::regex kitRegex(const std::string &name) {
    return std::regex(kitFile("regexes.json")["regexes"][name].asString());
}

// The array of a kit value, which must not be empty: an emptied kit file fails instead of passing with no checks.
const std::vector<kit::Json> &nonEmpty(const kit::Json &value) {
    REQUIRE(!value.array.empty());

    return value.array;
}

} // namespace

TEST_CASE("Kit: the digit grouping of lines.json") {
    const auto lines = kitFile("lines.json");

    for (const auto &entry : nonEmpty(lines["grouping"])) {
        CHECK(groupDigits(entry["value"].asInteger()) == entry["text"].asString());
    }
}

TEST_CASE("Kit: the formatter prints the producer blocks of lines.json byte for byte") {
    std::size_t producers = 0;

    const auto lines = kitFile("lines.json");

    for (const auto &entry : nonEmpty(lines["statistics"])) {
        if (entry["side"].asString() != "producer") {
            continue;
        }

        StatisticsSnapshot snapshot;

        snapshot.name = entry["name"].asString();
        snapshot.periodNanos = entry["interval_ns"].asInteger();
        snapshot.connections = entry["connections"].asInteger();

        for (const auto &instance : nonEmpty(entry["instances"])) {
            snapshot.deltas.push_back({instance["records"].asInteger(), instance["weighted_ns"].asInteger()});
        }

        CHECK(formatStatistics(snapshot) == entry["text"].asString());
        ++producers;
    }

    CHECK(producers >= 3);
}

TEST_CASE("Kit: the formatter prints the runtime lines of lines.json byte for byte") {
    const auto entries = kitFile("lines.json")["runtime"].array;

    REQUIRE(!entries.empty());

    for (const auto &entry : entries) {
        CHECK(formatRuntime(entry["name"].asString(), runtimeSample(entry["previous"]), runtimeSample(entry["current"]),
                            entry["interval_ns"].asInteger()) == entry["text"].asString());
    }
}

TEST_CASE("Kit: the expressions of Statistics.hpp and of regexes.json agree on the lines of lines.json") {
    const auto lines = kitFile("lines.json");
    const std::regex cSummary(SUMMARY_LINE_REGEX);
    const std::regex cRuntime(RUNTIME_LINE_REGEX);
    const auto summary = kitRegex("summary");
    const auto instance = kitRegex("instance");
    const auto runtime = kitRegex("runtime");

    for (const auto &entry : nonEmpty(lines["statistics"])) {
        const auto blockLines = splitLines(entry["text"].asString());
        const bool producer = entry["side"].asString() == "producer";

        REQUIRE(blockLines.size() == entry["instances"].array.size() + 1);
        CHECK(std::regex_search(blockLines[0], summary));
        // The C expression is the producer's: it does not take the consumer's label and negative lags.
        CHECK(std::regex_search(blockLines[0], cSummary) == producer);

        for (std::size_t index = 1; index < blockLines.size(); ++index) {
            CHECK(std::regex_search(blockLines[index], instance));
        }
    }

    for (const auto &entry : nonEmpty(lines["runtime"])) {
        CHECK(std::regex_search(entry["text"].asString(), runtime));
        CHECK(std::regex_search(entry["text"].asString(), cRuntime));
    }

    // The lines that the tool printed: each one is of a kind, and both expressions of its kind take it.
    const auto isolateRejection = kitRegex("isolate_rejection");
    std::size_t summaries = 0;

    for (const auto &captured : nonEmpty(lines["captured"])) {
        const auto &line = captured.asString();

        if (std::regex_search(line, cSummary)) {
            CHECK(std::regex_search(line, summary));
            ++summaries;
        } else if (std::regex_search(line, cRuntime)) {
            CHECK(std::regex_search(line, runtime));
        } else {
            CHECK((std::regex_search(line, instance) || std::regex_search(line, isolateRejection)));
        }
    }

    CHECK(summaries >= 2);
}

TEST_CASE("Kit: the isolate rejection line matches its expression") {
    const auto regex = kitRegex("isolate_rejection");

    CHECK(std::regex_search(isolateRejectionLine({"-Xmx64m", "-XX:+NetTestNoSuchOption"}), regex));
    CHECK(std::regex_search(isolateRejectionLine({"-Xss1m"}), regex));
}

TEST_CASE("Kit: the event pool gives the table and the first batch of pool-vectors.json") {
    const auto vectors = kitFile("pool-vectors.json");

    for (const auto &instance : nonEmpty(vectors["instances"])) {
        const auto index = static_cast<std::uint64_t>(instance["instance"].asInteger());
        const auto seed = hexValue(instance["seed"]);
        const auto &events = instance["events"];
        const auto poolSize = static_cast<std::size_t>(events["pool_size"].asInteger());
        const auto symbols = std::make_shared<const std::vector<std::string>>(
            generateSymbols(static_cast<std::int64_t>(poolSize)));

        INFO("instance " << index);
        CHECK(seed == 0x9E3779B97F4A7C15ULL * index);

        EventPool pool(EventKind::TRADE, symbols, seed);

        for (const auto &row : nonEmpty(instance["table_rows"])) {
            const auto at = static_cast<std::size_t>(row["row"].asInteger());

            CHECK(pool.prices()[at] == row["price"].asNumber());
            CHECK(pool.sizes()[at] == row["size"].asNumber());
        }

        // The first draws make the rows 0 to 3: the price from the first draw of a row, the size from the second.
        const auto &draws = instance["first_draws"].array;

        REQUIRE(draws.size() == 8);

        for (std::size_t row = 0; row < 4; ++row) {
            CHECK(pool.prices()[row] == static_cast<double>(100 + hexValue(draws[2 * row]) % 100'000) / 100.0);
            CHECK(pool.sizes()[row] == static_cast<double>(1 + hexValue(draws[2 * row + 1]) % 1000));
        }

        const auto &first = nonEmpty(events["first_events"]);
        EventBatch batch;

        pool.fillAt(batch, first.size(), false, 1'763'380'800'123'456'789LL);
        REQUIRE(batch.size() == first.size());

        for (std::size_t at = 0; at < first.size(); ++at) {
            const auto &expected = first[at];
            const auto draw = hexValue(expected["draw"]);
            const auto *trade = reinterpret_cast<const dxfg_trade_t *>(batch.event(at));
            const auto symbolIndex = static_cast<std::size_t>(expected["symbol_index"].asInteger());
            const auto row = static_cast<std::size_t>(expected["row"].asInteger());

            INFO("event " << at);
            CHECK(symbolIndex == (draw >> 32) % poolSize);
            CHECK(row == (draw >> 8) % EventPool::TABLE_SIZE);
            CHECK(std::string(trade->trade_base.market_event.event_symbol) == (*symbols)[symbolIndex]);
            CHECK(trade->trade_base.price == expected["price"].asNumber());
            CHECK(trade->trade_base.size == expected["size"].asNumber());
            CHECK(pool.prices()[row] == expected["price"].asNumber());
            CHECK(pool.sizes()[row] == expected["size"].asNumber());
        }
    }
}

TEST_CASE("Kit: the time packing of pool-vectors.json") {
    const auto vectors = kitFile("pool-vectors.json");

    for (const auto &entry : nonEmpty(vectors["pack_time_sequence"])) {
        const auto epochNanos = entry["epoch_ns"].asInteger();

        CHECK(static_cast<std::uint64_t>(packTimeSequence(
                  epochNanos, static_cast<std::uint32_t>(entry["sequence"].asInteger()))) == hexValue(entry["packed"]));
        CHECK(epochNanos / 1'000'000 == entry["time_ms"].asInteger());
        CHECK(epochNanos % 1'000'000 == entry["nano_part"].asInteger());
    }
}

TEST_CASE("Kit: the first generated symbols are those of symbols-generated-10.txt") {
    const auto expected = splitLines(kit::readText(KIT_DIR + "/symbols-generated-10.txt"));

    REQUIRE(expected.size() == 10);
    CHECK(generateSymbols(10) == expected);
}

} // namespace nettest
} // namespace dxfg
