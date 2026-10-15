package com.stickers.bank.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickers.bank.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SheetBg = Color(0xFFEEEAF4)
private val Ink = Color(0xFF1C1C1C)
private val Accent = Color(0xFF7740E3)

/**
 * One-time welcome bottom sheet — slides up from the bottom on first launch.
 * Drag it down or tap the scrim to dismiss. Never shown again (tracked via
 * [com.stickers.bank.data.OnboardingPreferences]).
 */
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scrimAlpha = remember { Animatable(0f) }
    val sheetOffsetFraction = remember { Animatable(1f) }
    var dragDelta by remember { mutableFloatStateOf(0f) }

    // Slide in on first composition.
    LaunchedEffect(Unit) {
        delay(80)
        coroutineScope {
            launch { scrimAlpha.animateTo(0.55f, tween(280)) }
            launch {
                sheetOffsetFraction.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                )
            }
        }
    }

    fun dismiss() {
        scope.launch {
            coroutineScope {
                launch { scrimAlpha.animateTo(0f, tween(220)) }
                launch { sheetOffsetFraction.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) }
            }
            onContinue()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Scrim — tap to dismiss.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha.value }
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::dismiss,
                ),
        )

        // Sheet — 520dp tall to show content without needing scroll.
        val sheetHeight = 520
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Raw drag: positive delta = finger moving down.
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (sheetOffsetFraction.value == 0f) {
                            // Only resist upward drag (negative delta); allow free downward movement.
                            dragDelta = (dragDelta + delta).coerceAtLeast(0f)
                        }
                    },
                    onDragStopped = { velocity ->
                        // Fling or threshold — either way dismiss; snap back if barely moved.
                        if (dragDelta > sheetHeight * 0.28f || velocity > 800f) {
                            dragDelta = 0f
                            dismiss()
                        } else {
                            scope.launch {
                                // Snap back with a gentle spring.
                                val start = dragDelta
                                val anim = Animatable(start)
                                anim.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                                dragDelta = 0f
                            }
                        }
                    },
                )
                .graphicsLayer {
                    translationY = (sheetOffsetFraction.value * sheetHeight.dp.toPx()) + dragDelta
                }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(SheetBg),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Drag handle pill.
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.35f)),
                )
                Spacer(Modifier.height(32.dp))

                // The SB sticker mark. The launcher foreground carries the adaptive-icon safe-zone
                // margin (art fills ~68% of it), so it's scaled up to make the disc itself 72dp.
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = "Stickers Bank",
                    modifier = Modifier.size(72.dp).graphicsLayer { scaleX = 1.47f; scaleY = 1.47f },
                )
                Spacer(Modifier.height(24.dp))

                Text(
                    "Your Sticker World Starts Here! 🚀",
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Default,
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    buildAnnotatedString {
                        append("Discover, create & share stickers that ")
                        withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Medium)) {
                            append("match your vibe")
                        }
                        append(".\nFrom LOLs to love — make every chat more expressive.")
                    },
                    color = Ink.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Default,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    buildAnnotatedString {
                        append("Your stickers. ")
                        withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.Medium)) {
                            append("Your vibe.")
                        }
                        append(" Your chat. 💜")
                    },
                    color = Ink.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Default,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = ::dismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) {
                    Text(
                        "EXPLORE THE VIBE →",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "swipe down to dismiss",
                    color = Ink.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
