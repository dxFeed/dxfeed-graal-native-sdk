# SPDX-License-Identifier: MPL-2.0

# Function to register BuildAll target
function(add_target_to_build_all target)
    set(BuildAll ${META_PROJECT_NAME}.build-all)
    if (NOT TARGET ${BuildAll})
        add_custom_target(${BuildAll})
    endif ()
    add_dependencies(${BuildAll} ${target})
endfunction()

# Set policy if policy is available
function(set_policy pol val)
    if (POLICY ${pol})
        cmake_policy(SET ${pol} ${val})
    endif ()
endfunction(set_policy)
