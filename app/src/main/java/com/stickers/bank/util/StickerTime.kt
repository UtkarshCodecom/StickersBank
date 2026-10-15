package com.stickers.bank.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StickerTime {

    /** How many calendar days count as a "new arrival" (today + the 2 days before). */
    const val NEW_ARRIVAL_DAYS = 3

    /**
     * Cutoff for NEW ARRIVAL, snapped to midnight — the window is date-based, not
     * time-based, so the list only changes at 12am, never mid-day.
     */
    fun newArrivalCutoff(days: Int = NEW_ARRIVAL_DAYS): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(days - 1))
        }.timeInMillis

    /** e.g. "14 Jul, 3:45 PM" — the upload date AND time shown under each sticker. */
    fun formatDateTime(millis: Long): String =
        if (millis <= 0L) "" else SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(millis))
}
