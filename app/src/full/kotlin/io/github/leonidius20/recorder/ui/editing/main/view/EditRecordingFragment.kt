package io.github.leonidius20.recorder.ui.editing.main.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentEditRecordingBinding
import io.github.leonidius20.recorder.ui.editing.main.viewmodel.EditRecordingViewModel
import io.github.leonidius20.recorder.ui.editing.plugins_list.view.PluginsListAdapter
import io.github.leonidius20.recorder.ui.editing.plugins_list.viewmodel.PluginsListViewModel

// todo: delete this file altogether, its unused
// just move name display to the plugin chain scrren
@Deprecated(message = "todo: delete this file altogether, its unused")
@AndroidEntryPoint
class EditRecordingFragment : Fragment() {

    private var _binding: FragmentEditRecordingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditRecordingViewModel by viewModels()
    private val pluginListViewModel: PluginsListViewModel by viewModels()

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
        // binding.recordingName.text = viewModel.fileName

        val adapter = PluginsListAdapter(onItemClick = {



            /*findNavController().navigate(
                EditRecordingFragmentDirections.actionEditRecordingToPluginDetails(
                    pluginId = it.id,
                    fileUri = viewModel.uri,
                    fileName = viewModel.fileName,
                )
            )*/
        })
        binding.pluginsList.adapter = adapter
        adapter.submitList(pluginListViewModel.get()) // todo: ui state and all
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}