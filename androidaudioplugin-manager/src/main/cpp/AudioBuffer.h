#ifndef AAP_CORE_AUDIOBUFFER_H
#define AAP_CORE_AUDIOBUFFER_H

#include "LocalDefinitions.h"
#define AAP_MANAGER_AUDIO_QUEUE_NX_FRAMES 4
#include <audio/choc_SampleBuffers.h>
#include <containers/choc_SingleReaderSingleWriterFIFO.h>
#include <aap/android-audio-plugin.h>

namespace aap {

    class AudioBuffer {
        static int32_t aapBufferGetNumFrames(aap_buffer_t &);
        static void *aapBufferGetBuffer(aap_buffer_t &, int32_t);
        static int32_t aapBufferGetBufferSize(aap_buffer_t &, int32_t);
        static int32_t aapBufferGetNumPorts(aap_buffer_t &);

    public:
        choc::buffer::ChannelArrayBuffer<float> audio;
        void *midi_in;
        void *midi_out;
        int32_t midi_capacity;

        AudioBuffer(int32_t numChannels, int32_t framesPerCallback,
                    int32_t midiBufferSize = AAP_MANAGER_MIDI_BUFFER_SIZE);
        ~AudioBuffer();

        aap_buffer_t asAAPBuffer();
    };

}

#endif //AAP_CORE_AUDIOBUFFER_H
