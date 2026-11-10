// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The core of the NetTest publisher tool (src/apps/NetTest) in one process: the command line (the options, the
// rejected QD flags, the validation, the durations and rates, the -D properties), the symbol pool (an IPF file in file
// order, the deterministic generated symbols) and the event pools (the batch fill of Trade and TimeAndSale with and
// without reuse), the rate limiter, the instances with their generators, the statistics formatter and the reporter.

#ifdef _WIN32
#    ifndef WIN32_LEAN_AND_MEAN
#        define WIN32_LEAN_AND_MEAN
#    endif
#    include <winsock2.h>
#else
#    include <arpa/inet.h>
#    include <netinet/in.h>
#    include <sys/socket.h>
#    include <unistd.h>
#endif

#include <doctest.h>
#include <dxfg_api.h>

#include <NetTest/Args.hpp>
#include <NetTest/EventPool.hpp>
#include <NetTest/RateLimiter.hpp>
#include <NetTest/Run.hpp>
#include <NetTest/Statistics.hpp>
#include <NetTest/StopControl.hpp>
#include <NetTest/SymbolPool.hpp>

#include "Hub.hpp"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <csignal>
#include <cstdint>
#include <cstdio>
#include <filesystem>
#include <fstream>
#include <memory>
#include <regex>
#include <set>
#include <sstream>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace dxfg {
namespace nettest {

namespace {

const std::string FIXTURE = std::string(NETTEST_TEST_DATA_DIR) + "/symbols.ipf";

ParseResult parse(std::vector<std::string> args) {
    args.insert(args.begin(), "NetTest");

    return parseArguments(args);
}

// Checks that the parsing failed and that the message contains the text.
void requireRejected(const std::vector<std::string> &args, const std::string &text) {
    const auto result = parse(args);

    INFO("the message: " << result.error);
    CHECK_FALSE(result.ok);
    CHECK(result.error.find(text) != std::string::npos);
}

graal_isolatethread_t *isolateThread() {
    static test::Isolate isolate;

    REQUIRE(isolate.thread != nullptr);

    return isolate.thread;
}

// A file of the system temp directory, removed at the end.
struct TempFile {
    std::string path;

    TempFile(const std::string &name, const std::string &content)
        : path((std::filesystem::temp_directory_path() / name).string()) {
        std::ofstream(path, std::ios::binary) << content;
    }

    TempFile(const TempFile &) = delete;
    TempFile &operator=(const TempFile &) = delete;

    ~TempFile() {
        std::remove(path.c_str());
    }
};

std::int64_t timeMillisOf(std::int64_t packed) {
    return (packed >> 32) * 1000 + ((packed >> 22) & 0x3FF);
}

std::int64_t nowMillis() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch())
        .count();
}

std::set<std::string> asSet(const std::vector<std::string> &symbols) {
    return {symbols.begin(), symbols.end()};
}

} // namespace

