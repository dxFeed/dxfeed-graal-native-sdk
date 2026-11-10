// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#include <NetTest/Sdk.hpp>

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

} // namespace nettest
} // namespace dxfg
