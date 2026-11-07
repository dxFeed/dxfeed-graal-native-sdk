#
# Executable name and options
#

# Target name
set(target SignalHandlersTest)

# Output name
set(output_name ${target})

set(CMAKE_CXX_STANDARD 20)
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
        DxFeedGraalNativeSdk
        )


#
# Compile definitions
#

target_compile_definitions(${target}
        PRIVATE
        ${DEFAULT_COMPILE_DEFINITIONS}
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

# The isolate installs the handlers of SIGPIPE and SIGXFSZ once per process, so each case runs in a process of its
# own, selected by its test suite; a test suite that matches no case fails instead of passing. Windows has no such
# signals: the test runs only on Linux and macOS.
if(NOT WIN32)
    add_test(NAME ${target}Installed COMMAND $<TARGET_FILE:${target}> --test-suite=Installed)
    add_test(NAME ${target}Disabled COMMAND $<TARGET_FILE:${target}> --test-suite=Disabled)
    add_test(NAME ${target}Kept COMMAND $<TARGET_FILE:${target}> --test-suite=Kept)
    set_tests_properties(${target}Installed ${target}Disabled ${target}Kept
            PROPERTIES FAIL_REGULAR_EXPRESSION "test cases: *0 [|]")
endif()


#
# Custom commands
#

# Copy shared library to output directory
add_custom_command(TARGET ${target} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:DxFeedGraalNativeSdk> $<TARGET_FILE_DIR:${target}>
        )
