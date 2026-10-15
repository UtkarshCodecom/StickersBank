package com.stickers.bank.data

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Anonymous usage counters for the web admin's Analytics page — how often each tab/screen is
 * opened, which categories/stickers/templates get looked at, how many stickers get sent out, what
 * people search for. Counts only: no user ids, no device ids, nothing per person.
 *
 * Counts pile up in memory and go out as Firestore increments every [FLUSH_EVERY_MS] (and when
 * the app goes to the background — see [flush]), so a busy session costs a couple of writes, not
 * one per tap. Two docs per day, keyed by the device's local date:
 *  - `analyticsDaily/{yyyy-MM-dd}`  — totals, plus maps of tab/screen/category/template/search counts
 *  - `analyticsStickers/{yyyy-MM-dd}` — `views` and `dumps` maps keyed by sticker id (kept apart
 *    because these grow with the catalog, while the daily doc stays small)
 */
class Analytics(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("analytics", Context.MODE_PRIVATE)

    // "group" -> key -> count; group "" = a top-level total. Guarded by `lock`.
    private val lock = Any()
    private var daily = HashMap<String, HashMap<String, Long>>()
    private var stickers = HashMap<String, HashMap<String, Long>>()

    init {
        scope.launch {
            while (true) {
                delay(FLUSH_EVERY_MS)
                runCatching { flush() }
            }
        }
    }

    // ---- What gets counted ----

    /** A cold start. Also counts the device once per day (and once ever, as a new install). */
    fun appOpened() {
        add(daily, "", "appOpens")
        val today = today()
        if (prefs.getString(KEY_LAST_ACTIVE_DAY, null) != today) {
            add(daily, "", "activeDevices")
            if (!prefs.getBoolean(KEY_SEEN_BEFORE, false)) add(daily, "", "newInstalls")
            prefs.edit().putString(KEY_LAST_ACTIVE_DAY, today).putBoolean(KEY_SEEN_BEFORE, true).apply()
        }
    }

    /** A bottom-nav tab, or any other screen, became the current one. */
    fun screenOpened(name: String) = add(daily, if (name in TABS) "tabs" else "screens", name)

    fun categoryOpened(categoryId: String) = add(daily, "categories", categoryId)

    fun templateOpened(templateId: String) = add(daily, "templates", templateId)

    fun stickerViewed(stickerId: String) {
        add(daily, "", "stickerViews")
        add(stickers, "views", stickerId)
    }

    /** Stickers handed to another app. [via]: whatsapp, whatsappSingle, telegram, gif, share. */
    fun stickersSent(via: String, stickerIds: List<String>) {
        if (stickerIds.isEmpty()) return
        add(daily, "dumps", via)
        add(daily, "", "stickersDumped", stickerIds.size.toLong())
        stickerIds.forEach { add(stickers, "dumps", it) }
    }

    /** A WhatsApp status saved to the gallery from the Status tab. */
    fun statusSaved() = add(daily, "", "statusSaves")

    fun searched(term: String) {
        val clean = term.trim().lowercase().take(40).trim('_')
        if (clean.length < 2) return
        add(daily, "", "searches")
        add(daily, "searchTerms", clean)
    }

    // ---- Plumbing ----

    private fun add(into: HashMap<String, HashMap<String, Long>>, group: String, key: String, by: Long = 1) {
        if (key.isBlank()) return
        synchronized(lock) {
            val g = into.getOrPut(group) { HashMap() }
            g[key] = (g[key] ?: 0L) + by
        }
    }

    /** Sends everything counted so far. Safe to call any time; a no-op when nothing's pending. */
    fun flush() {
        val (d, s) = synchronized(lock) {
            if (daily.isEmpty() && stickers.isEmpty()) return
            val pending = daily to stickers
            daily = HashMap(); stickers = HashMap()
            pending
        }
        val day = today()
        // A failed write (offline, rules, a key Firestore won't take) just drops those counts —
        // this is best-effort, retrying could double-count if the write actually landed, and it
        // must never crash the app (this runs from onStop and a background loop).
        runCatching {
            val batch = db.batch()
            if (d.isNotEmpty()) batch.set(db.collection("analyticsDaily").document(day), increments(d), SetOptions.merge())
            if (s.isNotEmpty()) batch.set(db.collection("analyticsStickers").document(day), increments(s), SetOptions.merge())
            batch.commit()
        }
    }

    /** Nested maps of increments — set+merge treats a map key literally (an id or a search term
     * containing a "." would otherwise be read as a field path). */
    private fun increments(groups: Map<String, Map<String, Long>>): Map<String, Any> = buildMap {
        groups.forEach { (group, counts) ->
            if (group.isEmpty()) counts.forEach { (k, v) -> put(k, FieldValue.increment(v)) }
            else put(group, counts.mapValues { FieldValue.increment(it.value) })
        }
    }

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private companion object {
        const val FLUSH_EVERY_MS = 30_000L
        const val KEY_LAST_ACTIVE_DAY = "last_active_day"
        const val KEY_SEEN_BEFORE = "seen_before"
        val TABS = setOf("vibe", "canvas", "freshDrops", "bank", "status", "settings")
    }
}
