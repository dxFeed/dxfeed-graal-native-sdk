// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

#ifndef DXFG_V4_API_TEST_ISOLATE_HPP
#define DXFG_V4_API_TEST_ISOLATE_HPP

#include <graal_isolate.h>

namespace dxfg {
namespace test {

// The thread of the isolate that all the tests of the executable share.
inline graal_isolatethread_t *isolateThread() {
    static graal_isolatethread_t *thread = [] {
        graal_isolate_t *isolate = nullptr;
        graal_isolatethread_t *result = nullptr;

        return graal_create_isolate(nullptr, &isolate, &result) == 0 ? result : nullptr;
    }();

    return thread;
}

} // namespace test
} // namespace dxfg

#endif // DXFG_V4_API_TEST_ISOLATE_HPP
