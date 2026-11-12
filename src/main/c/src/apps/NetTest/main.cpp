// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Args.hpp>
#include <NetTest/Run.hpp>
#include <NetTest/Sdk.hpp>
#include <NetTest/StopControl.hpp>
#include <NetTest/SymbolPool.hpp>

#include <dxfg_api.h>

#include <iostream>
#include <string>
#include <vector>

namespace {

constexpr int EXIT_BAD_ARGUMENTS = 2;

// Everything between the creation and the tear-down of the isolate. Returns the exit code.
int runInIsolate(graal_isolatethread_t *thread, const dxfg::nettest::Options &options) {
    using namespace dxfg::nettest;

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
    }

    return result.exitCode;
}

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

    if (const int created = createIsolate(options.isolateArguments, &isolate, &thread); created != 0) {
        if (created == ISOLATE_ARGUMENT_PARSING_FAILED) {
            std::cerr << "NetTest: the isolate rejected an -X or -XX: option (the reason is above)\n";

            return EXIT_BAD_ARGUMENTS;
        }

        std::cerr << "NetTest: cannot create the isolate (error " << created << ")\n";

        return EXIT_FAILURE;
    }

    const int exitCode = runInIsolate(thread, options);

    // The tear-down prints the summary options of the isolate (-XX:+PrintGCSummary).
    if (!tearDownIsolate(thread)) {
        std::cerr << "NetTest: cannot tear the isolate down\n";

        return exitCode == EXIT_SUCCESS ? EXIT_FAILURE : exitCode;
    }

    return exitCode;
}
