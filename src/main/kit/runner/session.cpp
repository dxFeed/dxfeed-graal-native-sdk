// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include "session.hpp"

#include <algorithm>
#include <cstdlib>
#include <iostream>

namespace kit {

namespace {

// The first words of the one-line responses (protocol.md, "Requests"); any other line is garbage.
const char *const responseWords[] = {"ok", "error", "present", "absent", "pending", "signaled", "timeout"};

bool isResponseLine(const std::string &line) {
    std::string word = line.substr(0, line.find(' '));

    return std::find(std::begin(responseWords), std::end(responseWords), word) != std::end(responseWords);
}

bool isPrintable(const std::string &line) {
    return std::none_of(line.begin(), line.end(), [](char c) {
        auto byte = static_cast<unsigned char>(c);

        return byte < 0x20 || byte == 0x7f;
    });
}

std::string shorten(const std::string &line) {
    constexpr std::size_t limit = 200;

    std::string result = line.size() > limit ? line.substr(0, limit) + "..." : line;

    for (char &c : result) {
        auto byte = static_cast<unsigned char>(c);

        if (byte < 0x20 || byte == 0x7f) {
            c = '?';
        }
    }

    return result;
}

// Reads the `key=value` tokens of a handshake line.
std::optional<std::string> valueOf(const std::vector<std::string> &tokens, const std::string &key) {
    for (std::size_t i = 1; i < tokens.size(); ++i) {
        if (tokens[i].compare(0, key.size() + 1, key + "=") == 0) {
            return tokens[i].substr(key.size() + 1);
        }
    }

    return std::nullopt;
}

} // namespace

Session::Session(std::vector<std::string> command, bool verbose) : command_(std::move(command)), verbose_(verbose) {
}

Session::~Session() = default;

std::int64_t Session::pid() const {
    return process_ ? process_->pid() : -1;
}

bool Session::hasCapability(const std::string &name) const {
    return std::find(capabilities_.begin(), capabilities_.end(), name) != capabilities_.end();
}

std::string Session::stderrTail() const {
    return process_ ? process_->stderrTail() : std::string();
}

void Session::kill() {
    if (process_) {
        process_->kill();
    }
}

Trouble Session::adapterGone(std::string &message, const std::string &while_) {
    // The stdout ended: the adapter exited or is exiting. Give it a moment to be reaped for its exit code.
    auto code = process_->waitExit(Clock::now() + std::chrono::seconds(2));

    if (code) {
        message = "the adapter exited with the code " + std::to_string(*code) + " " + while_;
    } else {
        process_->kill();
        message = "the adapter closed its stdout " + while_ + "; it was killed";
    }

    return Trouble::Adapter;
}

Trouble Session::readLine(std::string &line, Clock::time_point deadline, std::string &message) {
    switch (process_->readLine(line, deadline)) {
    case Process::ReadStatus::Line:
        if (verbose_) {
            std::cout << "    < " << shorten(line) << std::endl;
        }

        return Trouble::None;
    case Process::ReadStatus::Timeout:
        process_->kill();
        message = "no response before the deadline; the adapter was killed";

        return Trouble::Adapter;
    case Process::ReadStatus::TooLong:
        process_->kill();
        message = "a line longer than " + std::to_string(Process::maxLineLength) + " bytes; the adapter was killed";

        return Trouble::Adapter;
    case Process::ReadStatus::Eof:
    default:
        return adapterGone(message, "instead of responding");
    }
}

Trouble Session::start(const std::vector<std::pair<std::string, std::string>> &config, Clock::time_point deadline,
                       std::string &message) {
    std::string error;
    process_ = Process::spawn(command_, error);

    if (!process_) {
        message = error;

        return Trouble::Adapter;
    }

    if (verbose_) {
        std::cout << "  # adapter started, pid " << process_->pid() << std::endl;
    }

    std::string line;

    if (auto trouble = readLine(line, deadline, message); trouble != Trouble::None) {
        message = "handshake: " + message;

        return trouble;
    }

    auto tokens = splitTokens(line);

    if (tokens.empty() || tokens[0] != "hello") {
        process_->kill();
        message = "handshake: expected 'hello proto=" + std::to_string(protocolVersion) + " ...', got '" +
                  shorten(line) + "'";

        return Trouble::Mismatch;
    }

    auto proto = valueOf(tokens, "proto");

    if (!proto || *proto != std::to_string(protocolVersion)) {
        process_->kill();
        message = "handshake: the adapter speaks the protocol " + (proto ? *proto : std::string("(none)")) +
                  ", the runner speaks " + std::to_string(protocolVersion);

        return Trouble::Mismatch;
    }

    adapterName_ = valueOf(tokens, "adapter").value_or("");
    capabilities_.clear();

    if (auto caps = valueOf(tokens, "caps"); caps && !caps->empty()) {
        std::size_t start = 0;

        while (start <= caps->size()) {
            std::size_t end = caps->find(',', start);

            if (end == std::string::npos) {
                end = caps->size();
            }

            capabilities_.push_back(caps->substr(start, end - start));
            start = end + 1;
        }
    }

    std::vector<std::string> requests;

    for (const auto &[key, value] : config) {
        requests.push_back("config " + key + "=" + value);
    }

    requests.emplace_back("start");

    for (const auto &request : requests) {
        Response response;

        if (auto trouble = this->request(request, response, deadline, message); trouble != Trouble::None) {
            message = "handshake: " + message;

            return trouble;
        }

        if (request == "start") {
            auto readyTokens = splitTokens(response.line);
            auto advanced = valueOf(readyTokens, "advanced");

            if (response.block || readyTokens.empty() || readyTokens[0] != "ready" || !advanced ||
                (*advanced != "enabled" && *advanced != "disabled")) {
                process_->kill();
                message = "handshake: expected 'ready advanced=enabled|disabled version=...' after 'start', got '" +
                          shorten(response.line) + "'";

                return Trouble::Mismatch;
            }

            advancedEnabled_ = *advanced == "enabled";
            sdkVersion_ = valueOf(readyTokens, "version").value_or("");
        } else if (response.block || response.line != "ok") {
            process_->kill();
            message = "handshake: expected 'ok' after '" + request + "', got '" + shorten(response.line) + "'";

            return Trouble::Mismatch;
        }
    }

    return Trouble::None;
}

Trouble Session::request(const std::string &line, Response &response, Clock::time_point deadline,
                         std::string &message) {
    response = Response();

    if (verbose_) {
        std::cout << "    > " << line << std::endl;
    }

    if (!process_->writeLine(line)) {
        return adapterGone(message, "before the request could be written");
    }

    if (auto trouble = readLine(response.line, deadline, message); trouble != Trouble::None) {
        return trouble;
    }

    // "ready" answers only "start"; the handshake checks it.
    if (line == "start" && response.line.compare(0, 6, "ready ") == 0) {
        return Trouble::None;
    }

    auto header = splitTokens(response.line);

    if (header.empty() || header[0] != "events") {
        if (!isPrintable(response.line) || !isResponseLine(response.line)) {
            process_->kill();
            message = "not a protocol response: '" + shorten(response.line) + "'; the adapter was killed";

            return Trouble::Adapter;
        }

        return Trouble::None;
    }

    // An events block: "events <outcome> dropped=<n>", the event lines, "end".
    auto dropped = header.size() == 3 ? valueOf(header, "dropped") : std::nullopt;
    char *end = nullptr;

    if (dropped) {
        response.dropped = std::strtoll(dropped->c_str(), &end, 10);
    }

    if (!dropped || dropped->empty() || end != dropped->c_str() + dropped->size() ||
        (header[1] != "events" && header[1] != "no_events" && header[1] != "closed" && header[1] != "fail")) {
        process_->kill();
        message = "a malformed events header: '" + shorten(response.line) +
                  "' (expected 'events <events|no_events|closed|fail> dropped=<n>'); the adapter was killed";

        return Trouble::Adapter;
    }

    response.block = true;
    response.outcome = header[1];

    for (;;) {
        std::string eventLine;

        if (auto trouble = readLine(eventLine, deadline, message); trouble != Trouble::None) {
            message += " (inside an events block)";

            return trouble;
        }

        if (eventLine == "end") {
            break;
        }

        std::string error;
        auto event = parseEvent(eventLine, error);

        if (!event) {
            process_->kill();
            message = "inside an events block: " + error + "; the adapter was killed";

            return Trouble::Adapter;
        }

        response.events.push_back(std::move(*event));
    }

    if (response.outcome != "events" && !response.events.empty()) {
        process_->kill();
        message = "an events block with the outcome '" + response.outcome + "' has events; the adapter was killed";

        return Trouble::Adapter;
    }

    return Trouble::None;
}

std::optional<int> Session::awaitExit(Clock::time_point deadline) {
    if (!process_) {
        return std::nullopt;
    }

    auto code = process_->waitExit(deadline);

    if (!code) {
        process_->kill();
    }

    return code;
}

std::optional<int> Session::finish(Clock::time_point deadline) {
    if (!process_) {
        return std::nullopt;
    }

    process_->closeStdin();

    return awaitExit(deadline);
}

} // namespace kit
