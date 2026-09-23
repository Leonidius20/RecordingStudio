#ifndef AAP_CORE_PLUGINPLAYER_H
#define AAP_CORE_PLUGINPLAYER_H

#include "PluginPlayerConfiguration.h"
#include "AudioGraph.h"

namespace aap {
    class PluginPlayer {
        PluginPlayerConfiguration configuration;

        SimpleLinearAudioGraph graph;

    public:
        PluginPlayer(PluginPlayerConfiguration &configuration, int outFileFd);

        virtual ~PluginPlayer();

        SimpleLinearAudioGraph& getGraph() { return graph; }

        void setAudioSource(uint8_t *data, int32_t dataLength, const char *filename);

        void addMidiEvents(uint8_t* data, int32_t dataLength, uint64_t timestampInNanoseconds);

        void startProcessing();

        void pauseProcessing();

        void enableAudioRecorder();

        void setPresetIndex(int pluginNumber, int index);
    };
}


#endif //AAP_CORE_PLUGINPLAYER_H
