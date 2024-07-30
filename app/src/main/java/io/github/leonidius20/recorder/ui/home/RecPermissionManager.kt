package io.github.leonidius20.recorder.ui.home

import android.os.Build
import androidx.fragment.app.Fragment
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.scopes.FragmentScoped
import io.github.leonidius20.recorder.R
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
        val permissionsToGet = mutableListOf(
            android.Manifest.permission.RECORD_AUDIO,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToGet.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        PermissionX.init(fragment)
            .permissions(permissionsToGet)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList,
                    message = fragment.getString(R.string.permissions_rationale),
                    positiveText = fragment.getString(android.R.string.ok)
                )
            }.onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(deniedList,
                    message = fragment.getString(R.string.permissions_rationale_grant_in_settings, fragment.getString(R.string.permissions_rationale)),
                    positiveText = fragment.getString(android.R.string.ok))
            }.request { allGranted: Boolean, grantedList, deniedList ->
                it.resume(allGranted)
            }
    }

}