package io.github.leonidius20.recorder.ui.recordings_list.view

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.arkivanov.essenty.lifecycle.essentyLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding
import io.github.leonidius20.recorder.ui.common.RecStudioFragment
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListViewModel
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RecordingsListFragment : RecStudioFragment() {

    private var _binding: FragmentRecordingsListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: RecordingsListViewModel by viewModels()

    private lateinit var trashRecordingsIntentLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var deleteRecordingsIntentLauncher: ActivityResultLauncher<IntentSenderRequest>

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
                //storeFactory = storeFactory,
                //database = database,
                lifecycle = essentyLifecycle(),
                //instanceKeeper = instanceKeeper(),
                //dispatchers = dispatchers,
                //onItemSelected = onItemSelected,
            )


        trashRecordingsIntentLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // nothing
                } else {
                    Toast.makeText(requireContext(), "failure", Toast.LENGTH_SHORT).show()
                }
                //actionMode?.finish() todo migrate
            }

        deleteRecordingsIntentLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // nothing
                } else {
                    Toast.makeText(requireContext(), "failure", Toast.LENGTH_SHORT).show()
                }
                //actionMode?.finish() todo migrate
            }


        // registerForContextMenu(binding.recordingList)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("OnViewCreated calling..")
        controller.onViewCreated(RecordingsListViewImpl(binding, ::requireActivity),
            viewLifecycleOwner.essentyLifecycle())
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun trash() {
        val intent = viewModel.requestTrashingSelected()
        trashRecordingsIntentLauncher.launch(
            IntentSenderRequest.Builder(intent).build()
        )
    }

    fun delete() {
        //val positions = adapter.getSelectedItemsPositions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = viewModel.requestDeletingSelected()
            deleteRecordingsIntentLauncher.launch(
                IntentSenderRequest.Builder(intent).build()
            )
        } else {
            // todo: dialogFragment


            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Deleting files")
                .setMessage("Do you confirm deleting ${viewModel.state.value.numItemsSelected} selected file(s)?")
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    viewModel.legacyDeleteSelectedWithoutConfirmation()
                    // actionMode?.finish() todo migrate
                }
                .setNegativeButton(android.R.string.no) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

    }

    fun rename() {
        val selectedItem = viewModel.getFirstSelectedItem()

        // actionMode?.finish() todo migrate

        findNavController().navigate(
            RecordingsListFragmentDirections.actionNavigationRecordingsListToRenameDialogFragment(
                fileToRename = selectedItem.uri,
                currentFileName = selectedItem.name,
                id = selectedItem.id,
            )
        )
    }

}
