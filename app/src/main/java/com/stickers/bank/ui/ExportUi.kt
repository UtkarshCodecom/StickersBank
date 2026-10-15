package com.stickers.bank.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.export.PackBuilder
import com.stickers.bank.export.WaStickerPack
import com.stickers.bank.ui.components.PreparingPopup
import com.stickers.bank.export.StickerExporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** One WhatsApp pack to build and hand over. */
private data class PackRequest(
    val name: String,
    val stickers: List<Sticker>,
    val animated: Boolean,
    /** For Analytics: how it was sent, and which stickers count as sent. */
    val via: String = "whatsapp",
    val sentIds: List<String> = stickers.map { it.id },
)

/**
 * Builds WhatsApp packs off the main thread and hands them to WhatsApp with the official
 * for-result launch (so the add-pack screen appears and WhatsApp can report a validation error
 * back). Several packs — a big selection, or stills and animations together — are built first,
 * then offered one after another: each time WhatsApp's add screen closes, the next one opens.
 */
@Composable
private fun rememberWhatsAppPackSender(): (List<PackRequest>) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Built packs still waiting for their turn in WhatsApp's add screen, and which WhatsApp.
    val queue = remember { ArrayDeque<Pair<PackRequest, WaStickerPack>>() }
    var queueTarget by remember { mutableStateOf(StickerExporter.WHATSAPP) }
    var queueTotal by remember { mutableStateOf(0) }
    var launchNext: () -> Unit = {}
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val error = result.data?.getStringExtra("validation_error")
        if (error != null) {
            Toast.makeText(context, "WhatsApp couldn't add the pack: $error", Toast.LENGTH_LONG).show()
        }
        if (queue.isNotEmpty()) {
            // A beat for WhatsApp's screen to finish closing before the next one opens.
            scope.launch { delay(400); launchNext() }
        }
    }
    launchNext = launch@{
        val (req, pack) = queue.removeFirstOrNull() ?: return@launch
        val number = queueTotal - queue.size
        if (queueTotal > 1) toast(context, "Pack $number of $queueTotal: “${pack.name}”")
        try {
            launcher.launch(StickerExporter.whatsAppAddPackIntent(pack, queueTarget))
            ServiceLocator.analytics.stickersSent(req.via, req.sentIds)
            ServiceLocator.ratePrompt.stickersSent()
        } catch (e: Exception) {
            queue.clear()
            toast(context, "Couldn't open WhatsApp.")
        }
    }
    // Both flavours installed → the pack could go to either, so ask instead of guessing. Held
    // until the user picks, then the export runs against their choice.
    var chooseTargetFor by remember { mutableStateOf<List<PackRequest>?>(null) }
    // Title + note for the glass "preparing" popup while packs are being built; null = hidden.
    var preparing by remember { mutableStateOf<Pair<String, String?>?>(null) }

    val activity = com.stickers.bank.ads.rememberActivity()
    val send: (List<PackRequest>, String) -> Unit = { reqs, pkg ->
            scope.launch {
                // The interstitial plays while the packs are built; WhatsApp opens only once both
                // the ad is closed and the packs are ready.
                val adClosed = CompletableDeferred<Unit>()
                com.stickers.bank.ads.Ads.show(activity) { adClosed.complete(Unit) }
                val built = ArrayList<Pair<PackRequest, WaStickerPack>>()
                var notAdded = 0
                val mixed = reqs.map { it.animated }.distinct().size > 1
                for ((i, req) in reqs.withIndex()) {
                    preparing = (if (reqs.size > 1) "Preparing pack ${i + 1} of ${reqs.size}…" else "Preparing your stickers…") to when {
                        mixed -> "WhatsApp keeps animated and still stickers in separate packs, so they're added as ${reqs.size} packs."
                        reqs.size > 1 -> "WhatsApp packs hold up to ${PackBuilder.MAX_STICKERS} stickers, so these are added as ${reqs.size} packs."
                        stillsOfAnimated(req) -> "WhatsApp can't mix animated and still stickers in one pack — the animated ones go in as stills."
                        else -> "${req.stickers.size} sticker${if (req.stickers.size == 1) "" else "s"}"
                    }
                    val result = try {
                        withContext(Dispatchers.IO) {
                            StickerExporter.buildWhatsAppPack(context, req.name, req.stickers, animated = req.animated)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        preparing = null
                        throw e
                    } catch (e: Exception) {
                        Log.w("ExportUi", "pack build failed", e)
                        null // reported just below, like any other pack that couldn't be built
                    }
                    if (result == null) notAdded += req.sentIds.size
                    else { built += req to result.pack; notAdded += result.skipped }
                }
                adClosed.await()
                preparing = null
                if (built.isEmpty()) {
                    toast(
                        context,
                        if (reqs.all { it.animated })
                            "Couldn't build this pack — WhatsApp needs at least ${PackBuilder.MIN_STICKERS} animated stickers under 500KB each."
                        else "Couldn't build this pack. Check your connection and try again.",
                    )
                    return@launch
                }
                if (notAdded > 0) {
                    Toast.makeText(
                        context,
                        "$notAdded sticker${if (notAdded == 1) "" else "s"} couldn't be added (download failed or not WhatsApp-compatible).",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                queue.clear()
                queue.addAll(built)
                queueTarget = pkg
                queueTotal = built.size
                launchNext()
            }
            Unit
    }

    preparing?.let { (title, note) -> PreparingPopup(title, note) }

    chooseTargetFor?.let { reqs ->
        WhatsAppTargetDialog(
            onPick = { pkg -> chooseTargetFor = null; send(reqs, pkg) },
            onDismiss = { chooseTargetFor = null },
        )
    }

    return { reqs ->
        val targets = StickerExporter.whatsAppPackages(context)
        when {
            reqs.isEmpty() -> Unit
            targets.isEmpty() -> toast(context, "WhatsApp is not installed.")
            targets.size == 1 -> send(reqs, targets.first())
            else -> chooseTargetFor = reqs
        }
    }
}

/**
 * Returns (packName, stickers) -> installs those stickers in WhatsApp as NEW pack(s) named
 * after [packName] — never replacing a pack added before (see PackBuilder.identifierFor).
 *
 * WhatsApp's own rules decide the rest:
 *  - a pack needs at least [PackBuilder.MIN_STICKERS] stickers, so fewer than that is refused
 *    with a note to pick more (nothing is padded with filler stickers);
 *  - a pack holds at most [PackBuilder.MAX_STICKERS], so a big selection is split evenly;
 *  - a pack is all-still or all-animated: a selection with enough of BOTH becomes a still pack
 *    and an animated one, otherwise it all goes in as stills (animations show their first frame).
 */
@Composable
fun rememberPackExporter(): (String, List<Sticker>) -> Unit {
    val context = LocalContext.current
    val sender = rememberWhatsAppPackSender()
    return { name, stickers ->
        val count = stickers.distinctBy { it.id }.size
        if (count in 1 until PackBuilder.MIN_STICKERS) {
            toast(context, "Pick at least ${PackBuilder.MIN_STICKERS} stickers — WhatsApp packs need ${PackBuilder.MIN_STICKERS} or more.")
        } else {
            sender(packRequests(name, stickers))
        }
    }
}

private fun packRequests(name: String, stickers: List<Sticker>): List<PackRequest> {
    val unique = stickers.distinctBy { it.id }
    if (unique.size < PackBuilder.MIN_STICKERS) return emptyList()
    val (animated, still) = unique.partition { it.isAnimatedSticker }
    // Enough of each kind for a pack of its own → two packs. Otherwise one still pack: a still
    // pack can hold an animation's first frame, an animated pack can't hold a still.
    val groups = when {
        animated.size >= PackBuilder.MIN_STICKERS && still.size >= PackBuilder.MIN_STICKERS ->
            listOf(still to false, animated to true)
        still.isEmpty() -> listOf(animated to true)
        else -> listOf(unique to false)
    }
    val out = ArrayList<PackRequest>()
    for ((list, isAnimated) in groups) {
        val baseName = if (isAnimated && groups.size > 1) "$name Animated" else name
        // Even split: 31 stickers become 16 + 15, never 30 + a lone 1 that WhatsApp would refuse.
        val packs = (list.size + PackBuilder.MAX_STICKERS - 1) / PackBuilder.MAX_STICKERS
        val per = (list.size + packs - 1) / packs
        list.chunked(per).forEach { chunk -> out += PackRequest(baseName, chunk, animated = isAnimated) }
    }
    return out
}

/** A still pack that has animated stickers in it (they go in as their first frame). */
private fun stillsOfAnimated(req: PackRequest) = !req.animated && req.stickers.any { it.isAnimatedSticker }

/**
 * Returns (sticker) -> sends ONE sticker straight into a WhatsApp chat. A single sticker can't
 * be a pack (WhatsApp needs [PackBuilder.MIN_STICKERS]), so it goes the way a sticker is sent
 * from the gallery: WhatsApp opens its chat picker with it. Animated stickers go as their
 * original file, so they keep moving.
 */
@Composable
fun rememberSingleStickerToWhatsApp(): (Sticker) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    var chooseTargetFor by remember { mutableStateOf<Sticker?>(null) }

    val send: (Sticker, String) -> Unit = { sticker, pkg ->
        scope.launch {
            val intent = withContext(Dispatchers.IO) { StickerExporter.stickerToChatIntent(context, sticker, pkg) }
            if (intent == null) {
                toast(context, "Couldn't prepare this sticker.")
                return@launch
            }
            try {
                launcher.launch(intent)
                ServiceLocator.analytics.stickersSent("whatsappSingle", listOf(sticker.id))
                ServiceLocator.ratePrompt.stickersSent()
            } catch (e: Exception) {
                toast(context, "Couldn't open WhatsApp.")
            }
        }
        Unit
    }

    chooseTargetFor?.let { sticker ->
        WhatsAppTargetDialog(
            onPick = { pkg -> chooseTargetFor = null; send(sticker, pkg) },
            onDismiss = { chooseTargetFor = null },
        )
    }

    return { sticker ->
        val targets = StickerExporter.whatsAppPackages(context)
        when {
            targets.isEmpty() -> toast(context, "WhatsApp is not installed.")
            targets.size == 1 -> send(sticker, targets.first())
            else -> chooseTargetFor = sticker
        }
    }
}

/** Asks which WhatsApp to hand the stickers to. Only shown when both flavours are installed. */
@Composable
private fun WhatsAppTargetDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send to which WhatsApp?") },
        text = { Text("Both WhatsApp and WhatsApp Business are installed on this phone.") },
        confirmButton = {
            TextButton(onClick = { onPick(StickerExporter.WHATSAPP) }) { Text("WhatsApp") }
        },
        dismissButton = {
            TextButton(onClick = { onPick(StickerExporter.WHATSAPP_BUSINESS) }) { Text("WP Business") }
        },
    )
}

