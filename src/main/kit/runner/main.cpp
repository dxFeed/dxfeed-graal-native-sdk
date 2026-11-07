// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The conformance runner: drives an adapter of a C API wrapper through the scenarios of the kit over the line
// protocol of protocol.md, and prints a verdict per scenario and a summary.

#include "execute.hpp"
#include "scenario.hpp"
#include "session.hpp"

#include <algorithm>
#include <cerrno>
#include <cstdlib>
#include <filesystem>
#include <iostream>
#include <map>
#include <memory>
#include <set>
#include <sstream>
#include <string>
#include <vector>

namespace fs = std::filesystem;
using namespace kit;

namespace {

constexpr int exitPassed = 0;
constexpr int exitFailed = 1;
constexpr int exitAdapterError = 2;
constexpr int exitMismatch = 3;
constexpr int exitUsage = 64;

// The advanced revision that the scenarios were written for (DXFG_V4_ADVANCED_REVISION of the SDK they ship with).
constexpr int scenariosAdvancedRevision = 1;

struct Options {
    std::vector<std::string> adapter;
    std::vector<std::string> paths;
    std::set<std::string> only;
    std::set<std::string> allowedSkips;
    bool strict = false;
    bool verbose = false;
    long long expectedAdvancedRevision = scenariosAdvancedRevision;
    long long stepTimeoutMs = 10000;
    long long totalTimeoutMs = 100000;
};

const char *const usageText =
    R"(usage: kit-runner --adapter <program> [--adapter-arg <arg>]... [options] <scenario file or directory>...

Runs the scenarios against the adapter program, which speaks the line protocol of protocol.md on its stdin and stdout.
A directory is searched recursively for *.scenario files. The scenarios of a directory named own-process run in an
adapter process each; all the others run one after another in one adapter process.

options:
  --strict                          a skip whose reason is not allowed with --allow-skip fails, and a run in
                                    which no scenario passed fails
  --allow-skip <reason>             allows the skip reason in strict mode (repeatable)
  --expected-advanced-revision <n>  sent to the adapter as config expected_advanced_revision (default: 1)
  --step-timeout <ms>               the deadline of one step (default: 10000)
  --total-timeout <ms>              the deadline of the whole run (default: 100000)
  --only <scenario>                 runs only the named scenario (repeatable)
  --verbose                         prints every line exchanged with the adapter

exit codes: 0 passed, 1 failed, 2 adapter error, 3 handshake mismatch, 64 bad usage
)";

int usage(const std::string &message) {
    std::cerr << "kit-runner: " << message << "\n\n" << usageText;

    return exitUsage;
}

bool parseNumber(const std::string &text, long long minimum, long long &value) {
    errno = 0;
    char *end = nullptr;
    long long parsed = std::strtoll(text.c_str(), &end, 10);

    if (text.empty() || errno == ERANGE || end != text.c_str() + text.size() || parsed < minimum) {
        return false;
    }

    value = parsed;

    return true;
}

// Returns -1 when the options are good, else the exit code.
int parseOptions(int argc, char **argv, Options &options) {
    for (int i = 1; i < argc; ++i) {
        std::string argument = argv[i];
        auto value = [&](std::string &out) {
            if (i + 1 >= argc) {
                return false;
            }

            out = argv[++i];

            return true;
        };
        std::string text;

        if (argument == "--help" || argument == "-h") {
            std::cout << usageText;

            return exitPassed;
        } else if (argument == "--strict") {
            options.strict = true;
        } else if (argument == "--verbose") {
            options.verbose = true;
        } else if (argument == "--adapter" || argument == "--adapter-arg") {
            if (!value(text)) {
                return usage(argument + " needs a value");
            }

            if (argument == "--adapter" && !options.adapter.empty()) {
                return usage("--adapter is given twice");
            }

            if (argument == "--adapter-arg" && options.adapter.empty()) {
                return usage("--adapter-arg before --adapter");
            }

            options.adapter.push_back(text);
        } else if (argument == "--allow-skip" || argument == "--only") {
            if (!value(text) || text.empty()) {
                return usage(argument + " needs a value");
            }

            (argument == "--only" ? options.only : options.allowedSkips).insert(text);
        } else if (argument == "--expected-advanced-revision" || argument == "--step-timeout" ||
                   argument == "--total-timeout") {
            long long &target = argument == "--step-timeout"    ? options.stepTimeoutMs
                                : argument == "--total-timeout" ? options.totalTimeoutMs
                                                                : options.expectedAdvancedRevision;

            if (!value(text) || !parseNumber(text, argument == "--expected-advanced-revision" ? 0 : 1, target)) {
                return usage(argument + " needs a positive integer");
            }
        } else if (argument.compare(0, 2, "--") == 0) {
            return usage("unknown option " + argument);
        } else {
            options.paths.push_back(argument);
        }
    }

    if (options.adapter.empty()) {
        return usage("no --adapter");
    }

    if (options.paths.empty()) {
        return usage("no scenario file or directory");
    }

    return -1;
}

