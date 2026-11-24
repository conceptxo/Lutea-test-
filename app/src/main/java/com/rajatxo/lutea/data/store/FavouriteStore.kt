package com.rajatxo.lutea.data.store

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FavouriteStore — a tiny singleton holding the set of favourited song ids.
 * Persisted as one-per-line in internal storage. Used by the per-song menu's
 * "Fav" toggle and surfaced as a StateFlow so UI can react instantly.
 */
object FavouriteStore {

    private lateinit var appContext: Context
    private val file get() = java.io.File(appContext.filesDir, "favourites.txt")

    private val _favourites = MutableStateFlow<Set<Long>>(emptySet())
    val favourites: StateFlow<Set<Long>> = _favourites.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        load()
    }

    private fun load() {
        _favourites.value = try {
            if (file.exists()) {
                file.readLines()
                    .mapNotNull { it.trim().toLongOrNull() }
                    .toSet()
            } else emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun persist() {
        try {
            file.writeText(_favourites.value.joinToString("\n"))
        } catch (e: Exception) { /* silent fail */ }
    }

    fun isFavourite(songId: Long): Boolean = songId in _favourites.value

    fun toggle(songId: Long) {
        _favourites.value = if (songId in _favourites.value) {
            _favourites.value - songId
        } else {
            _favourites.value + songId
        }
        persist()
    }
}
