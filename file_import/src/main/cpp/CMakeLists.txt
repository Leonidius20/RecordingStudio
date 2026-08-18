cmake_minimum_required(VERSION 3.6)
project(dummy)

# This just ensures libc++_shared is packaged
add_library(dummy SHARED dummy.cpp fdstream.h fdstream.cpp)
target_link_libraries(dummy PUBLIC c++_shared)
