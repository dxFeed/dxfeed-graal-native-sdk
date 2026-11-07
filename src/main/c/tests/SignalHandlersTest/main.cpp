// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// On Linux and macOS the isolate installs no-op handlers of SIGPIPE and SIGXFSZ when their action is SIG_DFL, so a
// write to a socket closed by the peer fails with EPIPE instead of killing the process; -XX:-EnableSignalHandling
// turns this off, and a handler or SIG_IGN of the application is kept. The macOS x64 library, built with an older
// GraalVM, did not install them before MDAPI-501. The handlers are installed once per process, so CTest runs each test
// suite in a process of its own.

#include <doctest.h>

#ifndef _WIN32

#include <dxfg_api.h>

#include <csignal>
#include <cstring>
#include <signal.h>
#include <string>
#include <vector>

namespace dxfg {
namespace test {

namespace {

using Handler = void (*)(int);

void applicationHandler(int) {
}

void setHandler(int signal, Handler handler) {
    struct sigaction action {};

    action.sa_handler = handler;
    sigemptyset(&action.sa_mask);
    REQUIRE(sigaction(signal, &action, nullptr) == 0);
}

Handler currentHandler(int signal) {
    struct sigaction action {};

    REQUIRE(sigaction(signal, nullptr, &action) == 0);

    return action.sa_handler;
}

bool isNoopHandler(Handler handler) {
    return handler != SIG_DFL && handler != SIG_IGN && handler != applicationHandler;
}

// Creates the isolate with the arguments (without the program name) and tears it down.
void createAndTearDownIsolate(std::vector<std::string> arguments) {
    std::string programName = "SignalHandlersTest";
    std::vector<char *> argv{programName.data()};

    for (auto &argument : arguments) {
        argv.push_back(argument.data());
    }

    graal_create_isolate_params_t params;
    std::memset(&params, 0, sizeof(params));
    params.version = __graal_create_isolate_params_version;
    params.argc = static_cast<int>(argv.size());
    params.argv = argv.data();

    graal_isolate_t *isolate = nullptr;
    graal_isolatethread_t *thread = nullptr;

    REQUIRE(graal_create_isolate(&params, &isolate, &thread) == 0);
    REQUIRE(graal_tear_down_isolate(thread) == 0);
}

} // namespace

TEST_SUITE("Installed") {
    TEST_CASE("The isolate installs no-op handlers of SIGPIPE and SIGXFSZ, which stay after the teardown") {
        setHandler(SIGPIPE, SIG_DFL);
        setHandler(SIGXFSZ, SIG_DFL);

        createAndTearDownIsolate({});

        CHECK(isNoopHandler(currentHandler(SIGPIPE)));
        CHECK(isNoopHandler(currentHandler(SIGXFSZ)));

        // With SIG_DFL this kills the process, and CTest reports the test as failed.
        REQUIRE(raise(SIGPIPE) == 0);
        REQUIRE(raise(SIGXFSZ) == 0);
    }
}

TEST_SUITE("Disabled") {
    TEST_CASE("-XX:-EnableSignalHandling keeps SIG_DFL") {
        setHandler(SIGPIPE, SIG_DFL);
        setHandler(SIGXFSZ, SIG_DFL);

        createAndTearDownIsolate({"-XX:-EnableSignalHandling"});

        CHECK(currentHandler(SIGPIPE) == SIG_DFL);
        CHECK(currentHandler(SIGXFSZ) == SIG_DFL);
    }
}

TEST_SUITE("Kept") {
    TEST_CASE("A handler and SIG_IGN of the application are kept") {
        setHandler(SIGPIPE, applicationHandler);
        setHandler(SIGXFSZ, SIG_IGN);

        createAndTearDownIsolate({});

        CHECK(currentHandler(SIGPIPE) == applicationHandler);
        CHECK(currentHandler(SIGXFSZ) == SIG_IGN);
    }
}

} // namespace test
} // namespace dxfg

#endif
