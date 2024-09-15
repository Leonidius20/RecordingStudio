package io.github.leonidius20.recorder.ui.recordings_list.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
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
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.playback.PlaybackService
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding
import io.github.leonidius20.recorder.databinding.RenameDialogBinding
import io.github.leonidius20.recorder.ui.common.RecStudioFragment
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListViewModel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class RecordingsListFragment : RecStudioFragment() {

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


        val onItemClick: (Int) -> Unit = { position: Int ->
            if (actionMode != null) {
                viewModel.toggleSelection(position)
            } else {
                // start playback
                setPlayingFile(position)
            }
        }

        val onItemLongClick = { position: Int ->
            if (actionMode == null) {
                actionMode = requireActivity().startActionMode(actionModeCallback)
            }

            viewModel.toggleSelection(position)
        }

        adapter = RecordingsListAdapter(
            onItemClick,
            onItemLongClick
        )
        binding.recordingList.adapter = adapter

        viewModel.state.collectSinceStarted { state ->

            adapter.setData(ArrayList(state.recordings))
            binding.recordingList.scrollToPosition(0)

        }

        viewModel.state.collectDistinctSinceStarted({ it.numItemsSelected }) { numItemsSelected ->
            val shouldShowActionMode = numItemsSelected > 0

            // if should show actionMode, but it is not being shown yet
            if (shouldShowActionMode && actionMode == null) {
                actionMode = requireActivity().startActionMode(actionModeCallback)
            } else if (!shouldShowActionMode && actionMode != null) {
                // if should not show action mode but it is being shown
                actionMode!!.finish()
                actionMode = null
            }

            if (shouldShowActionMode) {
                actionMode!!.apply {
                    title = getString(R.string.recs_list_action_mode_num_selected, numItemsSelected)
                    invalidate()
                }
            }
        }

        // this has to happen every time that we go to this fragment. However
        // what if we cache data on disk, get that data in viewmodel.init(), and
        // then here we just run some viewModel.checkNewRecordings(), that will compare
        // the MediaStore version, if it's changed, it will compare some other thing,
        // then use DiffUtil to change the list (some stuff may have been deleted or renamed
        // between
        //viewModel.loadRecordings()

        trashRecordingsIntentLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    /*val selectedPositions = adapter.getSelectedItemsPositions()
                    adapter.removeItems(selectedPositions)
                    selectedPositions.forEach { mediaController?.removeMediaItem(it) }*/


                } else {
                    Toast.makeText(requireContext(), "failure", Toast.LENGTH_SHORT).show()
                }
                actionMode!!.finish()
            }

        deleteRecordingsIntentLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    /*val selectedPositions = adapter.getSelectedItemsPositions()
                    adapter.removeItems(selectedPositions)
                    selectedPositions.forEach { mediaController?.removeMediaItem(it) }*/
                } else {
                    Toast.makeText(requireContext(), "failure", Toast.LENGTH_SHORT).show()
                }
                actionMode!!.finish()
            }


        // registerForContextMenu(binding.recordingList)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    var actionMode: ActionMode? = null

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {

            if (viewModel.state.value.numItemsSelected > 1) {
                mode.menuInflater.inflate(
                    R.menu.recordings_list_multiple_recordings_context_menu,
                    menu
                )
            } else {
                mode.menuInflater.inflate(R.menu.recordings_list_one_recording_context_menu, menu)
            }

            // todo: this is temporary, remove once sharing is implemented
            menu.removeItem(R.id.recordings_list_action_share)

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // todo: invalidation happends on each toggling of selection
            // so we can add or remove menu elements here based on if it is
            // 1 element selected or multiple
            menu.clear()
            if (viewModel.state.value.numItemsSelected > 1) {
                mode.menuInflater.inflate(
                    R.menu.recordings_list_multiple_recordings_context_menu,
                    menu
                )
            } else {
                mode.menuInflater.inflate(R.menu.recordings_list_one_recording_context_menu, menu)
            }

            // todo: this is temporary, remove once sharing is implemented
            menu.removeItem(R.id.recordings_list_action_share)

            return true
        }

        @SuppressLint("NewApi") // the "trash" option requires api 30 but it isn't shown in the menu on lower apis
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            when (item.itemId) {
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
            //adapter.clearAllSelection()
            viewModel.clearSelection()
            actionMode = null
        }


    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun trash() {
        //val positions = adapter.getSelectedItemsPositions()
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
                    /* val selectedPositions = adapter.getSelectedItemsPositions()
                     adapter.removeItems(selectedPositions)
                     selectedPositions.forEach { mediaController?.removeMediaItem(it) }*/
                    actionMode!!.finish()
                }
                .setNegativeButton(android.R.string.no) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

    }

    fun rename() {
        //val position = adapter.getSelectedItemsPositions().first()
        // if success
        // todo: first stop actionmode, then show rename dialog, so that the need for payloads is evident
        val selectedItem = viewModel.getFirstSelectedItem()

        actionMode!!.finish()

        // showRenameDialog() // probably not very sustainable when we will implement
        // restoring the dialog after screen rotation. Maybe it is better to restore selected
        // items and then take position from there

        findNavController().navigate(
            RecordingsListFragmentDirections.actionNavigationRecordingsListToRenameDialogFragment(
                fileToRename = selectedItem.uri,
                currentFileName = selectedItem.name,
                id = selectedItem.id,
            )
        )



    }

    /**
     * shows rename dialog for the first time or after activity recreation
     */
    /*fun showRenameDialog() {
        // todo: dialog being shown is a part of UI state. It should be stored in viewmodel
        // and there should be a "render" function that simply renders out the state that is
        // saved in viewmodel

        // todo: it is lost when screen rotates


        // todo: this right here is a "feature envy" code smell. Need to refactor
        // and reachitect the ui state logic

        //todo: dialog fragment with callback?
        val oldName = viewModel.getFirstSelectedItemName()

        viewModel.renameFileNewName.value = oldName

        val dialogView = RenameDialogBinding.inflate(layoutInflater).also { binding ->
            // todo: remove all this after fixing 2 way data binding
            binding.fileNameEditText.setText(oldName)
            binding.fileNameEditText.addTextChangedListener {
                viewModel.renameFileNewName.value = binding.fileNameEditText.text!!.toString()
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.recordings_list_choose_new_name)
            .setView(dialogView.root)
            .setPositiveButton(android.R.string.ok) { d, i ->
                onRenameDialogSubmitted()
            }
            .show()
    }*/

    /*private fun onRenameDialogSubmitted() {
        viewModel.rename()
        // adapter.renameItemAt(position, newData.name)
    }*/

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null


    override fun onStart() {
        super.onStart()
        val context = requireContext()
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val factory = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = factory
        factory.addListener({
            mediaController = factory.let {
                if (it.isDone)
                    it.get()
                else
                    null
            }

            binding.playerView.player = mediaController

            viewModel.state
                .distinctUntilChangedBy { it.itemIds }
                .map { it.recordings }
                .collectSinceStarted { recordings ->

                    mediaController?.replaceMediaItems(0, mediaController!!.mediaItemCount,
                        recordings.map { recording ->
                            MediaItem.Builder()
                                .setUri(recording.uri)
                                .setMediaId(recording.id.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder().setDisplayTitle(recording.name).build()
                                ).build()
                        }
                    )
                }

            /*viewModel.recordings.value!!.forEach { recording ->
                mediaController?.addMediaItem(
                    MediaItem.Builder()
                        .setUri(recording.uri)
                        .setMediaId(recording.id.toString())
                        .setMediaMetadata(
                            MediaMetadata.Builder().setDisplayTitle(recording.name).build()
                        ).build()
                )
            }*/

            mediaController?.addListener(object : Player.Listener {

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    adapter.setPlaying(mediaController!!.currentMediaItemIndex)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        adapter.setPlaying(mediaController!!.currentMediaItemIndex)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        adapter.resetPlayingItemHighlighting()
                    }
                }

            })

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


    private fun setPlayingFile(position: Int) {
        with(mediaController!!) {
            seekTo(position, 0L)
            if (!isPlaying) play()
            // adapter.setPlaying(position)
        }
    }

    fun attachPlayerToView(player: Player) {
        binding.playerView.player = player
    }

}