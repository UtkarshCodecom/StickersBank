package com.stickers.bank.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.components.StickerThumb
import com.stickers.bank.ui.detail.StickerDetailSheet
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.Neo
import com.stickers.bank.ui.theme.NeoPalette
import com.stickers.bank.ui.theme.NeoPalettes
import com.stickers.bank.ui.theme.neuRaised

// Placeholders — change these to your real links/email.
private const val SUPPORT_EMAIL = "support@stickersbank.app"
private const val PRIVACY_URL = "https://stickersbank.app/privacy"

@Composable
fun SettingsScreen(nav: NavHostController) {
    val context = LocalContext.current
    val custom by ServiceLocator.customStickerRepository.stickers.collectAsStateSafe()
    val ordered = remember(custom) { custom.reversed() }
    val favoriteIds by ServiceLocator.favoritesRepository.ids.collectAsStateSafe()
    var detail by remember { mutableStateOf<Sticker?>(null) }

    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {

            SectionLabel("THEME")
            ThemeRow()

            SectionLabel("MY STICKERS")
            // My Stickers and Favorites are the same kind of thing — a place to open — so they
            // render as identical blocks via one shared row. My Stickers used to add a preview
            // strip above its block, which made the pair look mismatched.
            LibraryRow(
                icon = Icons.Rounded.Folder,
                title = "My Stickers folder",
                subtitle = "${ordered.size} sticker${if (ordered.size == 1) "" else "s"} · open to see names, bank or export",
            ) { nav.navigate(Routes.MY_STICKERS) }
            // Favorites moved here (used to be its own bottom-nav tab) — one fewer tab to
            // squeeze into the floating nav, same reach via Settings instead.
            LibraryRow(
                icon = Icons.Rounded.Favorite,
                title = "Favorites",
                subtitle = "${favoriteIds.size} sticker${if (favoriteIds.size == 1) "" else "s"} you've favorited",
            ) { nav.navigate(Routes.FAVORITES) }

            SectionLabel("FAQ")
            FAQ.forEach { (q, a) -> FaqItem(q, a) }

            SectionLabel("SUPPORT")
            SettingRow("Contact support", Icons.Rounded.MailOutline) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Stickers Bank support")
                        },
                    )
                }
            }
            SettingRow("Rate the app", Icons.Rounded.StarRate) {
                val id = context.packageName
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))) }
                    .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))) }
            }
            SettingRow("Share the app", Icons.Rounded.Share) {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Get Stickers Bank: https://play.google.com/store/apps/details?id=${context.packageName}")
                        },
                        "Share",
                    ),
                )
            }
            SettingRow("Privacy policy", Icons.Rounded.PrivacyTip) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))) }
            }

            SectionLabel("ABOUT")
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("Version", color = AppOnSurface, modifier = Modifier.weight(1f))
                Text(version, color = AppOnSurfaceMuted)
            }
            Text(
                "Stickers Bank",
                color = AppOnSurfaceMuted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = ordered,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
            onRemove = { st -> ServiceLocator.customStickerRepository.remove(st.id) },
        )
    }
}

private val FAQ = listOf(
    "How do I create a sticker?" to
        "Open the Create tab, tap Create sticker, add a photo (or a short video for animation), style it, then tap the checkmark to save.",
    "How do I add stickers to WhatsApp?" to
        "Select 3 or more stickers (long-press one, then slide across the rest) and tap WhatsApp. A pack needs at least 3 stickers.",
    "What is an animated sticker?" to
        "A short moving sticker (animated WebP). In the editor switch to Animation and pick a short video — it loops in WhatsApp.",
    "How do banks work?" to
        "Banks are your own sticker packs. Add stickers to a bank, then send the whole bank to WhatsApp or Telegram.",
    "Where are my created stickers?" to
        "In the Create tab under My Stickers, and here in Settings. Tap any to share, edit, or bank it.",
)

@Composable
private fun FaqItem(question: String, answer: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
            .neuRaised(cornerRadius = 14.dp, elevation = 6.dp)
            .clip(RoundedCornerShape(14.dp)).clickable { open = !open }.padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = Gold, modifier = Modifier.size(18.dp))
            Text(question, color = AppOnSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
            Icon(Icons.Rounded.ExpandMore, null, tint = AppOnSurfaceMuted, modifier = Modifier.size(20.dp))
        }
        AnimatedVisibility(open) {
            Text(answer, color = AppOnSurfaceMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, start = 28.dp))
        }
    }
}

/**
 * One block in the library section — outer rounded rectangle, then icon / title + subtitle /
 * chevron inside. My Stickers and Favorites both use this so the two stay identical.
 */
@Composable
private fun LibraryRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .neuRaised(cornerRadius = 14.dp, elevation = 7.dp)
            .clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = AppOnSurfaceMuted, fontSize = 12.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = AppOnSurfaceMuted, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SettingRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Gold, modifier = Modifier.size(20.dp))
        Text(label, color = AppOnSurface, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
    }
}

/** Horizontally-scrolling row of palette swatches — tap one to re-theme the whole app instantly. */
@Composable
private fun ThemeRow() {
    val current = Neo.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(NeoPalettes.all, key = { it.id }) { palette ->
            ThemeSwatch(palette, selected = palette.id == current.id) {
                ServiceLocator.themePreferences.setThemeId(palette.id)
            }
        }
    }
}

@Composable
private fun ThemeSwatch(palette: NeoPalette, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
        Box(
            Modifier
                .size(48.dp)
                .neuRaised(cornerRadius = 24.dp, elevation = 6.dp, background = palette.bg)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(palette.primary))
            if (selected) {
                Icon(
                    Icons.Rounded.Check, "Selected", tint = palette.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            palette.displayName, color = if (selected) Gold else AppOnSurfaceMuted,
            fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = Gold, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 18.dp, top = 22.dp, bottom = 6.dp),
    )
}
