// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "scenario.hpp"

#include <cerrno>
#include <cstdlib>
#include <fstream>

namespace kit {

namespace {

bool isName(const std::string &name) {
    if (name.empty()) {
        return false;
    }

    for (char c : name) {
        if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' ||
              c == '.')) {
            return false;
        }
    }

    return true;
}

bool isOperator(const std::string &op) {
    return op == "=" || op == "<=" || op == ">=";
}

std::optional<std::int64_t> parseInt(const std::string &text) {
    if (text.empty()) {
        return std::nullopt;
    }

    errno = 0;
    char *end = nullptr;
    long long value = std::strtoll(text.c_str(), &end, 10);

    if (errno == ERANGE || end != text.c_str() + text.size()) {
        return std::nullopt;
    }

    return static_cast<std::int64_t>(value);
}

std::string trim(const std::string &text) {
    std::size_t start = text.find_first_not_of(" \t");

    if (start == std::string::npos) {
        return {};
    }

    std::size_t end = text.find_last_not_of(" \t\r");

    return text.substr(start, end - start + 1);
}

class Parser {
  public:
    Parser(std::string path, std::vector<std::string> lines) : path_(std::move(path)), lines_(std::move(lines)) {
    }

    std::optional<Scenario> parse(bool ownProcess, std::string &error) {
        Scenario scenario;
        scenario.path = path_;
        scenario.ownProcess = ownProcess;

        std::string line;

        while (next(line)) {
            auto tokens = splitTokens(line);
            const std::string &word = tokens[0];

            if (word == "scenario" || word == "requires" || word == "uses" || word == "config") {
                if (!scenario.steps.empty()) {
                    return fail(error, "a header line after the first step");
                }

                if (!parseHeader(scenario, tokens, error)) {
                    return std::nullopt;
                }
            } else if (word == "send" || word == "expect" || word == "exit") {
                if (!scenario.steps.empty() && scenario.steps.back().kind == Step::Kind::Exit) {
                    return fail(error, "a step after 'exit'");
                }

                Step step;
                step.line = lineNumber_;

                if (!parseStep(step, scenario, line, tokens, error)) {
                    return std::nullopt;
                }

                scenario.steps.push_back(std::move(step));
            } else {
                return fail(error, "unknown line '" + line + "'");
            }
        }

        if (scenario.name.empty()) {
            lineNumber_ = 0;

            return fail(error, "no 'scenario <name>' line");
        }

        if (scenario.steps.empty()) {
            lineNumber_ = 0;

            return fail(error, "no steps");
        }

        return scenario;
    }

  private:
    // The next line that is neither empty nor a comment, trimmed.
    bool next(std::string &line) {
        while (index_ < lines_.size()) {
            line = trim(lines_[index_++]);
            lineNumber_ = static_cast<int>(index_);

            if (!line.empty() && line[0] != '#') {
                return true;
            }
        }

        return false;
    }

    std::nullopt_t fail(std::string &error, const std::string &message) const {
        error = path_ + (lineNumber_ > 0 ? ":" + std::to_string(lineNumber_) : std::string()) + ": " + message;

        return std::nullopt;
    }

    bool failed(std::string &error, const std::string &message) const {
        fail(error, message);

        return false;
    }

    bool parseHeader(Scenario &scenario, const std::vector<std::string> &tokens, std::string &error) {
        const std::string &word = tokens[0];

        if (word == "scenario") {
            if (tokens.size() != 2 || !isName(tokens[1])) {
                return failed(error, "expected 'scenario <name>' with a name of letters, digits, '_', '-' and '.'");
            }

            if (!scenario.name.empty()) {
                return failed(error, "a second 'scenario' line");
            }

            scenario.name = tokens[1];
        } else if (word == "requires") {
            if (tokens.size() != 3 || (tokens[1] != "feature" && tokens[1] != "capability") || !isName(tokens[2])) {
                return failed(error, "expected 'requires feature <name>' or 'requires capability <name>'");
            }

            (tokens[1] == "feature" ? scenario.features : scenario.capabilities).push_back(tokens[2]);
        } else if (word == "uses") {
            if (tokens.size() != 2 || tokens[1] != "experimental") {
                return failed(error, "expected 'uses experimental'");
            }

            scenario.usesExperimental = true;
        } else {
            auto equals = tokens.size() == 2 ? tokens[1].find('=') : std::string::npos;

            if (equals == std::string::npos || equals == 0) {
                return failed(error, "expected 'config <key>=<value>'");
            }

            if (!scenario.ownProcess) {
                return failed(error, "'config' is allowed only in a scenario of its own process (own-process/)");
            }

            scenario.config.emplace_back(tokens[1].substr(0, equals), tokens[1].substr(equals + 1));
        }

        return true;
    }

    bool parseStep(Step &step, const Scenario &scenario, const std::string &line,
                   const std::vector<std::string> &tokens, std::string &error) {
        const std::string &word = tokens[0];

        if (word == "send") {
            if (tokens.size() < 2 || tokens[1].empty()) {
                return failed(error, "expected 'send <request>'");
            }

            step.kind = Step::Kind::Send;
            step.request = joinTokens(tokens, 1);

            return true;
        }

        if (word == "exit") {
            auto code = tokens.size() == 2 ? parseInt(tokens[1]) : std::nullopt;

            if (!code) {
                return failed(error, "expected 'exit <code>'");
            }

            if (!scenario.ownProcess) {
                return failed(error, "'exit' is allowed only in a scenario of its own process (own-process/)");
            }

            step.kind = Step::Kind::Exit;
            step.exitCode = static_cast<int>(*code);

            return true;
        }

        auto arrow = line.find(" -> ");

        if (arrow == std::string::npos || arrow <= 7) {
            return failed(error, "expected 'expect <request> -> <matcher>'");
        }

        step.kind = Step::Kind::Expect;
        step.request = trim(line.substr(7, arrow - 7));

        return parseMatcher(step.matcher, splitTokens(trim(line.substr(arrow + 4))), error);
    }