/**
 * Returns (packName, stickers) -> sends to Telegram. Static stickers use Telegram's
 * CREATE_STICKER_PACK import (512 webp); animated .mp4 clips are shared as videos.
 * URL-aware: images are downloaded/re-encoded off the main thread.
 */
@Composable
fun rememberTelegramPackShare(): (String, List<Sticker>) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {}
    var preparing by remember { mutableStateOf<Pair<String, String?>?>(null) }
    preparing?.let { (title, note) -> PreparingPopup(title, note) }
    return remember {
        { name, stickers ->
            scope.launch {
                if (stickers.isEmpty()) return@launch
                if (!StickerExporter.telegramInstalled(context)) {
                    toast(context, "Telegram is not installed.")
                    return@launch
                }
                // Only real .mp4 clips go as videos. Telegram's sticker import can't take animated
                // WebP (it wants its own video/TGS formats), so those import as stills — they used
                // to be sent down the video path too, as WebP bytes in an .mp4 file.
                val videos = stickers.all { it.isVideoClip }
                preparing = "Preparing for Telegram…" to
                    if (!videos && stickers.any { it.isAnimatedSticker }) "Telegram imports animated stickers as still images." else null
                val intent = try {
                    withContext(Dispatchers.IO) {
                        if (videos) StickerExporter.animatedGifIntent(context, stickers, StickerExporter.TELEGRAM)
                        else StickerExporter.buildTelegramImportIntent(context, name, stickers.filterNot { it.isVideoClip })
                    }
                } finally {
                    preparing = null
                }
                if (intent == null) {
                    toast(context, "Couldn't prepare these stickers.")
                    return@launch
                }
                try {
                    launcher.launch(intent)
                    ServiceLocator.analytics.stickersSent("telegram", stickers.map { it.id })
                    ServiceLocator.ratePrompt.stickersSent()
                } catch (e: Exception) {
                    toast(context, "Couldn't open Telegram.")
                }
            }
        }
    }
}

