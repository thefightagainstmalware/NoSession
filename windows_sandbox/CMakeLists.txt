cmake_minimum_required(VERSION 3.25)
project(windows_sandbox)

set(CMAKE_CXX_STANDARD 20)

include_directories(src/main/cpp)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})
add_library(windows_sandbox SHARED
        src/main/cpp/gq_malwarefight_nosession_win_WindowsSandbox.cpp
        src/main/cpp/gq_malwarefight_nosession_win_WindowsSandbox.h
        src/main/cpp/ContainerCreate.cpp
        src/main/cpp/ContainerCreate.h
        )
