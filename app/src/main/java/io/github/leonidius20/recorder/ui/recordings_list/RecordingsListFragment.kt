package io.github.leonidius20.recorder.ui.recordings_list

import android.content.Context
import android.os.Bundle
import android.view.ActionMode
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding

@AndroidEntryPoint
class RecordingsListFragment : Fragment() {

    private var _binding: FragmentRecordingsListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: RecordingsListViewModel

    private lateinit var adapter: RecordingsListAdapter

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
            adapter = RecordingsListAdapter(recordings, onItemClick, onItemLongClick)
            binding.recordingList.adapter = adapter

        }

        viewModel.loadRecordings()

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
            mode.menuInflater.inflate(R.menu.recordings_list_context_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // todo: invalidation happends on each toggling of selection
            // so we can add or remove menu elements here based on if it is
            // 1 element selected or multiple
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            when(item.itemId) {
                R.id.recordings_list_action_rename -> {
                    // rename(position)
                }
                R.id.recordings_list_action_delete_forever -> {
                    // todo
                }
                /*R.id.recordings_list_action_share -> {
                    // todo
                }*/
                R.id.recordings_list_action_trash -> {
                    // todo
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            adapter.clearAllSelection()
            actionMode = null
        }


    }

}