TEST_CASE("U1: the defaults of a minimal command line") {
    const auto result = parse({"p", ":7000", "-S", "10"});

    REQUIRE(result.ok);
    CHECK(result.options.address == ":7000");
    CHECK(result.options.instances == 1);
    CHECK(result.options.totalSymbols == 10);
    CHECK(result.options.symbolsFile.empty());
    CHECK(result.options.batch == 100);
    CHECK(result.options.event == EventKind::TRADE);
    CHECK(result.options.periodMillis == 10'000);
    CHECK(result.options.name == "NetTest");
    CHECK_FALSE(result.options.recordsPerSecond.has_value());
    CHECK_FALSE(result.options.durationMillis.has_value());
    CHECK_FALSE(result.options.reuse);
    CHECK_FALSE(result.options.legacy);
    CHECK(result.options.properties.empty());
}

TEST_CASE("U1: every option lands in the options") {
    SUBCASE("rate with M") {
        const auto result = parse({"p", ":7000", "-S", "1", "-r", "*/1M"});

        REQUIRE(result.ok);
        CHECK(result.options.recordsPerSecond == 1'000'000);
    }

    SUBCASE("rate with k") {
        const auto result = parse({"p", ":7000", "-S", "1", "-r", "*/500k"});

        REQUIRE(result.ok);
        CHECK(result.options.recordsPerSecond == 500'000);
    }

    SUBCASE("rate with G and without a suffix") {
        CHECK(parse({"p", ":7000", "-S", "1", "-r", "*/2G"}).options.recordsPerSecond == 2'000'000'000);
        CHECK(parse({"p", ":7000", "-S", "1", "-r", "*/750"}).options.recordsPerSecond == 750);
        CHECK(parse({"p", ":7000", "-S", "1", "-r", "*/2.5M"}).options.recordsPerSecond == 2'500'000);
    }

    SUBCASE("period, duration, batch, event, name and the switches") {
        const auto result = parse({"p", "127.0.0.1:7000", "-S", "1", "-s", "1s", "--duration", "3s", "--batch", "1",
                                   "--event", "TimeAndSale", "--reuse", "--legacy", "-n", "run1", "-C", "3"});

        REQUIRE(result.ok);
        CHECK(result.options.periodMillis == 1000);
        CHECK(result.options.durationMillis == 3000);
        CHECK(result.options.batch == 1);
        CHECK(result.options.event == EventKind::TIME_AND_SALE);
        CHECK(result.options.reuse);
        CHECK(result.options.legacy);
        CHECK(result.options.name == "run1");
        CHECK(result.options.instances == 3);
    }

    SUBCASE("system properties keep their order and the value with '='") {
        const auto result =
            parse({"p", ":7000", "-S", "1", "-Ddxscheme.nanoTime=true", "-Dmonitoring.stat=10s", "-Dk=a=b", "-Dempty="});

        REQUIRE(result.ok);
        REQUIRE(result.options.properties.size() == 4);
        CHECK(result.options.properties[0] == std::make_pair(std::string("dxscheme.nanoTime"), std::string("true")));
        CHECK(result.options.properties[1] == std::make_pair(std::string("monitoring.stat"), std::string("10s")));
        CHECK(result.options.properties[2] == std::make_pair(std::string("k"), std::string("a=b")));
        CHECK(result.options.properties[3] == std::make_pair(std::string("empty"), std::string("")));
    }

    SUBCASE("flags may come before the address") {
        const auto result = parse({"p", "-S", "5", "--reuse", ":7000"});

        REQUIRE(result.ok);
        CHECK(result.options.address == ":7000");
        CHECK(result.options.totalSymbols == 5);
    }
}

TEST_CASE("U1: -S keeps the total and the file") {
    const auto result = parse({"p", ":7000", "-S", "500000@symbols.ipf"});

    REQUIRE(result.ok);
    CHECK(result.options.totalSymbols == 500'000);
    CHECK(result.options.symbolsFile == "symbols.ipf");

    // A path with a slash or a drive letter is a file, not a QD symbol split.
    const auto path = parse({"p", ":7000", "-S", "7@C:/data/some/symbols.ipf"});

    REQUIRE(path.ok);
    CHECK(path.options.symbolsFile == "C:/data/some/symbols.ipf");
}

TEST_CASE("U1: the QD flags that are not implemented are rejected by name") {
    requireRejected({"c", "127.0.0.1:7000", "-S", "10"}, "mode 'c'");
    requireRejected({"p", ":7000", "-S", "10", "-c", "stream"}, "-c");
    requireRejected({"p", ":7000", "-S", "10/r5"}, "/r");
    requireRejected({"p", ":7000", "-S", "10/s5"}, "/s");
    requireRejected({"p", ":7000", "-S", "10{3}"}, "{len,max}");
    requireRejected({"p", ":7000", "-S", "10{3,5}"}, "{len,max}");
    requireRejected({"p", ":7000", "-S", "10", "-r", "*/1M;5s"}, ";<time>");
    requireRejected({"x", ":7000", "-S", "10"}, "unknown mode");
}

TEST_CASE("U1: -C above 1 needs a connecting address") {
    requireRejected({"p", ":7000", "-S", "10", "-C", "2"}, "-C 2");

    const auto result = parse({"p", "127.0.0.1:7000", "-S", "10", "-C", "2"});

    REQUIRE(result.ok);
    CHECK(result.options.instances == 2);

    CHECK(parse({"p", ":7000", "-S", "10", "-C", "1"}).ok);
    CHECK(isServerAddress(":7000"));
    CHECK_FALSE(isServerAddress("localhost:7000"));
    CHECK_FALSE(isServerAddress(""));
}

TEST_CASE("U1: invalid command lines are rejected") {
    requireRejected({}, "no mode");
    requireRejected({"p", "-S", "10"}, "address");
    requireRejected({"p", ":7000"}, "-S is missing");
    requireRejected({"p", ":7000", "-S", "10", "--batch", "0"}, "--batch");
    requireRejected({"p", ":7000", "-S", "10", "--batch", "-5"}, "--batch");
    requireRejected({"p", ":7000", "-S", "10", "--batch", "x"}, "--batch");
    // A batch shares one time: above 2^22 events its 22-bit sequence would wrap.
    requireRejected({"p", ":7000", "-S", "10", "--batch", "4194305"}, "--batch");
    CHECK(parse({"p", ":7000", "-S", "10", "--batch", "4194304"}).ok);
    requireRejected({"p", ":7000", "-S", "10", "-r", "*/0"}, "-r");
    requireRejected({"p", ":7000", "-S", "10", "-r", "*/-1M"}, "-r");
    requireRejected({"p", ":7000", "-S", "10", "-r", "*/abc"}, "-r");
    requireRejected({"p", ":7000", "-S", "10", "-r", "1M"}, "-r");
    requireRejected({"p", ":7000", "-S", "10", "-s", "0s"}, "-s");
    requireRejected({"p", ":7000", "-S", "10", "-s", "10"}, "-s");
    requireRejected({"p", ":7000", "-S", "10", "--duration", "0s"}, "--duration");
    requireRejected({"p", ":7000", "-S", "10", "--duration", "5h"}, "--duration");
    requireRejected({"p", ":7000", "-S", "10", "--event", "Quote"}, "--event");
    requireRejected({"p", ":7000", "-S", "10", "-C", "0"}, "-C");
    requireRejected({"p", ":7000", "-S", "0"}, "-S");
    requireRejected({"p", ":7000", "-S", "abc"}, "-S");
    requireRejected({"p", ":7000", "-S", "10@"}, "-S");
    requireRejected({"p", ":7000", "-S", "10", "-n", ""}, "-n");
    requireRejected({"p", ":7000", "-S", "10", "--unknown"}, "--unknown");
    requireRejected({"p", ":7000", "-S", "10", "-Dnovalue"}, "-D");
    requireRejected({"p", ":7000", "-S", "10", "-D=value"}, "-D");
    requireRejected({"p", ":7000", "extra", "-S", "10"}, "extra");
}

TEST_CASE("U1: a flag with a missing value is rejected") {
    for (const auto *flag : {"-S", "-C", "-r", "-s", "-n", "--batch", "--event", "--duration"}) {
        requireRejected({"p", ":7000", "-S", "10", flag}, flag);
    }
}

TEST_CASE("U1: durations and rates") {
    CHECK(parseDuration("500ms") == 500);
    CHECK(parseDuration("10s") == 10'000);
    CHECK(parseDuration("2m") == 120'000);
    CHECK_FALSE(parseDuration("10").has_value());
    CHECK_FALSE(parseDuration("s").has_value());
    CHECK_FALSE(parseDuration("ms").has_value());
    CHECK_FALSE(parseDuration("0ms").has_value());
    CHECK_FALSE(parseDuration("-1s").has_value());
    CHECK_FALSE(parseDuration("1.5s").has_value());
    CHECK_FALSE(parseDuration("99999999999999999999s").has_value());

    CHECK(parseRate("1") == 1);
    CHECK(parseRate("500k") == 500'000);
    CHECK(parseRate("1M") == 1'000'000);
    CHECK(parseRate("1G") == 1'000'000'000);
    CHECK(parseRate("0.5k") == 500);
    CHECK_FALSE(parseRate("0").has_value());
    CHECK_FALSE(parseRate("0.1").has_value());
    CHECK_FALSE(parseRate("").has_value());
    CHECK_FALSE(parseRate("k").has_value());
    CHECK_FALSE(parseRate("1.2.3").has_value());
    CHECK_FALSE(parseRate("1e3").has_value());
    CHECK_FALSE(parseRate("+5").has_value());
    CHECK_FALSE(parseRate("9999999999999G").has_value());
}

TEST_CASE("U1: the usage text names the modes and the flags") {
    const auto usage = usageText();

    CHECK(usage.find("Usage: NetTest p") != std::string::npos);

    for (const auto *flag : {"-S", "-C", "-r", "-s", "-n", "--batch", "--event", "--reuse", "--legacy", "--duration",
                             "-D<key>=<value>"}) {
        CHECK(usage.find(flag) != std::string::npos);
    }
}

TEST_CASE("U1: -D properties reach the system properties of the isolate") {
    const auto result = parse({"p", ":7000", "-S", "1", "-Dnettest.test.first=one", "-Dnettest.test.second=a=b"});
    std::string error;

    REQUIRE(result.ok);
    REQUIRE(applySystemProperties(isolateThread(), result.options, error));
    CHECK(error.empty());

    for (const auto &[key, expected] : std::vector<std::pair<std::string, std::string>>{
             {"nettest.test.first", "one"}, {"nettest.test.second", "a=b"}}) {
        const char *value = nullptr;

        REQUIRE(dxfg_v4_system_get_property(isolateThread(), key.c_str(), &value) == DXFG_EXECUTE_SUCCESSFULLY);
        REQUIRE(value != nullptr);
        CHECK(std::string(value) == expected);
        dxfg_v4_system_release_property(isolateThread(), value);
    }
}

TEST_CASE("U2: the first symbols of the IPF file in file order") {
    const auto result = readSymbolsFromFile(isolateThread(), FIXTURE, 3);

    REQUIRE(result.ok);
    CHECK(result.warning.empty());
    CHECK(*result.symbols == std::vector<std::string>{"IBM", "AAPL", "MSFT"});
}

TEST_CASE("U2: a file with fewer symbols than the total gives the smaller pool and a warning") {
    const auto result = readSymbolsFromFile(isolateThread(), FIXTURE, 10);

    REQUIRE(result.ok);
    CHECK(*result.symbols == std::vector<std::string>{"IBM", "AAPL", "MSFT", "GOOG", "TSLA"});
    CHECK(result.warning.find("5") != std::string::npos);
    CHECK(result.warning.find("10") != std::string::npos);
}

TEST_CASE("U2: the pool is built from the options") {
    const auto fromFile = parse({"p", ":7000", "-S", "2@" + FIXTURE});

    REQUIRE(fromFile.ok);

    const auto filePool = buildSymbolPool(isolateThread(), fromFile.options);

    REQUIRE(filePool.ok);
    CHECK(*filePool.symbols == std::vector<std::string>{"IBM", "AAPL"});

    const auto generated = parse({"p", ":7000", "-S", "20"});

    REQUIRE(generated.ok);

    const auto generatedPool = buildSymbolPool(isolateThread(), generated.options);

    REQUIRE(generatedPool.ok);
    CHECK(generatedPool.symbols->size() == 20);
}

TEST_CASE("U2: an IPF file without symbols and a missing file fail with a message") {
    const TempFile empty("nettest-empty.ipf", "#STOCK::=TYPE,SYMBOL,DESCRIPTION\r\n##\r\n");
    const auto noSymbols = readSymbolsFromFile(isolateThread(), empty.path, 5);

    CHECK_FALSE(noSymbols.ok);
    CHECK(noSymbols.symbols == nullptr);
    CHECK(noSymbols.error.find("no symbols") != std::string::npos);

    const auto missing = readSymbolsFromFile(isolateThread(), FIXTURE + ".missing", 5);

    CHECK_FALSE(missing.ok);
    CHECK(missing.symbols == nullptr);
    CHECK(missing.error.find(".missing") != std::string::npos);

    // The thread has no pending exception after the failure was reported.
    dxfg_v4_exception_t *exception = nullptr;

    REQUIRE(dxfg_v4_get_and_clear_thread_exception(isolateThread(), &exception) == DXFG_EXECUTE_SUCCESSFULLY);
    CHECK(exception == nullptr);
}

TEST_CASE("U2: generated symbols are distinct capital letters and the same on every run") {
    const auto first = generateSymbols(1000);
    const auto second = generateSymbols(1000);

    REQUIRE(first.size() == 1000);
    CHECK(first == second);
    CHECK(asSet(first).size() == 1000);

    std::set<std::size_t> lengths;

    for (const auto &symbol : first) {
        lengths.insert(symbol.size());
        CHECK(symbol.size() >= 1);
        CHECK(symbol.size() <= 6);
        CHECK(std::all_of(symbol.begin(), symbol.end(), [](char letter) {
            return letter >= 'A' && letter <= 'Z';
        }));
    }

    // All the lengths show up in a pool of that size.
    CHECK(lengths.size() == 6);

    // A smaller pool is the start of a bigger one: the generator is a sequence.
    const auto fewer = generateSymbols(10);

    CHECK(std::equal(fewer.begin(), fewer.end(), first.begin()));
}

TEST_CASE("U2: the generator refuses an impossible total") {
    CHECK(generateSymbols(0).empty());
    CHECK(generateSymbols(-1).empty());
    CHECK(generateSymbols(1'000'000'000).empty());
}

TEST_CASE("U2: the time sequence packing") {
    // 2026-01-01T00:00:01.234567891Z
    const std::int64_t nanos = 1'767'225'601LL * 1'000'000'000LL + 234'567'891LL;
    const auto packed = packTimeSequence(nanos, 5);

    CHECK((packed >> 32) == 1'767'225'601LL);
    CHECK(((packed >> 22) & 0x3FF) == 234);
    CHECK((packed & 0x3FFFFF) == 5);
    CHECK((packTimeSequence(nanos, 0x400005) & 0x3FFFFF) == 5);
}

namespace {

constexpr std::int64_t ALLOWED_SKEW_MILLIS = 2000;

Symbols poolSymbols(std::size_t total) {
    return std::make_shared<const std::vector<std::string>>(generateSymbols(static_cast<std::int64_t>(total)));
}

void checkTime(std::int64_t packed, std::int32_t nanoPart, std::int64_t beforeMillis, std::int64_t afterMillis) {
    CHECK(timeMillisOf(packed) >= beforeMillis);
    CHECK(timeMillisOf(packed) <= afterMillis + ALLOWED_SKEW_MILLIS);
    CHECK(nanoPart >= 0);
    CHECK(nanoPart < 1'000'000);
}

} // namespace

TEST_CASE("U2: a filled Trade batch") {
    const auto symbols = poolSymbols(50);
    EventPool pool(EventKind::TRADE, symbols, 1);
    EventBatch batch;
    const auto before = nowMillis();

    REQUIRE(pool.size() == 50);
    pool.fill(batch, 20, false);

    const auto after = nowMillis();

    REQUIRE(batch.size() == 20);
    CHECK(batch.owned());
    CHECK(batch.list()->size == 20);
    CHECK(batch.list()->elements != nullptr);

    const auto poolSymbolSet = asSet(*symbols);
    std::int64_t previousSequence = -1;

    for (std::size_t index = 0; index < batch.size(); ++index) {
        const auto *trade = reinterpret_cast<const dxfg_trade_t *>(batch.event(index));

        REQUIRE(trade->trade_base.market_event.event_type.clazz == DXFG_EVENT_TRADE);
        CHECK(poolSymbolSet.count(trade->trade_base.market_event.event_symbol) == 1);
        checkTime(trade->trade_base.time_sequence, trade->trade_base.time_nano_part, before, after);

        const auto sequence = trade->trade_base.time_sequence & 0x3FFFFF;

        CHECK(sequence == previousSequence + 1);
        previousSequence = sequence;

        CHECK(std::find(pool.prices().begin(), pool.prices().end(), trade->trade_base.price) != pool.prices().end());
        CHECK(std::find(pool.sizes().begin(), pool.sizes().end(), trade->trade_base.size) != pool.sizes().end());
    }

    // The sequence goes on in the next batch.
    pool.fill(batch, 5, false);

    CHECK((reinterpret_cast<const dxfg_trade_t *>(batch.event(0))->trade_base.time_sequence & 0x3FFFFF) == 20);
}

TEST_CASE("U2: a filled TimeAndSale batch") {
    const auto symbols = poolSymbols(50);
    EventPool pool(EventKind::TIME_AND_SALE, symbols, 2);
    EventBatch batch;
    const auto before = nowMillis();

    pool.fill(batch, 20, false);

    const auto after = nowMillis();

    REQUIRE(batch.size() == 20);

    const auto poolSymbolSet = asSet(*symbols);
    std::int64_t previousSequence = -1;

    for (std::size_t index = 0; index < batch.size(); ++index) {
        const auto *sale = reinterpret_cast<const dxfg_time_and_sale_t *>(batch.event(index));

        REQUIRE(sale->market_event.event_type.clazz == DXFG_EVENT_TIME_AND_SALE);
        CHECK(poolSymbolSet.count(sale->market_event.event_symbol) == 1);
        checkTime(sale->index, sale->time_nano_part, before, after);

        const auto sequence = sale->index & 0x3FFFFF;

        CHECK(sequence == previousSequence + 1);
        previousSequence = sequence;

        CHECK(std::find(pool.prices().begin(), pool.prices().end(), sale->price) != pool.prices().end());
        CHECK(std::find(pool.sizes().begin(), pool.sizes().end(), sale->size) != pool.sizes().end());
        CHECK(sale->event_flags == 0);
    }
}

TEST_CASE("U2: the fill at a given time packs it exactly and the sequence wraps at 22 bits") {
    EventPool pool(EventKind::TRADE, poolSymbols(3), 3);
    EventBatch batch;
    const std::int64_t nanos = 1'700'000'000LL * 1'000'000'000LL + 123'456'789LL;

    // Run the counter to the edge of 22 bits.
    for (std::uint32_t used = 0; used < EventPool::SEQUENCE_MASK - 1; used += 1000) {
        pool.fillAt(batch, std::min<std::size_t>(1000, EventPool::SEQUENCE_MASK - 1 - used), false, nanos);
    }

    CHECK(pool.nextSequence() == EventPool::SEQUENCE_MASK - 1);

    pool.fillAt(batch, 4, false, nanos);

    REQUIRE(batch.size() == 4);

    const std::int64_t expected[] = {EventPool::SEQUENCE_MASK - 1, EventPool::SEQUENCE_MASK, 0, 1};

    for (std::size_t index = 0; index < 4; ++index) {
        const auto *trade = reinterpret_cast<const dxfg_trade_t *>(batch.event(index));

        CHECK((trade->trade_base.time_sequence >> 32) == 1'700'000'000LL);
        CHECK(((trade->trade_base.time_sequence >> 22) & 0x3FF) == 123);
        CHECK((trade->trade_base.time_sequence & 0x3FFFFF) == expected[index]);
        CHECK(trade->trade_base.time_nano_part == 456'789);
    }
}

TEST_CASE("U2: the pools of two seeds differ in prices and the same seed gives the same table") {
    const auto symbols = poolSymbols(10);
    const EventPool first(EventKind::TRADE, symbols, 1);
    const EventPool same(EventKind::TRADE, symbols, 1);
    const EventPool other(EventKind::TRADE, symbols, 2);

    CHECK(first.prices() == same.prices());
    CHECK(first.sizes() == same.sizes());
    CHECK(first.prices() != other.prices());
    CHECK(first.prices().size() == EventPool::TABLE_SIZE);

    for (std::size_t index = 0; index < first.prices().size(); ++index) {
        CHECK(first.prices()[index] >= 1.0);
        CHECK(first.prices()[index] < 1001.0);
        CHECK(first.sizes()[index] >= 1.0);
        CHECK(first.sizes()[index] <= 1000.0);
    }
}

TEST_CASE("U2: with reuse the batch points at the pool events") {
    for (const auto kind : {EventKind::TRADE, EventKind::TIME_AND_SALE}) {
        const auto symbols = poolSymbols(30);
        EventPool pool(kind, symbols, 4);
        EventBatch batch;

        pool.fill(batch, 10, true);

        REQUIRE(batch.size() == 10);
        CHECK_FALSE(batch.owned());

        std::set<const dxfg_event_type_t *> poolEvents;

        for (std::size_t index = 0; index < pool.size(); ++index) {
            poolEvents.insert(pool.poolEvent(index));
        }

        for (std::size_t index = 0; index < batch.size(); ++index) {
            CHECK(poolEvents.count(batch.event(index)) == 1);
        }

        // The list of the batch is the array of the same pointers.
        CHECK(batch.list()->size == 10);
        CHECK(batch.list()->elements[0] == batch.event(0));

        // Clearing the batch of a reusing fill leaves the pool events alone.
        batch.clear();
        CHECK(batch.size() == 0);
        CHECK(batch.list()->size == 0);

        for (std::size_t index = 0; index < pool.size(); ++index) {
            const auto *event = pool.poolEvent(index);
            const char *symbol = kind == EventKind::TRADE
                                     ? reinterpret_cast<const dxfg_trade_t *>(event)->trade_base.market_event.event_symbol
                                     : reinterpret_cast<const dxfg_time_and_sale_t *>(event)->market_event.event_symbol;

            CHECK(std::string(symbol) == (*symbols)[index]);
        }
    }
}

TEST_CASE("U2: with reuse a rewritten pool event carries the new time and sequence") {
    EventPool pool(EventKind::TRADE, poolSymbols(1), 5);
    EventBatch batch;

    pool.fillAt(batch, 1, true, 1'700'000'000LL * 1'000'000'000LL);
    pool.fillAt(batch, 1, true, 1'700'000'001LL * 1'000'000'000LL + 5);

    const auto *trade = reinterpret_cast<const dxfg_trade_t *>(batch.event(0));

    CHECK((trade->trade_base.time_sequence >> 32) == 1'700'000'001LL);
    CHECK((trade->trade_base.time_sequence & 0x3FFFFF) == 1);
    CHECK(trade->trade_base.time_nano_part == 5);
}

TEST_CASE("U2: without reuse the batch owns copies and freeing it leaves the pool intact") {
    for (const auto kind : {EventKind::TRADE, EventKind::TIME_AND_SALE}) {
        const auto symbols = poolSymbols(30);
        EventPool pool(kind, symbols, 6);
        EventBatch batch;

        pool.fill(batch, 10, true);
        pool.fill(batch, 10, false);

        REQUIRE(batch.size() == 10);
        CHECK(batch.owned());

        std::set<const dxfg_event_type_t *> poolEvents;
        std::set<const char *> poolSymbolPointers;

        for (std::size_t index = 0; index < pool.size(); ++index) {
            poolEvents.insert(pool.poolEvent(index));
            poolSymbolPointers.insert((*symbols)[index].c_str());
        }

        for (std::size_t index = 0; index < batch.size(); ++index) {
            CHECK(poolEvents.count(batch.event(index)) == 0);

            // The symbol string is a copy as well.
            const char *symbol = kind == EventKind::TRADE
                                     ? reinterpret_cast<const dxfg_trade_t *>(batch.event(index))
                                           ->trade_base.market_event.event_symbol
                                     : reinterpret_cast<const dxfg_time_and_sale_t *>(batch.event(index))
                                           ->market_event.event_symbol;

            CHECK(poolSymbolPointers.count(symbol) == 0);
            CHECK(std::find(symbols->begin(), symbols->end(), symbol) != symbols->end());
        }

        batch.clear();
        CHECK(batch.size() == 0);
        CHECK_FALSE(batch.owned());

        // The pool still works and holds its symbols.
        pool.fill(batch, 10, false);
        CHECK(batch.size() == 10);

        for (std::size_t index = 0; index < pool.size(); ++index) {
            const auto *event = pool.poolEvent(index);
            const char *symbol = kind == EventKind::TRADE
                                     ? reinterpret_cast<const dxfg_trade_t *>(event)->trade_base.market_event.event_symbol
                                     : reinterpret_cast<const dxfg_time_and_sale_t *>(event)->market_event.event_symbol;

            CHECK(std::string(symbol) == (*symbols)[index]);
        }
    }
}

TEST_CASE("U2: the batches are empty for an empty request and an empty pool") {
    EventBatch batch;
    EventPool pool(EventKind::TRADE, poolSymbols(5), 7);

    pool.fill(batch, 0, false);
    CHECK(batch.size() == 0);
    CHECK(batch.list()->size == 0);

    EventPool empty(EventKind::TIME_AND_SALE, std::make_shared<const std::vector<std::string>>(), 8);

    empty.fill(batch, 10, true);
    CHECK(batch.size() == 0);
}

TEST_CASE("U2: a batch of a pool event list can be published to a LOCAL_HUB endpoint") {
    test::Hub hub(isolateThread());

    REQUIRE(hub.ready);

    for (const auto kind : {EventKind::TRADE, EventKind::TIME_AND_SALE}) {
        for (const bool reuse : {true, false}) {
            EventPool pool(kind, poolSymbols(10), 9);
            EventBatch batch;

            pool.fill(batch, 25, reuse);

            dxfg_v4_event_type_list *list = batch.list();

            CHECK(dxfg_v4_DXPublisher_publishEvents(isolateThread(), hub.publisher, list) == DXFG_EXECUTE_SUCCESSFULLY);
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// U3: the rate limiter, the instances and the generators
// ---------------------------------------------------------------------------------------------------------------------

namespace {

using Steady = std::chrono::steady_clock;

#ifdef _WIN32
using NativeSocket = SOCKET;
constexpr NativeSocket INVALID_NATIVE_SOCKET = INVALID_SOCKET;
using SocketAddressLength = int;
#else
using NativeSocket = int;
constexpr NativeSocket INVALID_NATIVE_SOCKET = -1;
using SocketAddressLength = socklen_t;
#endif

// Returns a free loopback port: binds port 0, reads the port the OS picked and closes the socket.
std::uint16_t findFreeLoopbackPort() {
#ifdef _WIN32
    WSADATA wsaData{};

    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        throw std::runtime_error("findFreeLoopbackPort: WSAStartup failed");
    }
#endif

    const NativeSocket probe = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    sockaddr_in address{};
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    address.sin_port = 0; // the OS picks a free port

    SocketAddressLength length = sizeof(address);
    const bool isBound = probe != INVALID_NATIVE_SOCKET &&
                         bind(probe, reinterpret_cast<sockaddr *>(&address), sizeof(address)) == 0 &&
                         getsockname(probe, reinterpret_cast<sockaddr *>(&address), &length) == 0;

#ifdef _WIN32
    if (probe != INVALID_NATIVE_SOCKET) {
        closesocket(probe);
    }

    WSACleanup();
#else
    if (probe != INVALID_NATIVE_SOCKET) {
        close(probe);
    }
#endif

    if (!isBound) {
        throw std::runtime_error("findFreeLoopbackPort: socket() or bind() failed");
    }

    return ntohs(address.sin_port);
}

std::string freePort() {
    return std::to_string(findFreeLoopbackPort());
}

double secondsSince(Steady::time_point start) {
    return std::chrono::duration<double>(Steady::now() - start).count();
}

// Takes batches with acquire() on the calling thread until the deadline; counts those taken before it.
int takeBatchesUntil(RateLimiter &limiter, std::int64_t batch, Steady::time_point deadline) {
    int taken = 0;

    while (limiter.acquire(batch)) {
        if (Steady::now() > deadline) {
            break;
        }

        ++taken;
    }

    return taken;
}

// The result of a run of the tool with its output and its length.
struct Outcome {
    RunResult result{};
    std::string output{};
    double seconds = 0;
};

// Runs the tool on this thread (it must be the thread of the isolate) with the arguments after `p`.
Outcome runTool(std::vector<std::string> args, StopControl &stop) {
    args.insert(args.begin(), "p");

    const auto parsed = parse(args);

    REQUIRE_MESSAGE(parsed.ok, parsed.error);

    const auto symbols = buildSymbolPool(isolateThread(), parsed.options);

    REQUIRE_MESSAGE(symbols.ok, symbols.error);

    std::ostringstream output;
    const auto start = Steady::now();
    Outcome outcome;

    outcome.result = runNetTest(isolateThread(), parsed.options, symbols.symbols, stop, output);
    outcome.seconds = secondsSince(start);
    outcome.output = output.str();

    return outcome;
}

Outcome runTool(const std::vector<std::string> &args) {
    StopControl stop;

    return runTool(args, stop);
}

std::vector<std::string> linesOf(const std::string &text) {
    std::vector<std::string> lines;
    std::istringstream stream(text);
    std::string line;

    while (std::getline(stream, line)) {
        lines.push_back(line);
    }

    return lines;
}

// The summary lines of the output.
std::vector<std::string> summaryLines(const std::string &output) {
    std::vector<std::string> summaries;
    const std::regex summary(SUMMARY_LINE_REGEX);

    for (const auto &line : linesOf(output)) {
        if (std::regex_match(line, summary)) {
            summaries.push_back(line);
        }
    }

    return summaries;
}

// A PUBLISHER endpoint of the test that listens on the address; closed at the end.
struct Occupant {
    graal_isolatethread_t *thread;
    dxfg_v4_endpoint_t *endpoint = nullptr;
    bool connected = false;

    Occupant(graal_isolatethread_t *thread, const std::string &address) : thread(thread) {
        connected = dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_PUBLISHER, &endpoint) == 0 &&
                    dxfg_v4_DXEndpoint_connect(thread, endpoint, address.c_str()) == 0;
    }

    Occupant(const Occupant &) = delete;
    Occupant &operator=(const Occupant &) = delete;

    ~Occupant() {
        if (endpoint != nullptr) {
            dxfg_v4_DXEndpoint_closeAndAwaitTermination(thread, endpoint);
            dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);
        }
    }
};

} // namespace

TEST_CASE("U3: the limiter at 10,000 records per second with a batch of 100 gives 100 batches in a second") {
    StopControl stop;
    RateLimiter limiter(10'000, 100, stop);
    const auto deadline = Steady::now() + std::chrono::seconds(1);
    const auto taken = takeBatchesUntil(limiter, 100, deadline);

    INFO("batches taken: " << taken);
    CHECK(taken >= 98);
    CHECK(taken <= 102);
}

TEST_CASE("U3: after an idle second the bucket holds one second of tokens and no more") {
    StopControl stop;
    RateLimiter limiter(1000, 10, stop);

    CHECK(limiter.capacity() == doctest::Approx(1000.0));
    CHECK_FALSE(limiter.tryAcquire(10)); // the bucket starts empty

    std::this_thread::sleep_for(std::chrono::milliseconds(1300));

    int taken = 0;

    while (limiter.tryAcquire(10)) {
        ++taken;
    }

    INFO("batches taken after the idle second: " << taken);
    CHECK(taken >= 99);
    CHECK(taken <= 101);
}

TEST_CASE("U3: two generators share the rate of one limiter") {
    StopControl stop;
    RateLimiter limiter(10'000, 100, stop);
    const auto deadline = Steady::now() + std::chrono::seconds(1);
    std::atomic<int> first{0};
    std::atomic<int> second{0};

    std::thread firstThread([&] {
        first = takeBatchesUntil(limiter, 100, deadline);
    });
    std::thread secondThread([&] {
        second = takeBatchesUntil(limiter, 100, deadline);
    });

    firstThread.join();
    secondThread.join();

    INFO("batches taken: " << first << " and " << second);
    CHECK(first + second >= 98);
    CHECK(first + second <= 102);
    CHECK(first > 0);
    CHECK(second > 0);
}

TEST_CASE("U3: a rate below the batch size releases a batch every batch/rate seconds") {
    StopControl stop;
    RateLimiter limiter(50, 100, stop);

    CHECK(limiter.capacity() == doctest::Approx(100.0)); // one batch, not one second of 50 tokens

    const auto start = Steady::now();

    REQUIRE(limiter.acquire(100));

    const auto seconds = secondsSince(start);

    INFO("the first batch after " << seconds << " s");
    CHECK(seconds >= 1.8);
    CHECK(seconds <= 4.0);
    CHECK_FALSE(limiter.tryAcquire(100)); // no debt: the next batch needs another two seconds
}

TEST_CASE("U3: the stop during a limiter wait returns within one batch interval") {
    StopControl stop;
    RateLimiter limiter(50, 100, stop); // a batch every 2 s
    std::atomic<bool> acquired{true};
    std::atomic<double> returnedAfter{0};
    const auto start = Steady::now();

    std::thread waiting([&] {
        acquired = limiter.acquire(100);
        returnedAfter = secondsSince(start);
    });

    std::this_thread::sleep_for(std::chrono::milliseconds(200));
    stop.request();
    waiting.join();

    CHECK_FALSE(acquired);
    CHECK(returnedAfter < 1.5);
    CHECK_FALSE(limiter.acquire(100)); // a stopped limiter takes nothing
}

TEST_CASE("U3: a run of 2 seconds at 5k records per second counts about 10,000 records and exits with 0") {
    const auto port = freePort();
    const auto outcome = runTool({":" + port, "-S", "100", "-r", "*/5k", "-s", "1s", "--duration", "2s"});

    INFO("the error: " << outcome.result.error);
    INFO("the output: " << outcome.output);
    CHECK(outcome.result.exitCode == 0);
    CHECK(outcome.result.records >= 9000);
    CHECK(outcome.result.records <= 10'200);
    CHECK(outcome.seconds < 10);

    const auto summaries = summaryLines(outcome.output);

    REQUIRE_FALSE(summaries.empty());

    for (const auto &line : summaries) {
        CHECK(line.find("instances 1, connections 0") != std::string::npos);
    }

    CHECK(outcome.output.find("    NetTest.1 data ") != std::string::npos);
}

TEST_CASE("U3: the legacy publishing call, the reuse and TimeAndSale count the same order of records") {
    struct Variant {
        const char *name;
        std::vector<std::string> flags;
    };

    const std::vector<Variant> variants{
        {"legacy", {"--legacy"}},
        {"reuse", {"--reuse"}},
        {"legacy and reuse", {"--legacy", "--reuse"}},
        {"TimeAndSale", {"--event", "TimeAndSale"}},
        {"batch of 10", {"--batch", "10"}},
    };

    for (const auto &variant : variants) {
        SUBCASE(variant.name) {
            std::vector<std::string> args{":" + freePort(), "-S", "100", "-r", "*/5k", "-s", "1s", "--duration", "1s"};

            args.insert(args.end(), variant.flags.begin(), variant.flags.end());

            const auto outcome = runTool(args);

            INFO("the error: " << outcome.result.error);
            CHECK(outcome.result.exitCode == 0);
            CHECK(outcome.result.records >= 4000);
            CHECK(outcome.result.records <= 5200);
        }
    }
}

TEST_CASE("U3: without a rate the generator runs flat out") {
    const auto outcome = runTool({":" + freePort(), "-S", "100", "-s", "1s", "--duration", "500ms", "--reuse"});

    INFO("the error: " << outcome.result.error);
    CHECK(outcome.result.exitCode == 0);
    CHECK(outcome.result.records > 10'000);
}

TEST_CASE("U3: a client address with no listener is not a start error and reports no connections") {
    const auto port = freePort(); // free: nobody listens
    const auto outcome =
        runTool({"127.0.0.1:" + port, "-C", "2", "-S", "100", "-r", "*/5k", "-s", "1s", "--duration", "2500ms"});

    INFO("the error: " << outcome.result.error);
    INFO("the output: " << outcome.output);
    CHECK(outcome.result.exitCode == 0);
    REQUIRE(outcome.result.instanceRecords.size() == 2);
    CHECK(outcome.result.instanceRecords[0] > 0);
    CHECK(outcome.result.instanceRecords[1] > 0);

    const auto summaries = summaryLines(outcome.output);

    REQUIRE_FALSE(summaries.empty());

    for (const auto &line : summaries) {
        CHECK(line.find("instances 2, connections 0") != std::string::npos);
    }

    CHECK(outcome.output.find("    NetTest.1 data ") != std::string::npos);
    CHECK(outcome.output.find("    NetTest.2 data ") != std::string::npos);
}

TEST_CASE("U3: a malformed address fails the start") {
    for (const std::string address : {std::string("localhost:notaport"), std::string("(127.0.0.1:1"), std::string("::::")}) {
        const auto outcome = runTool({address, "-S", "10", "--duration", "1s"});

        INFO("the address: " << address << ", the error: " << outcome.result.error);
        CHECK(outcome.result.exitCode != 0);
        CHECK_FALSE(outcome.result.error.empty());
        CHECK(outcome.result.records == 0);
        CHECK(outcome.seconds < 5);
    }
}

TEST_CASE("U3: a server address on a port that another endpoint listens on fails the start") {
    const auto port = freePort();
    const Occupant occupant(isolateThread(), ":" + port);

    REQUIRE(occupant.connected);

    const auto outcome = runTool({":" + port, "-S", "10", "--duration", "1s"});

    INFO("the error: " << outcome.result.error);
    CHECK(outcome.result.exitCode != 0);
    CHECK(outcome.result.error.find(":" + port) != std::string::npos);
    CHECK(outcome.result.records == 0);
    CHECK(outcome.seconds < 5);
}

TEST_CASE("U3: the public signal of a failed server socket bind is a connector that stays connecting") {
    // The tool relies on it: the connector of a bound server socket is connected within milliseconds, the connector of
    // a socket whose port is taken stays connecting (QD logs the BindException; no connection error is recorded).
    const auto port = freePort();
    Occupant occupant(isolateThread(), ":" + port);

    REQUIRE(occupant.connected);

    Occupant second(isolateThread(), ":" + port);

    REQUIRE(second.connected); // the connect itself succeeds: the bind happens on a QD thread

    auto *thread = isolateThread();
    const auto stateOfServer = [&](Occupant &holder) {
        dxfg_v4_message_connector_list *list = nullptr;
        std::int32_t state = -1;

        REQUIRE(dxfg_v4_DXEndpoint_getConnectors(thread, holder.endpoint, &list) == 0);
        REQUIRE(list->size == 1);

        std::int32_t kind = -1;

        CHECK(dxfg_v4_MessageConnector_getKind(thread, list->elements[0], &kind) == 0);
        CHECK(kind == DXFG_V4_MESSAGE_CONNECTOR_KIND_SERVER_SOCKET);
        CHECK(dxfg_v4_MessageConnector_getState(thread, list->elements[0], &state) == 0);
        dxfg_v4_message_connector_list_release(thread, list);

        return state;
    };

    std::this_thread::sleep_for(std::chrono::milliseconds(1500));
    CHECK(stateOfServer(occupant) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTED);
    CHECK(stateOfServer(second) == DXFG_V4_MESSAGE_CONNECTOR_STATE_CONNECTING);
}

TEST_CASE("U3: the stop request before the duration ends the run with 0 and the last statistics") {
    StopControl stop;
    std::thread stopper([&] {
        std::this_thread::sleep_for(std::chrono::milliseconds(2500));
        stop.request();
    });
    const auto outcome =
        runTool({":" + freePort(), "-S", "100", "-r", "*/5k", "-s", "60m", "--duration", "60s"}, stop);

    stopper.join();
    INFO("the output: " << outcome.output);
    CHECK(outcome.result.exitCode == 0);
    CHECK(outcome.seconds < 10);
    CHECK(outcome.result.records > 0);
    // No tick of the hour period came (the wall clock rarely crosses an hour in the run), so the last statistics cover
    // the whole run of more than one second.
    CHECK(summaryLines(outcome.output).size() == 1);
}

TEST_CASE("U3: SIGINT ends the run like the stop request does") {
    StopControl stop;
    std::thread raiser;
    int exitCode = -1;
    double seconds = 0;

    {
        const SignalWatcher watcher(stop);

        raiser = std::thread([] {
            std::this_thread::sleep_for(std::chrono::milliseconds(500));
            std::raise(SIGINT);
        });

        const auto outcome = runTool({":" + freePort(), "-S", "100", "-r", "*/5k", "-s", "10s"}, stop);

        exitCode = outcome.result.exitCode;
        seconds = outcome.seconds;
    }

    raiser.join();
    CHECK(exitCode == 0);
    CHECK(seconds < 10);
}

TEST_CASE("U3: a connected feed endpoint counts as a connection") {
    const auto port = freePort();
    std::atomic<bool> release{false};
    test::Attached feedThread(graal_get_isolate(isolateThread()), [&](graal_isolatethread_t *thread) -> int32_t {
        std::this_thread::sleep_for(std::chrono::milliseconds(700));

        dxfg_v4_endpoint_t *feed = nullptr;
        int32_t code = dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_FEED, &feed);

        if (code == 0) {
            code = dxfg_v4_DXEndpoint_connect(thread, feed, ("127.0.0.1:" + port).c_str());

            while (!release) {
                std::this_thread::sleep_for(std::chrono::milliseconds(20));
            }

            dxfg_v4_DXEndpoint_closeAndAwaitTermination(thread, feed);
            dxfg_v4_JavaObjectHandler_release(thread, &feed->handler);
        }

        return code;
    });

    const auto outcome = runTool({":" + port, "-S", "100", "-r", "*/5k", "-s", "1s", "--duration", "3s"});

    release = true;
    CHECK(feedThread.join() == 0);
    INFO("the output: " << outcome.output);
    CHECK(outcome.result.exitCode == 0);

    const auto summaries = summaryLines(outcome.output);

    REQUIRE_FALSE(summaries.empty());
    CHECK(summaries.back().find("connections 1") != std::string::npos);
}

// ---------------------------------------------------------------------------------------------------------------------
// U4: the statistics formatter and the reporter
// ---------------------------------------------------------------------------------------------------------------------

namespace {

constexpr std::int64_t SECOND = 1'000'000'000;

StatisticsSnapshot snapshotOf(std::int64_t periodNanos, std::int64_t connections, std::vector<InstanceTotals> deltas) {
    StatisticsSnapshot snapshot;

    snapshot.periodNanos = periodNanos;
    snapshot.connections = connections;
    snapshot.deltas = std::move(deltas);

    return snapshot;
}

} // namespace

TEST_CASE("U4: the digits are grouped by thousands with commas") {
    CHECK(groupDigits(0) == "0");
    CHECK(groupDigits(7) == "7");
    CHECK(groupDigits(999) == "999");
    CHECK(groupDigits(1000) == "1,000");
    CHECK(groupDigits(123'456) == "123,456");
    CHECK(groupDigits(1'234'567) == "1,234,567");
    CHECK(groupDigits(1'000'000'000'000LL) == "1,000,000,000,000");
    CHECK(groupDigits(-1234) == "-1,234");
}

TEST_CASE("U4: the block of 1,234,567 records over 10 seconds with 2 instances and 1 connection") {
    const auto text =
        formatStatistics(snapshotOf(10 * SECOND, 1, {{600'000, 600'000 * 20'000LL}, {634'567, 634'567 * 20'000LL}}));
    const auto lines = linesOf(text);

    REQUIRE(lines.size() == 3);
    CHECK(lines[0] == "{*NetTestProducerSide-NetTest*} data 123,456 rps, lag 20 us, instances 2, connections 1");
    CHECK(lines[1] == "    NetTest.1 data 60,000 rps, lag 20 us");
    CHECK(lines[2] == "    NetTest.2 data 63,456 rps, lag 20 us");
    CHECK(text.back() != '\n'); // the reporter adds the line break
}

TEST_CASE("U4: zero records give zero rate and zero lag") {
    const auto lines = linesOf(formatStatistics(snapshotOf(SECOND, 0, {{0, 0}})));

    REQUIRE(lines.size() == 2);
    CHECK(lines[0] == "{*NetTestProducerSide-NetTest*} data 0 rps, lag 0 us, instances 1, connections 0");
    CHECK(lines[1] == "    NetTest.1 data 0 rps, lag 0 us");
}

TEST_CASE("U4: the name of the snapshot is in the summary line and in the instance lines") {
    auto snapshot = snapshotOf(SECOND, 0, {{10, 0}, {10, 0}});

    snapshot.name = "Bench";

    const auto lines = linesOf(formatStatistics(snapshot));

    REQUIRE(lines.size() == 3);
    CHECK(lines[0].rfind("{*NetTestProducerSide-Bench*} data 20 rps", 0) == 0);
    CHECK(lines[1].rfind("    Bench.1 data 10 rps", 0) == 0);
    CHECK(lines[2].rfind("    Bench.2 data 10 rps", 0) == 0);
}

TEST_CASE("U4: the lag is the record-weighted mean of the batch costs") {
    // Two batches of 100 records costing 10 us and 30 us.
    CHECK(lagMicros(200, 100 * 10'000 + 100 * 30'000) == 20);
    // A batch of 300 at 10 us and one of 100 at 50 us: 20 us by records, not 30 us by batches.
    CHECK(lagMicros(400, 300 * 10'000 + 100 * 50'000) == 20);
    CHECK(lagMicros(0, 0) == 0);

    const auto lines = linesOf(formatStatistics(snapshotOf(SECOND, 0, {{400, 300 * 10'000 + 100 * 50'000}})));

    REQUIRE(lines.size() == 2);
    CHECK(lines[0].find("lag 20 us") != std::string::npos);
}

TEST_CASE("U4: the rate uses the real length of the period") {
    CHECK(recordsPerSecond(1000, SECOND) == 1000);
    CHECK(recordsPerSecond(500, SECOND / 2) == 1000);
    CHECK(recordsPerSecond(1, 3 * SECOND) == 0);
    CHECK(recordsPerSecond(1000, 0) == 0);
    CHECK(recordsPerSecond(std::int64_t{9'000'000'000'000LL}, 1000 * SECOND) == 9'000'000'000LL);
}

TEST_CASE("U4: the summary line expression matches the summary line and nothing else") {
    const std::regex summary(SUMMARY_LINE_REGEX);
    const auto text = formatStatistics(snapshotOf(10 * SECOND, 3, {{1'234'567, 1'234'567 * 1500}}));
    const auto lines = linesOf(text);

    REQUIRE(lines.size() == 2);
    CHECK(std::regex_match(lines[0], summary));
    CHECK_FALSE(std::regex_match(lines[1], summary));
    CHECK_FALSE(std::regex_match("{*NetTestProducerSide-NetTest*} data 1 rps, lag 1 us, instances 1", summary));
    CHECK_FALSE(std::regex_match("data 1 rps, lag 1 us, instances 1, connections 0", summary));
}

namespace {

// A clock of the reporter that jumps: each sleep sets the time to its target and ends the reporter when the target is
// after the stop time (the time then becomes the stop time).
struct FakeClock {
    std::int64_t now = 0;
    std::int64_t stopAt = 0;
    std::vector<std::int64_t> targets{};

    ReporterClock clock() {
        ReporterClock result;

        result.nowNanos = [this] {
            return now;
        };
        result.sleepUntilNanos = [this](std::int64_t target) {
            targets.push_back(target);

            if (target > stopAt) {
                now = stopAt;

                return false;
            }

            now = target;

            return true;
        };

        return result;
    }
};

struct Printed {
    std::int64_t time;
    std::string text;
};

} // namespace

TEST_CASE("U4: the reporter primes on the first tick and prints on the whole seconds after it") {
    FakeClock fake;
    std::vector<Printed> printed;
    const std::int64_t start = 1000 * SECOND + SECOND / 3;
    const std::int64_t stop = start + 3 * SECOND + SECOND / 6; // a 3.5 s run

    fake.now = start;
    fake.stopAt = stop;

    // 1,000 records and 20 us per record each second.
    const auto source = [&] {
        StatisticsSample sample;
        const std::int64_t records = (fake.now - start) / 1'000'000; // 1 record per millisecond

        sample.instances.push_back({records, records * 20'000});
        sample.connections = 2;

        return sample;
    };

    runReporter("NetTest", 1000, fake.clock(), source, [&](const std::string &text) {
        printed.push_back({fake.now, text});
    });

    // The sleeps end at the next whole second each time: 1001 (primes), 1002, 1003, then 1004 passes the stop.
    REQUIRE(fake.targets.size() == 4);
    CHECK(fake.targets[0] == 1001 * SECOND);
    CHECK(fake.targets[1] == 1002 * SECOND);
    CHECK(fake.targets[2] == 1003 * SECOND);
    CHECK(fake.targets[3] == 1004 * SECOND);

    // Nothing at 1001; a block at 1002 and 1003; the last half second at the stop is too short to print.
    REQUIRE(printed.size() == 2);
    CHECK(printed[0].time == 1002 * SECOND);
    CHECK(printed[1].time == 1003 * SECOND);

    for (const auto &block : printed) {
        const auto lines = linesOf(block.text);

        REQUIRE(lines.size() == 2);
        CHECK(block.text.back() == '\n');
        CHECK(lines[0] == "{*NetTestProducerSide-NetTest*} data 1,000 rps, lag 20 us, instances 1, connections 2");
    }
}

namespace {

// Runs the reporter with the period from the start until the stop, both in nanoseconds of the fake clock; the generator
// adds 300 records of 5 us at the first sleep. Returns the printed blocks.
std::vector<Printed> runOnce(std::int64_t periodMillis, std::int64_t start, std::int64_t stopAt) {
    FakeClock fake;
    std::vector<Printed> printed;
    std::int64_t records = 0;

    fake.now = start;
    fake.stopAt = stopAt;

    auto clock = fake.clock();
    const auto sleep = clock.sleepUntilNanos;

    clock.sleepUntilNanos = [&](std::int64_t target) {
        records = 300;

        return sleep(target);
    };

    runReporter(
        "NetTest", periodMillis, clock,
        [&] {
            StatisticsSample sample;

            sample.instances.push_back({records, records * 5000});

            return sample;
        },
        [&](const std::string &text) {
            printed.push_back({fake.now, text});
        });

    return printed;
}

} // namespace

TEST_CASE("U4: a stop before the first tick prints the whole run scaled by its real length") {
    // 1.5 s after the start, before the first tick of the 10 s period.
    const auto printed = runOnce(10'000, 2000 * SECOND, 2000 * SECOND + SECOND * 3 / 2);

    REQUIRE(printed.size() == 1);
    CHECK(printed[0].time == 2000 * SECOND + SECOND * 3 / 2);
    CHECK(linesOf(printed[0].text)[0] ==
          "{*NetTestProducerSide-NetTest*} data 200 rps, lag 5 us, instances 1, connections 0");
}

TEST_CASE("U4: a last block shorter than one second is not printed") {
    // Half a second after the start, before the first tick.
    CHECK(runOnce(1000, 2000 * SECOND, 2000 * SECOND + SECOND / 2).empty());

    // A tick at 2005 primes, one at 2010 prints, and 999 ms after it the stop prints nothing; 1 s after it, a block.
    CHECK(runOnce(5000, 2000 * SECOND, 2011 * SECOND - 1'000'000).size() == 1);
    CHECK(runOnce(5000, 2000 * SECOND, 2011 * SECOND).size() == 2);
}

TEST_CASE("U4: a late wake-up does not move the later ticks") {
    FakeClock fake;

    fake.now = 100 * SECOND + 10;
    fake.stopAt = 104 * SECOND;

    auto clock = fake.clock();
    const auto sleep = clock.sleepUntilNanos;
    bool late = true;

    // The first wake-up is 400 ms late.
    clock.sleepUntilNanos = [&](std::int64_t target) {
        const bool result = sleep(target);

        if (late && result) {
            fake.now = target + SECOND * 4 / 10;
            late = false;
        }

        return result;
    };

    runReporter(
        "NetTest", 1000, clock,
        [] {
            return StatisticsSample{{{0, 0}}, 0};
        },
        [](const std::string &) {});

    REQUIRE(fake.targets.size() >= 3);
    CHECK(fake.targets[0] == 101 * SECOND);
    CHECK(fake.targets[1] == 102 * SECOND);
    CHECK(fake.targets[2] == 103 * SECOND);
}

TEST_CASE("U4: the reporter of the system clock prints aligned blocks until the stop") {
    StopControl stop;
    std::vector<std::int64_t> times;
    const auto clock = systemReporterClock(stop);

    std::thread stopper([&] {
        std::this_thread::sleep_for(std::chrono::milliseconds(1700));
        stop.request();
    });

    runReporter(
        "NetTest", 500, clock,
        [] {
            return StatisticsSample{{{0, 0}}, 0};
        },
        [&](const std::string &) {
            times.push_back(clock.nowNanos());
        });
    stopper.join();

    // 1.7 s at 0.5 s: three or four ticks, the first primes; the last block is shorter than one second.
    CHECK(times.size() >= 2);
    CHECK(times.size() <= 3);

    for (std::size_t index = 0; index < times.size(); ++index) {
        const auto offset = times[index] % (500 * 1'000'000LL);

        INFO("the tick " << index << " is " << offset << " ns after a multiple of the period");
        CHECK(offset < 300'000'000LL); // aligned, with a generous allowance for a late wake-up
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// U5: the end to end run, a feed endpoint of the same isolate receives the published events (the ctest entry
// NetTestTest.EndToEnd; the property dxscheme.nanoTime must come before the first endpoint of the process)
// ---------------------------------------------------------------------------------------------------------------------

namespace {

// What the listener of the feed saw. The listener runs on a thread of the SDK.
struct Collected {
    EventKind kind = EventKind::TRADE;
    std::set<std::string> poolSymbols{};
    std::atomic<std::int64_t> events{0};
    std::atomic<std::int64_t> foreignSymbols{0};
    std::atomic<std::int64_t> wrongClass{0};
    std::atomic<std::int64_t> staleTime{0};
    std::atomic<std::int64_t> withNanoPart{0};
    std::atomic<std::int64_t> badNanoPart{0};
    std::atomic<std::int64_t> withFlags{0};
    std::atomic<std::int64_t> startMillis{0};
};

void collectEvents(graal_isolatethread_t *, dxfg_v4_event_type_list *events, void *userData) {
    auto *collected = static_cast<Collected *>(userData);
    const bool isTrade = collected->kind == EventKind::TRADE;
    const auto expectedClass = isTrade ? DXFG_EVENT_TRADE : DXFG_EVENT_TIME_AND_SALE;

    for (std::int32_t index = 0; index < events->size; ++index) {
        const dxfg_event_type_t *event = events->elements[index];

        if (event->clazz != expectedClass) {
            ++collected->wrongClass;

            continue;
        }

        const char *symbol = nullptr;
        std::int64_t packed = 0;
        std::int32_t nanoPart = 0;

        if (isTrade) {
            const auto *trade = reinterpret_cast<const dxfg_trade_t *>(event);

            symbol = trade->trade_base.market_event.event_symbol;
            packed = trade->trade_base.time_sequence;
            nanoPart = trade->trade_base.time_nano_part;
        } else {
            const auto *sale = reinterpret_cast<const dxfg_time_and_sale_t *>(event);

            symbol = sale->market_event.event_symbol;
            packed = sale->index;
            nanoPart = sale->time_nano_part;

            if (sale->event_flags != 0) {
                ++collected->withFlags;
            }
        }

        ++collected->events;

        if (symbol == nullptr || collected->poolSymbols.count(symbol) == 0) {
            ++collected->foreignSymbols;
        }

        const auto eventMillis = timeMillisOf(packed);

        if (eventMillis < collected->startMillis - ALLOWED_SKEW_MILLIS ||
            eventMillis > nowMillis() + ALLOWED_SKEW_MILLIS) {
            ++collected->staleTime;
        }

        if (nanoPart < 0 || nanoPart >= 1'000'000) {
            ++collected->badNanoPart;
        } else if (nanoPart != 0) {
            ++collected->withNanoPart;
        }
    }
}

// The thread of the isolate for the end to end cases: the nanosecond part of the time is switched on before the first
// endpoint of the process, so these cases run in a process of their own (see the CMake file) or come first.
graal_isolatethread_t *endToEndThread() {
    static const bool isSet = [] {
        return dxfg_v4_system_set_property(isolateThread(), "dxscheme.nanoTime", "true") == DXFG_EXECUTE_SUCCESSFULLY;
    }();

    REQUIRE(isSet);

    return isolateThread();
}

// A FEED endpoint on a thread of its own: after a delay, when the server socket of the tool listens, it subscribes to
// the symbols for one event class and counts what arrives.
class CollectingFeed {
    std::atomic<bool> release_{false};
    std::unique_ptr<test::Attached> thread_;

  public:
    CollectingFeed(graal_isolate_t *isolate, Collected &collected, const std::vector<std::string> &symbols,
                   const std::string &port) {
        thread_ = std::make_unique<test::Attached>(isolate, [this, &collected, &symbols,
                                                             port](graal_isolatethread_t *thread) -> std::int32_t {
            std::this_thread::sleep_for(std::chrono::milliseconds(700));

            dxfg_v4_endpoint_t *endpoint = nullptr;
            dxfg_v4_feed_t *feed = nullptr;
            dxfg_v4_subscription_t *subscription = nullptr;
            dxfg_v4_feed_event_listener_t *listener = nullptr;
            const auto eventClass = collected.kind == EventKind::TRADE ? DXFG_EVENT_TRADE : DXFG_EVENT_TIME_AND_SALE;
            std::int32_t code = dxfg_v4_DXEndpoint_createWithRole(thread, DXFG_ENDPOINT_ROLE_FEED, &endpoint);

            code = code != 0 ? code : dxfg_v4_DXEndpoint_getFeed(thread, endpoint, &feed);
            code = code != 0 ? code : dxfg_v4_DXFeed_createSubscription(thread, feed, eventClass, &subscription);
            code = code != 0 ? code : dxfg_v4_DXFeedEventListener_new(thread, &collectEvents, &collected, &listener);
            // The listener comes before the symbols.
            code = code != 0 ? code : dxfg_v4_DXFeedSubscription_addEventListener(thread, subscription, listener);

            std::vector<dxfg_v4_string_symbol_t> strings;
            std::vector<dxfg_v4_symbol_t *> elements;

            strings.reserve(symbols.size());

            for (const auto &symbol : symbols) {
                strings.push_back(dxfg_v4_string_symbol_t{{STRING}, symbol.c_str()});
            }

            for (auto &string : strings) {
                elements.push_back(&string.supper);
            }

            dxfg_v4_symbol_list list{static_cast<std::int32_t>(elements.size()), elements.data()};

            code = code != 0 ? code : dxfg_v4_DXFeedSubscription_addSymbols(thread, subscription, &list);
            code = code != 0 ? code : dxfg_v4_DXEndpoint_connect(thread, endpoint, ("127.0.0.1:" + port).c_str());

            while (!release_) {
                std::this_thread::sleep_for(std::chrono::milliseconds(20));
            }

            if (subscription != nullptr) {
                dxfg_v4_DXFeedSubscription_close(thread, subscription);
                dxfg_v4_JavaObjectHandler_release(thread, &subscription->handler);
            }

            if (listener != nullptr) {
                dxfg_v4_JavaObjectHandler_release(thread, &listener->handler);
            }

            if (feed != nullptr) {
                dxfg_v4_JavaObjectHandler_release(thread, &feed->handler);
            }

            if (endpoint != nullptr) {
                dxfg_v4_DXEndpoint_closeAndAwaitTermination(thread, endpoint);
                dxfg_v4_JavaObjectHandler_release(thread, &endpoint->handler);
            }

            return code;
        });
    }

    CollectingFeed(const CollectingFeed &) = delete;
    CollectingFeed &operator=(const CollectingFeed &) = delete;

    // Closes the feed; 0 when everything it did succeeded.
    std::int32_t finish() {
        release_ = true;

        return thread_->join();
    }
};

// The run of the tool on a free loopback port with a feed of the event kind in the same isolate.
struct EndToEnd {
    Outcome outcome{};
    Collected collected{};
    std::int32_t feedCode = -1;

    EndToEnd(EventKind kind, const std::vector<std::string> &flags, const std::string &duration) {
        const auto thread = endToEndThread();
        const auto port = freePort();
        const auto symbols = generateSymbols(100);

        collected.kind = kind;
        collected.poolSymbols = asSet(symbols);
        collected.startMillis = nowMillis();

        CollectingFeed feed(graal_get_isolate(thread), collected, symbols, port);
        std::vector<std::string> args{":" + port, "-S", "100", "-r", "*/2k", "-s", "1s", "--duration", duration};

        args.insert(args.end(), flags.begin(), flags.end());
        outcome = runTool(args);
        feedCode = feed.finish();
    }
};

} // namespace

TEST_CASE("E2E: a feed endpoint receives the published Trade events with the nanosecond part") {
    const EndToEnd run(EventKind::TRADE, {}, "5s");

    INFO("the output: " << run.outcome.output);
    CHECK(run.outcome.result.exitCode == 0);
    CHECK(run.feedCode == 0);
    CHECK(run.collected.events >= 100);
    CHECK(run.collected.events <= run.outcome.result.records);
    CHECK(run.collected.wrongClass == 0);
    CHECK(run.collected.foreignSymbols == 0);
    CHECK(run.collected.staleTime == 0);
    CHECK(run.collected.badNanoPart == 0);
    // A nanosecond part of 0 in every event would mean that dxscheme.nanoTime did not reach the wire.
    CHECK(run.collected.withNanoPart > 0);

    const auto summaries = summaryLines(run.outcome.output);
    std::size_t connected = 0;

    CHECK(summaries.size() >= 2);

    for (const auto &line : summaries) {
        CHECK(line.find("instances 1") != std::string::npos);

        if (line.find("connections 1") != std::string::npos) {
            ++connected;
        }
    }

    CHECK(connected >= 2);
}

TEST_CASE("E2E: a feed endpoint receives the published TimeAndSale events") {
    const EndToEnd run(EventKind::TIME_AND_SALE, {"--event", "TimeAndSale"}, "3s");

    INFO("the output: " << run.outcome.output);
    CHECK(run.outcome.result.exitCode == 0);
    CHECK(run.feedCode == 0);
    CHECK(run.collected.events >= 100);
    CHECK(run.collected.wrongClass == 0);
    CHECK(run.collected.foreignSymbols == 0);
    CHECK(run.collected.staleTime == 0);
    CHECK(run.collected.badNanoPart == 0);
    CHECK(run.collected.withNanoPart > 0);
}

TEST_CASE("E2E: a feed endpoint receives the events of the legacy publishing call") {
    const EndToEnd run(EventKind::TRADE, {"--legacy"}, "3s");

    INFO("the output: " << run.outcome.output);
    CHECK(run.outcome.result.exitCode == 0);
    CHECK(run.feedCode == 0);
    CHECK(run.collected.events >= 100);
    CHECK(run.collected.foreignSymbols == 0);
    CHECK(run.collected.staleTime == 0);
}


} // namespace nettest
} // namespace dxfg
