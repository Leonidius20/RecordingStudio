#ifndef AAP_CORE_AAPMIDIEVENTTRANSLATOR_H
#define AAP_CORE_AAPMIDIEVENTTRANSLATOR_H

#include <aap/core/host/plugin-instance.h>
#ifndef CMIDI2_H_INCLUDED // it is only a workaround to avoid reference resolution failure at aap-juce-* repos.
#include <cmidi2.h>
#endif
#include "LocalDefinitions.h"

namespace aap {

    class AAPMidiEventTranslator {
        RemotePluginInstance* instance;
        // used when we need MIDI1<->UMP translation.
        uint8_t* translation_buffer{nullptr};
        int32_t midi_buffer_size;

        uint8_t* conversion_helper_buffer{nullptr};
        int32_t conversion_helper_buffer_size;

        // MIDI protocol type of the messages it receives via JNI
        int32_t receiver_midi_protocol;
        int32_t current_mapping_policy{AAP_PARAMETERS_MAPPING_POLICY_NONE};

        int32_t detectEndpointConfigurationMessage(uint8_t* bytes, size_t offset, size_t length);

    public:
        AAPMidiEventTranslator(RemotePluginInstance* instance, int32_t midiBufferSize = AAP_MANAGER_MIDI_BUFFER_SIZE, int32_t initialMidiProtocol = CMIDI2_PROTOCOL_TYPE_MIDI2);
        ~AAPMidiEventTranslator();

        int32_t translateMidiEvent(uint8_t *data, int32_t length);

        // If needed, translate MIDI1 bytestream to MIDI2 UMP.
        // returns 0 if translation did not happen. Otherwise return the size of translated buffer in translation_buffer.
        //
        // Note that it will update `bytes` contents and implicitly requires enough space for conversion.
        size_t translateMidiBufferIfNeeded(uint8_t* bytes, size_t offset, size_t length);

        uint8_t* getTranslationBuffer() { return translation_buffer; }

        // If needed, process MIDI mapping for parameters (CC/ACC/PNACC to sysex8) and presets (program).
        // returns 0 if translation did not happen. Otherwise return the size of translated buffer in translation_buffer.
        size_t runThroughMidi2UmpForMidiMapping(uint8_t* bytes, size_t offset, size_t length);
    };
}

#endif //AAP_CORE_AAPMIDIEVENTTRANSLATOR_H
