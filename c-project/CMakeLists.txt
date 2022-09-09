#
# Library name and options
#

# Target
set(target GraalvmTestNative)

# Print status
message(STATUS "Imported lib ${target}")


#
# Create target
#

# Add imported library
add_library(${target} SHARED IMPORTED GLOBAL)

# Set empty library prefix
set(lib_prefix "")
# Set empty library extension
set(lib_extension "")

# Add platform specific variable
if (WIN32)
    set(lib_prefix "")
    set(lib_extension "dll")
elseif (APPLE)
    set(lib_prefix "lib")
    set(lib_extension "dylib")
elseif (UNIX)
    set(lib_prefix "lib")
    set(lib_extension "so")
else()
    message(ERROR "Unknown platform!")
endif ()


#
# Project options
#

# Target properties
set_target_properties(${target}
        PROPERTIES
        INTERFACE_INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}"
        # Path to dynamic library
        IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/${lib_prefix}${target}.${lib_extension}"
        # For Windows
        IMPORTED_IMPLIB "${CMAKE_CURRENT_SOURCE_DIR}/${lib_prefix}${target}.lib"
        # For Linux and macOS
        IMPORTED_NO_SONAME "TRUE"
        )
