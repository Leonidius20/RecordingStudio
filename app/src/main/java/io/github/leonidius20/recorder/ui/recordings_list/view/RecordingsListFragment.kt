package io.github.leonidius20.recorder.ui.recordings_list.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arkivanov.essenty.instancekeeper.instanceKeeper
import com.arkivanov.essenty.lifecycle.essentyLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding
import io.github.leonidius20.recorder.ui.common.RecStudioFragment
import javax.inject.Inject

@AndroidEntryPoint
class RecordingsListFragment : RecStudioFragment() {

    private var _binding: FragmentRecordingsListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var controller: RecordingsListController

    @Inject
    lateinit var controllerFactory: RecordingsListController.Factory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingsListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        controller =
            controllerFactory.create(
                lifecycle = essentyLifecycle(),
                instanceKeeper = instanceKeeper(),
            )

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller.onViewCreated(RecordingsListViewImpl(binding, this),
            viewLifecycleOwner.essentyLifecycle())
    }

}
