#
# Executable name and options
#

# Target name
set(target SystemNativeTest)

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
        )


#
# Libraries
#

target_link_libraries(${target}
        PRIVATE
        ${DEFAULT_LIBRARIES}
        Catch2::Catch2WithMain
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

add_test(NAME ${target} COMMAND $<TARGET_FILE:${target}>)


#
# Custom commands
#

# Copy shared library to output directory
add_custom_command(TARGET ${target} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
        $<TARGET_FILE:DxFeedGraalNativeSdk> $<TARGET_FILE_DIR:${target}>
        )
