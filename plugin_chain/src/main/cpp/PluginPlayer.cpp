#include "PluginPlayer.h"
#include <aap/core/host/plugin-instance.h>

aap::PluginPlayer::PluginPlayer(aap::PluginPlayerConfiguration &pluginPlayerConfiguration, int outFileFd) :
                                configuration(pluginPlayerConfiguration),
                                graph(configuration.getSampleRate(), configuration.getFramesPerCallback(), configuration.getChannelCount(), outFileFd) {
}

aap::PluginPlayer::~PluginPlayer() {
    graph.pauseProcessing();
}

void aap::PluginPlayer::setAudioSource(uint8_t *data, int32_t dataLength, const char *filename) {
    graph.setAudioSource(data, dataLength, filename);
}

void aap::PluginPlayer::startProcessing() {
    graph.startProcessing();
}

void aap::PluginPlayer::pauseProcessing() {
    graph.pauseProcessing();
}

void aap::PluginPlayer::enableAudioRecorder() {
    graph.enableAudioRecorder();
}

void
aap::PluginPlayer::addMidiEvents(uint8_t *data, int32_t dataLength, uint64_t timestampInNanoseconds) {
    graph.addMidiEvent(data, dataLength, timestampInNanoseconds);
}

void aap::PluginPlayer::setPresetIndex(int pluginNumber, int index) {
    graph.setPresetIndex(index);
}
