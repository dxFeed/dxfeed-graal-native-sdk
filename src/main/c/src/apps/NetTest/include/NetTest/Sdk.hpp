// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The small helpers over the C API that the pieces of NetTest share.

#ifndef DXFG_NETTEST_SDK_HPP
#define DXFG_NETTEST_SDK_HPP

#include <dxfg_api.h>

#include <string>

namespace dxfg {
namespace nettest {

// Takes the exception of the thread (dxfg_v4_get_and_clear_thread_exception()), releases it and returns its class and
// message as one line; "no exception" when there is none.
std::string takeThreadException(graal_isolatethread_t *thread);

} // namespace nettest
} // namespace dxfg

#endif // DXFG_NETTEST_SDK_HPP
