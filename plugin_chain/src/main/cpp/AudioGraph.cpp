#include "AudioGraph.h"
#if ANDROID
#include <android/trace.h>
#endif
#include "FileAudioDeviceOut.h"

aap::SimpleLinearAudioGraph::~SimpleLinearAudioGraph() {
    for (auto node : nodes)
        node->pause(); // and leave destructors do the job
}

void aap::SimpleLinearAudioGraph::processAudio(AudioBuffer *audioData, int32_t numFrames) {
    struct timespec timeSpecBegin{}, timeSpecEnd{};
#if ANDROID
    if (ATrace_isEnabled()) {
        ATrace_beginSection("AAP::SimpleLinearAudioGraph_processAudio");
        clock_gettime(CLOCK_REALTIME, &timeSpecBegin);
    }
#endif

    for (auto node : nodes)
        if (!node->shouldSkip())
            node->processAudio(audioData, numFrames);

#if ANDROID
    if (ATrace_isEnabled()) {
        clock_gettime(CLOCK_REALTIME, &timeSpecEnd);
        ATrace_setCounter("AAP::SimpleLinearAudioGraph_processAudio",
                          (timeSpecEnd.tv_sec - timeSpecBegin.tv_sec) * 1000000000 + timeSpecEnd.tv_nsec - timeSpecBegin.tv_nsec);
        ATrace_endSection();
    }
#endif

}

void aap::SimpleLinearAudioGraph::setPlugin(aap::RemotePluginInstance *instance) {
    auto &pluginNode = plugins.emplace_back(
            std::make_shared<AudioPluginNode>(this, instance)
                    );
    auto ptr = pluginNode.get();
    nodes.insert(nodes.begin() + 3 + plugins.size(), ptr);
    //plugin.setPlugin(instance);
}

void
aap::SimpleLinearAudioGraph::setAudioSource(uint8_t *data, int dataLength, const char *filename) {
    int32_t targetFrames = audio_data.setAudioSource(data, dataLength, filename);
    output.getDevice()->setTargetNumFrames(targetFrames);
}

void aap::SimpleLinearAudioGraph::addMidiEvent(uint8_t *data, int32_t length, int64_t timestampInNanoseconds) {
    midi_input.addMidiEvent(data, length, timestampInNanoseconds);
}

void aap::SimpleLinearAudioGraph::playAudioData() {
    audio_data.setPlaying(true);
}

void aap::SimpleLinearAudioGraph::startProcessing() {
    if (isProcessing())
        return;
    is_processing = true;
    for (auto node : nodes)
        node->start();
}

void aap::SimpleLinearAudioGraph::pauseProcessing() {
    if (!isProcessing())
        return;
    is_processing = false;
    for (auto node : nodes)
        node->pause();
}

aap::SimpleLinearAudioGraph::SimpleLinearAudioGraph(int32_t sampleRate, uint32_t framesPerCallback, int32_t channelsInAudioBus, int outFileFd) :
        AudioGraph(sampleRate, framesPerCallback, channelsInAudioBus),
        input(this, AudioDeviceManager::getInstance()->openDefaultInput(sampleRate, framesPerCallback, channelsInAudioBus)),
        output(this,
               //std::make_shared<FileAudioDeviceOut>(sampleRate, framesPerCallback, channelsInAudioBus, outFileFd).get()
                AudioDeviceManager::getInstance()->openDefaultOutput(sampleRate, framesPerCallback, channelsInAudioBus, outFileFd)
               ),
        //plugin(this, nullptr),
        plugins(),
        audio_data(this),
        midi_input(this, nullptr, sampleRate, framesPerCallback, CMIDI2_PROTOCOL_TYPE_MIDI2, AAP_PLUGIN_PLAYER_DEFAULT_MIDI_RING_BUFFER_SIZE),
        midi_output(this, AAP_PLUGIN_PLAYER_DEFAULT_MIDI_RING_BUFFER_SIZE) {
    nodes.emplace_back(&input);
    nodes.emplace_back(&audio_data);
    nodes.emplace_back(&midi_input);

    //nodes.emplace_back(&plugin);
    nodes.emplace_back(&midi_output);
    nodes.emplace_back(&output);

    output.getDevice()->setAudioCallback(audio_callback, this);
}

void aap::SimpleLinearAudioGraph::enableAudioRecorder() {
    input.setPermissionGranted();
}

void aap::SimpleLinearAudioGraph::setPresetIndex(int index) {
    plugins[index]->setPresetIndex(index);
    // plugin.setPresetIndex(index);
}
