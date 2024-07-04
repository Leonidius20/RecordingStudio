package io.github.leonidius20.recorder.ui.home

import android.content.ContentValues
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.RecPermissionManager
import io.github.leonidius20.recorder.RecorderService
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import io.github.leonidius20.recorder.ui.common.setIcon
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
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


        val recPauseButton = binding.recordButton
        recPauseButton.setIcon(R.drawable.ic_record, BTN_IMG_TAG_RECORD)


        recPauseButton.setOnClickListener {
            // if playing then

            if (recPauseButton.tag == BTN_IMG_TAG_PAUSE) {
                /*recorder?.apply {
                    stop()
                    release()
                }
                recorder = null
                descriptor.close()*/
                recPauseButton.setIcon(R.drawable.ic_record, BTN_IMG_TAG_RECORD)
                requireActivity().stopService(
                    Intent(requireActivity(), RecorderService::class.java)
                )
            } else {

                    val permissionGranted = permissionManager
                        .obtainRecordingPermission(this@HomeFragment)




                    if (permissionGranted) {
                        // if (!recordingsDirectory.exists()) mkdirs

                        // todo: formatted time YYYY-MM-DD-HH-MM-SS-SSS
                        /*val fileName = "${System.currentTimeMillis()}"

                        descriptor = getRecFileUri(fileName)*/

                        requireActivity().startService(
                            Intent(requireActivity(), RecorderService::class.java)
                        )

                        /*recorder = MediaRecorder().apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                            setOutputFile(descriptor.fileDescriptor)
                            // todo: check what codecs there are and provide user with options
                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

                            try {
                                prepare()
                            } catch (e: IOException) {
                                Log.e("Recorder", "prepare() failed")
                            }

                            start()
                        }*/

                        // todo: recorder.maxAmplitude visalize

                        recPauseButton.setIcon(R.drawable.ic_pause, BTN_IMG_TAG_PAUSE)
                    } else {
                        Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show()
                    }

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

