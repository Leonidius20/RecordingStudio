package io.github.leonidius20.recorder.ui.home

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import io.github.leonidius20.recorder.ui.audio_settings.view_impl.AudioSettingsBottomSheet
import io.github.leonidius20.recorder.ui.common.RecStudioFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : RecStudioFragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @Inject
    lateinit var permissionManager: RecPermissionManager

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // todo: restoring the visualizer on screen rotation

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            stopButton.setOnClickListener { onStopBtnClick() }
            recordButton.setOnClickListener { onRecButtonClick() }
            audioSettingsButton.setOnClickListener { onAudioSettingsBtnClick() }

            // todo bug - visualizer doesn't get updated on screen rotate
            viewModel.amplitudes.collectSinceStarted { amplitude ->
                audioVisualizer.update(amplitude)
            }

            viewModel.uiState.observe(viewLifecycleOwner) {
                if (it is HomeViewModel.UiState.Idle) {
                    audioVisualizer.recreate()
                }

                recTimer.isVisible = it.isTimerVisible
                audioSettingsButton.isVisible = it.audioSettingsButtonVisible
                recordButton.isVisible = it.isRecPauseBtnVisible
                stopButton.isVisible = it.isStopButtonVisible

                recordButton.setIconResource(
                    when(it.recPauseBtnState) {
                        HomeViewModel.UiState.RecPauseBtnState.RECORD -> R.drawable.ic_record
                        HomeViewModel.UiState.RecPauseBtnState.PAUSE -> R.drawable.ic_pause
                    }
                )
                recordButton.contentDescription = getString(when(it.recPauseBtnState) {
                    HomeViewModel.UiState.RecPauseBtnState.RECORD -> R.string.btn_rec_desc
                    HomeViewModel.UiState.RecPauseBtnState.PAUSE -> R.string.btn_pause_desc
                })
            }

            viewModel.timerText.observe(viewLifecycleOwner) {
                recTimer.text = it
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
        val sheet = AudioSettingsBottomSheet()
        sheet.show(childFragmentManager, AudioSettingsBottomSheet.TAG)
    }

}
