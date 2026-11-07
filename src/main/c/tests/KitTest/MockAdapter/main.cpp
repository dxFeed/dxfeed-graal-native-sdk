// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// A test-only adapter for the self-tests of the kit runner. It speaks the protocol of src/main/kit/protocol.md without
// the SDK: publish stores the event lines, poll returns at most two of them per call (so the runner has to poll
// again), and the mock-* requests misbehave on purpose.
//
// Options:
//   --proto <n>              the protocol version in hello (default 1)
//   --exit-before-hello <n>  exits with the code at once
//   --caps <list>            the capabilities in hello (default publisher,async_poll)
//   --absent <feature>       answers absent for the feature (repeatable); every other feature is present
//   --revision <n>           the advanced revision of the "SDK" (default 1)
//   --dropped <n>            the first poll of a scenario reports dropped=<n>, the later ones dropped=0 (default 0)
//
// Requests besides the handshake: feature, publish, poll, reset, teardown, and
//   mock-ok                  answers ok
//   mock-error               answers error boom
//   mock-hang                never answers
//   mock-exit <code>         exits with the code
//   mock-garbage             writes a line that is not a response
//   mock-stderr <bytes>      writes the bytes to stderr, then the line MOCK-STDERR-END, then answers ok

#include <chrono>
#include <cstdlib>
#include <deque>
#include <iostream>
#include <set>
#include <string>
#include <thread>
#include <vector>

namespace {

std::vector<std::string> split(const std::string &line) {
    std::vector<std::string> tokens;
    std::size_t start = 0;

    while (start <= line.size()) {
        std::size_t end = line.find(' ', start);

        if (end == std::string::npos) {
            end = line.size();
        }

        tokens.push_back(line.substr(start, end - start));
        start = end + 1;
    }

    return tokens;
}

void respond(const std::string &line) {
    std::cout << line << '\n' << std::flush;
}

} // namespace

int main(int argc, char **argv) {
    std::string proto = "1";
    std::string caps = "publisher,async_poll";
    std::set<std::string> absent;
    long revision = 1;
    std::string firstDropped = "0";

    for (int i = 1; i + 1 < argc; i += 2) {
        std::string option = argv[i];
        std::string value = argv[i + 1];

        if (option == "--proto") {
            proto = value;
        } else if (option == "--exit-before-hello") {
            return std::atoi(value.c_str());
        } else if (option == "--caps") {
            caps = value;
        } else if (option == "--absent") {
            absent.insert(value);
        } else if (option == "--revision") {
            revision = std::atol(value.c_str());
        } else if (option == "--dropped") {
            firstDropped = value;
        } else {
            std::cerr << "mock adapter: unknown option " << option << std::endl;

            return 64;
        }
    }

    respond("hello proto=" + proto + " adapter=mock caps=" + caps);

    long expectedRevision = 1;
    std::deque<std::string> stored;
    std::string dropped = firstDropped;
    std::string line;

    while (std::getline(std::cin, line)) {
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }

        std::cerr << "mock adapter: request " << line << std::endl;

        auto tokens = split(line);
        const std::string &word = tokens[0];

        if (word == "config") {
            if (tokens.size() == 2 && tokens[1].compare(0, 27, "expected_advanced_revision=") == 0) {
                expectedRevision = std::atol(tokens[1].c_str() + 27);
                respond("ok");
            } else if (tokens.size() == 2 && tokens[1].compare(0, 16, "max_buffer_size=") == 0) {
                respond("ok");
            } else {
                respond("error unknown config " + line);
            }
        } else if (word == "start") {
            respond(std::string("ready advanced=") + (expectedRevision == revision ? "enabled" : "disabled") +
                    " version=mock");
        } else if (word == "feature" && tokens.size() == 2) {
            respond(absent.count(tokens[1]) > 0 ? "absent" : "present");
        } else if (word == "publish" && tokens.size() >= 3) {
            // publish <ep> <Type> <fields>: stores "event <Type> <fields>".
            stored.push_back("event " + line.substr(line.find(' ', 8) + 1));
            respond("ok");
        } else if (word == "poll") {
            respond(std::string(stored.empty() ? "events no_events" : "events events") + " dropped=" + dropped);
            dropped = "0";

            for (int i = 0; i < 2 && !stored.empty(); ++i) {
                respond(stored.front());
                stored.pop_front();
            }

            respond("end");
        } else if (word == "reset") {
            stored.clear();
            dropped = firstDropped;
            respond("ok");
        } else if (word == "teardown") {
            respond("ok");

            return 0;
        } else if (word == "mock-ok") {
            respond("ok");
        } else if (word == "mock-error") {
            respond("error boom");
        } else if (word == "mock-hang") {
            std::this_thread::sleep_for(std::chrono::hours(1));
        } else if (word == "mock-exit" && tokens.size() == 2) {
            return std::atoi(tokens[1].c_str());
        } else if (word == "mock-garbage") {
            respond("\x01\x02 garbage");
        } else if (word == "mock-stderr" && tokens.size() == 2) {
            std::string chunk(1024, 'e');

            for (long left = std::atol(tokens[1].c_str()); left > 0; left -= 1024) {
                std::cerr << chunk;
            }

            std::cerr << "\nMOCK-STDERR-END" << std::endl;
            respond("ok");
        } else {
            respond("error unknown-request " + word);
        }
    }

    return 0;
}
