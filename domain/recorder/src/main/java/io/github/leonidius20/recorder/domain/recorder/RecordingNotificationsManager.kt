package io.github.leonidius20.recorder.domain.recorder

interface RecordingNotificationsManager {

    fun sendNotificationAboutPausingOnCall()

    fun cancelPausedOnIncomingCallNotification()
    
    fun sendAbruptStopNotification(explanation: String)

    /**
     * should happen after toggling rec/pause or every second to update timer
     */
    fun updateNotification(
        state: RecordingState,
        supportsPausing: Boolean,
    )
}
