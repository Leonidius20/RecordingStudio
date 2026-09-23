#ifndef AAP_CORE_AUDIOGRAPH_H
#define AAP_CORE_AUDIOGRAPH_H

#include <cstdint>
#include <cassert>
#include <vector>
#include <memory>

#include "LocalDefinitions.h"
#include "AudioDeviceManager.h"
#include "AudioGraphNode.h"

namespace aap {
    AAP_OPEN_CLASS class AudioGraph {
        int32_t sample_rate;
        int32_t frames_per_callback;
        int32_t num_channels;

    public:
        explicit AudioGraph(int32_t sampleRate, int32_t framesPerCallback, int32_t channelsInAudioBus) :
                sample_rate(sampleRate),
                frames_per_callback(framesPerCallback),
                num_channels(channelsInAudioBus) {
        }

        virtual void processAudio(AudioBuffer* audioData, int32_t numFrames) = 0;

        int32_t getSampleRate() { return sample_rate; }

        int32_t getFramesPerCallback() { return frames_per_callback; }

        int32_t getChannelsInAudioBus() { return num_channels; }
    };

    class SimpleLinearAudioGraph : public AudioGraph {
        AudioDeviceInputNode input;
        AudioDeviceOutputNode output;
        std::vector<std::shared_ptr<AudioPluginNode>> plugins;
        // AudioPluginNode plugin;
        AudioDataSourceNode audio_data;
        MidiSourceNode midi_input;
        MidiDestinationNode midi_output;
        std::vector<AudioGraphNode*> nodes{};
        bool is_processing{false};

        static void audio_callback(void* callbackContext, AudioBuffer* audioData, int32_t numFrames) {
            ((SimpleLinearAudioGraph*) callbackContext)->processAudio(audioData, numFrames);
        }

    public:
        SimpleLinearAudioGraph(int32_t sampleRate, uint32_t framesPerCallback, int32_t channelsInAudioBus, int outFileFd);
        virtual ~SimpleLinearAudioGraph();

        void setPlugin(RemotePluginInstance* instance);

        void setAudioSource(uint8_t *data, int dataLength, const char *filename);

        void processAudio(AudioBuffer *audioData, int32_t numFrames) override;

        void addMidiEvent(uint8_t *data, int32_t dataLength, int64_t timestampInNanoseconds);

        void playAudioData();

        bool isProcessing() { return is_processing; }

        void startProcessing();

        void pauseProcessing();

        void enableAudioRecorder();

        void setPresetIndex(int index);
    };

    // Not planned to implement so far.
    class BasicAudioGraph : public AudioGraph {
    public:
        BasicAudioGraph(int32_t sampleRate, int32_t framesPerCallback, int32_t channelsInAudioBus) :
                AudioGraph(sampleRate, framesPerCallback, channelsInAudioBus) {
        }

        void attachNode(AudioGraphNode* sourceNode, int32_t sourceOutputBusIndex, AudioGraphNode* destinationNode, int32_t destinationInputBusIndex) {}
        void detachNode(AudioGraphNode* sourceNode, int32_t sourceOutputBusIndex) {}
    };
}

#endif //AAP_CORE_AUDIOGRAPH_H
