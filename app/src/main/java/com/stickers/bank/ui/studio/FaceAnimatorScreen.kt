package com.stickers.bank.ui.studio

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.PolygonProgress
import com.stickers.bank.ui.components.PreparingPopup
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.crop.PersonCutout
import com.stickers.bank.ui.rememberSingleStickerToWhatsApp
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppOutline
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised
import com.stickers.bank.util.BitmapUtils
import com.stickers.bank.util.VideoToWebp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FACE ANIMATOR — a photo's face (well, the person: the on-device cut-out keeps the whole
 * person) cut out on the phone, then played in a [FaceTemplate] with an optional caption, and
 * saved as an animated sticker to My Stickers — from where it goes to WhatsApp like any other.
 */
@Composable
fun FaceAnimatorScreen(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var face by remember { mutableStateOf<Bitmap?>(null) }
    var cutting by remember { mutableStateOf(false) }
    var template by remember { mutableStateOf(FaceTemplate.BOUNCE) }
    var caption by remember { mutableStateOf("") }
    var saveProgress by remember { mutableStateOf<Float?>(null) }
    var saved by remember { mutableStateOf<Sticker?>(null) }
    val sendToWhatsApp = rememberSingleStickerToWhatsApp()
    val display = remember {
        runCatching { Typeface.createFromAsset(context.assets, "go3v2.ttf") }.getOrDefault(Typeface.DEFAULT_BOLD)
    }
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            cutting = true
            saved = null
            val photo = withContext(Dispatchers.IO) { BitmapUtils.loadUriBitmap(context, uri, 1280) }
            if (photo == null) {
                cutting = false
                toast("Couldn't open that photo.")
                return@launch
            }
            val cut = PersonCutout.cut(photo)
            face = withContext(Dispatchers.Default) {
                if (cut != null) FaceRenderer.prepare(cut.bitmap, cut.bounds) else FaceRenderer.roundedPhoto(photo)
            }
            if (cut == null) toast("No face found — using the whole photo.")
            cutting = false
        }
    }
    fun pick() = pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    fun save() {
        val f = face ?: return
        val text = caption.trim()
        val tpl = template
        scope.launch {
            saveProgress = 0f
            val bytes = withContext(Dispatchers.Default) {
                VideoToWebp.encodeTransparent(
                    count = FACE_FRAMES,
                    frameMs = FACE_LOOP_MS / FACE_FRAMES,
                    frameAt = { i, _ ->
                        Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).also {
                            FaceRenderer.draw(AndroidCanvas(it), 512f, f, tpl, i / FACE_FRAMES.toFloat(), text, display)
                        }
                    },
                    onProgress = { saveProgress = it * 0.95f },
                )
            }
            if (bytes == null) {
                saveProgress = null
                toast("Couldn't make the sticker. Try again.")
                return@launch
            }
            val sticker = withContext(Dispatchers.IO) {
                val repo = ServiceLocator.customStickerRepository
                val file = repo.newImageFile("webp")
                file.writeBytes(bytes)
                repo.add(file, text.ifBlank { "Face ${tpl.label}" }, listOf("face", tpl.label.lowercase()), animated = true)
            }
            saveProgress = null
            saved = sticker
            toast("Saved to My Stickers")
        }
    }

    Column(Modifier.fillMaxSize()) {
        SbBackHeader("Face Animator", { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "The face is cut out on your phone — free, nothing uploaded.",
                color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )

            // Live preview — the same drawing code that makes every frame of the saved sticker.
            Box(
                Modifier.fillMaxWidth(0.82f).aspectRatio(1f)
                    .neuInset(cornerRadius = 24.dp, elevation = 7.dp).clip(RoundedCornerShape(24.dp))
                    .then(if (face == null && !cutting) Modifier.clickable { pick() } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                val f = face
                when {
                    cutting -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PolygonProgress(null, Modifier.size(64.dp))
                        Text("Finding the face…", color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                    f == null -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null, tint = OnGold, modifier = Modifier.size(30.dp))
                        }
                        Text("Pick a photo", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp))
                        Text("A clear, front-facing face works best.", color = AppOnSurfaceMuted, fontSize = 12.sp)
                    }
                    else -> {
                        val transition = rememberInfiniteTransition(label = "face")
                        val t by transition.animateFloat(
                            0f, 1f, infiniteRepeatable(tween(FACE_LOOP_MS, easing = LinearEasing)), label = "t",
                        )
                        Canvas(Modifier.fillMaxSize()) {
                            checker(this.size.width)
                            val side = this.size.minDimension
                            drawIntoCanvas { FaceRenderer.draw(it.nativeCanvas, side, f, template, t, caption.trim(), display) }
                        }
                    }
                }
            }

            Text(
                "ANIMATION", color = Gold, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FaceTemplate.entries.forEach { tpl ->
                    val on = tpl == template
                    Column(
                        Modifier.then(
                            if (on) Modifier.neuInset(cornerRadius = 14.dp, elevation = 4.dp, background = Gold)
                            else Modifier.neuRaised(cornerRadius = 14.dp, elevation = 5.dp),
                        ).clip(RoundedCornerShape(14.dp)).clickable { template = tpl }.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(tpl.emoji, fontSize = 20.sp)
                        Text(tpl.label, color = if (on) OnGold else AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 24) caption = it },
                singleLine = true,
                placeholder = { Text("Add a caption (optional)", color = AppOnSurfaceMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold, unfocusedBorderColor = AppOutline,
                    focusedTextColor = AppOnSurface, unfocusedTextColor = AppOnSurface, cursorColor = Gold,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(50.dp).neuRaised(cornerRadius = 25.dp, elevation = 6.dp)
                        .clip(RoundedCornerShape(50)).clickable { pick() },
                    contentAlignment = Alignment.Center,
                ) { Text(if (face == null) "Pick photo" else "Change photo", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Box(
                    Modifier.weight(1.3f).height(50.dp).clip(RoundedCornerShape(50))
                        .background(if (face != null) Gold else Gold.copy(alpha = 0.4f))
                        .clickable(enabled = face != null && saveProgress == null) { save() },
                    contentAlignment = Alignment.Center,
                ) { Text("Save sticker", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }

            saved?.let { s ->
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(50))
                        .border(1.5.dp, Gold, RoundedCornerShape(50)).clickable { sendToWhatsApp(s) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Gold, modifier = Modifier.size(16.dp))
                    Text("Add to WhatsApp", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    "Open My Stickers", color = AppOnSurfaceMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50))
                        .clickable { nav.navigate(Routes.MY_STICKERS) }.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Box(Modifier.height(40.dp))
        }
    }

    saveProgress?.let { PreparingPopup("Making your sticker…", progress = it) }
}

/** A light checkerboard, so the transparent background reads as transparent. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.checker(width: Float) {
    val tile = width / 16f
    drawRect(Color(0xFFF1F1F1))
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else tile
        while (x < size.width) {
            drawRect(Color(0xFFDCDCDC), Offset(x, y), Size(tile, tile))
            x += tile * 2
        }
        y += tile
        row++
    }
}
