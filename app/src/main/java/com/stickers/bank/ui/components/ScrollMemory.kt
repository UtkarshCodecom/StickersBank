package com.stickers.bank.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/**
 * Scroll-state that survives navigating away and back.
 *
 * The hard part isn't *storing* the position — it's that when the screen is torn down on
 * navigation, the LazyList gets a final measure pass that clobbers its own
 * firstVisibleItemIndex back to 0. So any holder that reads the live state at dispose time
 * (rememberSaveable's Saver, a ViewModel, a singleton of the state object) faithfully records 0.
 *
 * Fix: keep the (index, offset) in an external map and only write to it while the screen is
 * actually RESUMED. As navigation starts, the destination's lifecycle drops below RESUMED, the
 * collector is cancelled, and the teardown's reset-to-0 is never recorded. On return we rebuild
 * the state seeded from the last good position.
 *
 * Every screen passes its own unique [key].
 */
private object ScrollPositions {
    val map = HashMap<String, Pair<Int, Int>>()
}

@Composable
fun rememberSavedListState(key: String): LazyListState {
    val state = remember(key) {
        val (index, offset) = ScrollPositions.map[key] ?: (0 to 0)
        LazyListState(index, offset)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(state, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
                .collect { ScrollPositions.map[key] = it }
        }
    }
    return state
}

@Composable
fun rememberSavedGridState(key: String): LazyGridState {
    val state = remember(key) {
        val (index, offset) = ScrollPositions.map[key] ?: (0 to 0)
        LazyGridState(index, offset)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(state, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
                .collect { ScrollPositions.map[key] = it }
        }
    }
    return state
}
