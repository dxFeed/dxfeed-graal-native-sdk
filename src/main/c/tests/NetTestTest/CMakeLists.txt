#
# Executable name and options
#

# Target name
set(target NetTestTest)

# Output name
set(output_name ${target})

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CXX_EXTENSIONS OFF)
set(C_EXTENSIONS OFF)

# Print status
message(STATUS "Test ${target}")


#
# Sources
#

set(include_path "${CMAKE_CURRENT_SOURCE_DIR}/")
set(source_path "${CMAKE_CURRENT_SOURCE_DIR}/")

file(GLOB headers CONFIGURE_DEPENDS
        "${include_path}/*.h"
        "${include_path}/*.hpp"
        )
file(GLOB sources CONFIGURE_DEPENDS
        "${source_path}/*.c"
        "${source_path}/*.cpp"
        "${source_path}/*.h"
        "${source_path}/*.hpp"
        )


#
# Create executable
#

# Build executable
add_executable(${target})

# Add sources to target
target_sources(${target}
        PRIVATE
        "${headers}"
        "${sources}"
        )


#
# Project options
#

set_target_properties(${target}
        PROPERTIES
        ${DEFAULT_PROJECT_OPTIONS}
        FOLDER "${IDE_FOLDER}"
        OUTPUT_NAME "${output_name}"
        )


#
# Include directories
#

target_include_directories(${target}
        PRIVATE
        ${DEFAULT_INCLUDE_DIRECTORIES}
        ${PROJECT_BINARY_DIR}/include
        ${PROJECT_BINARY_DIR}/source/include
        ${CMAKE_CURRENT_SOURCE_DIR}/../common
        )


#
# Libraries
#

target_link_libraries(${target}
        PRIVATE
        ${DEFAULT_LIBRARIES}
        doctest::doctest_with_main
        NetTestCore
        DxFeedGraalNativeSdk
        $<$<PLATFORM_ID:Windows>:ws2_32>
        )


#
# Compile definitions
#

target_compile_definitions(${target}
        PRIVATE
        ${DEFAULT_COMPILE_DEFINITIONS}
        NETTEST_TEST_DATA_DIR="${CMAKE_CURRENT_SOURCE_DIR}"
        # The kit of the NetTest specification, which KitTest.cpp checks the tool against.
        NETTEST_KIT_DIR="${CMAKE_CURRENT_SOURCE_DIR}/../../../../../docs/wrapper-guide/nettest"
        )


#
# Compile options
#

target_compile_options(${target}
        PRIVATE
        ${DEFAULT_COMPILE_OPTIONS}
        )


#
# Linker options
#

target_link_options(${target}
        PRIVATE
        ${DEFAULT_LINKER_OPTIONS}
        )


#
# Testing
#

# The cases of one process: the unit cases, the end to end cases ("E2E: ...") in a process of their own, because the
# system property dxscheme.nanoTime must be set before the first endpoint of the process, and the cases of the builder
# property ("StreamFeed: ...") in a process that never sets the system property, and the consumer cases ("Consumer: ...")
# in a process of their own for their length. Each entry has its own timeout.
add_test(NAME ${target} COMMAND $<TARGET_FILE:${target}> "--test-case-exclude=E2E*,StreamFeed*,Consumer*")
set_tests_properties(${target} PROPERTIES TIMEOUT 60)

add_test(NAME ${target}.EndToEnd COMMAND $<TARGET_FILE:${target}> "--test-case=E2E*")
set_tests_properties(${target}.EndToEnd PROPERTIES TIMEOUT 60)

add_test(NAME ${target}.StreamFeed COMMAND $<TARGET_FILE:${target}> "--test-case=StreamFeed*")
set_tests_properties(${target}.StreamFeed PROPERTIES TIMEOUT 60)

add_test(NAME ${target}.Consumer COMMAND $<TARGET_FILE:${target}> "--test-case=Consumer*")
set_tests_properties(${target}.Consumer PROPERTIES TIMEOUT 120)

# The built tool in a process, and `c` against `p` in two processes: the exit codes and the statistics lines (the
# expressions are read from Statistics.hpp).
# The SDK library lies next to the tool (a post-build copy of its target), the rpath is $ORIGIN or @loader_path.
add_test(NAME NetTestProcess
        COMMAND ${CMAKE_COMMAND}
        "-DNETTEST=$<TARGET_FILE:NetTest>"
        "-DSTATISTICS_HEADER=${CMAKE_CURRENT_SOURCE_DIR}/../../src/apps/NetTest/include/NetTest/Statistics.hpp"
        -P "${CMAKE_CURRENT_SOURCE_DIR}/NetTestProcess.cmake")
set_tests_properties(NetTestProcess PROPERTIES TIMEOUT 150)

# NetTest built against the SDK archive in target/ as a user builds it outside the repository (src/apps/NetTest as the
# top-level project), with the generator and the compilers of this build, then run once.
if (WIN32)
    set(standalone_arch "amd64-windows")
elseif (APPLE)
    if (CMAKE_HOST_SYSTEM_PROCESSOR STREQUAL "arm64")
        set(standalone_arch "aarch64-osx")
    else ()
        set(standalone_arch "x86_64-osx")
    endif ()
elseif (CMAKE_SYSTEM_PROCESSOR MATCHES "^(aarch64|arm64)$")
    set(standalone_arch "aarch64-linux")
else ()
    set(standalone_arch "amd64-linux")
endif ()

# The generator settings that this platform and generator define (an undefined one is an error of --warn-uninitialized
# in the presets).
set(standalone_options "")

foreach (setting PLATFORM TOOLSET INSTANCE)
    if (DEFINED CMAKE_GENERATOR_${setting})
        list(APPEND standalone_options "-D${setting}=${CMAKE_GENERATOR_${setting}}")
    endif ()
endforeach ()

if (DEFINED CMAKE_OSX_ARCHITECTURES)
    list(APPEND standalone_options "-DOSX_ARCHITECTURES=${CMAKE_OSX_ARCHITECTURES}")
endif ()

add_test(NAME NetTestStandalone
        COMMAND ${CMAKE_COMMAND}
        "-DARCHIVE_GLOB=${CMAKE_CURRENT_SOURCE_DIR}/../../../../../target/graal-native-sdk-${standalone_arch}*.zip"
        "-DSOURCE=${CMAKE_CURRENT_SOURCE_DIR}/../../src/apps/NetTest"
        "-DWORK=${CMAKE_CURRENT_BINARY_DIR}/NetTestStandalone"
        "-DCONFIG=$<CONFIG>"
        "-DGENERATOR=${CMAKE_GENERATOR}"
        ${standalone_options}
        "-DC_COMPILER=${CMAKE_C_COMPILER}"
        "-DCXX_COMPILER=${CMAKE_CXX_COMPILER}"
        "-DSTATISTICS_HEADER=${CMAKE_CURRENT_SOURCE_DIR}/../../src/apps/NetTest/include/NetTest/Statistics.hpp"
        -P "${CMAKE_CURRENT_SOURCE_DIR}/NetTestStandalone.cmake")
set_tests_properties(NetTestStandalone PROPERTIES TIMEOUT 300)


#
# Custom commands
#

# Copy shared library to output directory
add_custom_command(TARGET ${target} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:DxFeedGraalNativeSdk> $<TARGET_FILE_DIR:${target}>
        )
