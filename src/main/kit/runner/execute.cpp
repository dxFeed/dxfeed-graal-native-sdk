// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "execute.hpp"

#include <algorithm>
#include <cerrno>
#include <cstdlib>
#include <optional>

namespace kit {

namespace {

// The timeout that a request asks the adapter to wait, in milliseconds: the third token of poll, await and ready-wait
// when it is not negative (protocol.md, "Requests").
Clock::duration ownTimeout(const std::string &request) {
    auto tokens = splitTokens(request);

    if (tokens.size() < 3 || (tokens[0] != "poll" && tokens[0] != "await" && tokens[0] != "ready-wait")) {
        return Clock::duration::zero();
    }

    errno = 0;
    char *end = nullptr;
    long long value = std::strtoll(tokens[2].c_str(), &end, 10);

    if (errno == ERANGE || end != tokens[2].c_str() + tokens[2].size() || value < 0) {
        return Clock::duration::zero();
    }

    return std::chrono::milliseconds(std::min<long long>(value, 24LL * 3600 * 1000));
}

bool isPoll(const std::string &request) {
    return request.compare(0, 5, "poll ") == 0;
}

std::string describe(const Response &response) {
    if (!response.block) {
        return "'" + response.line + "'";
    }

    return "an events block with the outcome '" + response.outcome + "', " + std::to_string(response.events.size()) +
           " event(s), dropped=" + std::to_string(response.dropped);
}

std::string symbolOf(const Event &event) {
    const std::string *symbol = event.find("event_symbol");

    if (symbol == nullptr || *symbol == "~") {
        return {};
    }

    return percentDecode(*symbol).value_or(*symbol);
}

// Compares the listed fields of the expected events with the received ones.
std::optional<std::string> compareEvents(const std::vector<Event> &expected, const std::vector<Event> &actual) {
    if (expected.size() != actual.size()) {
        return "expected " + std::to_string(expected.size()) + " event(s), got " + std::to_string(actual.size());
    }

    for (std::size_t i = 0; i < expected.size(); ++i) {
        std::string position = "event " + std::to_string(i + 1);

        if (expected[i].type != actual[i].type) {
            return position + ": expected the type " + expected[i].type + ", got " + actual[i].type;
        }

        for (const auto &field : expected[i].fields) {
            const std::string *value = actual[i].find(field.name);

            if (value == nullptr) {
                return position + " (" + actual[i].type + "): the adapter sent no field '" + field.name + "'";
            }

            if (!valuesEqual(field.value, *value)) {
                return position + " (" + actual[i].type + "): " + field.name + ": expected " + field.value + ", got " +
                       *value;
            }
        }
    }

    return std::nullopt;
}

class StepRunner {
  public:
    StepRunner(Session &session, const Timing &timing) : session_(session), timing_(timing) {
    }

    Outcome run(const Scenario &scenario) {
        for (const auto &step : scenario.steps) {
            prefix_ = "line " + std::to_string(step.line) + ": ";
            Outcome outcome = runStep(step);

            if (outcome.kind != Outcome::Kind::Pass) {
                return outcome;
            }
        }

        return {};
    }

  private:
    Outcome fail(const std::string &message) const {
        return {Outcome::Kind::Fail, prefix_ + message};
    }

    Outcome error(Trouble trouble, const std::string &message) const {
        return {trouble == Trouble::Mismatch ? Outcome::Kind::Mismatch : Outcome::Kind::Error, prefix_ + message};
    }

    Outcome runStep(const Step &step) {
        if (step.kind == Step::Kind::Exit) {
            auto code = session_.awaitExit(stepDeadline(timing_));

            if (!code) {
                return {Outcome::Kind::Error, prefix_ + "the adapter did not exit before the deadline; it was killed"};
            }

            if (*code != step.exitCode) {
                return fail("expected the exit code " + std::to_string(step.exitCode) + ", got " +
                            std::to_string(*code));
            }

            return {};
        }

        if (step.kind == Step::Kind::Send) {
            Response response;
            std::string message;

            if (auto trouble = request(step.request, response, message); trouble != Trouble::None) {
                return error(trouble, step.request + ": " + message);
            }

            if (response.block || response.line != "ok") {
                return fail(step.request + ": expected 'ok', got " + describe(response));
            }

            return {};
        }

        const Matcher &matcher = step.matcher;

        if (matcher.kind == Matcher::Kind::Until || (matcher.kind == Matcher::Kind::Events && isPoll(step.request))) {
            return repeat(step);
        }

        Response response;
        std::string message;

        if (auto trouble = request(step.request, response, message); trouble != Trouble::None) {
            return error(trouble, step.request + ": " + message);
        }

        std::string got = describe(response);

        switch (matcher.kind) {
        case Matcher::Kind::Ok:
            if (response.block || response.line != "ok") {
                return fail(step.request + ": expected 'ok', got " + got);
            }

            break;
        case Matcher::Kind::Error:
            if (response.block || response.line.compare(0, 5, "error") != 0) {
                return fail(step.request + ": expected 'error ...', got " + got);
            }

            break;
        case Matcher::Kind::Reply:
            if (response.block || response.line != matcher.text) {
                return fail(step.request + ": expected '" + matcher.text + "', got " + got);
            }

            break;
        case Matcher::Kind::Outcome:
            if (!response.block || response.outcome != matcher.text) {
                return fail(step.request + ": expected the outcome '" + matcher.text + "', got " + got);
            }

            break;
        case Matcher::Kind::Count:
            if (!response.block || (response.outcome != "events" && response.outcome != "no_events") ||
                !compare(static_cast<std::int64_t>(response.events.size()), matcher.op, matcher.number)) {
                return fail(step.request + ": expected a count " + matcher.op + " " + std::to_string(matcher.number) +
                            ", got " + got);
            }

            break;
        case Matcher::Kind::Events:
        default:
            if (!response.block || response.outcome != "events") {
                return fail(step.request + ": expected the outcome 'events', got " + got);
            }

            if (auto mismatch = compareEvents(matcher.events, response.events)) {
                return fail(step.request + ": " + *mismatch);
            }

            break;
        }

        return {};
    }

