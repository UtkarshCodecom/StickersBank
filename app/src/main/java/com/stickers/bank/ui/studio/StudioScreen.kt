package com.stickers.bank.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.neuRaised

/**
 * STUDIO — its own corner of the app (opened from Settings), so nothing here touches the editor
 * or anything else. Every tool runs entirely on the phone: free, offline once set up, nothing
 * uploaded.
 */
@Composable
fun StudioScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize()) {
        SbBackHeader("Studio", { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                "Free tools that run on your phone — nothing is uploaded.",
                color = AppOnSurfaceMuted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 14.dp),
            )
            StudioCard(
                icon = Icons.Rounded.SwitchAccount,
                title = "Face Swap",
                body = "Pick a face photo and a video — the video's face morphs into yours: it turns, talks and blinks with the video. Save the video or make an animated sticker.",
            ) { nav.navigate(Routes.STUDIO_SWAP) }
            StudioCard(
                icon = Icons.Rounded.ClosedCaption,
                title = "Auto Subtitles",
                body = "Pick a video — its speech becomes captions automatically. Choose a style (karaoke, pop, neon…), keep or add music, and save the video.",
            ) { nav.navigate(Routes.STUDIO_SUBTITLES) }
        }
    }
}

@Composable
private fun StudioCard(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 14.dp)
            .neuRaised(cornerRadius = 20.dp, elevation = 8.dp).clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Gold),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = AppBackground, modifier = Modifier.size(28.dp)) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, color = AppOnSurface, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("FREE · ON YOUR PHONE", color = Gold, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Gold, modifier = Modifier.size(20.dp))
        }
        Text(body, color = AppOnSurfaceMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
    }
}