// Collects the scenario files; returns false and sets `error` when a path does not exist.
bool collect(const Options &options, std::vector<std::pair<fs::path, bool>> &files, std::string &error) {
    std::set<fs::path> seen;
    auto add = [&](const fs::path &file) {
        fs::path normal = file.lexically_normal();

        if (seen.insert(normal).second) {
            files.emplace_back(normal, normal.parent_path().filename() == "own-process");
        }
    };

    for (const auto &path : options.paths) {
        std::error_code code;
        fs::path root = fs::u8path(path);

        if (fs::is_directory(root, code)) {
            std::vector<fs::path> found;

            for (const auto &entry : fs::recursive_directory_iterator(root, code)) {
                if (entry.is_regular_file() && entry.path().extension() == ".scenario") {
                    found.push_back(entry.path());
                }
            }

            std::sort(found.begin(), found.end());

            for (const auto &file : found) {
                add(file);
            }
        } else if (fs::is_regular_file(root, code)) {
            add(root);
        } else {
            error = path + ": no such file or directory";

            return false;
        }
    }

    return true;
}

std::string indent(const std::string &text, const std::string &prefix) {
    std::istringstream input(text);
    std::string line;
    std::string result;

    while (std::getline(input, line)) {
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }

        result += prefix + line + "\n";
    }

    return result;
}

// The last lines of the adapter's stderr, for a failure or an error.
std::string stderrSection(const std::string &tail) {
    if (tail.empty()) {
        return {};
    }

    constexpr std::size_t maxBytes = 2048;
    std::string shown = tail.size() > maxBytes ? tail.substr(tail.size() - maxBytes) : tail;

    if (tail.size() > maxBytes) {
        shown = shown.substr(std::min(shown.find('\n') + 1, shown.size()));
    }

    return "  adapter stderr (last lines):\n" + indent(shown, "    ");
}

class Run {
  public:
    explicit Run(const Options &options)
        : options_(options),
          timing_{std::chrono::milliseconds(options.stepTimeoutMs),
                  Clock::now() + std::chrono::milliseconds(options.totalTimeoutMs)} {
    }

    int execute(const std::vector<Scenario> &scenarios) {
        std::vector<const Scenario *> shared;
        std::vector<const Scenario *> own;

        for (const auto &scenario : scenarios) {
            (scenario.ownProcess ? own : shared).push_back(&scenario);
        }

        for (const Scenario *scenario : shared) {
            if (mismatch_) {
                break;
            }

            runShared(*scenario);
        }

        if (session_) {
            closeShared();
        }

        for (const Scenario *scenario : own) {
            if (mismatch_) {
                break;
            }

            runOwn(*scenario);
        }

        std::cout << "summary passed=" << passed_ << " failed=" << failed_ << " skipped=" << skipped_
                  << " errors=" << errors_;

        if (!skipReasons_.empty()) {
            std::cout << " skips=";
            bool first = true;

            for (const auto &[reason, count] : skipReasons_) {
                std::cout << (first ? "" : ",") << reason << ":" << count;
                first = false;
            }
        }

        std::cout << std::endl;

        if (mismatch_) {
            return exitMismatch;
        }

        if (errors_ > 0) {
            return exitAdapterError;
        }

        if (failed_ > 0) {
            return exitFailed;
        }

        // A strict run checks the wrapper: one in which everything was skipped checked nothing.
        if (options_.strict && passed_ == 0) {
            std::cout << "FAIL no scenario passed, and strict mode needs at least one" << std::endl;

            return exitFailed;
        }

        return exitPassed;
    }

  private:
    std::vector<std::pair<std::string, std::string>> baseConfig() const {
        return {{"expected_advanced_revision", std::to_string(options_.expectedAdvancedRevision)}};
    }

