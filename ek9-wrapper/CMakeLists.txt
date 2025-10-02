cmake_minimum_required(VERSION 3.10)

# Force clang compiler on all platforms (must be set BEFORE project())
set(CMAKE_C_COMPILER "clang")

project(ek9-wrapper C)

# Set C standard
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Platform-specific compiler flags
if(UNIX AND NOT APPLE)
    # Linux
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")
elseif(APPLE)
    # macOS
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")
elseif(WIN32)
    # Windows with clang-cl (MSVC-compatible frontend)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")
endif()

# Source file
set(SOURCE_FILE ${CMAKE_CURRENT_SOURCE_DIR}/src/main/c/ek9.c)

# Executable target
add_executable(ek9 ${SOURCE_FILE})

# Output directory
set_target_properties(ek9 PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
)

# Platform-specific linking
if(WIN32)
    # Windows requires additional libraries for GetModuleFileName
    target_link_libraries(ek9 PRIVATE kernel32)
endif()
