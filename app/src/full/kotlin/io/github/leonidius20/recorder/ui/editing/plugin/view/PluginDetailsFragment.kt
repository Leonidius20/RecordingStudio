package io.github.leonidius20.recorder.ui.editing.plugin.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentPluginDetailsBinding
import io.github.leonidius20.recorder.ui.editing.plugin.model.PluginDetailsState
import io.github.leonidius20.recorder.ui.editing.plugin.viewmodel.PluginDetailsViewModel
import io.github.leonidius20.recorder.ui.editing.plugins_list.view.PluginsListFragment
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveBtn.setOnClickListener {
            viewModel.saveFile()
        }
        binding.playPauseBtn.setOnClickListener {
            viewModel.toggleProcessing()
        }

        val adapter = PluginsChainAdapter(
            toggleParamsVisibility = { pluginIndex ->
                viewModel.togglePluginExpandedState(pluginIndex)
            },
            changePluginParam = { pluginIndex, paramIndex, newVal ->
                viewModel.changeParam(paramIndex, newVal, pluginIndex)
            }
        )

        binding.pluginChainList.adapter = adapter

        binding.addPluginBtn.setOnClickListener {
            PluginsListFragment().show(childFragmentManager, "plugin-selector-dialog")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pluginChain.collect { chain ->
                    adapter.submitList(chain, binding.pluginChainList)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    binding.saveBtn.isVisible = it.saveBtnVisibility()
                    binding.playPauseBtn.isVisible = it.playBtnVisibility()
                    binding.pluginAndPlaybackControls.isVisible = it is PluginDetailsState.Connected
                    binding.progressCircle.isVisible = it is PluginDetailsState.Connecting
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
