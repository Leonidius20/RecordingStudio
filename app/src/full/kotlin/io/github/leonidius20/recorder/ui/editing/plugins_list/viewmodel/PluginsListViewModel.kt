package io.github.leonidius20.recorder.ui.editing.plugins_list.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.data.plugins.PluginsRepository
import javax.inject.Inject

@HiltViewModel
class PluginsListViewModel @Inject constructor(
    private val repo: PluginsRepository,
) : ViewModel() {

    fun get() = repo.list

}