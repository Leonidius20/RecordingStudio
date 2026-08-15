package io.github.leonidius20.recorder.ui.audio_settings.view_impl

import androidx.core.view.isVisible
import com.arkivanov.mvikotlin.core.utils.diff
import com.arkivanov.mvikotlin.core.view.BaseMviView
import com.arkivanov.mvikotlin.core.view.ViewRenderer
import com.google.android.material.slider.Slider
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.settings.AudioChannels
import io.github.leonidius20.recorder.data.settings.BitRateSettingType
import io.github.leonidius20.recorder.data.settings.Codec
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.databinding.BottomSheetAudioSettingsBinding
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.Intent
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.State
import io.github.leonidius20.recorder.ui.audio_settings.view.AudioSettingsView
import java.text.DecimalFormat

class AudioSettingsViewImpl(
    val binding: BottomSheetAudioSettingsBinding,
    val fragment: AudioSettingsBottomSheet,
) : BaseMviView<State, Intent>(), AudioSettingsView {

    private val context get() = binding.root.context

    private val audioSourcesAdapter = ChipSettingsAdapter<Settings.AudioSourceOption> { source ->
        dispatch(Intent.SetAudioSource(source))
    }

    private val containersAdapter = ChipSettingsAdapter<Container> { container ->
        dispatch(Intent.SetContainerFormat(container))
    }

    private val codecsAdapter = ChipSettingsAdapter<Codec> { codec ->
        dispatch(Intent.SetCodec(codec))
    }

    private val channelsAdapter = ChipSettingsAdapter<AudioChannels> { channels ->
        dispatch(Intent.SetChannels(channels))
    }

    init {
        // audio source selection chips
        binding.audioSourcesList.adapter = audioSourcesAdapter

        // format selection chips
        binding.outputFormatList.adapter = containersAdapter

        // codec selection chips
        binding.codecsList.adapter = codecsAdapter

        // audio channels (mono/stereo) chips
        binding.channelOptionsList.adapter = channelsAdapter

        // bit rate continuous
        // todo - remove, use predefined values?
        binding.audioSettingsBitrateContinuousSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: Slider) {
                dispatch(Intent.SetBitRate(slider.value))
            }
        })

        val df = DecimalFormat("#.##")
        binding.audioSettingsBitrateContinuousSlider.addOnChangeListener { _, value, _ ->
            binding.currentBitrateSliderValue.text =
                context.getString(R.string.value_in_kbps, df.format(value))
        }
    }

    override val renderer: ViewRenderer<State> = diff {
        diff(get = { (it as? State.Ready)?.audioSources }, set = {
            audioSourcesAdapter.submitList(it)
        })

        diff(get = { (it as? State.Ready)?.audioSource }, set = {
            binding.audioSourceDescriptionText.text = it?.description
        })

        diff(get = { (it as? State.Ready)?.containers }, set = {
            containersAdapter.submitList(it)
        })

        diff(get = { (it as? State.Ready)?.codecs }, set = {
            codecsAdapter.submitList(it)
        })

        diff(get = { (it as? State.Ready)?.channelOptions }, set = {
            channelsAdapter.submitList(it)
        })

        diff(get = { (it as? State.Ready)?.sampleRates }, set = { values ->
            if (values == null) return@diff

            // todo: should we remove this search here?
            // todo: maybe set adapter in slider??
            val selectedIndex = values.indexOfFirst { it.isSelected }

            binding.audioSettingsSampleRateSlider.apply {
                setValues(
                    list = values.map { it.toString() },
                    selectedIndex = selectedIndex
                )

                setOnSelectionChangeListener { newIndex ->
                    if (selectedIndex != newIndex) {
                        val newSampleRate = values[newIndex]
                        dispatch(Intent.SetSampleRate(newSampleRate.rate))
                    }
                }
            }

        })

        diff(get = { (it as? State.Ready)?.bitDepths }, set = { availableBitDepths ->
            if (availableBitDepths.isNullOrEmpty()) {
                binding.bitDepthSettingsBlock.isVisible = false
            } else {
                binding.bitDepthSettingsBlock.isVisible = true

                // todo: refactor to avoid this search?
                val currentIndex = availableBitDepths.indexOfFirst { it.isSelected }

                binding.audioSettingsBitDepthSlider.apply {
                    setValues(
                        list = availableBitDepths.map { it.depth.displayName },
                        selectedIndex = currentIndex,
                    )

                    setOnSelectionChangeListener { newIndex ->
                        if (newIndex != currentIndex) {
                            val newBitDepth = availableBitDepths[newIndex]
                            dispatch(Intent.SetBitDepth(newBitDepth.depth))
                        }
                    }
                }
            }
        })

        diff(get = { (it as? State.Ready)?.bitRateSettings }, set = { bitRateSettings ->
            binding.bitRateSettingsBlock.isVisible = bitRateSettings != null

            if (bitRateSettings == null) return@diff

            val bitRateSettingType = bitRateSettings.type

            binding.audioSettingsBitrateDiscreteSelector.isVisible =
                bitRateSettingType is BitRateSettingType.BitRateDiscreteValues

            binding.audioSettingsBitrateContinuousSlider.isVisible =
                bitRateSettingType is BitRateSettingType.BitRateContinuousRange
            binding.bitrateSliderLabels.isVisible =
                bitRateSettingType is BitRateSettingType.BitRateContinuousRange

            when (bitRateSettingType) {
                is BitRateSettingType.BitRateDiscreteValues -> {
                    val availableRates = bitRateSettingType.bitRateOptions

                    if (availableRates.isEmpty()) {
                        binding.bitRateSettingsBlock.isVisible = false
                        return@diff
                    }

                    binding.audioSettingsBitrateDiscreteSelector.apply {
                        val currentIndex = availableRates.indexOf(bitRateSettings.current)

                        // todo: also refactor
                        setValues(
                            list = availableRates.map { "$it kbps" },
                            selectedIndex = currentIndex
                        )

                        // todo: this logic has to be part of Selector class,
                        //  as it's used every time
                        setOnSelectionChangeListener { newIndex ->
                            if (newIndex != currentIndex) {
                                val newBitRate = availableRates[newIndex]
                                dispatch(Intent.SetBitRate(newBitRate))
                            }
                        }
                    }
                }

                is BitRateSettingType.BitRateContinuousRange -> {
                    binding.audioSettingsBitrateContinuousSlider.apply {
                        valueFrom = bitRateSettingType.min
                        valueTo = bitRateSettingType.max
                        value = bitRateSettings.current
                    }

                    binding.maxBitrateSliderValue.text =
                        String.format("%.0f", bitRateSettingType.max)
                    binding.minBitrateSliderValue.text =
                        String.format("%.0f", bitRateSettingType.min)
                }
            }
        }
        )
    }

    override fun handleLabel(label: AudioSettingsStore.Label) {

    }

}
