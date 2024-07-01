package io.github.leonidius20.recorder.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.MainActivity
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.FragmentHomeBinding
import io.github.leonidius20.recorder.ui.common.setIcon
import kotlinx.coroutines.launch

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

            if (recPauseButton.tag == BTN_IMG_TAG_PAUSE) {
                recPauseButton.setIcon(R.drawable.ic_record, BTN_IMG_TAG_RECORD)
            } else {
                lifecycleScope.launch {
                    val permissionGranted = (requireActivity() as MainActivity)
                        .permissionManager.checkOrRequestRecordingPermission()


                    if (permissionGranted) {
                        Toast.makeText(context, "Granted", Toast.LENGTH_SHORT).show()
                        recPauseButton.setIcon(R.drawable.ic_pause, BTN_IMG_TAG_PAUSE)
                    } else {
                        Toast.makeText(context, "Denied", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

