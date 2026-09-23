#include "AudioGraphNode.h"
#include "AudioGraph.h"
#include <audio/choc_AudioFileFormat_WAV.h>
#include <audio/choc_AudioFileFormat_MP3.h>
#include <audio/choc_AudioFileFormat_Ogg.h>
#include <audio/choc_AudioFileFormat_FLAC.h>
#include <audio/choc_SincInterpolator.h>


/*class SeekableByteBuffer : public std::streambuf {
public:
    SeekableByteBuffer(uint8_t* data, std::size_t size) {
        setg(reinterpret_cast<char*>(data), reinterpret_cast<char*>(data), reinterpret_cast<char*>(data) + size);
    }

    std::streampos seekoff(std::streamoff off, std::ios_base::seekdir dir, std::ios_base::openmode which) override {
        switch (dir) {
            case std::ios_base::beg: setg(eback(), eback() + off, egptr()); break;
            case std::ios_base::cur: gbump((int) off); break;
            case std::ios_base::end: setg(eback(), egptr() + off, egptr()); break;
        }
        return gptr() - eback();
    }
};*/

class SeekableByteBuffer : public std::streambuf {
public:
    SeekableByteBuffer(uint8_t* data, std::size_t size)
            : begin(reinterpret_cast<char*>(data)),
              end(reinterpret_cast<char*>(data) + size)
    {
        setg(begin, begin, end);
    }

protected:
    std::streampos seekoff(std::streamoff off, std::ios_base::seekdir dir,
                           std::ios_base::openmode which) override {
        char* new_gptr = nullptr;

        if (dir == std::ios_base::beg) {
            new_gptr = begin + off;
        } else if (dir == std::ios_base::cur) {
            new_gptr = gptr() + off;
        } else if (dir == std::ios_base::end) {
            new_gptr = end + off;
        }

        if (new_gptr < begin || new_gptr > end)
            return -1; // causes failbit

        setg(begin, new_gptr, end);
        return gptr() - begin;
    }

    std::streampos seekpos(std::streampos pos,
                           std::ios_base::openmode which) override {
        return seekoff(pos, std::ios_base::beg, which);
    }

private:
    char* begin;
    char* end;
};


aap::AudioDataSourceNode::AudioDataSourceNode(aap::AudioGraph *ownerGraph) :
        AudioGraphNode(ownerGraph),
        audio_data(std::make_unique<aap::AudioBuffer>(ownerGraph->getChannelsInAudioBus(), ownerGraph->getFramesPerCallback())) {
}

bool aap::AudioDataSourceNode::shouldSkip() {
    // We do not filter out active. If !active and playing, then it consumes the stream
    return !hasData() || !playing;
}

void aap::AudioDataSourceNode::processAudio(AudioBuffer *audioData, int32_t numFrames) {
    // It ignores errors or empty buffers
    read(audioData, numFrames);
}

void aap::AudioDataSourceNode::start() {
    active = true;
}

void aap::AudioDataSourceNode::pause() {
    active = false;
}

void aap::AudioDataSourceNode::setPlaying(bool newPlayingState) {
    // if it was already playing, reset current position.
    if (playing)
        current_frame_offset = 0;
    playing = newPlayingState;
}

int32_t aap::AudioDataSourceNode::read(AudioBuffer *dst, int32_t numFrames) {

    uint32_t size = std::min(audio_data->audio.getNumFrames() - current_frame_offset,
                             (uint32_t) numFrames);
    if (size <= 0)
        return 0;

    if (shouldConsumeButBypass()) {
        current_frame_offset += size;
        return size;
    }

    // read only if it is not locked.
    if (std::unique_lock<NanoSleepLock> tryLock(data_source_mutex, std::try_to_lock); tryLock.owns_lock()) {

        choc::buffer::FrameRange range{(uint32_t) current_frame_offset, current_frame_offset + size};
        choc::buffer::copyRemappingChannels(dst->audio.getStart(size),
                                            audio_data->audio.getFrameRange(range));
        current_frame_offset += size;
        return size;
    }
    else
        return 0;
}

choc::audio::WAVAudioFileFormat<false> formatWav{};
choc::audio::MP3AudioFileFormat formatMp3{};
choc::audio::OggAudioFileFormat<false> formatOgg{};
choc::audio::FLACAudioFileFormat<false> formatFlac{};
choc::audio::AudioFileFormat* formats[] {&formatWav, &formatMp3, &formatOgg, &formatFlac};

int32_t aap::AudioDataSourceNode::setAudioSource(uint8_t *data, int dataLength, const char *filename) {
    const std::lock_guard <NanoSleepLock> lock{data_source_mutex};

    for (auto format : formats) {
        if (format->filenameSuffixMatches(filename)) {
            SeekableByteBuffer buffer(data, dataLength);
            auto stream = std::make_shared<std::istream>(&buffer);

            // so we can replace this with file input?

            auto reader = format->createReader(stream);
            auto props = reader->getProperties();
            // AudioBuffer tmpData{(int32_t) props.numChannels, (int32_t) props.numFrames};
            audio_data = std::make_unique<AudioBuffer>((int32_t) props.numChannels, (int32_t) props.numFrames);
            if (!reader->readFrames(0, audio_data->audio)) {
                AAP_ASSERT_FALSE;
                return -1;
            }

            // todo: set processing chain to work in sample rate of file

            // resample
            //auto durationInSeconds = 1.0 * props.numFrames / props.sampleRate;
            //auto targetFrames = (int32_t) (durationInSeconds * graph->getSampleRate());
            //audio_data = std::make_unique<AudioBuffer>((int32_t) props.numChannels, targetFrames);
            //choc::interpolation::sincInterpolate(audio_data->audio, tmpData.audio);

            // return targetFrames;
            return (int32_t) props.numFrames;
        }
    }

    return false;
}

aap::AudioDataSourceNode::~AudioDataSourceNode() {
    playing = false;
    active = false;
}

