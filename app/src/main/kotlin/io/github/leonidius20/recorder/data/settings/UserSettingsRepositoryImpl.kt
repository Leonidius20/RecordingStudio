package io.github.leonidius20.recorder.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.BoolRes
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.di.Scope
import io.github.leonidius20.recorder.domain.settings.UserSettings
import io.github.leonidius20.recorder.domain.settings.UserSettingsReadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Scope.App private val appScope: CoroutineScope,
) : UserSettingsReadRepository {

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    // stored here so that it's not garbage collected.
    // prefs only store weak ref
    lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    // todo: also split data source and repo?
    override val userSettings = callbackFlow {
        trySend(getData())

        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { pref, key ->
            trySend(getData())
        }

        pref.registerOnSharedPreferenceChangeListener(prefListener)

        awaitClose {
            pref.unregisterOnSharedPreferenceChangeListener(prefListener)
            // todo: delete ref
        }
    }.stateIn(appScope, SharingStarted.Eagerly, getData())

    private fun getData() =
        UserSettings(
            stopOnLowBattery = pref.getBoolean(
                R.string.stop_on_low_battery_pref_key,
                R.bool.stop_on_low_battery_default
            ),
            stopOnLowStorage = pref.getBoolean(
                R.string.stop_on_low_storage_pref_key,
                R.bool.stop_on_storage_default
            ),
            pauseOnCall = pref.getBoolean(
                R.string.pause_on_call_pref_key,
                R.bool.pause_on_call_default
            ),
        )

    // todo: move default values and keys into code
    //  so as not to depend on context here?
    private fun SharedPreferences.getBoolean(
        @StringRes key: Int,
        @BoolRes defaultValue: Int,
    ) = getBoolean(
        context.getString(key),
        context.resources.getBoolean(defaultValue)
    )

}
