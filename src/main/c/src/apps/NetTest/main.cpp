// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Args.hpp>
#include <NetTest/Metrics.hpp>
#include <NetTest/Run.hpp>
#include <NetTest/Sdk.hpp>
#include <NetTest/StopControl.hpp>
#include <NetTest/Summary.hpp>
#include <NetTest/SymbolPool.hpp>

#include <dxfg_api.h>

#include <chrono>
#include <cstdlib>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

namespace {

constexpr int EXIT_BAD_ARGUMENTS = 2;

// The timeouts of the starvation watchdog as <after the last record ms>,<after the connection ms>, for the process
// tests only; the tool documents no such variable.
constexpr const char *WATCHDOG_VARIABLE = "DXFG_NETTEST_WATCHDOG";

dxfg::nettest::RunHooks runHooks() {
    dxfg::nettest::RunHooks hooks;
    const char *value = std::getenv(WATCHDOG_VARIABLE);

    if (value == nullptr) {
        return hooks;
    }

    const std::string text(value);
    const auto comma = text.find(',');

    try {
        if (comma != std::string::npos) {
            hooks.noDataAfterRecord = std::chrono::milliseconds(std::stoll(text.substr(0, comma)));
            hooks.noDataAfterConnect = std::chrono::milliseconds(std::stoll(text.substr(comma + 1)));
        }
    } catch (const std::exception &) {
        return dxfg::nettest::RunHooks{};
    }

    return hooks;
}

std::int64_t epochNanos() {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
               std::chrono::system_clock::now().time_since_epoch())
        .count();
}

// The SDK version, empty when the call fails.
std::string sdkVersion(graal_isolatethread_t *thread) {
    const char *version = nullptr;

    if (dxfg_v4_system_get_version(thread, &version) != DXFG_EXECUTE_SUCCESSFULLY) {
        dxfg::nettest::takeThreadException(thread);

        return {};
    }

    std::string text = version == nullptr ? "" : version;

    if (version != nullptr) {
        dxfg_v4_String_release(thread, version);
    }

    return text;
}

// A value of the build info (DXFG_V4_BUILD_INFO_*), empty when the library does not know it or the call fails.
std::string buildInfo(graal_isolatethread_t *thread, const char *key) {
    const char *value = nullptr;

    if (dxfg_v4_system_get_build_info(thread, key, &value) != DXFG_EXECUTE_SUCCESSFULLY) {
        dxfg::nettest::takeThreadException(thread);

        return {};
    }

    std::string text = value == nullptr ? "" : value;

    if (value != nullptr) {
        dxfg_v4_String_release(thread, value);
    }

    return text;
}

// Writes the summary file when --summary is given. Returns the exit code of the status, or 1 when the file cannot be
// written.
int finish(const dxfg::nettest::Options &options, const dxfg::nettest::RunReport &report,
           const dxfg::nettest::SummaryStatus &status) {
    using namespace dxfg::nettest;

    if (options.summaryFile.empty()) {
        return status.exitCode;
    }

    std::string error;

    if (!writeSummary(options.summaryFile, formatSummary(options, report, status), error)) {
        std::cerr << "NetTest: cannot write the summary file '" << options.summaryFile << "': " << error << "\n";

        return status.exitCode == EXIT_SUCCESS ? EXIT_FAILURE : status.exitCode;
    }

    return status.exitCode;
}

// Everything between the creation and the tear-down of the isolate. Returns the exit code.
int runInIsolate(graal_isolatethread_t *thread, const dxfg::nettest::Options &options) {
    using namespace dxfg::nettest;

    const bool producer = options.mode == Mode::PRODUCER;
    SummaryStatus status;
    RunReport failedReport;

    status.version = sdkVersion(thread);
    status.qdVersion = buildInfo(thread, DXFG_V4_BUILD_INFO_QD_VERSION);
    status.vendor = buildInfo(thread, DXFG_V4_BUILD_INFO_VENDOR);
    status.march = buildInfo(thread, DXFG_V4_BUILD_INFO_MARCH);
    status.gc = buildInfo(thread, DXFG_V4_BUILD_INFO_GC);
    failedReport.startEpochNs = epochNanos();

    // A failure before the run: the message on stderr and the summary of the status `error`.
    const auto failBeforeRun = [&](const std::string &message) {
        std::cerr << "NetTest: " << message << "\n";
        failedReport.endEpochNs = epochNanos();
        status.status = runStatusName(RunStatus::FAILED);
        status.exitCode = EXIT_FAILURE;
        status.error = message;

        return finish(options, failedReport, status);
    };

    // The system properties come before the first endpoint.
    std::string error;

    if (!applySystemProperties(thread, options, error)) {
        return failBeforeRun(error);
    }

    // The /metrics port is bound before the first endpoint; the server reads only the live counters, declared before
    // it so that it stops first.
    LiveCounters live(options.instances);
    std::unique_ptr<MetricsServer> metrics;

    if (options.metricsPort.has_value()) {
        metrics = std::make_unique<MetricsServer>(options.metricsHost, *options.metricsPort);

        if (!metrics->ok()) {
            return failBeforeRun(metrics->error());
        }

        MetricsInfo info;

        info.side = producer ? "producer" : "consumer";
        info.name = options.name;
        info.version = status.version;
        info.role = roleName(endpointRole(options));
        info.event = eventName(options.event);
        info.collector = options.collector.has_value() ? collectorName(*options.collector) : "";
        info.scheduled = producer && options.recordsPerSecond.has_value();
        metrics->start([info, &live] {
            return formatMetrics(info, live);
        });
        std::cout << "{*NetTestMetrics-" << options.name << "*} " << metrics->url() << std::endl;
    }

    const auto pool = buildSymbolPool(thread, options);

    if (!pool.warning.empty()) {
        std::cerr << "NetTest: warning: " << pool.warning << "\n";
    }

    if (!pool.ok) {
        return failBeforeRun(pool.error);
    }

    // Ctrl+C and SIGTERM end the run like --duration does: the last statistics are printed, the summary is written and
    // the endpoints close.
    StopControl stop;
    const SignalWatcher signalWatcher(stop);
    const auto result = runNetTest(thread, options, pool.symbols, stop, std::cout, runHooks(), &live);

    if (result.exitCode != EXIT_SUCCESS) {
        std::cerr << "NetTest: " << result.error << "\n";
    }

    status.status = result.status == RunStatus::OK && signalWatcher.received() ? std::string("stopped_by_signal")
                                                                               : runStatusName(result.status);
    status.exitCode = result.exitCode;
    status.error = result.error;

    return finish(options, result.report, status);
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

    if (options.help) {
        std::cout << usageText();

        return EXIT_SUCCESS;
    }

    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;

    if (const int created = createIsolate(options.isolateArguments, &isolate, &thread); created != 0) {
        if (created == ISOLATE_ARGUMENT_PARSING_FAILED) {
            // The reason that the isolate prints (GraalVM "Error: ...") stays above the line.
            std::cerr << isolateRejectionLine(options.isolateArguments) << "\n";

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
