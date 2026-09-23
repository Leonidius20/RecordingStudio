#ifndef AAP_CORE_AUDIOGRAPHNODE_H
#define AAP_CORE_AUDIOGRAPHNODE_H

#include "AudioDevice.h"
#include "AAPMidiEventTranslator.h"
#include "FileAudioDeviceOut.h"
#include <aap/core/host/plugin-instance.h>
#include <aap/unstable/utility.h>
#ifndef CMIDI2_H_INCLUDED // it is only a workaround to avoid reference resolution failure at aap-juce-* repos.
#include <cmidi2.h>
#endif

namespace aap {
    class AudioGraph;

    class AudioGraphNode {

    protected:
        AudioGraph* graph;

        AudioGraphNode(AudioGraph* ownerGraph) : graph(ownerGraph) {}

    public:
        virtual ~AudioGraphNode() {}
        virtual bool shouldSkip() { return false; }
        virtual void start() = 0;
        virtual void pause() = 0;
        virtual void processAudio(AudioBuffer* audioData, int32_t numFrames) = 0;
    };

    /**
     * AudioDeviceInputNode outputs the audio input from the device callback at processing.
     *
     * Single producer, multiple consumer: more than one node could retrieve the same input buffer.
     *
     * It internally uses ring buffer for audio inputs. (TODO)
     *
     * In case of failed audio processing, it may resend the same output chunk.
     * It does not check if the input was already consumed or not.
     *
     */
    class AudioDeviceInputNode : public AudioGraphNode {
        AudioDeviceIn* input;
        int32_t consumer_position{0};
        bool permission_granted{false};

    public:
        AudioDeviceInputNode(AudioGraph* ownerGraph, AudioDeviceIn* input) :
                AudioGraphNode(ownerGraph),
                input(input) {
        }
        virtual ~AudioDeviceInputNode();

        AudioDeviceIn* getDevice() { return input; }

        bool shouldSkip() override;

        void start() override;
        void pause() override;
        void processAudio(AudioBuffer* audioData, int32_t numFrames) override;

        void setPermissionGranted();
    };

    /**
     * AudioDeviceOutputNode outputs its input directly to the device at processing.
     *
     * Single producer (app), single consumer (audio device callback).
     *
     * On a live audio processor, the Oboe audio device callback would kick the overall
     * AudioGraph processing, and this node is typically laid out at the end of one of the chains.
     *
     * AudioGraph itself does not handle output copying - it might just store the processing
     * results on an AudioDestinationNode and let the client app retrieve the buffer.
     */
    class AudioDeviceOutputNode : public AudioGraphNode {
        FileAudioDeviceOut* output;
        int32_t consumer_position{0};

    public:
        AudioDeviceOutputNode(AudioGraph* ownerGraph, FileAudioDeviceOut* output) :
                AudioGraphNode(ownerGraph),
                output(output) {
        }
        virtual ~AudioDeviceOutputNode();

        FileAudioDeviceOut* getDevice() { return output; }

        void start() override;
        void pause() override;
        void processAudio(AudioBuffer* audioData, int32_t numFrames) override;
    };

    class AudioPluginNode : public AudioGraphNode {
        RemotePluginInstance* plugin;

    public:
        AudioPluginNode(AudioGraph* ownerGraph, RemotePluginInstance* plugin) :
                AudioGraphNode(ownerGraph),
                plugin(plugin) {
        }
        virtual ~AudioPluginNode();

        void setPlugin(RemotePluginInstance* instance) { plugin = instance; }

        void start() override;
        void pause() override;
        bool shouldSkip() override;
        void processAudio(AudioBuffer* audioData, int32_t numFrames) override;

        // FIXME: this should be generalized to invoke arbitrary extension functions.
        void setPresetIndex(int index);
    };

    class AudioDataSourceNode : public AudioGraphNode {
        bool active{false};
        bool playing{false};
        std::unique_ptr<AudioBuffer> audio_data{nullptr};
        NanoSleepLock data_source_mutex{};

        int32_t current_frame_offset{0};

    public:
        explicit AudioDataSourceNode(AudioGraph* ownerGraph);
        virtual ~AudioDataSourceNode();

        void start() override;
        void pause() override;
        bool shouldSkip() override;
        virtual bool shouldConsumeButBypass() { return playing && !active; }
        void processAudio(AudioBuffer* audioData, int32_t numFrames) override;

        bool hasData() { return audio_data != nullptr && current_frame_offset < audio_data->audio.getNumFrames(); };

        void setPlaying(bool newPlayingState);


        /// returns the size in frames that were successfully read. 0 if it is empty.
        int32_t read(AudioBuffer* dst, int32_t numFrames);

        /// Sets PCM data of some formats, as indicated by the filename.
        /// Currently ogg, mp3 and wav formats work.
        /// Returns positive length of audio in frames after resampling if success, -1 otherwise
        ///
        /// It locks the data source until it finishes loading and converting data into `audio_data`.
        int32_t setAudioSource(uint8_t *data, int dataLength, const char *filename);
    };


    class MidiSourceNode : public AudioGraphNode {
        uint8_t* buffer;
        int32_t capacity;

        NanoSleepLock midi_buffer_mutex{};
        AAPMidiEventTranslator translator;

        // needed for sample accurate time calculation
        int32_t sample_rate{0};
        int32_t aap_frame_size{1024};
        struct timespec last_aap_process_time{0, 0};

    public:
        MidiSourceNode(AudioGraph* ownerGraph,
                       RemotePluginInstance* instance,
                       int32_t sampleRate,
                       int32_t audioNumFramesPerCallback,
                       int32_t initialMidiProtocol = CMIDI2_PROTOCOL_TYPE_MIDI2,
                       int32_t internalBufferSize = AAP_PLUGIN_PLAYER_DEFAULT_MIDI_RING_BUFFER_SIZE);

        virtual ~MidiSourceNode();

        void addMidiEvent(uint8_t *data, int32_t length, int64_t timestampInNanoseconds);

        void start() override;
        void pause() override;
        void processAudio(AudioBuffer* audioData, int32_t numFrames) override;
    };

    class MidiDestinationNode : public AudioGraphNode {
        uint8_t* buffer;

        NanoSleepLock midi_buffer_mutex{};

    public:
        MidiDestinationNode(AudioGraph* ownerGraph, int32_t internalBufferSize = AAP_PLUGIN_PLAYER_DEFAULT_MIDI_RING_BUFFER_SIZE);

        virtual ~MidiDestinationNode();

        void start() override;
        void pause() override;
        void processAudio(AudioBuffer* audioData, int32_t numFrames) override;
    };
}



#endif //AAP_CORE_AUDIOGRAPHNODE_H
