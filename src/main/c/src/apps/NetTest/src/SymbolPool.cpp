// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Sdk.hpp>
#include <NetTest/SymbolPool.hpp>

#include <unordered_set>

namespace dxfg {
namespace nettest {

namespace {

constexpr std::uint64_t SYMBOL_SEED = 0x5EED0FD4F3ED5EEDULL;
constexpr std::int64_t MAX_GENERATED = 26LL + 26LL * 26 + 26LL * 26 * 26 + 26LL * 26 * 26 * 26 +
                                       26LL * 26 * 26 * 26 * 26 + 26LL * 26 * 26 * 26 * 26 * 26;

// SplitMix64: a small generator that gives the same numbers on every platform (unlike the std distributions).
class SplitMix64 {
    std::uint64_t state_;

  public:
    explicit SplitMix64(std::uint64_t seed) : state_(seed) {
    }

    std::uint64_t next() {
        std::uint64_t value = (state_ += 0x9E3779B97F4A7C15ULL);

        value = (value ^ (value >> 30)) * 0xBF58476D1CE4E5B9ULL;
        value = (value ^ (value >> 27)) * 0x94D049BB133111EBULL;

        return value ^ (value >> 31);
    }
};

// Releases the profile list and the reader at the end of the scope.
struct ProfileResources {
    graal_isolatethread_t *thread;
    dxfg_v4_instrument_profile_reader_t *reader = nullptr;
    dxfg_v4_instrument_profile_data_list *profiles = nullptr;

    explicit ProfileResources(graal_isolatethread_t *thread) : thread(thread) {
    }

    ProfileResources(const ProfileResources &) = delete;
    ProfileResources &operator=(const ProfileResources &) = delete;

    ~ProfileResources() {
        if (profiles != nullptr) {
            dxfg_v4_instrument_profile_data_list_release(thread, profiles);
        }

        if (reader != nullptr) {
            dxfg_v4_JavaObjectHandler_release(thread, &reader->handler);
        }
    }
};

} // namespace

std::vector<std::string> generateSymbols(std::int64_t total) {
    std::vector<std::string> symbols;

    if (total < 1 || total > MAX_GENERATED) {
        return symbols;
    }

    symbols.reserve(static_cast<std::size_t>(total));

    std::unordered_set<std::string> seen;
    SplitMix64 random(SYMBOL_SEED);

    seen.reserve(static_cast<std::size_t>(total));

    while (static_cast<std::int64_t>(symbols.size()) < total) {
        const auto length = static_cast<std::size_t>(random.next() % 6) + 1;
        std::string symbol(length, 'A');

        for (auto &letter : symbol) {
            letter = static_cast<char>('A' + random.next() % 26);
        }

        if (seen.insert(symbol).second) {
            symbols.push_back(std::move(symbol));
        }
    }

    return symbols;
}

SymbolPoolResult readSymbolsFromFile(graal_isolatethread_t *thread, const std::string &file, std::int64_t total) {
    SymbolPoolResult result;
    ProfileResources resources(thread);

    if (dxfg_v4_InstrumentProfileReader_new(thread, &resources.reader) != DXFG_EXECUTE_SUCCESSFULLY) {
        result.error = "cannot create the IPF reader: " + takeThreadException(thread);

        return result;
    }

    if (dxfg_v4_InstrumentProfileReader_readFromFile(thread, resources.reader, file.c_str(), &resources.profiles) !=
        DXFG_EXECUTE_SUCCESSFULLY) {
        result.error = "cannot read the IPF file '" + file + "': " + takeThreadException(thread);

        return result;
    }

    auto symbols = std::make_shared<std::vector<std::string>>();

    if (resources.profiles != nullptr && resources.profiles->elements != nullptr) {
        for (std::int32_t index = 0;
             index < resources.profiles->size && static_cast<std::int64_t>(symbols->size()) < total; ++index) {
            const auto *profile = resources.profiles->elements[index];

            if (profile != nullptr && profile->symbol != nullptr && profile->symbol[0] != '\0') {
                symbols->emplace_back(profile->symbol);
            }
        }
    }

    if (symbols->empty()) {
        result.error = "the IPF file '" + file + "' has no symbols";

        return result;
    }

    if (static_cast<std::int64_t>(symbols->size()) < total) {
        result.warning = "the IPF file '" + file + "' has " + std::to_string(symbols->size()) +
                         " symbols, fewer than the requested " + std::to_string(total);
    }

    result.symbols = std::move(symbols);
    result.ok = true;

    return result;
}

SymbolPoolResult buildSymbolPool(graal_isolatethread_t *thread, const Options &options) {
    if (!options.symbolsFile.empty()) {
        return readSymbolsFromFile(thread, options.symbolsFile, options.totalSymbols);
    }

    SymbolPoolResult result;
    auto symbols = generateSymbols(options.totalSymbols);

    if (symbols.empty()) {
        result.error = "cannot generate " + std::to_string(options.totalSymbols) + " distinct symbols";

        return result;
    }

    result.symbols = std::make_shared<const std::vector<std::string>>(std::move(symbols));
    result.ok = true;

    return result;
}

} // namespace nettest
} // namespace dxfg
