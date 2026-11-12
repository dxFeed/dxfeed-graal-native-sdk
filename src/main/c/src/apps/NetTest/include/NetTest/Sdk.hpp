// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The small helpers over the C API that the pieces of NetTest share.

#ifndef DXFG_NETTEST_SDK_HPP
#define DXFG_NETTEST_SDK_HPP

#include <dxfg_api.h>

#include <string>
#include <vector>

namespace dxfg {
namespace nettest {

// The result of graal_create_isolate() when the isolate rejects an argument (CEntryPointErrors.ARGUMENT_PARSING_FAILED
// of GraalVM); the isolate prints the reason to stderr.
constexpr int ISOLATE_ARGUMENT_PARSING_FAILED = 22;

// Creates the isolate with the arguments (-Xmx<size>, -XX:<option>; an unknown -XX: option or a wrong value fails the
// creation, other -X arguments are ignored), or with the defaults when there are none, and attaches the current thread.
// Returns the result of graal_create_isolate(): 0 on success.
int createIsolate(const std::vector<std::string> &arguments, graal_isolate_t **isolate, graal_isolatethread_t **thread);

// Closes all the endpoints (dxfg_v4_system_close_all_and_await_termination()) and tears the isolate down, so that the
// options that print at the tear-down (-XX:+PrintGCSummary) print. The other threads must be detached. Returns false
// if a step fails.
bool tearDownIsolate(graal_isolatethread_t *thread);

// Takes the exception of the thread (dxfg_v4_get_and_clear_thread_exception()), releases it and returns its class and
// message as one line; "no exception" when there is none.
std::string takeThreadException(graal_isolatethread_t *thread);

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_SDK_HPP
