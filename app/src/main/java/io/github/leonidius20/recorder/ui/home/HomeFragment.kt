package io.github.leonidius20.recorder.ui.home

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.RecPermissionManager
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import javax.inject.Inject

/**
 * a tag value used to mark that the record button shows "Record" icon (as
 * opposed to "Pause"). Used for testing purposes because it is impossible
 * to compare drawables in a test case.
 */
const val BTN_IMG_TAG_RECORD = "record"
const val BTN_IMG_TAG_PAUSE = "pause"

@AndroidEntryPoint
class HomeFragment : Fragment() {

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
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.fragment = this

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionManager.registerForRecordingPermission(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onStopBtnClick() {
        viewModel.onStopRecording()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun toggleRecPause() {
        viewModel.onPauseOrResumeRecording()
    }

    private fun startRecording() {
        val permissionGranted = permissionManager
            .obtainRecordingPermission(this@HomeFragment)

        if (!permissionGranted) {
            Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.onStartRecording()
    }

    fun onRecButtonClick() {
        if (viewModel.uiState.value is HomeViewModel.UiState.Idle) {
            startRecording()
        } else {
            toggleRecPause()
        }
    }
}

