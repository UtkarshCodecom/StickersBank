package com.stickers.bank.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.BucketShape
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.neuRaised

@Composable
fun CreateScreen(nav: NavHostController) {
    val display = com.stickers.bank.ui.theme.rememberDisplayFont()
    val heroShape = remember { BucketShape(topInset = 24.dp, corner = 32.dp) }
    var animMode by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Sticker / Animation choice.
        Row(
            Modifier.fillMaxWidth().neuRaised(cornerRadius = 16.dp, elevation = 7.dp).clip(RoundedCornerShape(16.dp)).padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            ModeTab("Sticker", Icons.Rounded.Image, !animMode, Modifier.weight(1f)) { animMode = false }
            ModeTab("Animation", Icons.Rounded.Movie, animMode, Modifier.weight(1f)) { animMode = true }
        }

        Spacer(Modifier.height(16.dp))

        // Start-creating hero — opens the editor in the chosen mode.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(heroShape)
                .background(Gold)
                .clickable { nav.navigate(Routes.editor(animated = animMode)) }
                .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(AppBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (animMode) Icons.Rounded.Movie else Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(34.dp))
            }
            Text(
                if (animMode) "CREATE AN\nANIMATION" else "CREATE A\nSTICKER",
                color = OnGold, fontFamily = display, fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                if (animMode) "Pick a short video — it loops as an animated sticker."
                else "Turn a photo into a WhatsApp & Telegram sticker.",
                color = OnGold.copy(alpha = 0.72f), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                Modifier.padding(top = 18.dp).clip(RoundedCornerShape(50)).background(AppBackground).padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Start creating", color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Gold, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth().neuRaised(cornerRadius = 16.dp, elevation = 6.dp).clip(RoundedCornerShape(16.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Your saved stickers now live in Settings ▸ My Stickers.", color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeTab(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) Gold else AppSurfaceVariant).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) AppBackground else Gold, modifier = Modifier.size(18.dp))
        Text(label, color = if (selected) AppBackground else AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}
