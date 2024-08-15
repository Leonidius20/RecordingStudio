package io.github.leonidius20.recorder.ui.home

import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

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

        val audioSources = mutableListOf(
            Triple(MediaRecorder.AudioSource.DEFAULT, "Default", "explanation"),
            Triple(MediaRecorder.AudioSource.MIC, "Mic", "explanation"),
            Triple(MediaRecorder.AudioSource.CAMCORDER, "Camcorder", "explanation"),
            Triple(MediaRecorder.AudioSource.VOICE_RECOGNITION, "Voice recognition", "Tuned for voice recognition"),
            Triple(MediaRecorder.AudioSource.VOICE_COMMUNICATION, "Voice communication", "Tuned for VoIP and the like. Applies processing like echo cancellation or gain control (determined by device manufacturer)"),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioSources.add(Triple(MediaRecorder.AudioSource.UNPROCESSED, "Unprocessed", "No processing if the phone supports it, default otherwise"))
        }

        audioSources.forEach { source ->
            val chip = Chip(context).apply {
                isSelected = (source.first == viewModel.settings.state.value.audioSource)
                text = source.second
                // todo: set on choose listener
            }

            binding.audioSourceChipGroup.addView(chip)
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
        viewModel.onStopRecording()

        if (requireActivity().intent?.action == MediaStore.Audio.Media.RECORD_SOUND_ACTION) {
            // activity was lauched with intent and we need to return the recording
            val replyIntent = Intent().apply { setData(viewModel.getUri()) }

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
        with (qualityBottomSheetBehavior) {
            state = if (state == BottomSheetBehavior.STATE_HIDDEN) {
                BottomSheetBehavior.STATE_EXPANDED
            } else BottomSheetBehavior.STATE_HIDDEN
        }
    }

    fun onSelectAudioSource(source: Int) {
        // todo
    }

}

