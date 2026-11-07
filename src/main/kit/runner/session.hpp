// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include "process.hpp"
#include "values.hpp"

#include <cstdint>
#include <memory>
#include <optional>
#include <string>
#include <utility>
#include <vector>

namespace kit {

/// The protocol version that the runner speaks.
constexpr int protocolVersion = 1;

/// A response of the adapter: one line, or an events block.
struct Response {
    bool block = false;
    std::string line;    // the line, or the header line of the block
    std::string outcome; // events, no_events, closed or fail
    std::int64_t dropped = 0;
    std::vector<Event> events;
};

/// How a session failed.
enum class Trouble {
    None,
    Adapter,  // exit, hang, garbage: the verdict is error
    Mismatch, // a handshake or protocol-version mismatch: the run stops with the exit code 3
};

/// One adapter process and the protocol exchange with it.
class Session {
  public:
    Session(std::vector<std::string> command, bool verbose);
    ~Session();

    /// Starts the adapter and runs the handshake: hello, the config lines and start (protocol.md, "Handshake").
    Trouble start(const std::vector<std::pair<std::string, std::string>> &config, Clock::time_point deadline,
                  std::string &message);

    /// Sends the request and reads its response (one line or an events block) until the deadline.
    Trouble request(const std::string &line, Response &response, Clock::time_point deadline, std::string &message);

    /// Waits until the adapter exits; kills it at the deadline. Returns the exit code, or nothing after a kill.
    std::optional<int> awaitExit(Clock::time_point deadline);

    /// Closes the adapter's stdin and waits until it exits; kills it at the deadline.
    std::optional<int> finish(Clock::time_point deadline);

    /// Kills the adapter and reaps it.
    void kill();

    bool hasCapability(const std::string &name) const;
    bool advancedEnabled() const {
        return advancedEnabled_;
    }
    const std::string &adapterName() const {
        return adapterName_;
    }
    const std::string &sdkVersion() const {
        return sdkVersion_;
    }
    std::int64_t pid() const;

    /// The last lines that the adapter wrote to stderr.
    std::string stderrTail() const;

  private:
    Trouble readLine(std::string &line, Clock::time_point deadline, std::string &message);
    Trouble adapterGone(std::string &message, const std::string &while_);

    std::vector<std::string> command_;
    bool verbose_;
    std::unique_ptr<Process> process_;
    std::vector<std::string> capabilities_;
    bool advancedEnabled_ = false;
    std::string adapterName_;
    std::string sdkVersion_;
};

} // namespace kit
