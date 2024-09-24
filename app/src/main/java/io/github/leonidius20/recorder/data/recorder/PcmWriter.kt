package io.github.leonidius20.recorder.data.recorder

import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@ServiceScoped
class PcmWriter @Inject constructor(
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
    private val audioReceiver: AudioReceiver,
) {

    fun CoroutineScope.go(desc: ParcelFileDescriptor) {
        val outStream = FileOutputStream(desc.fileDescriptor)

        val job = this.launch(ioDispatcher) {
            while (this.isActive) {
                val bytes = audioReceiver.channel.receive()
                outStream.write(bytes)
            }

            // TODO: on channel cancelling - close stream

        }

    }

}