    bool parseMatcher(Matcher &matcher, const std::vector<std::string> &tokens, std::string &error) {
        if (tokens.empty()) {
            return failed(error, "no matcher after '->'");
        }

        const std::string &word = tokens[0];

        if (word == "ok" && tokens.size() == 1) {
            matcher.kind = Matcher::Kind::Ok;
        } else if (word == "error" && tokens.size() == 1) {
            matcher.kind = Matcher::Kind::Error;
        } else if (word == "reply" && tokens.size() >= 2) {
            matcher.kind = Matcher::Kind::Reply;
            matcher.text = joinTokens(tokens, 1);
        } else if (word == "outcome" && tokens.size() == 2 &&
                   (tokens[1] == "events" || tokens[1] == "no_events" || tokens[1] == "closed" ||
                    tokens[1] == "fail")) {
            matcher.kind = Matcher::Kind::Outcome;
            matcher.text = tokens[1];
        } else if (word == "count" && tokens.size() == 3 && isOperator(tokens[1]) && parseInt(tokens[2])) {
            matcher.kind = Matcher::Kind::Count;
            matcher.op = tokens[1];
            matcher.number = *parseInt(tokens[2]);
        } else if (word == "events" && tokens.size() == 1) {
            matcher.kind = Matcher::Kind::Events;

            return parseEventBlock(matcher, error);
        } else if (word == "until" && tokens.size() == 2) {
            matcher.kind = Matcher::Kind::Until;
            matcher.text = tokens[1];

            return parseCheckBlock(matcher, error);
        } else {
            return failed(error, "unknown matcher '" + joinTokens(tokens) + "'");
        }

        return true;
    }

    bool parseEventBlock(Matcher &matcher, std::string &error) {
        std::string line;

        while (next(line)) {
            if (line == "end") {
                if (matcher.events.empty()) {
                    return failed(error, "an 'events' matcher with no event lines");
                }

                return true;
            }

            std::string eventError;
            auto event = parseEvent(line, eventError);

            if (!event) {
                return failed(error, eventError);
            }

            matcher.events.push_back(std::move(*event));
        }

        return failed(error, "an 'events' matcher without 'end'");
    }

    bool parseCheckBlock(Matcher &matcher, std::string &error) {
        std::string line;

        while (next(line)) {
            if (line == "end") {
                return true;
            }

            auto tokens = splitTokens(line);
            Check check;
            check.line = lineNumber_;
            const std::string &word = tokens[0];

            if (word == "count" && tokens.size() == 4 && isOperator(tokens[2]) && parseInt(tokens[3])) {
                check.kind = Check::Kind::Count;
                check.symbol = tokens[1];
                check.op = tokens[2];
                check.number = *parseInt(tokens[3]);
            } else if (word == "last" && tokens.size() == 3 && tokens[2].find('=') != std::string::npos &&
                       tokens[2].find('=') > 0) {
                check.kind = Check::Kind::Last;
                check.symbol = tokens[1];
                check.field = tokens[2].substr(0, tokens[2].find('='));
                check.value = tokens[2].substr(tokens[2].find('=') + 1);
            } else if (word == "increasing" && tokens.size() == 3) {
                check.kind = Check::Kind::Increasing;
                check.symbol = tokens[1];
                check.field = tokens[2];
            } else if (word == "accounted" && tokens.size() == 3 && parseInt(tokens[2])) {
                check.kind = Check::Kind::Accounted;
                check.symbol = tokens[1];
                check.number = *parseInt(tokens[2]);
            } else if (word == "dropped" && tokens.size() == 3 && isOperator(tokens[1]) && parseInt(tokens[2])) {
                check.kind = Check::Kind::Dropped;
                check.op = tokens[1];
                check.number = *parseInt(tokens[2]);
            } else if (word == "monotonic" && tokens.size() == 2 && tokens[1] == "dropped") {
                check.kind = Check::Kind::Monotonic;
            } else {
                return failed(error, "unknown check '" + line + "'");
            }

            matcher.checks.push_back(std::move(check));
        }

        return failed(error, "an 'until' matcher without 'end'");
    }

    std::string path_;
    std::vector<std::string> lines_;
    std::size_t index_ = 0;
    int lineNumber_ = 0;
};

} // namespace

std::optional<Scenario> parseScenario(const std::string &path, bool ownProcess, std::string &error) {
    std::ifstream input(path, std::ios::binary);

    if (!input) {
        error = path + ": cannot be read";

        return std::nullopt;
    }

    std::vector<std::string> lines;
    std::string line;

    while (std::getline(input, line)) {
        lines.push_back(line);
    }

    return Parser(path, std::move(lines)).parse(ownProcess, error);
}

bool compare(std::int64_t a, const std::string &op, std::int64_t b) {
    if (op == "<=") {
        return a <= b;
    }

    if (op == ">=") {
        return a >= b;
    }

    return a == b;
}

} // namespace kit
