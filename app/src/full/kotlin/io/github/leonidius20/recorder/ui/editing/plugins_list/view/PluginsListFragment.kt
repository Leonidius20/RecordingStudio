package io.github.leonidius20.recorder.ui.editing.plugins_list.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentPluginsListBinding
import io.github.leonidius20.recorder.ui.editing.plugin.viewmodel.PluginDetailsViewModel
import io.github.leonidius20.recorder.ui.editing.plugins_list.viewmodel.PluginsListViewModel

@AndroidEntryPoint
class PluginsListFragment : DialogFragment() {

    private var _binding: FragmentPluginsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PluginsListViewModel by viewModels()
    private val editingViewModel: PluginDetailsViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPluginsListBinding
            .inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = PluginsListAdapter(onItemClick = {
            editingViewModel.addPlugin(it)
            dismiss()
            /*findNavController().navigate(
                PluginsListFragmentDirections.actionPluginsListToDetails(
                    pluginId = it.id,

                )
            )*/
        })
        binding.pluginsList.adapter = adapter
        adapter.submitList(viewModel.get()) // todo: ui state and all
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}