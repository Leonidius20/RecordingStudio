package io.github.leonidius20.recorder.ui.recordings_list

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding
import io.github.leonidius20.recorder.databinding.RenameDialogBinding

@AndroidEntryPoint
class RecordingsListFragment : Fragment() {

    private var _binding: FragmentRecordingsListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: RecordingsListViewModel

    private lateinit var adapter: RecordingsListAdapter

    private lateinit var trashRecordingsIntentLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var deleteRecordingsIntentLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingsListBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewModel =
            ViewModelProvider(this).get(RecordingsListViewModel::class.java)

        binding.recordingList.setHasFixedSize(true) // supposedly improves performance


        val onItemClick = { position: Int ->
            if (actionMode != null) toggleSelection(position)
        }

        val onItemLongClick = { position: Int ->
            if (actionMode == null) {
                actionMode = requireActivity().startActionMode(actionModeCallback)
            }

            toggleSelection(position)
        }

        viewModel.recordings.observe(viewLifecycleOwner) { recordings ->
            // todo: DiffUtil here
            adapter = RecordingsListAdapter(ArrayList(recordings.toMutableList()), onItemClick, onItemLongClick)
            binding.recordingList.adapter = adapter


        }

        viewModel.loadRecordings()

        trashRecordingsIntentLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
                adapter.removeItems(adapter.getSelectedItemsPositions())
            } else {
                Toast.makeText(requireContext(), "failure", Toast.LENGTH_SHORT).show()
            }
            actionMode!!.finish()
            // todo: notify item removed? or maybe use diffutil
        }

        deleteRecordingsIntentLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
                adapter.removeItems(adapter.getSelectedItemsPositions())
            } else {
                Toast.makeText(requireContext(), "failure", Toast.LENGTH_SHORT).show()
            }
            actionMode!!.finish()
            // todo: notify item removed? or maybe use diffutil
        }


        // registerForContextMenu(binding.recordingList)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun rename(datasetPosition: Int) {
        // show rename dialog
        val currentName = viewModel.recordings.value!![datasetPosition].name
        Toast.makeText(requireContext(), currentName, Toast.LENGTH_SHORT).show()
    }

    fun toggleSelection(position: Int) {
        adapter.toggleSelection(position)
        val selectedCount = adapter.getSelectedItemsCount()

        if (selectedCount == 0) {
            actionMode!!.finish()
        } else {
            actionMode!!.setTitle("${selectedCount} selected")
            actionMode!!.invalidate()
        }
    }


    var actionMode: ActionMode? = null

    val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {

            if (adapter.getSelectedItemsCount() > 1) {
                mode.menuInflater.inflate(R.menu.recordings_list_multiple_recordings_context_menu, menu)
            } else {
                mode.menuInflater.inflate(R.menu.recordings_list_one_recording_context_menu, menu)
            }

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // todo: invalidation happends on each toggling of selection
            // so we can add or remove menu elements here based on if it is
            // 1 element selected or multiple
            menu.clear()
            if (adapter.getSelectedItemsCount() > 1) {
                mode.menuInflater.inflate(R.menu.recordings_list_multiple_recordings_context_menu, menu)
            } else {
                mode.menuInflater.inflate(R.menu.recordings_list_one_recording_context_menu, menu)
            }



            return true
        }

        @SuppressLint("NewApi") // the "trash" option requires api 30 but it isn't shown in the menu on lower apis
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            when(item.itemId) {
                R.id.recordings_list_action_rename -> {
                    rename()
                }
                R.id.recordings_list_action_delete_forever -> {
                    delete()
                }
                R.id.recordings_list_action_share -> {
                    // todo
                }
                R.id.recordings_list_action_trash -> {
                    trash()
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            adapter.clearAllSelection()
            actionMode = null
        }


    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun trash() {
        val positions = adapter.getSelectedItemsPositions()
        val intent = viewModel.requestTrashing(positions)
        trashRecordingsIntentLauncher.launch(
            IntentSenderRequest.Builder(intent).build()
        )
    }

    fun delete() {
        val positions = adapter.getSelectedItemsPositions()
        val intent = viewModel.requestDeleting(positions)
        deleteRecordingsIntentLauncher.launch(
            IntentSenderRequest.Builder(intent).build()
        )
    }

    fun rename() {
        val position = adapter.getSelectedItemsPositions().first()
        // if success
        // todo: first stop actionmode, then show rename dialog, so that the need for payloads is evident

        // todo: it is lost when screen rotates
        val dialogView = RenameDialogBinding.inflate(layoutInflater)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.recordings_list_choose_new_name)
            .setView(dialogView.root)
            .setPositiveButton(android.R.string.ok) { d, i ->

            }
            .show()

        actionMode!!.finish()
        val newData = viewModel.recordings.value!![position].copy(
            name = "new name"
        )
        adapter.replaceItemAt(position, newData)
    }

    /**
     * shows rename dialog for the first time or after activity recreation
     */
    fun showRenameDialog() {}

    fun onRenameDialogSubmitted() {

    }


}