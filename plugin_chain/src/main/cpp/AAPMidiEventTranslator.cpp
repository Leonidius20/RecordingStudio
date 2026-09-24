#include "aap/core/aap_midi2_helper.h"
#include "AAPMidiEventTranslator.h"

aap::AAPMidiEventTranslator::AAPMidiEventTranslator(RemotePluginInstance* instance, int32_t midiBufferSize, int32_t initialMidiProtocol) :
        instance(instance),
        midi_buffer_size(midiBufferSize),
        conversion_helper_buffer_size(AAP_MAX_EXTENSION_URI_SIZE + AAP_MAX_EXTENSION_DATA_SIZE + 16),
        receiver_midi_protocol(initialMidiProtocol) {
    translation_buffer = (uint8_t*) calloc(1, midi_buffer_size);
    conversion_helper_buffer = (uint8_t*) calloc(1, conversion_helper_buffer_size);
}

aap::AAPMidiEventTranslator::~AAPMidiEventTranslator() {
    if (translation_buffer)
        free(translation_buffer);
    if (conversion_helper_buffer)
        free(conversion_helper_buffer);
}

int32_t aap::AAPMidiEventTranslator::translateMidiEvent(uint8_t * bytes, int32_t length) {
    size_t translated = translateMidiBufferIfNeeded(bytes, 0, length);
    if (translated > 0)
        length = translated;
    translated = runThroughMidi2UmpForMidiMapping(bytes, 0, length);
    if (translated > 0)
        length = translated;
    return length;
}

size_t aap::AAPMidiEventTranslator::runThroughMidi2UmpForMidiMapping(uint8_t* bytes, size_t offset, size_t length) {
    int32_t translatedIndex = 0;
    CMIDI2_UMP_SEQUENCE_FOREACH(bytes + offset, length, iter) {
        auto ump = (cmidi2_ump*) iter;
        int32_t parameterIndex = -1;
        uint32_t parameterValueI32 = 0;
        int32_t parameterKey = 0;
        int32_t parameterExtra = 0;
        int32_t presetIndex = -1;
        switch (cmidi2_ump_get_message_type(ump)) {
            case CMIDI2_MESSAGE_TYPE_MIDI_2_CHANNEL:
                switch (cmidi2_ump_get_status_code(ump)) {
                    case CMIDI2_STATUS_CC:
                        if ((current_mapping_policy & AAP_PARAMETERS_MAPPING_POLICY_CC) != 0) {
                            parameterIndex = cmidi2_ump_get_midi2_cc_index(ump);
                            parameterValueI32 = cmidi2_ump_get_midi2_cc_data(ump);
                        }
                        break;
                    case CMIDI2_STATUS_PER_NOTE_ACC:
                        if ((current_mapping_policy & AAP_PARAMETERS_MAPPING_POLICY_ACC) != 0 &&
                            (current_mapping_policy & AAP_PARAMETERS_MAPPING_POLICY_SYSEX8) == 0)
                            parameterKey = cmidi2_ump_get_midi2_pnacc_note(ump);
                        // no break; go to case CMIDI2_STATUS_NRPN
                    case CMIDI2_STATUS_NRPN:
                        if ((current_mapping_policy & AAP_PARAMETERS_MAPPING_POLICY_ACC) != 0 &&
                            (current_mapping_policy & AAP_PARAMETERS_MAPPING_POLICY_SYSEX8) == 0) {
                            parameterIndex = cmidi2_ump_get_midi2_nrpn_msb(ump) * 0x80 +
                                             cmidi2_ump_get_midi2_nrpn_lsb(ump);
                            parameterValueI32 = cmidi2_ump_get_midi2_nrpn_data(ump);
                        }
                        break;
                    case CMIDI2_STATUS_PROGRAM:
                        // unless the plugin requires it to be passed directly, treat them as preset setter.
                        if ((current_mapping_policy & AAP_PARAMETERS_MAPPING_POLICY_PROGRAM) == 0) {
                            bool bankValid = (cmidi2_ump_get_midi2_program_options(ump) & CMIDI2_PROGRAM_CHANGE_OPTION_BANK_VALID) != 0;
                            auto bank = bankValid ?
                                        cmidi2_ump_get_midi2_program_bank_msb(ump) * 0x80 +
                                        cmidi2_ump_get_midi2_program_bank_lsb(ump) : 0;
                            presetIndex = cmidi2_ump_get_midi2_program_program(ump) + bank * 0x80;
                        }
                        break;
                }
                break;
        }
        if (presetIndex >= 0) {
            if (instance->getInstanceState() == aap::PluginInstantiationState::PLUGIN_INSTANTIATION_STATE_ACTIVE) {
                auto aapxsInstance = instance->getAAPXSDispatcher().getPluginAAPXSByUri(AAP_PRESETS_EXTENSION_URI);
                auto size = aap_midi2_generate_aapxs_sysex8(
                        (uint32_t *) (translation_buffer + translatedIndex),
                        (midi_buffer_size - translatedIndex) / 4,
                        conversion_helper_buffer,
                        conversion_helper_buffer_size,
                        0,
                        instance->aapxsRequestIdSerial(),
                        0, // FIXME: provide valid URID
                        AAP_PRESETS_EXTENSION_URI,
                        OPCODE_SET_PRESET_INDEX,
                        (const uint8_t *) aapxsInstance->serialization->data,
                        aapxsInstance->serialization->data_size);
                translatedIndex += size;
            }
            else
                // Trigger non-RT extension event at inactive state.
                instance->getStandardExtensions().setCurrentPresetIndex(presetIndex);
        }
        // If a translated AAP parameter change message is detected, then output sysex8.
        if (parameterIndex < 0) {
            auto size = cmidi2_ump_get_message_size_bytes(ump);
            memcpy(translation_buffer + translatedIndex, ump, size);
            translatedIndex += size;
        }
        else {
            auto intBuf = (uint32_t*) ((uint8_t*) translation_buffer + translatedIndex);
            aapMidi2ParameterSysex8(intBuf, intBuf + 1, intBuf + 2, intBuf + 3,
                                    cmidi2_ump_get_group(ump), cmidi2_ump_get_channel(ump), parameterKey, parameterExtra, parameterIndex, *(float*) (void*) &parameterValueI32);
            translatedIndex += 16;
        }
    }
    memcpy(bytes + offset, translation_buffer, translatedIndex);
    return translatedIndex;
}

