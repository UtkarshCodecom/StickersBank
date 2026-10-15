package com.stickers.bank.data

import android.content.Context
import com.stickers.bank.ui.theme.NeoPalettes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists which [com.stickers.bank.ui.theme.NeoPalette] the user picked in Settings, so the
 * neumorphic theme survives restarts. Same SharedPreferences + StateFlow shape as
 * [FilterPreferences] — nothing here needs DataStore.
 */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("sb_theme", Context.MODE_PRIVATE)

    private val _themeId = MutableStateFlow(prefs.getString(KEY_THEME_ID, null) ?: NeoPalettes.Classic.id)
    val themeId: StateFlow<String> = _themeId.asStateFlow()

    fun setThemeId(id: String) {
        _themeId.value = id
        runCatching { prefs.edit().putString(KEY_THEME_ID, id).apply() }
    }

    private companion object {
        const val KEY_THEME_ID = "theme_id"
    }
}
