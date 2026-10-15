package com.stickers.bank.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Parse an admin-picked hex color ("#RRGGBB" or "#AARRGGBB", with or without the leading #).
 * Returns null for blank/garbage so callers can fall back to their own default.
 */
fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching {
        val clean = hex.removePrefix("#")
        val argb = when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            else -> return null
        }
        Color(argb.toLong(16).toInt())
    }.getOrNull()
}

/** Inverse of [parseHexColor] — "#RRGGBB", dropping alpha (templates store opaque swatches). */
fun colorToHex(color: Color): String {
    val rgb = color.toArgb() and 0xFFFFFF
    return "#" + rgb.toString(16).padStart(6, '0').uppercase()
}
