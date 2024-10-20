package io.github.leonidius20.recorder.ui.home

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.data.settings.AudioChannels
import io.github.leonidius20.recorder.data.settings.Codec
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.leonidius20.recorder.R

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @Inject
    lateinit var permissionManager: RecPermissionManager

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var qualityBottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.fragment = this

        qualityBottomSheetBehavior = BottomSheetBehavior.from(binding.qualitySettingsBottomSheet)
        qualityBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

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
                    values.map { it.toString() }
                )
                setSelected(values.indexOf(viewModel.currentSampleRate))

                setOnSelectionChangeListener { newIndex ->
                    val oldIndex = values.indexOf(viewModel.currentSampleRate)

                    if (oldIndex != newIndex) {
                        val newSampleRate = values[newIndex]

                        Toast.makeText(
                            requireContext(),
                            "selected $newSampleRate",
                            Toast.LENGTH_SHORT
                        ).show()

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
                    setValues(availableBitDepths.map { it.displayName })
                    setSelected(availableBitDepths.indexOf(viewModel.currentBitDepth))
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

        binding.bitRateSettingsBlock.isVisible = false // todo remove

        // val pager = root.findViewById<ViewPager2>(R.id.text_pager)
        //pager.adapter = Adapter(
        //    requireContext(), arrayOf("cock", "balls")
        //)
        val data = arrayOf("cock", "balls")
        binding.audioSettingsBitrateSlider.setValues(
            data
        )

        binding.audioSettingsBitrateSlider.setOnSelectionChangeListener { newPos ->
            // if (same as in settings) don't do anything
            //Toast.makeText(requireContext(), "Selected ${data[newPos]}", Toast.LENGTH_SHORT).show()
        }

        // todo: restoring the visualizer on screen rotation

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // todo bug - visualizer doesn't get updated on screen rotate
                viewModel.amplitudes.onEach { amplitude ->
                    binding.audioVisualizer.update(amplitude)
                }.launchIn(this)

            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            if (it is HomeViewModel.UiState.Idle) {
                binding.audioVisualizer.recreate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onStopBtnClick() {
        val recordingUri = viewModel.getUri()
        viewModel.onStopRecording()

        if (requireActivity().intent?.action == MediaStore.Audio.Media.RECORD_SOUND_ACTION) {
            // activity was launched with intent and we need to return the recording
            val replyIntent = Intent().apply {
                setData(recordingUri)
                clipData = ClipData.newRawUri("", recordingUri)
                setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            requireActivity().run {
                setResult(RESULT_OK, replyIntent)
                finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun toggleRecPause() {
        viewModel.onPauseOrResumeRecording()
    }

    private fun startRecording() {
        viewLifecycleOwner.lifecycleScope.launch {
            val permissionGranted = permissionManager
                .checkOrRequestRecordingPermission(this@HomeFragment)

            if (!permissionGranted) {
                Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.onStartRecording()
            }


        }
    }

    fun onRecButtonClick() {
        if (viewModel.uiState.value is HomeViewModel.UiState.Idle) {
            startRecording()
        } else {
            toggleRecPause()
        }
    }

    fun onAudioSettingsBtnClick() {
        with(qualityBottomSheetBehavior) {
            state = if (state == BottomSheetBehavior.STATE_HIDDEN) {
                BottomSheetBehavior.STATE_EXPANDED
            } else BottomSheetBehavior.STATE_HIDDEN
        }
    }

}

