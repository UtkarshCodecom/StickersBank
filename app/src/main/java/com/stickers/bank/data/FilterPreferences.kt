package com.stickers.bank.data

import android.content.Context
import com.stickers.bank.ui.allstickers.FilterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the shared browse/search filter used by BOTH the Featured search and NEW ARRIVAL,
 * persisted across restarts. Exposed as a StateFlow so the top-bar sort button and the
 * screens all stay in sync.
 */
class FilterPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("sb_filters", Context.MODE_PRIVATE)

    private val _filter = MutableStateFlow(load())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    fun setFilter(state: FilterState) {
        _filter.value = state
        runCatching {
            prefs.edit()
                .putString(KEY_FILTER, AppJson.encodeToString(FilterState.serializer(), state))
                .apply()
        }
    }

    private fun load(): FilterState = runCatching {
        prefs.getString(KEY_FILTER, null)
            ?.let { AppJson.decodeFromString(FilterState.serializer(), it) }
    }.getOrNull() ?: FilterState()

    private companion object {
        const val KEY_FILTER = "browse_filter"
    }
}
