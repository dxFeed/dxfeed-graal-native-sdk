# SPDX-License-Identifier: MPL-2.0

#
# Executable name and options
#

# Target name
set(target DxfgClient)

# Output name
set(output_name ${target})

# Print status
message(STATUS "App ${target}")


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

# Create namespaced alias
add_executable(${META_PROJECT_NAME}::${target} ALIAS ${target})


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
        )


#
# Libraries
#

target_link_libraries(${target}
        PRIVATE
        ${DEFAULT_LIBRARIES}
        DxFeedGraalNativeSdk
        DxfgUtils
        DxfgEventFormatter
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
# Deployment
#

# Executable
install(TARGETS ${target}
        RUNTIME DESTINATION ${INSTALL_BIN} COMPONENT runtime
        BUNDLE  DESTINATION ${INSTALL_BIN} COMPONENT runtime
        )


#
# Custom commands
#

# Copy shared library to output directory
add_custom_command(TARGET ${target} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:DxFeedGraalNativeSdk> $<TARGET_FILE_DIR:${target}>
        )
