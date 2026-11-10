# SPDX-License-Identifier: MPL-2.0

#
# The publisher tool of the C API: the static library NetTestCore (everything but main) and the executable NetTest
#
# The tool is not installed: no install() rule, so neither the SDK archive nor the cpack zip contains it.
#

set(core NetTestCore)
set(target NetTest)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CXX_EXTENSIONS OFF)
set(C_EXTENSIONS OFF)

# Print status
message(STATUS "App ${target}")


#
# Sources
#

file(GLOB core_headers CONFIGURE_DEPENDS
        "${CMAKE_CURRENT_SOURCE_DIR}/include/NetTest/*.hpp"
        )
file(GLOB core_sources CONFIGURE_DEPENDS
        "${CMAKE_CURRENT_SOURCE_DIR}/src/*.cpp"
        )


#
# Core library
#

add_library(${core} STATIC)

target_sources(${core}
        PRIVATE
        "${core_headers}"
        "${core_sources}"
        )

set_target_properties(${core}
        PROPERTIES
        ${DEFAULT_PROJECT_OPTIONS}
        FOLDER "${IDE_FOLDER}"
        )

target_include_directories(${core}
        PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        PRIVATE
        ${DEFAULT_INCLUDE_DIRECTORIES}
        ${PROJECT_BINARY_DIR}/include
        ${PROJECT_BINARY_DIR}/source/include
        )

target_link_libraries(${core}
        PUBLIC
        ${DEFAULT_LIBRARIES}
        DxFeedGraalNativeSdk
        )

target_compile_definitions(${core}
        PRIVATE
        ${DEFAULT_COMPILE_DEFINITIONS}
        )

target_compile_options(${core}
        PRIVATE
        ${DEFAULT_COMPILE_OPTIONS}
        )


#
# Executable
#

add_executable(${target})

target_sources(${target}
        PRIVATE
        "${CMAKE_CURRENT_SOURCE_DIR}/main.cpp"
        )

set_target_properties(${target}
        PROPERTIES
        ${DEFAULT_PROJECT_OPTIONS}
        FOLDER "${IDE_FOLDER}"
        OUTPUT_NAME "${target}"
        )

target_include_directories(${target}
        PRIVATE
        ${DEFAULT_INCLUDE_DIRECTORIES}
        ${PROJECT_BINARY_DIR}/include
        ${PROJECT_BINARY_DIR}/source/include
        )

target_link_libraries(${target}
        PRIVATE
        ${DEFAULT_LIBRARIES}
        ${core}
        DxFeedGraalNativeSdk
        )

target_compile_definitions(${target}
        PRIVATE
        ${DEFAULT_COMPILE_DEFINITIONS}
        )

target_compile_options(${target}
        PRIVATE
        ${DEFAULT_COMPILE_OPTIONS}
        )

target_link_options(${target}
        PRIVATE
        ${DEFAULT_LINKER_OPTIONS}
        )


#
# Custom commands
#

# Copy shared library to output directory
add_custom_command(TARGET ${target} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:DxFeedGraalNativeSdk> $<TARGET_FILE_DIR:${target}>
        )
