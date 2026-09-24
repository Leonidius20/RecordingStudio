#include "AudioGraphNode.h"
#include "AudioGraph.h"

aap::AudioDeviceInputNode::~AudioDeviceInputNode() {
    getDevice()->stopCallback();
}

void aap::AudioDeviceInputNode::processAudio(AudioBuffer *audioData, int32_t numFrames) {
    // copy current audio input data into `audioData`
    getDevice()->read(audioData, consumer_position, numFrames);
    // TODO: adjust ring buffer offset, not just simple adder.
    consumer_position += numFrames;
}

void aap::AudioDeviceInputNode::start() {
    if (!shouldSkip())
        getDevice()->startCallback();
}

void aap::AudioDeviceInputNode::pause() {
    if (!shouldSkip())
        getDevice()->stopCallback();
}

void aap::AudioDeviceInputNode::setPermissionGranted() {
    permission_granted = true;
}

bool aap::AudioDeviceInputNode::shouldSkip() {
    return getDevice()->isPermissionRequired() && !permission_granted;
}

//--------

aap::AudioDeviceOutputNode::~AudioDeviceOutputNode() {
    getDevice()->stopCallback();
}

void aap::AudioDeviceOutputNode::processAudio(AudioBuffer *audioData, int32_t numFrames) {
    // copy `audioData` into current audio output buffer
    getDevice()->write(audioData, consumer_position, numFrames);
    // TODO: adjust ring buffer offset, not just simple adder.
    consumer_position += numFrames;
}

void aap::AudioDeviceOutputNode::start() {
    getDevice()->startCallback();
}

void aap::AudioDeviceOutputNode::pause() {
    getDevice()->stopCallback();
}

