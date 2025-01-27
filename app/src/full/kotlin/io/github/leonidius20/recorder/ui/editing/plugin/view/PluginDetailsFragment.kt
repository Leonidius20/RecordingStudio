package io.github.leonidius20.recorder.ui.editing.plugin.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentPluginDetailsBinding
import io.github.leonidius20.recorder.ui.editing.plugin.viewmodel.PluginDetailsViewModel

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}