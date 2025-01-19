package io.github.leonidius20.recorder.ui.editing.plugins.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentPluginsListBinding
import io.github.leonidius20.recorder.ui.editing.plugins.viewmodel.PluginsListViewModel

@AndroidEntryPoint
class PluginsListFragment : Fragment() {

    private var _binding: FragmentPluginsListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PluginsListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPluginsListBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = PluginsListAdapter()
        binding.pluginsList.adapter = adapter
        adapter.submitList(viewModel.get()) // todo: ui state and all
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}