#ifndef AAP_CORE_AUDIODEVICE_H
#define AAP_CORE_AUDIODEVICE_H

#include <cstdint>
#include "LocalDefinitions.h"
#include "AudioBuffer.h"

#include <oboe/Oboe.h>

namespace aap {

    typedef void(AudioDeviceCallback(void* callbackContext, AudioBuffer* audioData, int32_t numFrames));

    class AAP_PUBLIC_API AudioDeviceIn {

    public:
        AudioDeviceIn() {}

        AAP_PUBLIC_API virtual void startCallback() = 0;
        AAP_PUBLIC_API virtual void stopCallback() = 0;

        virtual void setAudioCallback(AudioDeviceCallback audioDeviceCallback, void* callbackContext) = 0;

        /// reads the audio data from the backend into `dstAudioData`.
        virtual void read(AudioBuffer *dstAudioData, int32_t bufferPosition, int32_t numFrames) = 0;

        /// The platform backend may require audio recording permission (e.g. Android).
        /// The backend implementation should return true if that is the case.
        virtual bool isPermissionRequired() { return false; }
    };

    class AAP_PUBLIC_API AudioDeviceOut {
    public:
        AudioDeviceOut() {}

        AAP_PUBLIC_API virtual void startCallback() = 0;
        AAP_PUBLIC_API virtual void stopCallback() = 0;

        /// Sets an abstracted `AudioDeviceCallback` to be called within the actual device callback.
        ///
        /// The platform backend (e.g. OboeAudioDeviceOut) will invoke `audioDeviceCallback`
        /// before it attempts to write its cached output buffer.
        ///
        /// A user app is supposed to call `write()` within the actual callback. `write()` will then
        /// store the audio buffer within the instance of this class so that the platform backend
        /// can write to its audio buffer (e.g. `audioData` in Oboe `onAudioReady()`).
        virtual void setAudioCallback(AudioDeviceCallback audioDeviceCallback, void* callbackContext) = 0;

        /// writes `audioDataToWrite` into the backend audio output.
        /// Note that the actual outputting is done through the backend audio callback.
        virtual void write(AudioBuffer *audioDataToWrite, int32_t bufferPosition, int32_t numFrames) = 0;
    };
}

#endif //AAP_CORE_AUDIODEVICE_H