    bool deadlinePassed(const Scenario &scenario) {
        if (Clock::now() < timing_.total) {
            return false;
        }

        report(scenario, "ERROR", "not run: the total deadline of " + std::to_string(options_.totalTimeoutMs) +
                                      " ms passed");
        ++errors_;

        return true;
    }

    // Starts a session; reports the scenario as an error or a mismatch and returns null when the handshake fails.
    std::unique_ptr<Session> startSession(const Scenario &scenario,
                                          const std::vector<std::pair<std::string, std::string>> &config) {
        auto session = std::make_unique<Session>(options_.adapter, options_.verbose);
        std::string message;
        Trouble trouble = session->start(config, stepDeadline(timing_), message);

        if (trouble == Trouble::None) {
            return session;
        }

        std::string stderrText = stderrSection(session->stderrTail());
        session->kill();

        if (trouble == Trouble::Mismatch) {
            mismatch_ = true;
            report(scenario, "ERROR", message + "\n  the run stops: the adapter and the runner do not agree on the protocol",
                   stderrText);
        } else {
            report(scenario, "ERROR", message, stderrText);
        }

        ++errors_;

        return nullptr;
    }

    // Tells the skip reason of the scenario on this session, or an empty string to run it.
    std::string skipReason(Session &session, const Scenario &scenario, bool &failed) {
        failed = false;

        for (const auto &capability : scenario.capabilities) {
            if (!session.hasCapability(capability)) {
                return "capability-absent";
            }
        }

        if (scenario.usesExperimental && !session.advancedEnabled()) {
            return "advanced-disabled";
        }

        for (const auto &feature : scenario.features) {
            Response response;
            std::string message;
            Trouble trouble =
                session.request("feature " + feature, response, stepDeadline(timing_), message);

            if (trouble != Trouble::None || response.block ||
                (response.line != "present" && response.line != "absent")) {
                failed = true;
                std::string got = trouble != Trouble::None ? message : "'" + response.line + "'";
                report(scenario, trouble != Trouble::None ? "ERROR" : "FAIL",
                       "feature " + feature + ": expected 'present' or 'absent', got " + got,
                       stderrSection(session.stderrTail()));
                ++(trouble != Trouble::None ? errors_ : failed_);
                mismatch_ = mismatch_ || trouble == Trouble::Mismatch;

                return {};
            }

            if (response.line == "absent") {
                return "feature-absent";
            }
        }

        return {};
    }

    void reportSkip(const Scenario &scenario, const std::string &reason) {
        if (options_.strict && options_.allowedSkips.count(reason) == 0) {
            report(scenario, "FAIL", "skipped (" + reason + "), and strict mode does not allow it (--allow-skip " +
                                         reason + ")");
            ++failed_;

            return;
        }

        std::cout << "SKIP " << scenario.name << " (" << reason << ")" << std::endl;
        ++skipped_;
        ++skipReasons_[reason];
    }

    void report(const Scenario &scenario, const char *verdict, const std::string &message,
                const std::string &extra = {}) {
        std::cout << verdict << " " << scenario.name << " (" << scenario.path << ")\n"
                  << indent(message, "  ") << extra << std::flush;
    }

    // Records the outcome of the steps; returns true when the session stays usable.
    bool record(Session &session, const Scenario &scenario, const Outcome &outcome) {
        switch (outcome.kind) {
        case Outcome::Kind::Pass:
            return true;
        case Outcome::Kind::Fail:
            report(scenario, "FAIL", outcome.message, stderrSection(session.stderrTail()));
            ++failed_;

            return false;
        case Outcome::Kind::Mismatch:
            mismatch_ = true;
            [[fallthrough]];
        case Outcome::Kind::Error:
        default:
            report(scenario, "ERROR", outcome.message, stderrSection(session.stderrTail()));
            ++errors_;

            return false;
        }
    }

    void pass(const Scenario &scenario) {
        std::cout << "PASS " << scenario.name << std::endl;
        ++passed_;
    }

