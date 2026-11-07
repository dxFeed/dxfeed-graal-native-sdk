# Copyright (c) 2026 Devexperts LLC.
# SPDX-License-Identifier: MPL-2.0

# The conformance runner of the kit. It only starts the adapter and talks to it over pipes, so it does not need the
# SDK: build it with any C++17 compiler, e.g. cmake -S kit/runner -B build/runner && cmake --build build/runner.
cmake_minimum_required(VERSION 3.20)

project(dxfeed-graal-kit-runner LANGUAGES CXX)

find_package(Threads REQUIRED)

add_executable(kit-runner
        main.cpp
        execute.cpp
        execute.hpp
        process.cpp
        process.hpp
        scenario.cpp
        scenario.hpp
        session.cpp
        session.hpp
        values.cpp
        values.hpp
        )

set_target_properties(kit-runner PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF
        )

if (MSVC)
    target_compile_options(kit-runner PRIVATE /W4 /utf-8 /EHsc)
    target_compile_definitions(kit-runner PRIVATE _CRT_SECURE_NO_WARNINGS)
else ()
    target_compile_options(kit-runner PRIVATE -Wall -Wextra)
endif ()

target_link_libraries(kit-runner PRIVATE Threads::Threads)
