package io.github.leonidius20.recorder.ui.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

class LiteImportFileSettingHandler @Inject constructor() : ImportFileSettingHandler() {

    override fun launchImport() {
        // nothing
    }

    override fun register() {
        // nothing
    }

}

@Module
@InstallIn(FragmentComponent::class)
abstract class LiteSettingsModule {

    @FragmentScoped
    @Binds
    abstract fun bindImportHandler(
        handler: LiteImportFileSettingHandler,
    ) : ImportFileSettingHandler

}
