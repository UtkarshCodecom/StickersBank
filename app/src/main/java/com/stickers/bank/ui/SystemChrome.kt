package com.stickers.bank.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lets a screen ask MainActivity to paint white behind the status bar (with dark icons) instead
 * of the app's usual dark backdrop collage — used by screens with their own light theme (the
 * canvas template editor), where the dark collage peeking through the status bar looks broken
 * against a white top bar. Screens flip this on for their lifetime via DisposableEffect and
 * flip it back off on the way out.
 */
object SystemChrome {
    var lightStatusBar by mutableStateOf(false)
}
