// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The C tool against the kit of the NetTest specification (docs/wrapper-guide/nettest.md): the expressions of
// Statistics.hpp and of regexes.json agree on the lines, the formatter prints the byte samples of lines.json, and the
// event pool and the symbol generator give the values of pool-vectors.json, symbols-generated-10.txt and
// symbols-generated-vectors.json, and the parser gives the settings and the errors of cli-vectors.json.

#include <doctest.h>
#include <dxfg_api.h>

#include <NetTest/Args.hpp>
#include <NetTest/EventPool.hpp>
#include <NetTest/Sdk.hpp>
#include <NetTest/Statistics.hpp>
#include <NetTest/SymbolPool.hpp>

#include "KitJson.hpp"

#include <algorithm>
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

TEST_CASE("Kit: the formatter prints the blocks of both sides of lines.json byte for byte") {
    std::size_t producers = 0;
    std::size_t consumers = 0;

    const auto lines = kitFile("lines.json");

    for (const auto &entry : nonEmpty(lines["statistics"])) {
        const bool producer = entry["side"].asString() == "producer";
        StatisticsSnapshot snapshot;

        snapshot.side = producer ? Side::PRODUCER : Side::CONSUMER;
        snapshot.name = entry["name"].asString();
        snapshot.periodNanos = entry["interval_ns"].asInteger();
        snapshot.connections = entry["connections"].asInteger();

        for (const auto &instance : nonEmpty(entry["instances"])) {
            snapshot.deltas.push_back(
                {instance["records"].asInteger(), instance[producer ? "weighted_ns" : "lag_sum_ns"].asInteger()});
        }

        CHECK(formatStatistics(snapshot) == entry["text"].asString());
        ++(producer ? producers : consumers);
    }

    CHECK(producers >= 3);
    CHECK(consumers >= 2);
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

        REQUIRE(blockLines.size() == entry["instances"].array.size() + 1);
        CHECK(std::regex_search(blockLines[0], summary));
        CHECK(std::regex_search(blockLines[0], cSummary));

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

namespace {

// A setting of the summary config as the parser gives it, in the text of a JSON value (null when absent).
std::string settingOf(const Options &options, const std::string &key) {
    const auto quoted = [](const std::string &text) {
        return "\"" + text + "\"";
    };
    const auto number = [](const std::optional<std::int64_t> &value) {
        return value.has_value() ? std::to_string(*value) : std::string("null");
    };
    const bool producer = options.mode == Mode::PRODUCER;

    if (key == "side") {
        return quoted(producer ? "producer" : "consumer");
    }

    if (key == "address") {
        return quoted(options.address);
    }

    if (key == "symbols") {
        return std::to_string(options.totalSymbols);
    }

    if (key == "symbols_source") {
        return quoted(options.symbolsFile.empty() ? "generated" : "file");
    }

    if (key == "instances") {
        return std::to_string(options.instances);
    }

    if (key == "rate_limit") {
        return producer ? number(options.recordsPerSecond) : "null";
    }

    if (key == "batch") {
        return producer ? std::to_string(options.batch) : "null";
    }

    if (key == "event") {
        return quoted(eventName(options.event));
    }

    if (key == "collector") {
        return options.collector.has_value() ? quoted(collectorName(*options.collector)) : "null";
    }

    if (key == "role") {
        return quoted(roleName(endpointRole(options)));
    }

    if (key == "reuse") {
        return producer ? (options.reuse ? "true" : "false") : "null";
    }

    if (key == "period_ms") {
        return std::to_string(options.periodMillis);
    }

    // The warm-up is not an option of this tool yet.
    if (key == "warmup_ms") {
        return "null";
    }

    if (key == "duration_ms") {
        return number(options.durationMillis);
    }

    if (key == "name") {
        return quoted(options.name);
    }

    if (key == "conflated") {
        return isConflated(options) ? "true" : "false";
    }

    if (key == "isolate_arguments") {
        std::string text = "[";

        for (const auto &argument : options.isolateArguments) {
            text += (text.size() > 1 ? "," : "") + quoted(argument);
        }

        return text + "]";
    }

    return "unknown setting " + key;
}

// The kit value as settingOf() prints it.
std::string kitValueText(const kit::Json &value) {
    switch (value.kind) {
    case kit::Json::Kind::NUL:
        return "null";
    case kit::Json::Kind::BOOLEAN:
        return value.boolean ? "true" : "false";
    case kit::Json::Kind::INTEGER:
        return std::to_string(value.integer);
    case kit::Json::Kind::STRING:
        return "\"" + value.string + "\"";
    case kit::Json::Kind::ARRAY: {
        std::string text = "[";

        for (const auto &element : value.array) {
            text += (text.size() > 1 ? "," : "") + kitValueText(element);
        }

        return text + "]";
    }
    default:
        return "unsupported kit value";
    }
}

// The sections of cli-vectors.json that name the options of later tickets (--warmup, --summary, --metrics-port).
bool isLaterSection(const std::string &section) {
    return section == "consumer" || section == "summary" || section == "metrics";
}

// The tool has the isolate arguments and the legacy publishing call, so an entry that needs their absence is skipped.
bool needsAbsentFeature(const kit::Json &entry) {
    if (!entry.has("requires")) {
        return false;
    }

    const auto &features = entry["requires"].array;

    return std::any_of(features.begin(), features.end(), [](const kit::Json &feature) {
        return !feature.asString().empty() && feature.asString()[0] == '!';
    });
}

} // namespace

TEST_CASE("Kit: the parser gives the settings and the errors of cli-vectors.json") {
    std::size_t checked = 0;
    std::size_t skipped = 0;
    const auto vectors = kitFile("cli-vectors.json");

    for (const auto &entry : nonEmpty(vectors["vectors"])) {
        if (isLaterSection(entry["section"].asString()) || needsAbsentFeature(entry)) {
            ++skipped;

            continue;
        }

        std::vector<std::string> args{"NetTest"};
        std::string commandLine;

        for (const auto &argument : entry["args"].array) {
            args.push_back(argument.asString());
            commandLine += " " + argument.asString();
        }

        const auto result = parseArguments(args);

        INFO("NetTest" << commandLine << " -> " << (result.ok ? "ok" : result.error));

        if (entry["exit"].asInteger() == 0) {
            REQUIRE(result.ok);

            if (entry.has("stdout_contains")) {
                CHECK(result.options.help);
                CHECK(usageText().find(entry["stdout_contains"].asString()) != std::string::npos);
            } else {
                CHECK_FALSE(result.options.help);
            }

            if (entry.has("settings")) {
                for (const auto &[key, value] : entry["settings"].object) {
                    INFO("the setting " << key);
                    CHECK(settingOf(result.options, key) == kitValueText(value));
                }
            }
        } else {
            CHECK_FALSE(result.ok);

            if (entry.has("error")) {
                CHECK(result.error == entry["error"].asString());
            } else if (entry.has("error_prefix")) {
                CHECK(result.error.rfind(entry["error_prefix"].asString(), 0) == 0);
            }
        }

        ++checked;
    }

    MESSAGE("cli-vectors.json: " << checked << " checked, " << skipped << " skipped (later tickets or !features)");
    CHECK(checked >= 60);
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

TEST_CASE("Kit: the generated symbols give the samples and the hashes of symbols-generated-vectors.json") {
    const auto vectors = kitFile("symbols-generated-vectors.json");

    for (const auto &entry : nonEmpty(vectors["cases"])) {
        const auto total = entry["total"].asInteger();
        const auto symbols = generateSymbols(total);

        CAPTURE(total);
        REQUIRE(static_cast<std::int64_t>(symbols.size()) == total);

        for (const auto &sample : nonEmpty(entry["samples"])) {
            const auto index = sample["index"].asInteger();

            REQUIRE(index >= 0);
            REQUIRE(index < total);
            CHECK(symbols[static_cast<std::size_t>(index)] == sample["symbol"].asString());
        }

        std::uint64_t hash = 0xCBF29CE484222325ULL;

        for (const auto &symbol : symbols) {
            for (const auto byte : symbol + '\n') {
                hash = (hash ^ static_cast<unsigned char>(byte)) * 0x100000001B3ULL;
            }
        }

        CHECK(hash == hexValue(entry["fnv1a64"]));
    }
}

} // namespace nettest
} // namespace dxfg
