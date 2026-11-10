// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Args.hpp>
#include <NetTest/Run.hpp>
#include <NetTest/StopControl.hpp>
#include <NetTest/SymbolPool.hpp>

#include <dxfg_api.h>

#include <iostream>
#include <string>
#include <vector>

namespace {

constexpr int EXIT_BAD_ARGUMENTS = 2;

} // namespace

int main(int argc, char *argv[]) {
    using namespace dxfg::nettest;

    const std::vector<std::string> args(argv, argv + argc);

    if (args.size() < 2) {
        std::cout << usageText();

        return EXIT_BAD_ARGUMENTS;
    }

    const auto parsed = parseArguments(args);

    if (!parsed.ok) {
        std::cerr << "NetTest: " << parsed.error << "\n\n" << usageText();

        return EXIT_BAD_ARGUMENTS;
    }

    const auto &options = parsed.options;
    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;

    if (graal_create_isolate(nullptr, &isolate, &thread) != 0) {
        std::cerr << "NetTest: cannot create the isolate\n";

        return EXIT_FAILURE;
    }

    // The system properties come before the first endpoint.
    std::string error;

    if (!applySystemProperties(thread, options, error)) {
        std::cerr << "NetTest: " << error << "\n";

        return EXIT_FAILURE;
    }

    const auto pool = buildSymbolPool(thread, options);

    if (!pool.warning.empty()) {
        std::cerr << "NetTest: warning: " << pool.warning << "\n";
    }

    if (!pool.ok) {
        std::cerr << "NetTest: " << pool.error << "\n";

        return EXIT_FAILURE;
    }

    // Ctrl+C and SIGTERM end the run like --duration does: the last statistics are printed and the endpoints close.
    StopControl stop;
    const SignalWatcher signalWatcher(stop);
    const auto result = runNetTest(thread, options, pool.symbols, stop, std::cout);

    if (result.exitCode != EXIT_SUCCESS) {
        std::cerr << "NetTest: " << result.error << "\n";

        return result.exitCode;
    }

    return EXIT_SUCCESS;
}
