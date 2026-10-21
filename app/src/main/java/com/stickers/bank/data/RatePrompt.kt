package com.stickers.bank.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * When to invite the user to rate the app on the Play Store: right after they've sent stickers
 * out — the moment the app has just done its job for them. First time after the first send;
 * after "Later", again only once they've sent [SENDS_BETWEEN_ASKS] more and [DAYS_BETWEEN_ASKS]
 * have passed. Never again once they've tapped "Rate now".
 */
class RatePrompt(context: Context) {
    private val prefs = context.getSharedPreferences("rate_prompt", Context.MODE_PRIVATE)

    private val _pending = MutableStateFlow(false)
    /** True when the next return to the app should show the prompt. */
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    fun stickersSent() {
        if (prefs.getBoolean(KEY_DONE, false)) return
        val sends = prefs.getInt(KEY_SENDS, 0) + 1
        prefs.edit().putInt(KEY_SENDS, sends).apply()
        val lastAsked = prefs.getLong(KEY_LAST_ASKED, 0L)
        val sendsAtLastAsk = prefs.getInt(KEY_SENDS_AT_LAST_ASK, 0)
        val due = lastAsked == 0L ||
            (sends - sendsAtLastAsk >= SENDS_BETWEEN_ASKS &&
                System.currentTimeMillis() - lastAsked >= DAYS_BETWEEN_ASKS * 24 * 60 * 60 * 1000L)
        if (due) _pending.value = true
    }

    /** The prompt was shown; [done] = "Rate now" was tapped (never show it again). */
    fun answered(done: Boolean) {
        _pending.value = false
        prefs.edit()
            .putLong(KEY_LAST_ASKED, System.currentTimeMillis())
            .putInt(KEY_SENDS_AT_LAST_ASK, prefs.getInt(KEY_SENDS, 0))
            .putBoolean(KEY_DONE, done || prefs.getBoolean(KEY_DONE, false))
            .apply()
    }

    private companion object {
        const val SENDS_BETWEEN_ASKS = 3
        const val DAYS_BETWEEN_ASKS = 2
        const val KEY_SENDS = "sends"
        const val KEY_LAST_ASKED = "last_asked"
        const val KEY_SENDS_AT_LAST_ASK = "sends_at_last_ask"
        const val KEY_DONE = "done"
    }
}
