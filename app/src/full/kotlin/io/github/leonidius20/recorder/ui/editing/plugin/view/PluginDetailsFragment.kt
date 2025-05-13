package io.github.leonidius20.recorder.ui.editing.plugin.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentPluginDetailsBinding
import io.github.leonidius20.recorder.ui.editing.plugin.model.PluginDetailsState
import io.github.leonidius20.recorder.ui.editing.plugin.viewmodel.PluginDetailsViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PluginDetailsFragment : Fragment() {

    private var _binding: FragmentPluginDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<PluginDetailsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPluginDetailsBinding.inflate(
            inflater, container, false
        )
        binding.lifecycleOwner = this
        binding.vm = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PluginParamsAdapter()
        binding.paramsList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is PluginDetailsState.Connected) {
                        adapter.submitList(state.info.parameters)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
