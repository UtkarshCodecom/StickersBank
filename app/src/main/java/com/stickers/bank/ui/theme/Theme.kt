package com.stickers.bank.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.stickers.bank.ui.SystemChrome

@Composable
fun StickersBankTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    // Read during composition (not inside the effect) so picking a new theme in Settings, or a
    // screen flipping SystemChrome, recomposes this and re-runs the SideEffect below with the
    // new values, instead of writers racing over the same window flags.
    val p = Neo.current
    val lightStatusBarOverride = SystemChrome.lightStatusBar
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            // A screen can force dark icons (the canvas template editor, always-white chrome)
            // regardless of the active palette; otherwise the icons just follow it.
            insets.isAppearanceLightStatusBars = if (lightStatusBarOverride) true else !p.isDark
            insets.isAppearanceLightNavigationBars = !p.isDark
        }
    }
    val poppins = rememberBodyFont()
    val typography = remember(poppins) { Typography().applyFontFamily(poppins) }
    val colorScheme = remember(p) {
        if (p.isDark) {
            darkColorScheme(
                primary = p.primary, onPrimary = p.onPrimary,
                background = p.bg, onBackground = p.text,
                surface = p.surface, onSurface = p.text,
                surfaceVariant = p.bg, onSurfaceVariant = p.muted,
                error = p.danger,
                secondaryContainer = p.bg, onSecondaryContainer = p.text,
                outline = p.dark,
            )
        } else {
            lightColorScheme(
                primary = p.primary, onPrimary = p.onPrimary,
                background = p.bg, onBackground = p.text,
                surface = p.surface, onSurface = p.text,
                surfaceVariant = p.bg, onSurfaceVariant = p.muted,
                error = p.danger,
                secondaryContainer = p.bg, onSecondaryContainer = p.text,
                outline = p.dark,
            )
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = typography) {
        // Poppins as the default face for every plain Text (headers override with Bangers).
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = poppins)) {
            content()
        }
    }
}
