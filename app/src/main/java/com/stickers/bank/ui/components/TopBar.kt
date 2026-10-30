package com.stickers.bank.ui.components

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickers.bank.admin.AdminActivity
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.TopTab
import com.stickers.bank.ui.featured.FeaturedSearch
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.Danger
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised

/** Whether long-pressing the logo opens the admin panel. Off in every build; set to true (or to
 * BuildConfig.DEBUG for debug builds only) to bring it back. */
private const val ADMIN_LONG_PRESS = false

/**
 * Top header = wordmark + filter (long-press on the wordmark opens admin only if
 * ADMIN_LONG_PRESS is on). The tab menu now floats at the bottom.
 * On Featured (showSearch), the row is always: wordmark (fixed 40% width, left) → search bar
 * (fills whatever's left, search icon INSIDE it on the left) → filter icon — the search bar is
 * just there from the start, plain and always usable, not something you have to tap an icon
 * to reveal, and not auto-focused/keyboard-popped on its own either — it behaves like any
 * ordinary text field. Pressing system Back while it's focused clears focus first (so the
 * blinking cursor doesn't linger once the keyboard's gone) instead of leaving the screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SbHomeTopBar(
    showFilter: Boolean = false,
    filterActiveCount: Int = 0,
    onOpenFilter: () -> Unit = {},
    showSearch: Boolean = false,
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var searchFocused by remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = searchFocused) { focusManager.clearFocus() }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp)) {
        if (showSearch) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sized to its content, capped so the search bar always keeps a usable width.
                Row(
                    Modifier
                        .widthIn(max = (LocalConfiguration.current.screenWidthDp * 0.48f).dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            // Long-press the logo → the PIN-gated admin panel.
                            // Long-press the logo → the PIN-gated admin panel: switched off (see
                            // ADMIN_LONG_PRESS); a long-press does nothing in any build.
                            onLongClick = if (ADMIN_LONG_PRESS) ({ context.startActivity(Intent(context, AdminActivity::class.java)) }) else null,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppLogo(34.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "STICKERS BANK",
                        color = Gold,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                SbSearchBar(
                    value = FeaturedSearch.query,
                    onChange = { FeaturedSearch.query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Search",
                    showLeadingIcon = true,
                    onFocusChanged = { searchFocused = it; FeaturedSearch.focused = it },
                )
                if (showFilter) {
                    Spacer(Modifier.width(6.dp))
                    FilterIcon(filterActiveCount, onOpenFilter)
                }
            }
        } else {
            // Keep the wordmark centred: reserve matching space on the left for the right-side icons.
            val rightIcons = if (showFilter) 1 else 0
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width((rightIcons * 32).dp))
                Row(
                    Modifier
                        .weight(1f)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = if (ADMIN_LONG_PRESS) ({ context.startActivity(Intent(context, AdminActivity::class.java)) }) else null,
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppLogo(38.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "STICKERS BANK",
                        color = Gold,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        maxLines = 1,
                    )
                }
                if (showFilter) FilterIcon(filterActiveCount, onOpenFilter)
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

/**
 * The round sticker mark from the launcher icon. Its foreground layer carries the adaptive-icon
 * safe-zone margin (the art fills only ~68% of it), so it's drawn scaled up past [size] — the
 * visible disc ends up [size] across while the layout slot stays [size] too.
 */
@Composable
private fun AppLogo(size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(com.stickers.bank.R.mipmap.ic_launcher_foreground),
        contentDescription = null,
        modifier = Modifier.size(size).graphicsLayer { scaleX = 1.47f; scaleY = 1.47f },
    )
}

@Composable
private fun FilterIcon(filterActiveCount: Int, onOpenFilter: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Icon(
            Icons.Rounded.Tune, "Sort & filter",
            tint = if (filterActiveCount > 0) Gold else AppOnSurface,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenFilter).padding(horizontal = 8.dp).padding(vertical = 3.dp).size(22.dp),
        )
        if (filterActiveCount > 0) {
            Box(
                Modifier.size(14.dp).clip(CircleShape).background(Danger).padding(bottom = 2.dp),
                contentAlignment = Alignment.TopCenter,
            ) { Text("$filterActiveCount", color = androidx.compose.ui.graphics.Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
        }
    }
}

/**
 * Floating bottom navigation. The active tab expands to show its icon AND label; the others
 * are icon-only. The bar itself is a raised neumorphic pill — an embossed surface floating
 * above the flat page, instead of the old glassy-dark-pill-with-a-gold-glow look (that glow was
 * built for the dark-collage backdrop, which neumorphism doesn't use).
 */
