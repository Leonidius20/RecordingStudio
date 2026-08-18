package io.github.leonidius20.recorder.ui.recordings_list.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.RenameDialogBinding
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RenameDialogViewModel

@AndroidEntryPoint
class RenameDialogFragment : DialogFragment() {

    private val renameDialogViewModel: RenameDialogViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = RenameDialogBinding.inflate(layoutInflater).apply {
            viewModel = renameDialogViewModel
            lifecycleOwner = viewLifecycleOwner

            renameDialogCancelButton.setOnClickListener {
                dismiss()
            }

            renameDialogConfirmButton.setOnClickListener {
                renameDialogViewModel.rename()
                dismiss()
            }
        }

        dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        return binding.root
    }

    /*override fun getTheme(): Int {
        return R.style.Theme_RecordingStudio_DialogActivity
    }*/

}