/** Returns (sticker) -> shares a single sticker image via the system chooser. */
@Composable
fun rememberSingleShare(): (Sticker) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {}
    return remember {
        { sticker ->
            scope.launch {
                val intent = withContext(Dispatchers.IO) {
                    StickerExporter.shareSingleIntent(context, sticker)
                }
                if (intent == null) {
                    toast(context, "Couldn't prepare this sticker.")
                    return@launch
                }
                try {
                    launcher.launch(Intent.createChooser(intent, "Share sticker"))
                    ServiceLocator.analytics.stickersSent("share", listOf(sticker.id))
                    ServiceLocator.ratePrompt.stickersSent()
                } catch (e: Exception) {
                    toast(context, "Nothing available to share to.")
                }
            }
        }
    }
}

/** Returns (bankName, stickers) -> creates a bank with those stickers and toasts. */
@Composable
fun rememberBankAdder(): (String, List<Sticker>) -> Unit {
    val context = LocalContext.current
    return remember {
        { name, stickers ->
            val msg = if (stickers.isEmpty()) {
                "Nothing to add"
            } else {
                val bank = ServiceLocator.bankRepository.createBank(name)
                stickers.forEach { ServiceLocator.bankRepository.addSticker(bank.id, it) }
                "Added ${stickers.size} stickers to ${bank.name}"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}

/** Returns (stickers) -> shares animated .mp4 stickers to WhatsApp's GIF flow. */
@Composable
fun rememberGifShare(): (List<Sticker>) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {}
    var chooseTargetFor by remember { mutableStateOf<List<Sticker>?>(null) }
    var preparing by remember { mutableStateOf(false) }
    if (preparing) PreparingPopup("Preparing GIF…")

    val send: (List<Sticker>, String?) -> Unit = { stickers, pkg ->
            scope.launch {
                preparing = true
                val intent = try {
                    withContext(Dispatchers.IO) { StickerExporter.animatedGifIntent(context, stickers, pkg) }
                } finally {
                    preparing = false
                }
                if (intent == null) {
                    toast(context, "Couldn't prepare these animations.")
                    return@launch
                }
                val toLaunch = if (pkg != null) intent else Intent.createChooser(intent, "Send GIF")
                try {
                    launcher.launch(toLaunch)
                    ServiceLocator.analytics.stickersSent("gif", stickers.map { it.id })
                    ServiceLocator.ratePrompt.stickersSent()
                } catch (e: Exception) {
                    toast(context, "Couldn't open WhatsApp.")
                }
            }
            Unit
    }

    chooseTargetFor?.let { stickers ->
        WhatsAppTargetDialog(
            onPick = { pkg -> chooseTargetFor = null; send(stickers, pkg) },
            onDismiss = { chooseTargetFor = null },
        )
    }

    return { stickers ->
        if (stickers.isNotEmpty()) {
            val targets = StickerExporter.whatsAppPackages(context)
            // No WhatsApp at all still goes out through the system chooser (pkg = null), the way
            // it always has — only an actual both-installed ambiguity is worth a prompt.
            if (targets.size > 1) chooseTargetFor = stickers
            else send(stickers, targets.firstOrNull())
        }
    }
}

private fun toast(context: android.content.Context, msg: String) =
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
