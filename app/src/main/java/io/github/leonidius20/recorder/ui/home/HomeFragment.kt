package io.github.leonidius20.recorder.ui.home

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaRecorder
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.RecPermissionManager
import io.github.leonidius20.recorder.RecorderService
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import io.github.leonidius20.recorder.ui.common.setIcon
import javax.inject.Inject

/**
 * a tag value used to mark that the record button shows "Record" icon (as
 * opposed to "Pause"). Used for testing purposes because it is impossible
 * to compare drawables in a test case.
 */
const val BTN_IMG_TAG_RECORD = "record"
const val BTN_IMG_TAG_PAUSE = "pause"

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @Inject
    lateinit var permissionManager: RecPermissionManager

    private var recorder: MediaRecorder? = null

    private lateinit var descriptor: ParcelFileDescriptor

    private var binder: RecorderService.Binder? = null

    private val isRecServiceRunning
        get() = binder != null


    enum class UiState {
        IDLE,
        RECORDING,
        PAUSED
    }

    private val uiState = MutableLiveData<UiState>(UiState.IDLE)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        /*val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }*/

        uiState.observe(viewLifecycleOwner) {
            when(it) {
                UiState.IDLE -> {
                    binding.recordButton.setIcon(R.drawable.ic_record, BTN_IMG_TAG_RECORD)
                    binding.stopButton.visibility = View.GONE
                }
                UiState.RECORDING -> {
                    binding.recordButton.setIcon(R.drawable.ic_pause, BTN_IMG_TAG_PAUSE)
                    binding.stopButton.visibility = View.VISIBLE
                }
                UiState.PAUSED -> {
                    binding.recordButton.setIcon(R.drawable.ic_record, BTN_IMG_TAG_RECORD)
                    binding.stopButton.visibility = View.VISIBLE
                }
                else -> throw IllegalStateException()
            }
        }

        val stopButton = binding.stopButton
        stopButton.setOnClickListener {
            requireActivity().stopService(
                Intent(requireActivity(), RecorderService::class.java)
            )
            uiState.value = UiState.IDLE
        }


        val recPauseButton = binding.recordButton
        recPauseButton.setOnClickListener {
            // if playing then

            if (isRecServiceRunning) {
                binder!!.service.toggleRecPause()
            } else {
                val permissionGranted = permissionManager
                    .obtainRecordingPermission(this@HomeFragment)

                if (!permissionGranted) {
                    Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }



                // if (!recordingsDirectory.exists()) mkdirs

                // todo: formatted time YYYY-MM-DD-HH-MM-SS-SSS
                /*val fileName = "${System.currentTimeMillis()}"

                descriptor = getRecFileUri(fileName)*/

                ActivityCompat.startForegroundService(
                    requireActivity(),
                    Intent(requireActivity(), RecorderService::class.java)
                )

                requireActivity().bindService(
                    Intent(requireActivity(), RecorderService::class.java),
                    object: ServiceConnection {
                        override fun onServiceConnected(
                            name: ComponentName?,
                            service: IBinder?
                        ) {
                            binder = service as RecorderService.Binder
                            binder!!.service.state.observe(viewLifecycleOwner) {
                                when(it) {
                                    RecorderService.State.RECORDING -> uiState.value = UiState.RECORDING
                                    RecorderService.State.PAUSED -> uiState.value = UiState.PAUSED
                                    else -> throw IllegalStateException()
                                }
                            }
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            binder!!.service.state.removeObservers(viewLifecycleOwner)
                            // nothing?
                        }


                    },
                    Context.BIND_IMPORTANT // todo: understand these values
                )





            }


        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionManager.registerForRecordingPermission(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getRecFileUri(name: String): ParcelFileDescriptor {
        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/3gpp") // todo: other types
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Recordings/RecordingStudio")
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

        return resolver.openFileDescriptor(uri!!, "w")!!
    }
}

