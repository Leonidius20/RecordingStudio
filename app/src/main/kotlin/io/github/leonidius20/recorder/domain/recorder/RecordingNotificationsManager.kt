package io.github.leonidius20.recorder.domain.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.MainActivity
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.recorder.RecordingControlBroadcastReceiver
import javax.inject.Inject

private const val REC_IN_PROGRESS_CHANNEL_ID = "io.github.leonidius20.recorder.inprogress"
private const val REC_ABRUPT_STOP_CHANNEL_ID = "io.github.leonidius20.recorder.stopped"
private const val REC_STOPPED_LOW_BATTERY_OR_STORAGE_NOTIFICATION_ID = 1
private const val REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID = 2
const val PERSISTENT_NOTIFICATION_ID = 100

class RecordingNotificationsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * create a notification channel for the persistent notification that is
     * shown while the recording is in progress or paused
     */
    fun createRecInProgressNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Recording status"
            val descriptionText = "Shown while a recording is in progress or paused"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(REC_IN_PROGRESS_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    fun createPrematureStopNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Recording stopped abruptly"
            val descriptionText =
                "Sent if a recording was stopped because the device was running out of battery or storage"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(REC_ABRUPT_STOP_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    fun sendNotificationAboutPausingOnCall() {
        if (PermissionX.isGranted(context, PermissionX.permission.POST_NOTIFICATIONS)) {
            NotificationCompat.Builder(context, REC_ABRUPT_STOP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Recording paused")
                .setContentText("Incoming phone call")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(true)
                .build().also { notification ->
                    NotificationManagerCompat.from(context)
                        .notify(REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID, notification)
                }
        }
    }

    fun cancelPausedOnIncomingCallNotification() {
        NotificationManagerCompat.from(context).cancel(REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID)
    }

    fun sendAbruptStopNotification(explanation: String) {
// todo: can we somehow make it so that PermissionX check
        //  tells compiler that permission is granted and removes this highlight?
        if (PermissionX.isGranted(context, PermissionX.permission.POST_NOTIFICATIONS)) {
            NotificationCompat.Builder(context, REC_ABRUPT_STOP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Recording stopped")
                .setContentText(explanation)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(true)
                .build().also { notification ->
                    NotificationManagerCompat.from(context)
                        .notify(REC_STOPPED_LOW_BATTERY_OR_STORAGE_NOTIFICATION_ID, notification)
                }
        }
    }

    /**
     * should happen after toggling rec/pause or every second to update timer
     */
    fun updateNotification(
        state: RecordAudioUseCase.State,
        supportsPausing: Boolean,
    ) {
        NotificationManagerCompat.from(context).notify(
            PERSISTENT_NOTIFICATION_ID, buildPersistentNotification(state, supportsPausing)
        )
    }

    fun buildPersistentNotification(
        state: RecordAudioUseCase.State,
        supportsPausing: Boolean,
    ): Notification {
        val titleText = when (state) {
            is RecordAudioUseCase.State.Recording -> context.getString(R.string.notif_recording_in_progress)
            is RecordAudioUseCase.State.Paused -> context.getString(R.string.notif_recording_paused)
            else -> ""
        }

        val recPauseToggleActionText = when (state) {
            is RecordAudioUseCase.State.Recording -> context.getString(R.string.notif_action_pause)
            is RecordAudioUseCase.State.Paused -> context.getString(R.string.notif_action_resume)
            else -> ""
        }


        val notificationB = NotificationCompat.Builder(context, REC_IN_PROGRESS_CHANNEL_ID)
            // Create the notification to display while the service is running
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_microphone)
            .setContentTitle(titleText)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

        // todo: make it always once we re-implement recording with a lower-level api
        if (supportsPausing) {
            val toggleRecPauseIntent =
                Intent(RecordingControlBroadcastReceiver.ACTION_PAUSE_OR_RESUME)
            notificationB.addAction(
                R.drawable.ic_pause,
                recPauseToggleActionText,
                PendingIntent.getBroadcast(
                    context,
                    0,
                    toggleRecPauseIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        val stopIntent = Intent(RecordingControlBroadcastReceiver.ACTION_STOP)
        notificationB.addAction(
            R.drawable.ic_stop,
            context.getString(R.string.notif_action_stop),
            PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        )

        return notificationB.build()
    }

}
