# Copyright (c) 2026 Devexperts LLC.
# SPDX-License-Identifier: MPL-2.0

# The reference adapter of the conformance kit. In an archive of the SDK:
#   cmake -S kit/adapter -B build/adapter && cmake --build build/adapter --config Release
# The SDK's CTest adds this directory after it has defined the DxFeedGraalNativeSdk target.

cmake_minimum_required(VERSION 3.21)

project(dxfeed-graal-kit-adapter LANGUAGES CXX)

if (NOT TARGET DxFeedGraalNativeSdk)
    # The root of the archive: its CMakeLists.txt defines the imported library DxFeedGraalNativeSdk.
    set(GRAAL_NATIVE_SDK_INSTALL OFF)
    add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/../.." "${CMAKE_CURRENT_BINARY_DIR}/sdk")
endif ()

find_package(Threads REQUIRED)

add_executable(kit-reference-adapter
        main.cpp
        Events.cpp
        Events.hpp
        ReadyObject.hpp
        )

set_target_properties(kit-reference-adapter PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON
        CXX_EXTENSIONS OFF
        # The library is copied next to the executable, which finds it there.
        BUILD_WITH_INSTALL_RPATH ON
        )

if (APPLE)
    set_target_properties(kit-reference-adapter PROPERTIES INSTALL_RPATH "@loader_path")
elseif (UNIX)
    set_target_properties(kit-reference-adapter PROPERTIES INSTALL_RPATH "$ORIGIN")
endif ()

if (MSVC)
    target_compile_options(kit-reference-adapter PRIVATE /W4 /utf-8 /EHsc)
    target_compile_definitions(kit-reference-adapter PRIVATE _CRT_SECURE_NO_WARNINGS)
else ()
    target_compile_options(kit-reference-adapter PRIVATE -Wall -Wextra)
endif ()

target_link_libraries(kit-reference-adapter PRIVATE DxFeedGraalNativeSdk Threads::Threads)

add_custom_command(TARGET kit-reference-adapter POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:DxFeedGraalNativeSdk> $<TARGET_FILE_DIR:kit-reference-adapter>
        )
