// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Sdk.hpp>

#include <cstring>

namespace dxfg {
namespace nettest {

std::string takeThreadException(graal_isolatethread_t *thread) {
    dxfg_v4_exception_t *exception = nullptr;

    if (dxfg_v4_get_and_clear_thread_exception(thread, &exception) != DXFG_EXECUTE_SUCCESSFULLY ||
        exception == nullptr) {
        return "no exception";
    }

    std::string text = exception->class_name != nullptr ? exception->class_name : "exception";

    if (exception->message != nullptr) {
        text += ": ";
        text += exception->message;
    }

    dxfg_v4_Exception_release(thread, exception);

    return text;
}

int createIsolate(const std::vector<std::string> &arguments, graal_isolate_t **isolate,
                  graal_isolatethread_t **thread) {
    if (arguments.empty()) {
        return graal_create_isolate(nullptr, isolate, thread);
    }

    // The isolate skips argv[0], the name of the program. The strings live until the call returns.
    std::vector<std::string> strings{"NetTest"};

    strings.insert(strings.end(), arguments.begin(), arguments.end());

    std::vector<char *> argv;

    argv.reserve(strings.size());

    for (auto &string : strings) {
        argv.push_back(string.data());
    }

    graal_create_isolate_params_t params;

    std::memset(&params, 0, sizeof(params));
    params.version = __graal_create_isolate_params_version;
    params.argc = static_cast<int>(argv.size());
    params.argv = argv.data();
    // An unknown option fails the creation instead of being dropped.
    params.ignore_unrecognized_args = 0;

    return graal_create_isolate(&params, isolate, thread);
}

bool tearDownIsolate(graal_isolatethread_t *thread) {
    // The threads of an open endpoint would block the tear-down forever.
    const bool closed = dxfg_v4_system_close_all_and_await_termination(thread) == DXFG_EXECUTE_SUCCESSFULLY;

    if (!closed) {
        static_cast<void>(takeThreadException(thread));

        return false;
    }

    return graal_tear_down_isolate(thread) == 0;
}

} // namespace nettest
} // namespace dxfg
