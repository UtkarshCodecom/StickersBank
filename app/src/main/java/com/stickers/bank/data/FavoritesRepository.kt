package com.stickers.bank.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sticker ids the user marked as favorite — device-local only (SharedPreferences), no cloud writes. */
class FavoritesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("sb_favorites", Context.MODE_PRIVATE)

    private val _ids = MutableStateFlow(load())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun isFavorite(id: String): Boolean = id in _ids.value

    fun toggle(id: String) {
        val next = _ids.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _ids.value = next
        runCatching { prefs.edit().putStringSet(KEY_IDS, next).apply() }
    }

    private fun load(): Set<String> = prefs.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()

    private companion object {
        const val KEY_IDS = "favorite_ids"
    }
}
