package io.github.leonidius20.recorder.ui.editing.main.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentEditRecordingBinding
import io.github.leonidius20.recorder.ui.editing.main.viewmodel.EditRecordingViewModel

@AndroidEntryPoint
class EditRecordingFragment : Fragment() {

    private var _binding: FragmentEditRecordingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditRecordingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditRecordingBinding.inflate(
            inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recordingName.text = "Editing file"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}