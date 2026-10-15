package com.stickers.bank.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The heading/display face — Bangers, a bold comic/graffiti caps font. Used for big headers
 * (editor title, hero, "ADD A PHOTO"). Swap the asset path (e.g. Knewave-Regular) to change
 * every display header at once.
 */
@Composable
fun rememberDisplayFont(): FontFamily {
    val assets = LocalContext.current.assets
    return remember { FontFamily(Font("fonts/Bangers-Regular.ttf", assets)) }
}

/** The body face — Poppins (Regular/Medium/SemiBold/Bold). Applied app-wide via the theme. */
@Composable
fun rememberBodyFont(): FontFamily {
    val assets = LocalContext.current.assets
    return remember {
        FontFamily(
            Font("fonts/Poppins-Regular.ttf", assets, FontWeight.Normal),
            Font("fonts/Poppins-Medium.ttf", assets, FontWeight.Medium),
            Font("fonts/Poppins-SemiBold.ttf", assets, FontWeight.SemiBold),
            Font("fonts/Poppins-Bold.ttf", assets, FontWeight.Bold),
        )
    }
}

private val base = Typography()

// The reference app uses a rounded, bold, wide-tracked display style for headers.
val StickersTypography = base.copy(
    titleLarge = base.titleLarge.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = 3.sp,
    ),
    titleMedium = base.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
    ),
    labelLarge = base.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    ),
)

val HeaderStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    letterSpacing = 4.sp,
    fontSize = 20.sp,
)

/** Apply one font family to every style in a Typography (used to make Material text Poppins). */
fun Typography.applyFontFamily(ff: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = ff),
    displayMedium = displayMedium.copy(fontFamily = ff),
    displaySmall = displaySmall.copy(fontFamily = ff),
    headlineLarge = headlineLarge.copy(fontFamily = ff),
    headlineMedium = headlineMedium.copy(fontFamily = ff),
    headlineSmall = headlineSmall.copy(fontFamily = ff),
    titleLarge = titleLarge.copy(fontFamily = ff),
    titleMedium = titleMedium.copy(fontFamily = ff),
    titleSmall = titleSmall.copy(fontFamily = ff),
    bodyLarge = bodyLarge.copy(fontFamily = ff),
    bodyMedium = bodyMedium.copy(fontFamily = ff),
    bodySmall = bodySmall.copy(fontFamily = ff),
    labelLarge = labelLarge.copy(fontFamily = ff),
    labelMedium = labelMedium.copy(fontFamily = ff),
    labelSmall = labelSmall.copy(fontFamily = ff),
)
