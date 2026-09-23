#ifndef AAP_CORE_PLUGINPLAYERCONFIGURATION_H
#define AAP_CORE_PLUGINPLAYERCONFIGURATION_H

#include "AudioDevice.h"

#include <cstdint>

namespace aap {
    class PluginPlayerConfiguration {
        int32_t sample_rate;
        int32_t frames_per_callback;
        int32_t channel_count;

    public:
        PluginPlayerConfiguration(
                /** should default to device optimal settings */
                int32_t sampleRate,
                /** should default to device optimal settings */
                int32_t framesPerCallback,
                int32_t channelCount) :
                sample_rate(sampleRate),
                frames_per_callback(framesPerCallback),
                channel_count(channelCount) {
        }

        int32_t getSampleRate() { return sample_rate; }

        int32_t getFramesPerCallback() { return frames_per_callback; }

        int32_t getChannelCount() { return channel_count; }
    };

}


#endif //AAP_CORE_PLUGINPLAYERCONFIGURATION_H
