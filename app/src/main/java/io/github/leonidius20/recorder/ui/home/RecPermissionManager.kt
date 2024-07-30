package io.github.leonidius20.recorder.ui.home

import androidx.fragment.app.Fragment
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@FragmentScoped
class RecPermissionManager @Inject constructor() {

    /**
     * Checks if the recording permission is granted, and requests it if it's not.
     * @return true if the permission is granted, false otherwise
     */
    suspend fun checkOrRequestRecordingPermission(
        fragment: Fragment
    ): Boolean = suspendCoroutine {
        PermissionX.init(fragment)
            .permissions(android.Manifest.permission.RECORD_AUDIO)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList,
                    message = "The app needs audio recording permission to work.",
                    positiveText = fragment.getString(android.R.string.ok)
                )
            }.onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(deniedList,
                    message = "The app needs audio recording permission to work. You need to grant it manually in the Settings.",
                    positiveText = fragment.getString(android.R.string.ok))
            }.request { allGranted: Boolean, grantedList, deniedList ->
                it.resume(allGranted)
            }
    }

}