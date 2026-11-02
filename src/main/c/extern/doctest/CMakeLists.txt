# SPDX-License-Identifier: MPL-2.0

# The standard of the tests: parts of doctest.h depend on it, so the implementation is compiled with the same one.
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Target
set(target doctest)
# Add library
add_library(${target} INTERFACE)
# Add header-only
target_include_directories(${target} INTERFACE .)
# Create namespaced alias
add_library(doctest::${target} ALIAS ${target})

# Target
set(target doctest_with_main)
# Add library: the implementation and the main function, compiled once for all tests and only when a test is built
add_library(${target} STATIC EXCLUDE_FROM_ALL doctest_main.cpp)
# Add the code model of the tests
set_target_properties(${target} PROPERTIES POSITION_INDEPENDENT_CODE ON)
# Add the header
target_link_libraries(${target} PUBLIC doctest)
# Create namespaced alias
add_library(doctest::${target} ALIAS ${target})
