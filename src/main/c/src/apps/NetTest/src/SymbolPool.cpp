// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Sdk.hpp>
#include <NetTest/SymbolPool.hpp>

#include <cstdint>
#include <string>
#include <unordered_set>

namespace dxfg {
namespace nettest {

namespace {

// The seed of QD NetTest (com.devexperts.qd.tools.SymbolGenerator).
constexpr std::int64_t SYMBOL_SEED = 1416948710541751LL;
// The largest -S of the specification. The generator can give about 2.2e9 distinct symbols, so this always ends, but
// 1e8 takes on the order of 1e9 draws and several GB, as in QD NetTest.
constexpr std::int64_t MAX_GENERATED = 100'000'000;

// java.util.Random as its Javadoc specifies it: a 48-bit linear congruential generator.
class JavaRandom {
    static constexpr std::uint64_t MULTIPLIER = 0x5DEECE66DULL;
    static constexpr std::uint64_t ADDEND = 0xBULL;
    static constexpr std::uint64_t MASK = (1ULL << 48) - 1;

    std::uint64_t seed_;

    std::int32_t next(int bits) {
        seed_ = (seed_ * MULTIPLIER + ADDEND) & MASK;

        // The top `bits` of the 48-bit seed; at most 31 here, so the value fits a non-negative int32.
        return static_cast<std::int32_t>(seed_ >> (48 - bits));
    }

  public:
    explicit JavaRandom(std::int64_t seed) : seed_((static_cast<std::uint64_t>(seed) ^ MULTIPLIER) & MASK) {
    }

    // Random.nextInt(bound) for bound > 0.
    std::int32_t nextInt(std::int32_t bound) {
        const std::int32_t bits = next(31);
        const std::int32_t mask = bound - 1;

        if ((bound & mask) == 0) {
            return static_cast<std::int32_t>((static_cast<std::int64_t>(bound) * bits) >> 31);
        }

        // Java rejects a draw while `u - r + (bound - 1)` overflows int; here the sum is computed in 64 bits.
        std::int32_t value = bits % bound;

        for (std::int32_t draw = bits; static_cast<std::int64_t>(draw) - value + mask > INT32_MAX;) {
            draw = next(31);
            value = draw % bound;
        }

        return value;
    }
};

// SymbolGenerator.generateSymbol of QD NetTest with its default length.
std::string nextSymbol(JavaRandom &random) {
    // Two statements: C++ leaves the order of the operands of + unspecified, Java draws nextInt(3) first.
    const auto shortPart = random.nextInt(3);
    const auto length = shortPart + random.nextInt(4) + 1;
    std::string symbol;

    symbol.reserve(static_cast<std::size_t>(length));

    for (std::int32_t index = 0; index < length; ++index) {
        if (random.nextInt(50) == 0) {
            symbol.push_back(static_cast<char>('0' + random.nextInt(10)));
        } else {
            symbol.push_back(static_cast<char>('A' + random.nextInt(26)));
        }
    }

    return symbol;
}

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
    JavaRandom random(SYMBOL_SEED);

    seen.reserve(static_cast<std::size_t>(total));

    while (static_cast<std::int64_t>(symbols.size()) < total) {
        auto symbol = nextSymbol(random);

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
