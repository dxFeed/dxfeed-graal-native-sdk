# SPDX-License-Identifier: MPL-2.0

# Target
set(target Catch2)
# Add library
add_library(${target} INTERFACE)
# Add header-only
target_include_directories(${target} INTERFACE .)
# Create namespaced alias
add_library(Catch2::${target} ALIAS ${target})

# Target
set(target Catch2WithMain)
# Add library
add_library(${target} INTERFACE)
# Add header-only
target_include_directories(${target} INTERFACE .)
# Add compile definition for build without main
target_compile_definitions(${target} INTERFACE CATCH_CONFIG_MAIN)
# Create namespaced alias
add_library(Catch2::${target} ALIAS ${target})
