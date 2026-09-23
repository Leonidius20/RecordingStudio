#include "AudioBuffer.h"
#include "aap/unstable/utility.h"

aap::AudioBuffer::AudioBuffer(int32_t numChannels, int32_t framesPerCallback, int32_t midiBufferSize) {
    audio = choc::buffer::createChannelArrayBuffer(numChannels,
                                                   framesPerCallback,
                                                   []() { return (float) 0; });
    if (midiBufferSize <= 0)
        AAP_ASSERT_FALSE; // note: should not reach here
    audio.clear();
    midi_capacity = midiBufferSize;
    midi_in = midiBufferSize > 0 ? calloc(1, midiBufferSize) : nullptr;
    midi_out = midiBufferSize > 0 ? calloc(1, midiBufferSize) : nullptr;
}

aap::AudioBuffer::~AudioBuffer() {
    if (midi_in)
        free(midi_in);
    if (midi_out)
        free(midi_out);
}

aap_buffer_t aap::AudioBuffer::asAAPBuffer() {
    aap_buffer_t ret{};
    ret.impl = this;
    ret.num_frames = aapBufferGetNumFrames;
    ret.get_buffer = aapBufferGetBuffer;
    ret.get_buffer_size = aapBufferGetBufferSize;
    ret.num_ports = aapBufferGetNumPorts;
    return ret;
}

int32_t aap::AudioBuffer::aapBufferGetNumFrames(aap_buffer_t &b) {
    return ((AudioBuffer*) b.impl)->audio.getNumFrames();
}

void *aap::AudioBuffer::aapBufferGetBuffer(aap_buffer_t &b, int32_t portIndex) {
    auto data = (AudioBuffer*) b.impl;
    if (portIndex < data->audio.getChannelRange().end)
        return data->audio.getChannel(portIndex).data.data;
    switch (portIndex - data->audio.getChannelRange().end) {
        case 0: return data->midi_in;
        case 1: return data->midi_out;
    }
    AAP_ASSERT_FALSE; // should not reach here
    return nullptr;
}

int32_t aap::AudioBuffer::aapBufferGetBufferSize(aap_buffer_t &b, int32_t portIndex) {
    auto data = (AudioBuffer*) b.impl;
    if (portIndex < data->audio.getChannelRange().end)
        return data->audio.getNumFrames() * sizeof(float);
    switch (portIndex - data->audio.getChannelRange().end) {
        case 0:
        case 1:
            return data->midi_capacity;
    }
    AAP_ASSERT_FALSE; // should not reach here
    return 0;
}

int32_t aap::AudioBuffer::aapBufferGetNumPorts(aap_buffer_t &b) {
    auto data = (AudioBuffer*) b.impl;
    return data->audio.getNumChannels() + 2;
}