/** How much of the screen bottom [SbBottomNav] covers (pill + its outer padding). Anything pinned
 * to the bottom of a root tab has to sit at least this high, or it ends up hidden behind it. */
val SbBottomNavClearance = 88.dp

@Composable
fun SbBottomNav(current: String, onTabSelected: (TopTab) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .neuRaised(cornerRadius = 30.dp, elevation = 9.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopTab.entries.forEach { tab -> AnimatedTabItem(tab, current == tab.route, onTabSelected) }
        }
    }
}

@Composable
private fun AnimatedTabItem(tab: TopTab, selected: Boolean, onTabSelected: (TopTab) -> Unit) {
    // One full turn each time this tab becomes the active one. An Animatable spun from wherever
    // it currently sits (rather than a 0↔360 target) so a fast double-tap keeps turning forwards
    // instead of snapping back, and a tab going inactive just stops where it is.
    val spin = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected) spin.animateTo(spin.value + 360f, tween(300, easing = FastOutSlowInEasing))
    }

    // Create is the bar's one primary action, not another destination — it stays a neumorphic
    // circle (pressed in while it's the open tab) instead of expanding into a labelled pill.
    if (tab == TopTab.Create) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.height(56.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .then(
                        if (selected) Modifier.neuInset(cornerRadius = 22.dp, elevation = 5.dp)
                        else Modifier.neuRaised(cornerRadius = 22.dp, elevation = 5.dp),
                    )
                    .clip(CircleShape)
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconFor(tab.route), tab.short,
                    tint = if (selected) Gold else AppOnSurfaceMuted,
                    modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = spin.value },
                )
            }
        }
        return
    }

    val bg by androidx.compose.animation.animateColorAsState(
        if (selected) Gold else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(220), label = "tabBg",
    )
    val iconTint by androidx.compose.animation.animateColorAsState(
        if (selected) AppBackground else AppOnSurfaceMuted,
        animationSpec = androidx.compose.animation.core.tween(220), label = "tabIconTint",
    )
    val iconSize by androidx.compose.animation.core.animateDpAsState(
        if (selected) 19.dp else 23.dp,
        animationSpec = androidx.compose.animation.core.tween(220), label = "tabIconSize",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.height(56.dp)) {
        Row(
            Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(bg)
                .clickable { onTabSelected(tab) }
                .animateContentSize(androidx.compose.animation.core.tween(220))
                // Seven tabs now: tight enough that even "Fresh Drops" open fits a 360dp phone.
                .padding(horizontal = if (selected) 13.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tab == TopTab.CanvasTemplates) {
                CanvasMonogram(
                    iconTint, iconSize,
                    Modifier.graphicsLayer { rotationZ = spin.value },
                )
            } else {
                Icon(
                    iconFor(tab.route), tab.short, tint = iconTint,
                    modifier = Modifier.size(iconSize).graphicsLayer { rotationZ = spin.value },
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) +
                    androidx.compose.animation.expandHorizontally(androidx.compose.animation.core.tween(220)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) +
                    androidx.compose.animation.shrinkHorizontally(androidx.compose.animation.core.tween(200)),
            ) {
                Text(
                    tab.short, color = OnGold, fontSize = 12.sp, fontWeight = FontWeight.Black,
                    maxLines = 1, modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}

/**
 * The Canvas tab's mark: a bold "C" set inside a ring. Material ships no canvas/artboard glyph,
 * and the Dashboard grid this replaced read as "tiles" rather than "canvas" — a monogram is the
 * section's own badge instead of an unrelated icon borrowed for its shape. Everything scales off
 * [size] so it keeps its proportions through the selected/unselected size animation.
 */
@Composable
private fun CanvasMonogram(tint: Color, size: Dp, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Box(
        modifier.size(size).border(width = size * 0.085f, color = tint, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "C",
            color = tint,
            fontSize = with(density) { (size * 0.68f).toSp() },
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            // Font padding off, otherwise the glyph's own leading pushes the C off-centre inside
            // a ring this small and it reads as sitting low.
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
    }
}

private fun iconFor(route: String): ImageVector = when (route) {
    Routes.FEATURED -> Icons.Rounded.Home
    Routes.NEW_ARRIVAL -> Icons.Rounded.Whatshot
    Routes.FAVORITES -> Icons.Rounded.Favorite
    Routes.CREATE -> Icons.Rounded.Add
    Routes.BANK -> Icons.Rounded.Bookmarks
    Routes.STATUS -> Icons.Rounded.DonutLarge // reads like WhatsApp's status ring
    Routes.SETTINGS -> Icons.Rounded.Settings
    else -> Icons.Rounded.Home
}
