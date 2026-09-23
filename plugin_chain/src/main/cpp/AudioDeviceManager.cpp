#include "AudioDeviceManager.h"


#if ANDROID
#include "OboeAudioDeviceManager.h"

aap::OboeAudioDeviceManager audioDeviceManager{};

aap::AudioDeviceManager* aap::AudioDeviceManager::getInstance() {
    return &audioDeviceManager;
}

#else

// implement desktop version?

#endif
