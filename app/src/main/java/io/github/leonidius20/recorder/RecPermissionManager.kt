package io.github.leonidius20.recorder

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.scopes.FragmentScoped
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@FragmentScoped
class RecPermissionManager @Inject constructor(

) {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private val isPermissionGranted = MutableLiveData<Boolean>()

    fun registerForRecordingPermission(fragment: Fragment) {
        requestPermissionLauncher =
            fragment.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    isPermissionGranted.value = true
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    isPermissionGranted.value = false
                }
            }
    }

    fun obtainRecordingPermission(fragment: Fragment) = runBlocking {
        checkOrRequestRecordingPermission(fragment)
    }


    /**
     * Checks if the recording permission is granted, and requests it if it's not.
     * @return true if the permission is granted, false otherwise
     */
    private suspend fun checkOrRequestRecordingPermission(
        fragment: Fragment
    ): Boolean = suspendCoroutine {
        when {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                it.resume(true)
            }
            // todo: explain to the user why the permission is needed

            ActivityCompat.shouldShowRequestPermissionRationale(
                fragment.requireActivity(), android.Manifest.permission.RECORD_AUDIO) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                    AlertDialog.Builder(fragment.requireContext())
                        .setTitle("Permission needed")
                        .setMessage("This permission is needed for recording audio")
                        .setPositiveButton("ok") { _, _ ->
                            isPermissionGranted.observe(fragment) { isGranted ->
                                println("isGranted: $isGranted")
                                if (isGranted) {
                                    it.resume(true)
                                    isPermissionGranted.removeObservers(fragment)
                                } else {
                                    it.resume(false)
                                    isPermissionGranted.removeObservers(fragment)
                                }
                            }

                            requestPermissionLauncher.launch(
                                android.Manifest.permission.RECORD_AUDIO)
                        }
                        .setNegativeButton("cancel") { _, _ ->
                            it.resume(false)
                        }
                        //.create()
                        .show()

            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.

                isPermissionGranted.observe(fragment) { isGranted ->
                    println("isGranted: $isGranted")
                    if (isGranted) {
                        it.resume(true)
                        isPermissionGranted.removeObservers(fragment)
                    } else {
                        it.resume(false)
                        isPermissionGranted.removeObservers(fragment)
                    }
                }

                requestPermissionLauncher.launch(
                    android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }



}