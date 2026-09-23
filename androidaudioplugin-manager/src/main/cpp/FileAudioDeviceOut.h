//
// Created by leoni on 18.05.2025.
//

#ifndef AAP_CORE_FILEAUDIODEVICEOUT_H
#define AAP_CORE_FILEAUDIODEVICEOUT_H

#include "AudioDevice.h"
#include "fdstream.h"
#include <audio/choc_SampleBuffers.h>
#include <audio/choc_AudioFileFormat_WAV.h>
// #include <android/log.h>

#define LOG_TAG "FileAudioDeviceOut"

namespace aap {

    class FileAudioDeviceOut : public AudioDeviceOut {

    private:
        uint32_t sampleRate;
        int32_t numChannels;

        int32_t numFramesProcessed = 0;
        bool isRunning = false;

    protected:
        void* callback_context;
        AudioDeviceCallback *aap_callback;
        int fileDescriptor;
        std::unique_ptr<choc::audio::AudioFileWriter> fileWriter;
        AudioBuffer aap_buffer;
        uint32_t framesPerCallback;
        int32_t targetNumOfFrames;

    public:


        FileAudioDeviceOut(uint32_t sampleRate, uint32_t framesPerCallback, int32_t numChannels, int outputFileDescriptor)
            : sampleRate(sampleRate), numChannels(numChannels), fileDescriptor(outputFileDescriptor), aap_buffer(numChannels, (int32_t) framesPerCallback), framesPerCallback(framesPerCallback) {

        }

        ~FileAudioDeviceOut() {
            aap_callback = nullptr;
        }

        void setTargetNumFrames(int32_t targetNumFrames) {
            targetNumOfFrames = targetNumFrames;

            if (fileDescriptor == -1) {
                this->fileWriter = std::unique_ptr<choc::audio::AudioFileWriter>(nullptr);
                // __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Invalid out file descriptor (-1)");
            } else {
                auto stream = createOstreamFromFd(fileDescriptor);
                choc::audio::WAVAudioFileFormat<true> formatWav{};
                choc::audio::AudioFileProperties props {
                        "wav",
                        static_cast<double>(sampleRate),
                        static_cast<uint64_t>(targetNumFrames),
                        static_cast<uint32_t>(numChannels),
                };


                this->fileWriter = formatWav.createWriter(stream, props);
            }
        }

        void setAudioCallback(
                aap::AudioDeviceCallback *audioDeviceCallback,
                void *callbackContext) override {
            aap_callback = audioDeviceCallback;
            callback_context = callbackContext;
        }

        void startCallback() override {
            isRunning = true;
            startLoop();
        }

        void stopCallback() override {
            isRunning = false;
            // moved flushing to flushFile();
        }

        void write(aap::AudioBuffer *audioDataToWrite, int32_t bufferPosition, int32_t numFrames) override {
            writeOut(audioDataToWrite, bufferPosition, numFrames);
        }

        void writeOut(aap::AudioBuffer *audioDataToWrite, int32_t bufferPosition, int32_t numFrames) {
            // write data to file and buffer (idk why we would need it in the buffer tbh)
            //choc::buffer::FrameRange range{0, (uint32_t) numFrames};
            //choc::buffer::copy(aap_buffer.audio.getFrameRange(range), audioDataToWrite->audio.getView().getFrameRange(range));
            if (fileWriter) {
                fileWriter->appendFrames(audioDataToWrite->audio.getStart(numFrames));
            }
            numFramesProcessed += numFrames;

            // clear the buffers to prepare for new request
            //aap_buffer.audio.clear();
            //memset(aap_buffer.midi_in, 0, aap_buffer.midi_capacity);
            //memset(aap_buffer.midi_out, 0, aap_buffer.midi_capacity);
            // memset(oboeAudioData, 0, numFrames * sizeof(float));
        }

    private:

        // request audio from the framework
        void startLoop() {
            while(numFramesProcessed < targetNumOfFrames && isRunning) {
                // kick callback to generate some data
                aap_callback(callback_context, &aap_buffer, framesPerCallback);

                // write out
                // todo: i suppose we can just remove this call from here altogether
                // writeOut(&aap_buffer, 0, framesPerCallback);

                // numFramesProcessed += framesPerCallback;
            }
            // maybe stop after that??
            flushFile();
        }

        void flushFile() {
            if (fileWriter) {
                fileWriter->flush();
            }
        }

    };

} // aap

#endif //AAP_CORE_FILEAUDIODEVICEOUT_H
