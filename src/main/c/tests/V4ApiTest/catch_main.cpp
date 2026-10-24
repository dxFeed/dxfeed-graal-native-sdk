// Copyright (c) 2026 Devexperts LLC.
// SPDX-License-Identifier: MPL-2.0

// The main function of the test. Catch2::Catch2WithMain defines CATCH_CONFIG_MAIN for every source file, and the test
// has two of them (main.cpp and parity.cpp), so the Catch2 implementation is compiled here only.
#define CATCH_CONFIG_MAIN
#include <catch.hpp>
