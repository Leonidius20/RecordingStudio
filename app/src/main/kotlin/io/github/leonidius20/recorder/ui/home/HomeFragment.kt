package io.github.leonidius20.recorder.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arkivanov.essenty.instancekeeper.instanceKeeper
import com.arkivanov.essenty.lifecycle.essentyLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import io.github.leonidius20.recorder.ui.common.RecStudioFragment
import io.github.leonidius20.recorder.ui.home.controller.HomeController
import io.github.leonidius20.recorder.ui.home.view_impl.HomeViewImpl
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : RecStudioFragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var controller: HomeController

    @Inject
    lateinit var permissionManager: RecPermissionManager

    @Inject
    lateinit var controllerFactory: HomeController.Factory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        controller = controllerFactory.create(
            instanceKeeper = instanceKeeper()
        )

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller.onViewCreated(HomeViewImpl(
            binding, this, permissionManager
        ), viewLifecycleOwner.essentyLifecycle())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
