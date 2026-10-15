package com.stickers.bank.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stickers.bank.data.model.PageBackground

/**
 * Where a page background actually gets painted: at the very root of the app, BEHIND the status
 * bar, the wordmark row and everything else, so an admin-set image covers the whole screen edge
 * to edge instead of only the content area under the top bar.
 *
 * Tab backgrounds come straight from the route (see `pageBackgroundKeyFor`). A screen with its
 * own background that isn't route-derived — a category, with the backdrop set on its card —
 * pushes it here via [ProvidePageBackdrop] for as long as it's on screen.
 */
object PageBackdropHost {
    /** Set by the current screen; wins over the route's own background while non-null. */
    var override by mutableStateOf<PageBackground?>(null)
}

/** Publishes [bg] as the app-wide backdrop for as long as this composable is in the tree. */
@Composable
fun ProvidePageBackdrop(bg: PageBackground?) {
    DisposableEffect(bg) {
        PageBackdropHost.override = bg
        onDispose { PageBackdropHost.override = null }
    }
}