    void runShared(const Scenario &scenario) {
        if (deadlinePassed(scenario)) {
            return;
        }

        if (!session_) {
            session_ = startSession(scenario, baseConfig());

            if (!session_) {
                return;
            }
        }

        bool failed = false;
        std::string reason = skipReason(*session_, scenario, failed);

        if (failed) {
            dropSession();

            return;
        }

        if (!reason.empty()) {
            reportSkip(scenario, reason);

            return;
        }

        Outcome outcome = runSteps(*session_, scenario, timing_);

        if (!record(*session_, scenario, outcome)) {
            dropSession();

            return;
        }

        // The reset closes what the scenario created; a reset that fails taints the process.
        Response response;
        std::string message;
        Trouble trouble = session_->request("reset", response, stepDeadline(timing_), message);

        if (trouble != Trouble::None || response.block || response.line != "ok") {
            std::string got = trouble != Trouble::None ? message : "'" + response.line + "'";
            report(scenario, trouble != Trouble::None ? "ERROR" : "FAIL",
                   "the steps passed, but 'reset' after them failed: expected 'ok', got " + got,
                   stderrSection(session_->stderrTail()));
            ++(trouble != Trouble::None ? errors_ : failed_);
            mismatch_ = mismatch_ || trouble == Trouble::Mismatch;
            dropSession();

            return;
        }

        pass(scenario);
    }

    void dropSession() {
        if (session_) {
            session_->kill();
            session_.reset();
        }
    }

    // Ends the shared process: EOF on stdin must make the adapter exit with the code 0.
    void closeShared() {
        auto code = session_->finish(stepDeadline(timing_));

        if (!code || *code != 0) {
            std::cout << "ERROR adapter: "
                      << (code ? "exited with the code " + std::to_string(*code) + " after EOF on stdin"
                               : std::string("did not exit after EOF on stdin; it was killed"))
                      << "\n"
                      << stderrSection(session_->stderrTail()) << std::flush;
            ++errors_;
        }

        session_.reset();
    }

    void runOwn(const Scenario &scenario) {
        if (deadlinePassed(scenario)) {
            return;
        }

        auto config = baseConfig();
        config.insert(config.end(), scenario.config.begin(), scenario.config.end());
        auto session = startSession(scenario, config);

        if (!session) {
            return;
        }

        bool failed = false;
        std::string reason = skipReason(*session, scenario, failed);

        if (failed) {
            session->kill();

            return;
        }

        if (!reason.empty()) {
            session->finish(stepDeadline(timing_));
            reportSkip(scenario, reason);

            return;
        }

        Outcome outcome = runSteps(*session, scenario, timing_);

        if (!record(*session, scenario, outcome)) {
            session->kill();

            return;
        }

        if (scenario.steps.back().kind != Step::Kind::Exit) {
            auto code = session->finish(stepDeadline(timing_));

            if (!code || *code != 0) {
                report(scenario, code ? "FAIL" : "ERROR",
                       code ? "the adapter exited with the code " + std::to_string(*code) + " after EOF on stdin"
                            : std::string("the adapter did not exit after EOF on stdin; it was killed"),
                       stderrSection(session->stderrTail()));
                ++(code ? failed_ : errors_);

                return;
            }
        }

        pass(scenario);
    }

    const Options &options_;
    Timing timing_;
    std::unique_ptr<Session> session_;
    bool mismatch_ = false;
    int passed_ = 0;
    int failed_ = 0;
    int skipped_ = 0;
    int errors_ = 0;
    std::map<std::string, int> skipReasons_;
};

} // namespace

int main(int argc, char **argv) {
    std::ios::sync_with_stdio(false);

    Options options;

    if (int code = parseOptions(argc, argv, options); code >= 0) {
        return code;
    }

    std::vector<std::pair<fs::path, bool>> files;
    std::string error;

    if (!collect(options, files, error)) {
        return usage(error);
    }

    std::vector<Scenario> scenarios;
    std::set<std::string> names;

    for (const auto &[file, ownProcess] : files) {
        auto scenario = parseScenario(file.u8string(), ownProcess, error);

        if (!scenario) {
            std::cerr << "kit-runner: " << error << std::endl;

            return exitUsage;
        }

        if (!names.insert(scenario->name).second) {
            std::cerr << "kit-runner: " << file.u8string() << ": the scenario name '" << scenario->name
                      << "' is used twice" << std::endl;

            return exitUsage;
        }

        if (options.only.empty() || options.only.count(scenario->name) > 0) {
            scenarios.push_back(std::move(*scenario));
        }
    }

    if (scenarios.empty()) {
        std::cout << "summary passed=0 failed=0 skipped=0 errors=0" << std::endl;
        std::cerr << "kit-runner: no scenario to run" << std::endl;

        return exitFailed;
    }

    return Run(options).execute(scenarios);
}
