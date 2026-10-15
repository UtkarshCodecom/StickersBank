package com.stickers.bank.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Remembers whether the one-time welcome letter has been shown. Same SharedPreferences +
 * StateFlow shape as [ThemePreferences].
 */
class OnboardingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("sb_onboarding", Context.MODE_PRIVATE)

    private val _seenWelcome = MutableStateFlow(prefs.getBoolean(KEY_SEEN_WELCOME, false))
    val seenWelcome: StateFlow<Boolean> = _seenWelcome.asStateFlow()

    fun markWelcomeSeen() {
        _seenWelcome.value = true
        runCatching { prefs.edit().putBoolean(KEY_SEEN_WELCOME, true).apply() }
    }

    /** Lets the welcome letter be replayed from Settings. */
    fun resetWelcome() {
        _seenWelcome.value = false
        runCatching { prefs.edit().putBoolean(KEY_SEEN_WELCOME, false).apply() }
    }

    private companion object {
        const val KEY_SEEN_WELCOME = "seen_welcome"
    }
}
