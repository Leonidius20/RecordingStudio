package io.github.leonidius20.recorder.ui.audio_settings.view_impl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.arkivanov.essenty.instancekeeper.instanceKeeper
import com.arkivanov.essenty.lifecycle.essentyLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.settings.AudioChannels
import io.github.leonidius20.recorder.data.settings.BitRateSettingType
import io.github.leonidius20.recorder.data.settings.Codec
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.databinding.BottomSheetAudioSettingsBinding
import io.github.leonidius20.recorder.doOnApplyWindowInsets
import io.github.leonidius20.recorder.ui.audio_settings.view.AudioSettingsController
import io.github.leonidius20.recorder.ui.home.HomeViewModel
import java.text.DecimalFormat
import javax.inject.Inject

@AndroidEntryPoint
class AudioSettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAudioSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var controller: AudioSettingsController

    @Inject
    lateinit var controllerFactory: AudioSettingsController.Factory

    override fun getTheme() = R.style.ThemeOverlay_App_BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAudioSettingsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        controller = controllerFactory.create(
            instanceKeeper = instanceKeeper(),
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller.onViewCreated(AudioSettingsViewImpl(binding, this),
            viewLifecycleOwner.essentyLifecycle())

        binding.scrollableLayout.doOnApplyWindowInsets { view, windowInsets, initialPadding ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                initialPadding.bottom + insets.bottom
            )

            windowInsets
        }

        // audio source selection chips
        viewModel.audioSources.forEach { source ->
            val chipViewId = View.generateViewId()

            val chip = Chip(context).apply {
                isCheckedIconVisible = true
                isCheckable = true
                isClickable = true
                text = source.name
                id = chipViewId
                tag = source
                isChecked = viewModel.isChecked(source)
            }

            binding.audioSourceChipGroup.addView(chip)

            if (chip.isChecked) {
                binding.audioSourceDescriptionText.text = source.description
            }
        }

        binding.audioSourceChipGroup.setOnCheckedStateChangeListener { group, _ ->
            val selectedSource =
                group.findViewById<Chip>(group.checkedChipId).tag as Settings.AudioSourceOption
            viewModel.selectAudioSource(selectedSource.value)
            binding.audioSourceDescriptionText.text = selectedSource.description
        }

        // format selection chips
        viewModel.outputFormats.forEach { format ->
            val chipViewId = View.generateViewId()

            val chip = Chip(context).apply {
                isCheckedIconVisible = true
                isCheckable = true
                isClickable = true
                text = format.displayName
                id = chipViewId
                tag = format
                isChecked = viewModel.isChecked(format)
            }

            binding.outputFormatChipGroup.addView(chip)
        }

        binding.outputFormatChipGroup.setOnCheckedStateChangeListener { group, _ ->
            val selectedFormat = group.findViewById<Chip>(group.checkedChipId).tag as Container
            viewModel.selectOutputFormat(selectedFormat)
        }

        // codec selection chips
        viewModel.encoderOptions.observe(viewLifecycleOwner) { codecs ->
            binding.codecChipGroup.removeAllViews()

            codecs.forEach { codec ->
                val chipViewId = View.generateViewId()

                val chip = Chip(context).apply {
                    isCheckedIconVisible = true
                    isCheckable = true
                    isClickable = true
                    text = codec.displayName
                    id = chipViewId
                    tag = codec
                    isChecked = viewModel.isEncoderChecked(codec)
                }

                binding.codecChipGroup.addView(chip)
            }
        }

        // codec selection chips
        binding.codecChipGroup.setOnCheckedStateChangeListener { group, _ ->
            val selectedCodec = group.findViewById<Chip>(group.checkedChipId).tag as Codec
            viewModel.setEncoder(selectedCodec)
        }

        // audio channels (mono/stereo) chips
        viewModel.audioChannelsOptions.forEach { option ->
            val chipViewId = View.generateViewId()

            val chip = Chip(context).apply {
                isCheckedIconVisible = true
                isCheckable = true
                isClickable = true
                text = getString(option.title)
                id = chipViewId
                tag = option
                isChecked = viewModel.isChannelsOptionsChecked(option)
            }

            binding.channelsChipGroup.addView(chip)
        }

        binding.channelsChipGroup.setOnCheckedStateChangeListener { group, _ ->
            val selectedChannels =
                group.findViewById<Chip>(group.checkedChipId).tag as AudioChannels
            viewModel.setChannels(selectedChannels)
        }

        binding.audioSettingsSampleRateSlider.apply {

            viewModel.supportedSampleRates.observe(viewLifecycleOwner) { values ->
                setValues(
                    list = values.map { it.toString() },
                    selectedIndex = values.indexOf(viewModel.currentSampleRate)
                )
                // setSelected(values.indexOf(viewModel.currentSampleRate))

                setOnSelectionChangeListener { newIndex ->
                    val oldIndex = values.indexOf(viewModel.currentSampleRate)

                    if (oldIndex != newIndex) {
                        val newSampleRate = values[newIndex]

                        viewModel.setSampleRate(newSampleRate)
                    }


                }
            }
        }

        // bit depth selection
        viewModel.availableBitDepths.observe(viewLifecycleOwner) { availableBitDepths ->
            if (availableBitDepths == null || availableBitDepths.isEmpty()) {
                binding.bitDepthSettingsBlock.isVisible = false
            } else {
                binding.bitDepthSettingsBlock.isVisible = true

                binding.audioSettingsBitDepthSlider.apply {
                    setValues(
                        list = availableBitDepths.map { it.displayName },
                        selectedIndex = availableBitDepths.indexOf(viewModel.currentBitDepth)
                    )
                    // setSelected(availableBitDepths.indexOf(viewModel.currentBitDepth))
                    setOnSelectionChangeListener { newIndex ->
                        val prevIndex = availableBitDepths.indexOf(viewModel.currentBitDepth)
                        if (newIndex != prevIndex) {
                            val newBitDepth = availableBitDepths[newIndex]

                            viewModel.setBitDepth(newBitDepth)
                        }
                    }
                }
            }
        }

        binding.audioSettingsBitrateContinuousSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.setBitRate(slider.value)
            }
        })

        val df = DecimalFormat("#.##")

        binding.audioSettingsBitrateContinuousSlider.addOnChangeListener { slider, value, fromUser ->
            binding.currentBitrateSliderValue.text =
                getString(R.string.value_in_kbps, df.format(value))
        }

        // bit rate selection
        viewModel.availableBitRates.observe(viewLifecycleOwner) { bitRateSettingOption ->
            binding.bitRateSettingsBlock.isVisible = bitRateSettingOption != null

            binding.audioSettingsBitrateDiscreteSelector.isVisible =
                bitRateSettingOption is BitRateSettingType.BitRateDiscreteValues

            binding.audioSettingsBitrateContinuousSlider.isVisible =
                bitRateSettingOption is BitRateSettingType.BitRateContinuousRange
            binding.bitrateSliderLabels.isVisible =
                bitRateSettingOption is BitRateSettingType.BitRateContinuousRange

            when(bitRateSettingOption) {
                is BitRateSettingType.BitRateDiscreteValues -> {
                    val availableRates = bitRateSettingOption.bitRateOptions

                    if (availableRates.isEmpty()) {
                        binding.bitRateSettingsBlock.isVisible = false
                        return@observe
                    }

                    binding.audioSettingsBitrateDiscreteSelector.apply {
                        setValues(
                            list = availableRates.map { "$it kbps" },
                            selectedIndex = availableRates.indexOf(viewModel.currentBitRate)
                        )

                        setOnSelectionChangeListener { newIndex ->
                            val prevIndex = availableRates.indexOf(viewModel.currentBitRate)
                            if (newIndex != prevIndex) {
                                val newBitRate = availableRates[newIndex]
                                viewModel.setBitRate(newBitRate)
                            }
                        }
                    }
                }
                is BitRateSettingType.BitRateContinuousRange -> {
                    binding.audioSettingsBitrateContinuousSlider.apply {
                        valueFrom = bitRateSettingOption.min
                        valueTo = bitRateSettingOption.max
                        value = viewModel.currentBitRate!!
                    }

                    binding.maxBitrateSliderValue.text = String.format("%.0f", bitRateSettingOption.max)
                    binding.minBitrateSliderValue.text = String.format("%.0f", bitRateSettingOption.min)
                }
            }
        }
    }

    companion object {
        const val TAG = "AudioSettingsBottomSheet"
    }

}