    // Repeats a poll until the expected events, or the sentinel, have arrived (protocol.md, "Matchers").
    Outcome repeat(const Step &step) {
        const Matcher &matcher = step.matcher;
        auto deadline = std::min(Clock::now() + timing_.step + ownTimeout(step.request), timing_.total);
        std::vector<Event> received;
        std::vector<std::int64_t> droppedValues;
        int polls = 0;

        for (;;) {
            Response response;
            std::string message;

            if (auto trouble = request(step.request, response, message); trouble != Trouble::None) {
                return error(trouble, step.request + ": " + message);
            }

            ++polls;

            if (!response.block || (response.outcome != "events" && response.outcome != "no_events")) {
                return fail(step.request + ": expected the outcome 'events' or 'no_events', got " +
                            describe(response));
            }

            droppedValues.push_back(response.dropped);
            bool sentinel = false;

            for (auto &event : response.events) {
                sentinel = sentinel || (matcher.kind == Matcher::Kind::Until && symbolOf(event) == matcher.text);
                received.push_back(std::move(event));
            }

            if (matcher.kind == Matcher::Kind::Events ? received.size() >= matcher.events.size() : sentinel) {
                break;
            }

            if (Clock::now() >= deadline) {
                std::string what = matcher.kind == Matcher::Kind::Events
                                       ? std::to_string(matcher.events.size()) + " event(s)"
                                       : "the sentinel " + matcher.text;

                return fail(step.request + ": " + what + " did not arrive before the deadline (" +
                            std::to_string(polls) + " poll(s), " + std::to_string(received.size()) +
                            " event(s) received)");
            }
        }

        if (matcher.kind == Matcher::Kind::Events) {
            if (auto mismatch = compareEvents(matcher.events, received)) {
                return fail(step.request + ": " + *mismatch);
            }

            return {};
        }

        for (const auto &check : matcher.checks) {
            if (auto failure = runCheck(check, received, droppedValues)) {
                prefix_ = "line " + std::to_string(check.line) + ": ";

                return fail(*failure);
            }
        }

        return {};
    }

    static std::optional<std::string> runCheck(const Check &check, const std::vector<Event> &received,
                                               const std::vector<std::int64_t> &droppedValues) {
        std::vector<const Event *> events;

        for (const auto &event : received) {
            if (symbolOf(event) == check.symbol) {
                events.push_back(&event);
            }
        }

        auto count = static_cast<std::int64_t>(events.size());
        std::int64_t dropped = droppedValues.empty() ? 0 : droppedValues.back();

        switch (check.kind) {
        case Check::Kind::Count:
            if (!compare(count, check.op, check.number)) {
                return "count " + check.symbol + ": expected " + check.op + " " + std::to_string(check.number) +
                       ", got " + std::to_string(count);
            }

            break;
        case Check::Kind::Last: {
            if (events.empty()) {
                return "last " + check.symbol + ": no event of the symbol arrived";
            }

            const std::string *value = events.back()->find(check.field);

            if (value == nullptr || !valuesEqual(check.value, *value)) {
                return "last " + check.symbol + ": " + check.field + ": expected " + check.value + ", got " +
                       (value != nullptr ? *value : std::string("no such field"));
            }

            break;
        }
        case Check::Kind::Increasing:
            for (std::size_t i = 0; i < events.size(); ++i) {
                const std::string *value = events[i]->find(check.field);
                auto number = value != nullptr ? parseNumber(*value) : std::nullopt;

                if (!number) {
                    return "increasing " + check.symbol + ": event " + std::to_string(i + 1) + " has no number in '" +
                           check.field + "'";
                }

                if (i > 0 && !(*number > *parseNumber(*events[i - 1]->find(check.field)))) {
                    return "increasing " + check.symbol + ": event " + std::to_string(i + 1) + " has " + check.field +
                           "=" + *value + " after " + *events[i - 1]->find(check.field);
                }
            }

            break;
        case Check::Kind::Accounted:
            if (count + dropped != check.number) {
                return "accounted " + check.symbol + ": received " + std::to_string(count) + " + dropped " +
                       std::to_string(dropped) + " != " + std::to_string(check.number);
            }

            break;
        case Check::Kind::Dropped:
            if (!compare(dropped, check.op, check.number)) {
                return "dropped: expected " + check.op + " " + std::to_string(check.number) + ", got " +
                       std::to_string(dropped);
            }

            break;
        case Check::Kind::Monotonic:
        default:
            for (std::size_t i = 1; i < droppedValues.size(); ++i) {
                if (droppedValues[i] < droppedValues[i - 1]) {
                    return "monotonic dropped: poll " + std::to_string(i + 1) + " reported " +
                           std::to_string(droppedValues[i]) + " after " + std::to_string(droppedValues[i - 1]);
                }
            }

            break;
        }

        return std::nullopt;
    }

    Trouble request(const std::string &line, Response &response, std::string &message) {
        return session_.request(line, response, stepDeadline(timing_, ownTimeout(line)), message);
    }

    Session &session_;
    const Timing &timing_;
    std::string prefix_;
};

} // namespace

Clock::time_point stepDeadline(const Timing &timing, Clock::duration extra) {
    return std::min(Clock::now() + timing.step + extra, timing.total);
}

Outcome runSteps(Session &session, const Scenario &scenario, const Timing &timing) {
    return StepRunner(session, timing).run(scenario);
}

} // namespace kit
