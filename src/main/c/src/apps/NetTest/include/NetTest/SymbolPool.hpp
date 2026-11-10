// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The symbol pool of NetTest: the symbols of an IPF file or generated ones.

#ifndef DXFG_NETTEST_SYMBOL_POOL_HPP
#define DXFG_NETTEST_SYMBOL_POOL_HPP

#include <NetTest/Args.hpp>

#include <dxfg_api.h>

#include <cstdint>
#include <memory>
#include <string>
#include <vector>

namespace dxfg {
namespace nettest {

// The symbols of a pool; shared by the event pools of all instances.
using Symbols = std::shared_ptr<const std::vector<std::string>>;

// The result of building a pool: the symbols, or the error; the warning may come with either.
struct SymbolPoolResult {
    bool ok = false;
    Symbols symbols{};
    std::string error{};
    std::string warning{};
};

// Generates `total` distinct symbols of 1-6 capital letters from a fixed seed: the same call gives the same symbols on
// every run and platform. Returns an empty vector when `total` is below 1 or above the number of the possible symbols.
std::vector<std::string> generateSymbols(std::int64_t total);

// Reads the symbols of the profiles of the IPF file in file order, at most `total`, through the SDK reader. A file
// with fewer symbols gives the smaller pool and a warning; no symbols, a missing or an unreadable file is an error.
SymbolPoolResult readSymbolsFromFile(graal_isolatethread_t *thread, const std::string &file, std::int64_t total);

// Builds the pool of the options: from the IPF file when the options name one, generated otherwise.
SymbolPoolResult buildSymbolPool(graal_isolatethread_t *thread, const Options &options);

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_SYMBOL_POOL_HPP
