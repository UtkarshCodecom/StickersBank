package com.stickers.bank.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuRaised

/**
 * Invites a Play Store rating after stickers have been sent (see RatePrompt for when). Shown on
 * the user's RETURN to the app — the send hands off to WhatsApp/Telegram first, so popping it up
 * straight away would flash it on screen just before the other app covers it.
 *
 * Deliberately no "Do you like the app?" first: Play's policy bars asking for an opinion before
 * pointing someone at the rating, so this is a plain, skippable invitation.
 */
@Composable
fun RatePromptHost() {
    val context = LocalContext.current
    val prompt = ServiceLocator.ratePrompt
    var show by remember { mutableStateOf(false) }
    // Runs on every resume — but not at the moment the send itself happens (the app is still in
    // front then), only once the user comes back from where the stickers went.
    LifecycleResumeEffect(Unit) {
        if (prompt.pending.value) show = true
        onPauseOrDispose { }
    }
    if (!show) return

    fun close(done: Boolean) {
        show = false
        prompt.answered(done)
    }

    Dialog(onDismissRequest = { close(done = false) }) {
        Column(
            Modifier.fillMaxWidth().neuRaised(cornerRadius = 26.dp, elevation = 9.dp).padding(horizontal = 30.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("⭐⭐⭐⭐⭐", fontSize = 26.sp)
            Text(
                "Rate Stickers Bank", color = AppOnSurface, fontWeight = FontWeight.Black, fontSize = 19.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Your stickers are on their way! If you have a moment, a rating on the Play Store " +
                    "helps others find the app.",
                color = AppOnSurfaceMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Box(
                Modifier.padding(top = 22.dp).fillMaxWidth().height(50.dp)
                    .clip(RoundedCornerShape(50)).background(Gold)
                    .clickable { close(done = true); openPlayStore(context) },
                contentAlignment = Alignment.Center,
            ) { Text("Rate now", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Text(
                "Later", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.padding(top = 14.dp).clip(RoundedCornerShape(50)).clickable { close(done = false) }
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            )
        }
    }
}

/** The app's own Play Store page — the Play Store app if there is one, the website otherwise. */
private fun openPlayStore(context: Context) {
    val id = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
