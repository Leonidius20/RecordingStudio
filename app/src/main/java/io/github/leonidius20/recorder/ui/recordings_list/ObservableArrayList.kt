package io.github.leonidius20.recorder.ui.recordings_list

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.collections.ArrayList

class ObservableArrayList<T> {

    val list: ArrayList<T>

    constructor(initialCapacity: Int = 0) {
        list = ArrayList(initialCapacity)
    }

    constructor(collection: Collection<T>) {
        list = ArrayList(collection)
    }

    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event>
        get() = _events

    sealed interface Event

    inner class ItemAdded(val position: Int, item: T): Event

    data class ItemRemoved(val position: Int): Event

    suspend fun addFirst(item: T) {
        list.add(0, item)
        _events.emit(ItemAdded(position = 0, item))
    }

    suspend fun addLast(item: T) {
        list.add(item)
        _events.emit(ItemAdded(position = list.size - 1, item))
    }

    suspend fun removeAt(position: Int) {
        list.removeAt(position)
        _events.emit(ItemRemoved(position))
    }

    operator fun get(index: Int) = list[index]

    fun <V> map(transform: (T) -> V): List<V> {
        return list.map(transform)
    }

    fun forEachIndexed(action: (Int, T) -> Unit) {
        list.forEachIndexed(action)
    }

}