package io.github.leonidius20.recorder.ui.recordings_list

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.net.Uri
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.playback.PlaybackService
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


        val onItemClick: (Int)->Unit = { position: Int ->
            if (actionMode != null) {
                toggleSelection(position)
            } else {
                // start playback
                setFile(position)
            }
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

        // this has to happen every time that we go to this fragment. However
        // what if we cache data on disk, get that data in viewmodel.init(), and
        // then here we just run some viewModel.checkNewRecordings(), that will compare
        // the MediaStore version, if it's changed, it will compare some other thing,
        // then use DiffUtil to change the list (some stuff may have been deleted or renamed
        // between
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






        actionMode!!.finish()

        showRenameDialog(position) // probably not very sustainable when we will implement
        // restoring the dialog after screen rotation. Maybe it is better to restore selected
        // items and then take position from there


    }

    /**
     * shows rename dialog for the first time or after activity recreation
     */
    fun showRenameDialog(position: Int) {
        // todo: dialog being shown is a part of UI state. It should be stored in viewmodel
        // and there should be a "render" function that simply renders out the state that is
        // saved in viewmodel

        // todo: it is lost when screen rotates


        // todo: this right here is a "feature envy" code smell. Need to refactor
        // and reachitect the ui state logic

        //todo: dialog fragment with callback?
        viewModel.renameFileNewName.value = viewModel.recordings.value!![position].name

        val dialogView = RenameDialogBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.recordings_list_choose_new_name)
            .setView(dialogView.root)
            .setPositiveButton(android.R.string.ok) { d, i ->
                onRenameDialogSubmitted(position)
            }
            .show()
    }

    private fun onRenameDialogSubmitted(position: Int) {
        val newData = viewModel.recordings.value!![position].copy(
            name = viewModel.renameFileNewName.value!!
        )
        // todo: actually rename in Repository
        adapter.replaceItemAt(position, newData)
    }

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null


    override fun onStart() {
        super.onStart()
        val context = requireContext()
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val factory = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = factory
        factory.addListener( {
            mediaController = factory?.let {
                if (it.isDone)
                    it.get()
                else
                    null
            }

            binding.playerView.player = mediaController

            viewModel.recordings.value!!.forEach { recording ->
                mediaController?.addMediaItem(
                    MediaItem.Builder().setUri(recording.uri).setMediaMetadata(
                        MediaMetadata.Builder().setDisplayTitle(recording.name).build()
                    ).build()
                )
            }

            mediaController?.prepare()


        }, MoreExecutors.directExecutor())
    }
    //todo: replace with lifecycle aware component

    override fun onStop() {
        super.onStop()
        MediaController.releaseFuture(controllerFuture!!)
        controllerFuture = null
        mediaController = null
    }


    private fun setFile(position: Int) {
        with(mediaController!!) {
            seekTo(position, 0L)
        }
    }

}