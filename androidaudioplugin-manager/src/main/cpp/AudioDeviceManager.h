#ifndef AAP_CORE_AUDIODEVICEMANAGER_H
#define AAP_CORE_AUDIODEVICEMANAGER_H

#include <stdint.h>
#include <memory>

#include "AudioDevice.h"
#include "FileAudioDeviceOut.h"

namespace aap {
    class AudioDeviceManager {
    public:
        // This needs to be implemented for each platform
        static AudioDeviceManager* getInstance();

        virtual AudioDeviceIn* openDefaultInput(uint32_t sampleRate, uint32_t framesPerCallback, int32_t numChannels) = 0;
        virtual FileAudioDeviceOut* openDefaultOutput(uint32_t sampleRate, uint32_t framesPerCallback, int32_t numChannels, int outFileFd) = 0;
    };
}


#endif //AAP_CORE_AUDIODEVICEMANAGER_H
