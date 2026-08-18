package io.github.leonidius20.recorder.ui.settings

import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped
import dagger.hilt.components.SingletonComponent
import io.github.leonidius20.recorder.data.import_file.NativeBridge
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

class FullImportFileSettingHandler @Inject constructor(
    val nativeBridge: Provider<NativeBridge>,
    val recordingsListRepository: RecordingsListRepository,
) : ImportFileSettingHandler() {

    lateinit var launcher: ActivityResultLauncher<Array<String>>

    override fun launchImport() {
        launcher.launch(arrayOf("audio/*"))
    }

    override fun register() = with(fragment) {
        launcher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { selectedUri ->
                if (selectedUri == null) return@registerForActivityResult
                val resolver = requireContext().contentResolver
                val mime = resolver.getType(selectedUri) ?: "audio/ogg"
                val copyUri = recordingsListRepository.createRecordingFile("imported_file", mime)

                val sourceFd = resolver.openFileDescriptor(selectedUri, "r")!!
                val destFd = resolver.openFileDescriptor(copyUri, "w")!!

                try {
                    val result = nativeBridge.get().copyFile(sourceFd.fd, destFd.fd)
                    Toast.makeText(
                        requireContext(),
                        if (result) "success" else "fail",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (t: Throwable) {
                    t.printStackTrace() // todo: replace with timber or crash reporting
                    Toast.makeText(requireContext(), "fail (error)", Toast.LENGTH_SHORT).show()
                } finally {
                    sourceFd.close()
                    destFd.close()
                }

                //val bytesCopied = resolver.openOutputStream(copyUri, "rw")!!.use { out ->
                //    resolver.openInputStream(selectedUri)!!.use { input ->
                //        input.copyTo(out)
                //    }
                //}
                //resolver.update(copyUri, ContentValues().apply {
                //    put(MediaStore.MediaColumns.SIZE, bytesCopied)
                //}, null, null)

            }
    }

}

@Module
@InstallIn(FragmentComponent::class)
abstract class FullSettingsModule {

    @FragmentScoped
    @Binds
    abstract fun bindImportHandler(
        handler: FullImportFileSettingHandler,
    ) : ImportFileSettingHandler

}

@Module
@InstallIn(SingletonComponent::class)
object NativeModule {

    @Singleton
    @Provides
    fun provideNativeBridge() = NativeBridge()

}
