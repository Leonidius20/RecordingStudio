package io.github.leonidius20.recorder.audio_config.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import com.arkivanov.essenty.instancekeeper.instanceKeeper
import com.arkivanov.essenty.lifecycle.essentyLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.audio_config.presentation.view.AudioSettingsController
import io.github.leonidius20.recorder.audio_config.ui.databinding.BottomSheetAudioSettingsBinding
import io.github.leonidius20.recorder.common.ui.R
import io.github.leonidius20.recorder.common.ui.doOnApplyWindowInsets
import javax.inject.Inject

@AndroidEntryPoint
class AudioSettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAudioSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
    }

    companion object {
        const val TAG = "AudioSettingsBottomSheet"
    }

}
