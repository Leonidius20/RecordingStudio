package io.github.leonidius20.recorder.ui.common.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

abstract class FragmentWithBinding<B : ViewBinding> : RecStudioFragment() {

    private var _binding: B? = null

    protected val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBindingInstance(layoutInflater, container)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun createBindingInstance(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToRoot: Boolean = false,
    ): B {
        val vbType = (javaClass.genericSuperclass as ParameterizedType)
            .actualTypeArguments[0]

        val vbClass = vbType as Class<B>
        val method = vbClass.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )

        // Call VB.inflate(inflater, container, false) Java static method
        return method.invoke(null, inflater, container, attachToRoot) as B
    }


}