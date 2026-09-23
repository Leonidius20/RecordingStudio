cmake_minimum_required(VERSION 3.5.1)

project(androidaudioplugin-manager LANGUAGES CXX)

# List of sources. Android build has some additional sources.
set (androidaudioplugin-manager_SOURCES
		#zix/ring.cpp
        AudioBuffer.cpp
		AudioDevice.cpp
        AudioDeviceManager.cpp
		OboeAudioDeviceManager.cpp
		VirtualAudioDeviceManager.cpp
		FileAudioDeviceManager.cpp
		FileAudioDeviceOut.cpp
		AudioGraph.cpp
		AudioGraphNode.AudioDevice.cpp
		AudioGraphNode.DataSource.cpp
		AudioGraphNode.Plugin.cpp
		AudioGraphNode.Midi.cpp
		AAPMidiEventTranslator.cpp
		PluginPlayer.cpp
		PluginPlayerConfiguration.cpp
		JNI.cpp
		fdstream.cpp
		nodes/input/file/FileAudioDataSourceNode.cpp
	)

set(CMAKE_BUILD_TYPE Debug)

if (ANDROID)
set (androidaudioplugin-manager_SOURCES
	${androidaudioplugin-manager_SOURCES}
	)
else (ANDROID)
set (androidaudioplugin-manager_SOURCES
	${androidaudioplugin-manager_SOURCES}
	)
endif (ANDROID)

find_package(oboe REQUIRED CONFIG)
find_package(androidaudioplugin REQUIRED CONFIG)

add_library (androidaudioplugin-manager
  SHARED
  ${androidaudioplugin-manager_SOURCES}
  )

target_compile_options (androidaudioplugin-manager
		PRIVATE
		-std=c++17 -Wall -Wshadow
		-Werror=unguarded-availability
		)

target_compile_definitions (androidaudioplugin-manager
		PUBLIC
		__ANDROID_UNAVAILABLE_SYMBOLS_ARE_WEAK__=1
		)

target_include_directories (androidaudioplugin-manager
		PRIVATE
		"../../../../include/"
		"../../../../external/cmidi2"
		"../../../../external/choc"
		)

if (ANDROID)
target_include_directories (androidaudioplugin-manager
		PRIVATE
		)

target_link_libraries (androidaudioplugin-manager
		androidaudioplugin::androidaudioplugin
		oboe::oboe
		log
		android
		)
endif (ANDROID)

# You can set it via build.gradle.
if (${AAP_ENABLE_ASAN})
target_compile_options (androidaudioplugin-manager
		PUBLIC
		-fsanitize=address -fno-omit-frame-pointer
		)

target_link_options(androidaudioplugin-manager
		PUBLIC
		-fsanitize=address
		)

set_target_properties(androidaudioplugin-manager
		PROPERTIES LINK_FLAGS -fsanitize=address
		)
endif()
