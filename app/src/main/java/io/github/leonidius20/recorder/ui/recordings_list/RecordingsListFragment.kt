package io.github.leonidius20.recorder.ui.recordings_list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding

@AndroidEntryPoint
class RecordingsListFragment : Fragment() {

    private var _binding: FragmentRecordingsListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: RecordingsListViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingsListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel =
            ViewModelProvider(this).get(RecordingsListViewModel::class.java)

        viewModel.recordings.observe(viewLifecycleOwner) { recordings ->
            // todo: DiffUtil here
            binding.recordingList.adapter =
                RecordingsListAdapter(recordings)
        }

        viewModel.loadRecordings()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}