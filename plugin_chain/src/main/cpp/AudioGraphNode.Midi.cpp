#include "AudioGraph.h"

aap::MidiSourceNode::MidiSourceNode(AudioGraph* ownerGraph,
                                    RemotePluginInstance* instance,
                                    int32_t sampleRate,
                                    int32_t audioNumFramesPerCallback,
                                    int32_t initialMidiProtocol,
                                    int32_t internalBufferSize) :
        AudioGraphNode(ownerGraph),
        capacity(internalBufferSize),
        translator(instance, internalBufferSize, initialMidiProtocol),
        sample_rate(sampleRate),
        aap_frame_size(audioNumFramesPerCallback) {
    buffer = (uint8_t*) calloc(1, internalBufferSize);
}

aap::MidiSourceNode::~MidiSourceNode() {
    if (buffer)
        free(buffer);
}

void aap::MidiSourceNode::processAudio(AudioBuffer *audioData, int32_t numFrames) {
    auto dstBuffer = (AAPMidiBufferHeader*) audioData->midi_in;
    // MIDI event might be being added, so we wait a bit using "almost-spin" lock (uses nano-sleep).
    if (std::unique_lock<NanoSleepLock> tryLock(midi_buffer_mutex, std::try_to_lock); tryLock.owns_lock()) {
        auto srcBuffer = (AAPMidiBufferHeader*) buffer;
        if (srcBuffer->length)
            memcpy(dstBuffer, srcBuffer, sizeof(AAPMidiBufferHeader) + srcBuffer->length);
        srcBuffer->length = 0;
    } else {
        // failed to acquire lock; we do not send anything this time.
        dstBuffer->length = 0;
    }
}

void aap::MidiSourceNode::addMidiEvent(uint8_t *bytes, int32_t length, int64_t timestampInNanoseconds) {

    // This function is invoked every time Kotlin PluginPlayer.addMidiEvent() is invoked, immediately.
    // On the other hand, AAPs don't process MIDI events immediately (it is handled at `process()`),
    // so we have to buffer them and flush them to the instrument plugin every time process() is invoked.
    //
    // Since we don't really know when it is actually processed but have to accurately
    // pass delta time for each input events, we assign event timecode from
    // 1) argument timestampInNanoseconds and 2) actual time passed from previous call to
    // this function.
    int64_t actualTimestamp = timestampInNanoseconds;
    struct timespec curtime{};
    clock_gettime(CLOCK_REALTIME, &curtime);

    // it is 99.999... percent true since audio loop must have started before any MIDI events...
    if (last_aap_process_time.tv_sec > 0) {
        int64_t diff = (curtime.tv_sec - last_aap_process_time.tv_sec) * 1000000000 +
                       curtime.tv_nsec - last_aap_process_time.tv_nsec;
        auto nanosecondsPerCycle = (int64_t) (1.0 * aap_frame_size / sample_rate * 1000000000);
        actualTimestamp = (timestampInNanoseconds + diff) % nanosecondsPerCycle;
    }

    // apply translation at this step (every time event is added, non-RT processing, unlocked)
    size_t translatedLength = translator.translateMidiEvent(bytes, length);
    auto actualData = translatedLength > 0 ? translator.getTranslationBuffer() : bytes;
    auto actualLength = translatedLength > 0 ? translatedLength : length;

    // update MIDI input buffer with lock
    { // lock scope
        auto dst8 = (uint8_t *) buffer;
        auto dstMBH = (AAPMidiBufferHeader *) dst8;
        const std::lock_guard <NanoSleepLock> lock{midi_buffer_mutex};

        if (dst8 != nullptr) {
            uint32_t savedOffset = dstMBH->length;
            uint32_t currentOffset = savedOffset;
            int32_t tIter = 0;
            auto headerSize = sizeof(AAPMidiBufferHeader);
            for (int64_t ticks = actualTimestamp / (1000000000 / 31250);
                 ticks > 0; ticks -= 31250, tIter++) {
                *(int32_t * )(dst8 + headerSize + currentOffset + tIter * 4) =
                        (int32_t) cmidi2_ump_jr_timestamp_direct(0,
                                                                 ticks > 31250 ? 31250 : ticks);
            }
            currentOffset += tIter * 4;
            memcpy(dst8 + headerSize + currentOffset, actualData, actualLength);
            currentOffset += actualLength;

            if (savedOffset != dstMBH->length) {
                // updated
                memcpy(dst8 + headerSize, dst8 + headerSize + savedOffset, actualLength);
                dstMBH->length = currentOffset - savedOffset;
            } else
                dstMBH->length = currentOffset;
        }
    }
}


void aap::MidiSourceNode::start() {
}

void aap::MidiSourceNode::pause() {

}

//--------

aap::MidiDestinationNode::MidiDestinationNode(AudioGraph* ownerGraph, int32_t internalBufferSize) :
        AudioGraphNode(ownerGraph) {
    buffer = (uint8_t*) calloc(1, internalBufferSize);
}

aap::MidiDestinationNode::~MidiDestinationNode() {
    if (buffer)
        free(buffer);
}

void aap::MidiDestinationNode::processAudio(AudioBuffer *audioData, int32_t numFrames) {
    // MIDI event might be being consumed, so we wait a bit using "almost-spin" lock (uses nano-sleep).
    if (std::unique_lock<NanoSleepLock> tryLock(midi_buffer_mutex, std::try_to_lock); tryLock.owns_lock()) {
        auto srcBuffer = (AAPMidiBufferHeader*) audioData->midi_out;
        auto dstBuffer = (AAPMidiBufferHeader*) buffer;
        if (srcBuffer->length) {
            memcpy(dstBuffer + 1, srcBuffer + 1, srcBuffer->length);
            dstBuffer->length += srcBuffer->length;
        }
        srcBuffer->length = 0;
    } else {
        // failed to acquire lock; we skip copying and keep source length.
    }
}

void aap::MidiDestinationNode::start() {

}

void aap::MidiDestinationNode::pause() {

}

