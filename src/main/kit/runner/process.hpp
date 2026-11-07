// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#pragma once

#include <chrono>
#include <condition_variable>
#include <cstdint>
#include <deque>
#include <memory>
#include <mutex>
#include <optional>
#include <string>
#include <thread>
#include <vector>

namespace kit {

using Clock = std::chrono::steady_clock;

/// A child process whose stdin, stdout and stderr are pipes of the runner.
///
/// Reader threads drain stdout (split into lines) and stderr (only the tail is kept), so a full pipe never blocks the
/// child. Every wait ends on a line, a deadline or the end of the stream. The child runs in a Job Object with
/// kill-on-close (Windows) or in a process group of its own (POSIX), so a kill also ends its children.
class Process {
  public:
    /// Starts the program `argv[0]` with the arguments `argv[1..]`. Returns null and sets `error` on failure.
    static std::unique_ptr<Process> spawn(const std::vector<std::string> &argv, std::string &error);

    /// Kills the child if it still runs, reaps it and joins the reader threads.
    ~Process();

    Process(const Process &) = delete;
    Process &operator=(const Process &) = delete;

    /// Writes the line and an LF to the child's stdin. Returns false when the child no longer reads (broken pipe).
    bool writeLine(const std::string &line);

    /// Closes the child's stdin: the child reads EOF.
    void closeStdin();

    enum class ReadStatus { Line, Timeout, Eof, TooLong };

    /// Waits for the next line of stdout until the deadline. A CR before the LF is removed. Eof means the child closed
    /// its stdout (usually: it exited) and no line is left; TooLong means a line exceeded the limit.
    ReadStatus readLine(std::string &line, Clock::time_point deadline);

    /// Waits until the child exits or the deadline passes. Returns the exit code (128 + the signal number when a
    /// signal ended the child on POSIX), or nothing at the deadline.
    std::optional<int> waitExit(Clock::time_point deadline);

    /// Kills the child and its children, then reaps it. Does nothing if the child was already reaped.
    void kill();

    /// The last bytes the child wrote to stderr.
    std::string stderrTail() const;

    /// The process id of the child.
    std::int64_t pid() const;

    /// The longest accepted line of stdout, in bytes.
    static constexpr std::size_t maxLineLength = 4 * 1024 * 1024;

  private:
    struct Native;

    Process();
    void startReaders();
    void readStdout();
    void readStderr();

    std::unique_ptr<Native> native_;

    mutable std::mutex mutex_;
    std::condition_variable linesChanged_;
    std::deque<std::string> lines_;
    bool stdoutEnded_ = false;
    bool lineTooLong_ = false;
    std::string stderrTail_;

    std::thread stdoutReader_;
    std::thread stderrReader_;
};

} // namespace kit
