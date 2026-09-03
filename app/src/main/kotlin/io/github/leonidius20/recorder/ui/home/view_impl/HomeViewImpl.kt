package io.github.leonidius20.recorder.ui.home.view_impl

import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.arkivanov.mvikotlin.core.utils.diff
import com.arkivanov.mvikotlin.core.view.BaseMviView
import com.arkivanov.mvikotlin.core.view.ViewRenderer
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import io.github.leonidius20.recorder.ui.audio_settings.view_impl.AudioSettingsBottomSheet
import io.github.leonidius20.recorder.ui.home.HomeViewModel
import io.github.leonidius20.recorder.ui.home.RecPermissionManager
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Intent
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Label
import io.github.leonidius20.recorder.ui.home.view.HomeView
import io.github.leonidius20.recorder.ui.home.view.HomeView.Model
import kotlinx.coroutines.launch

class HomeViewImpl(
    private val binding: FragmentHomeBinding,
    private val fragment: Fragment,
    private val permissionManager: RecPermissionManager,
) : BaseMviView<Model, Intent>(), HomeView {

    private val context get() = binding.root.context

    init {
        with(binding) {
            stopButton.setOnClickListener {
                dispatch(Intent.StopRecording)
            }
            recordButton.setOnClickListener {
                dispatch(Intent.ToggleRecPause)
            }
            audioSettingsButton.setOnClickListener {
                dispatch(Intent.OpenAudioSettings)
            }
        }
    }

    override val renderer: ViewRenderer<Model>
        get() = diff {
            diff(Model::isTimerVisible) {
                binding.recTimer.isVisible = it
            }

            diff(Model::audioSettingsButtonVisible) {
                binding.audioSettingsButton.isVisible = it
            }

            diff(Model::isRecPauseBtnVisible) {
                binding.recordButton.isVisible = it
            }

            diff(Model::isStopButtonVisible) {
                binding.stopButton.isVisible = it
            }

            diff(Model::recPauseBtnState) {
                binding.recordButton.apply {
                    setIconResource(
                        when(it) {
                            HomeViewModel.UiState.RecPauseBtnState.RECORD -> R.drawable.ic_record
                            HomeViewModel.UiState.RecPauseBtnState.PAUSE -> R.drawable.ic_pause
                        }
                    )
                    contentDescription = context.getString(when(it) {
                        HomeViewModel.UiState.RecPauseBtnState.RECORD -> R.string.btn_rec_desc
                        HomeViewModel.UiState.RecPauseBtnState.PAUSE -> R.string.btn_pause_desc
                    })
                }
            }
        }

    override fun handleLabel(label: Label) {
        when (label) {
            is Label.OpenAudioSettings -> {
                val sheet = AudioSettingsBottomSheet()
                sheet.show(fragment.childFragmentManager, AudioSettingsBottomSheet.TAG)
            }

            is Label.CheckRecordingPermissions -> {
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    val permissionGranted = permissionManager
                        .checkOrRequestRecordingPermission(fragment)

                    if (!permissionGranted) {
                        Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show()
                    } else {
                        dispatch(Intent.NotifyRecordingPermissionsConfirmed)
                    }
                }
            }

            is Label.NotifyRecordingDone -> {
                if (fragment.requireActivity().intent?.action == MediaStore.Audio.Media.RECORD_SOUND_ACTION) {
                    // activity was launched with intent, and we need to return the recording
                    val replyIntent = android.content.Intent().apply {
                        setData(label.result)
                        clipData = ClipData.newRawUri("", label.result)
                        setFlags(FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    fragment.requireActivity().run {
                        setResult(RESULT_OK, replyIntent)
                        finish()
                    }
                }
            }

            is Label.RecreateVisualizer -> {
                binding.audioVisualizer.recreate()
            }
        }
    }

    override fun handleTimerTick(text: String) {
        binding.recTimer.text = text
    }

    override fun handleAmplitudeUpdate(amp: Int) {
        binding.audioVisualizer.update(amp)
    }

}
