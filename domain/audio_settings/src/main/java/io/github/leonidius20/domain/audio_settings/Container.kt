package io.github.leonidius20.domain.audio_settings

import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container

/**
 * we consider the container to be supported if the API level of the device is high
 * enough (isSupportedByDevice) and if there is at least one CodecId that can be put
 * in this container that is supported by the device.
 */
fun Container.isSupported(capabilities: DeviceAudioCapabilities): Boolean
     = availableCodecs(capabilities).isNotEmpty()

/**
 * default CodecId for this container
 */
fun Container.defaultCodec(capabilities: DeviceAudioCapabilities): Codec<*>
    = capabilities.codecById[supportedCodecIds.first { id ->
        // exists in the list of codecs available on device
        capabilities.codecs.any { it.id == id }
    }]!!

/**
 * CodecIds that can be put into this container and that are supported by device
 */
fun Container.availableCodecs(capabilities: DeviceAudioCapabilities): List<Codec<*>>
    = supportedCodecIds.mapNotNull { capabilities.codecById[it] }

fun Container.supports(codec: Codec<*>, capabilities: DeviceAudioCapabilities) = availableCodecs(capabilities).contains(codec)

fun Container.Companion.getByValue(value: Int, capabilities: DeviceAudioCapabilities) =
    capabilities.containersMap[value]!!

fun Container.Companion.supportedContainers(
    capabilities: DeviceAudioCapabilities
) = capabilities.containers.filter { it.isSupported(capabilities) }