int32_t aap::AAPMidiEventTranslator::detectEndpointConfigurationMessage(uint8_t* bytes, size_t offset, size_t length) {
    // treat bytes as UMP native endian stream
    if (length < 16)
        return 0;
    auto int1 = ((int32_t*) bytes)[0];
    if ((int1 & 0xF0050000) != 0xF0050000)
        return 0;
    // all those reserved bytes must be 0 (which would effectively eliminate possible conflicts with MIDI1 bytes)
    for (int i = 4; i < 16; i++)
        if (bytes[offset + i] != 0)
            return 0;
    return (int1 >> 8) & 0x3; // 1 or 2. All other values are reserved.
}

size_t aap::AAPMidiEventTranslator::translateMidiBufferIfNeeded(uint8_t* bytes, size_t offset, size_t length) {
    if (length == 0)
        return 0;
    // FIXME: We will use this hacky MIDI 2.0 Endpoint switcher implementation
    //  until proper MIDI-CI implementation lands in Android MIDI API:
    //  https://issuetracker.google.com/issues/227690391
    auto protocol = detectEndpointConfigurationMessage(bytes, offset, length);
    if (protocol != 0) {
        receiver_midi_protocol = protocol;
        // Do not process the rest, it should contain only the Stream Configuration message
        //  (Not an official standard requirement, but the legacy Set New Protocol message was
        //  defined that there must be some rational wait time until the next messages.)
        return 0;
    }

    if (receiver_midi_protocol != CMIDI2_PROTOCOL_TYPE_MIDI2) {
        // It receives MIDI1 bytestream. We translate to MIDI2 UMPs.
        cmidi2_midi_conversion_context context;
        cmidi2_midi_conversion_context_initialize(&context);
        context.midi1 = bytes + offset;
        context.midi_protocol = CMIDI2_PROTOCOL_TYPE_MIDI2;
        context.midi1_num_bytes = length;
        context.ump = (cmidi2_ump*) translation_buffer;
        context.ump_num_bytes = sizeof(translation_buffer);
        context.group = 0;

        if (cmidi2_convert_midi1_to_ump(&context) != CMIDI2_CONVERSION_RESULT_OK) {
            aap::a_log_f(AAP_LOG_LEVEL_ERROR, AAP_MANAGER_LOG_TAG, "Failed to translate MIDI 1.0 inputs to MIDI 2.0 UMPs");
            return 0;
        }

        // We do not copy result into bytes in this implementation, as `bytes` does not have sufficient space.
        //memcpy(bytes + offset, translation_buffer, context.ump_proceeded_bytes);
        return context.ump_proceeded_bytes;
    }
    return 0;
}
