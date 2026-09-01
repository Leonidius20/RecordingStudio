package io.github.leonidius20.recorder.ui.recordings_list.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.RenameDialogBinding
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RenameDialogViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RenameDialogFragment : DialogFragment() {

    private val viewModel: RenameDialogViewModel by viewModels()

    private var _binding: RenameDialogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RenameDialogBinding.inflate(layoutInflater).apply {

            renameDialogCancelButton.setOnClickListener {
                dismiss()
            }

            renameDialogConfirmButton.setOnClickListener {
                viewModel.rename()
                dismiss()
            }

            fileNameEditText.addTextChangedListener(onTextChanged = { new, _, _, _ ->
                viewModel.updateText(new?.toString() ?: "")
            })
        }

        dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fileName.collect { name ->
                    if (binding.fileNameEditText.text.toString() != name) {
                        binding.fileNameEditText.setText(name)

                        // Maintain cursor position at the end of the text
                        binding.fileNameEditText.setSelection(name.length)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    /*override fun getTheme(): Int {
        return R.style.Theme_RecordingStudio_DialogActivity
    }*/

}
