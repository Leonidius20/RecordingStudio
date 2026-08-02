package io.github.leonidius20.recorder.ui.recordings_list.view

import android.app.Activity
import android.content.ComponentName
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import com.arkivanov.essenty.lifecycle.essentyLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.data.playback.PlaybackService
import io.github.leonidius20.recorder.databinding.FragmentRecordingsListBinding
import io.github.leonidius20.recorder.ui.common.RecStudioFragment
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListViewModel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
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

        binding.recordingList.setHasFixedSize(true) // supposedly improves performance


        // todo migrate logic

        /*val onItemClick: (Int) -> Unit = { position: Int ->
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
            requireContext(),
            onItemClick,
            onItemLongClick
        )
        binding.recordingList.adapter = adapter

        // todo also toggle visibility of the empty rec text
        viewModel.state.collectSinceStarted { state ->

            adapter.setData(ArrayList(state.recordings))
            //binding.recordingList.scrollToPosition(0)

            binding.emptyListText.isVisible = state.recordings.isEmpty()
        }*/


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

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // todo: handle labels

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

            mediaController?.addListener(object : Player.Listener {

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // adapter.setPlaying(mediaController!!.currentMediaItemIndex) todo migrate to diffing approach
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        // adapter.setPlaying(mediaController!!.currentMediaItemIndex) todo migrate to diffing approach
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // adapter.resetPlayingItemHighlighting() todo migrate to diffing approach
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
        }
    }

}