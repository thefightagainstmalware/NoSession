cmake_minimum_required(VERSION 3.25)
project(chromium_bindings)

set(CMAKE_CXX_STANDARD 20)

include_directories(src/main/cpp)
include_directories(chromium)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})
include_directories(chromium/third_party/abseil-cpp/)
include_directories(chromium/third_party/googletest/src/googletest/include)
add_library(chromium_bindings
        src/main/cpp/gq_malwarefight_nosession_win_WindowsSandbox.cpp
        src/main/cpp/gq_malwarefight_nosession_win_WindowsSandbox.h)
