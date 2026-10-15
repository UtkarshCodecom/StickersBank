package com.stickers.bank.admin.ui

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stickers.bank.admin.data.AdminCanvasTemplate
import com.stickers.bank.admin.data.AdminCategory
import com.stickers.bank.admin.data.AdminCollection
import com.stickers.bank.admin.data.AdminGroup
import com.stickers.bank.admin.data.AdminRepository
import com.stickers.bank.admin.data.AdminSticker
import com.stickers.bank.admin.data.AdminTemplateCategory
import com.stickers.bank.admin.r2.R2Config
import com.stickers.bank.ui.components.checkerboard
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.Danger
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.neuClickable
import kotlinx.coroutines.launch

/**
 * Runs [block] and returns its result, or [default] with a Toast on a genuine failure.
 * Deliberately lets [kotlinx.coroutines.CancellationException] propagate instead of catching it
 * — a `LaunchedEffect` whose composable leaves composition mid-load is cancelled this way, and
 * treating that as a "load failed" error (as a plain `runCatching{}.getOrElse{}` would) shows a
 * confusing "The coroutine scope left the composition" toast for what is normal navigation, not
 * an actual failure.
 */
private suspend fun <T> loadOrToast(context: android.content.Context, default: T, block: suspend () -> T): T =
    try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Toast.makeText(context, "Load failed: ${e.message}", Toast.LENGTH_LONG).show()
        default
    }

/** Shared selection across admin screens (avoids encoding names in nav args). */
class AdminSelection {
    var category by mutableStateOf<AdminCategory?>(null)
    var group by mutableStateOf<AdminGroup?>(null)
    var collection by mutableStateOf<AdminCollection?>(null)
    var templateCategory by mutableStateOf<AdminTemplateCategory?>(null)
    // Set right before navigating into "templateEditor" to edit an EXISTING template (its real
    // Firestore doc id) — left null to build a fresh one instead. See AdminTemplateListScreen.
    var editingTemplateId by mutableStateOf<String?>(null)
}

@Composable
private fun AdminHeader(title: String, subtitle: String?, onBack: () -> Unit, onAdd: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).neuClickable(cornerRadius = 20.dp, elevation = 6.dp, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = AppOnSurface, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title.uppercase(), color = Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = AppOnSurfaceMuted, fontSize = 11.sp)
        }
        if (onAdd != null) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Gold).clickable(onClick = onAdd).padding(8.dp),
            ) { Icon(Icons.Rounded.Add, "Add", tint = AppBackground, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun NameRow(name: String, onOpen: (() -> Unit)?, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppSurface)
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = AppOnSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.Delete, "Delete", tint = AppOnSurfaceMuted, modifier = Modifier.size(20.dp).clickable(onClick = onDelete))
        if (onOpen != null) Icon(Icons.Rounded.ChevronRight, null, tint = Gold, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun AddDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppSurface,
        title = { Text(title, color = AppOnSurface) },
        text = {
            TextField(
                text, { text = it }, singleLine = true, placeholder = { Text("Name", color = AppOnSurfaceMuted) },
                colors = adminFieldColors(),
            )
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("Add", color = Gold, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppOnSurfaceMuted) } },
    )
}

@Composable
private fun adminFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AppSurfaceVariant, unfocusedContainerColor = AppSurfaceVariant,
    focusedIndicatorColor = Gold, unfocusedIndicatorColor = Color.Transparent,
    cursorColor = Gold, focusedTextColor = AppOnSurface, unfocusedTextColor = AppOnSurface,
)

@Composable
private fun AddCollectionDialog(onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var text by remember { mutableStateOf("") }
    var animated by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppSurface,
        title = { Text("New name", color = AppOnSurface) },
        text = {
            Column {
                TextField(
                    text, { text = it }, singleLine = true,
                    placeholder = { Text("Name (e.g. Deepika, Animations)", color = AppOnSurfaceMuted) },
                    colors = adminFieldColors(),
                )
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Animated (GIF)", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Uploads .mp4 videos, shared as GIFs", color = AppOnSurfaceMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = animated, onCheckedChange = { animated = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppBackground, checkedTrackColor = Gold),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim(), animated) }) { Text("Add", color = Gold, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppOnSurfaceMuted) } },
    )
}

// ---------- Categories ----------

@Composable
fun AdminCategoriesScreen(nav: NavHostController, repo: AdminRepository, selection: AdminSelection) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<AdminCategory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(reload) {
        loading = true
        items = loadOrToast(context, emptyList()) { repo.listCategories() }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        AdminHeader("Categories", "Main sections (Hindi, English…)", onBack = { (context as? Activity)?.finish() }, onAdd = { showAdd = true })
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp)).background(AppSurfaceVariant)
                .clickable { nav.navigate("templateCategories") }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🎨", fontSize = 18.sp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("Canvas Templates", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Build ready-made canvases for the Templates tab", color = AppOnSurfaceMuted, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Gold)
        }
        Box(Modifier.height(4.dp))
        AdminList(loading, items.isEmpty(), "No categories yet. Tap + to add one.") {
            items(items, key = { it.id }) { c ->
                NameRow(c.name, onOpen = { selection.category = c; nav.navigate("groups") }) {
                    scope.launch { repo.deleteCategory(c.id); reload++ }
                }
            }
        }
    }
    if (showAdd) AddDialog("New category", { showAdd = false }) { name ->
        showAdd = false; scope.launch { repo.addCategory(name); reload++ }
    }
}

// ---------- Groups ----------

@Composable
fun AdminGroupsScreen(nav: NavHostController, repo: AdminRepository, selection: AdminSelection) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val category = selection.category
    var items by remember { mutableStateOf<List<AdminGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(reload, category?.id) {
        if (category == null) return@LaunchedEffect
        loading = true
        items = loadOrToast(context, emptyList()) { repo.listGroups(category.id) }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        AdminHeader(category?.name ?: "Groups", "Types (Actress, Comedian…)", onBack = { nav.popBackStack() }, onAdd = { showAdd = true })
        AdminList(loading, items.isEmpty(), "No types yet. Tap + to add one.") {
            items(items, key = { it.id }) { g ->
                NameRow(g.name, onOpen = { selection.group = g; nav.navigate("collections") }) {
                    scope.launch { repo.deleteGroup(g.id); reload++ }
                }
            }
        }
    }
    if (showAdd) AddDialog("New type", { showAdd = false }) { name ->
        showAdd = false; category?.let { scope.launch { repo.addGroup(it.id, name); reload++ } }
    }
}

// ---------- Collections ----------

@Composable
fun AdminCollectionsScreen(nav: NavHostController, repo: AdminRepository, selection: AdminSelection) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val category = selection.category
    val group = selection.group
    var items by remember { mutableStateOf<List<AdminCollection>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(reload, group?.id) {
        if (group == null) return@LaunchedEffect
        loading = true
        items = loadOrToast(context, emptyList()) { repo.listCollections(group.id) }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        AdminHeader(group?.name ?: "Collections", "Names (Deepika, Alia…)", onBack = { nav.popBackStack() }, onAdd = { showAdd = true })
        AdminList(loading, items.isEmpty(), "No names yet. Tap + to add one.") {
            items(items, key = { it.id }) { c ->
                NameRow(c.name, onOpen = { selection.collection = c; nav.navigate("detail") }) {
                    scope.launch { repo.deleteCollection(c.id); reload++ }
                }
            }
        }
    }
    if (showAdd) AddCollectionDialog({ showAdd = false }) { name, animated ->
        showAdd = false
        if (category != null && group != null) scope.launch { repo.addCollection(category.id, group.id, name, animated); reload++ }
    }
}

// ---------- Collection detail: mass upload + tags ----------

@Composable
fun AdminCollectionDetailScreen(nav: NavHostController, repo: AdminRepository, selection: AdminSelection) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val collection = selection.collection
    val categoryName = selection.category?.name ?: ""
    val groupName = selection.group?.name ?: ""
    val isAnimated = collection?.animated == true

    var picked by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var tags by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 0) }
    var stickers by remember { mutableStateOf<List<AdminSticker>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload, collection?.id) {
        if (collection == null) return@LaunchedEffect
        stickers = loadOrToast(context, emptyList()) { repo.listStickers(collection.id) }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(100),
    ) { uris -> picked = uris }

    Column(Modifier.fillMaxSize()) {
        AdminHeader(collection?.name ?: "Collection", "$categoryName › $groupName", onBack = { nav.popBackStack() }, onAdd = null)

        Column(Modifier.padding(horizontal = 16.dp)) {
            if (isAnimated) {
                Text(
                    "🎞 ANIMATED (GIF) collection — uploads .mp4",
                    color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            if (!R2Config.isConfigured) {
                Text(
                    "⚠ R2 not configured — set ENDPOINT / BUCKET / PUBLIC_BASE_URL in R2Config.kt before uploading.",
                    color = Danger, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppSurface)
                    .clickable(enabled = !uploading) {
                        picker.launch(
                            PickVisualMediaRequest(
                                if (isAnimated) ActivityResultContracts.PickVisualMedia.VideoOnly
                                else ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    }.padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when {
                        picked.isEmpty() && isAnimated -> "＋  Pick videos (.mp4)"
                        picked.isEmpty() -> "＋  Pick images (mass)"
                        else -> "${picked.size} ${if (isAnimated) "videos" else "images"} selected — tap to change"
                    },
                    color = Gold, fontWeight = FontWeight.Bold,
                )
            }
            TextField(
                tags, { tags = it }, singleLine = false,
                placeholder = { Text("Mass tags (comma separated) applied to all", color = AppOnSurfaceMuted) },
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp)),
            )
            Text(
                "Auto-tags: ${listOf(categoryName, groupName, collection?.name).filter { !it.isNullOrBlank() }.joinToString(", ").lowercase()}",
                color = AppOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
            )

            if (uploading) {
                LinearProgressIndicator(
                    progress = { if (progress.second == 0) 0f else progress.first.toFloat() / progress.second },
                    color = Gold, trackColor = AppSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Text("Uploading ${progress.first}/${progress.second}…", color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            } else {
                Box(
                    Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (picked.isEmpty()) AppSurfaceVariant else Gold)
                        .clickable(enabled = picked.isNotEmpty()) {
                            val coll = collection ?: return@clickable
                            uploading = true; progress = 0 to picked.size
                            scope.launch {
                                val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                val count = runCatching {
                                    if (isAnimated)
                                        repo.massUploadAnimated(context, coll, categoryName, groupName, picked, tagList) { d, t -> progress = d to t }
                                    else
                                        repo.massUpload(context, coll, categoryName, groupName, picked, tagList) { d, t -> progress = d to t }
                                }.getOrElse { e ->
                                    Toast.makeText(context, "Upload error: ${e.message}", Toast.LENGTH_LONG).show(); 0
                                }
                                uploading = false; picked = emptyList(); tags = ""
                                Toast.makeText(context, "Uploaded $count ${if (isAnimated) "animations" else "stickers"}", Toast.LENGTH_SHORT).show()
                                reload++
                            }
                        }.padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (picked.isEmpty()) "Pick images first" else "Upload ${picked.size} stickers",
                        color = if (picked.isEmpty()) AppOnSurfaceMuted else AppBackground, fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text("${stickers.size} stickers in this collection", color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(stickers, key = { it.id }) { s ->
                Box(
                    Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(AppSurface).checkerboard(),
                ) {
                    AsyncImage(model = s.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(3.dp))
                    Icon(
                        Icons.Rounded.Delete, "Delete", tint = Danger,
                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(18.dp)
                            .clickable { scope.launch { repo.deleteSticker(s.id); reload++ } },
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminList(loading: Boolean, empty: Boolean, emptyMsg: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    when {
        loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Gold) }
        empty -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
            Text(emptyMsg, color = AppOnSurfaceMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        else -> LazyColumn(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), content = content,
        )
    }
}

// ---------- Canvas templates ----------
// Building a template here reuses the SAME rich editor the app's end users get for filling one
// in (photo/text/sticker/overlay/shape tools, adjust/filter/draw/erase/layer tabs) — see
// CanvasTemplateEditorScreen's adminMode param — instead of a bespoke, much simpler builder,
// since that editor already does everything a template needs and is what makes this so much
// easier than the web admin's own from-scratch canvas (the whole point of this screen).

private data class RatioOption(val label: String, val value: Float)

private val TEMPLATE_RATIO_OPTIONS = listOf(
    RatioOption("Square — 1:1", 1f),
    RatioOption("Portrait — 4:5", 0.8f),
    RatioOption("Portrait — 3:4", 0.75f),
    RatioOption("Portrait — 2:3", 0.6667f),
    RatioOption("Portrait — 1:2", 0.5f),
    RatioOption("Story / Reel — 9:16", 0.5625f),
    RatioOption("Landscape — 16:9", 1.7778f),
    RatioOption("Landscape — 4:3", 1.3333f),
    RatioOption("Landscape — 3:2", 1.5f),
    RatioOption("Wide — 21:9", 2.3333f),
)

private fun ratioLabel(ratio: Float): String {
    TEMPLATE_RATIO_OPTIONS.firstOrNull { kotlin.math.abs(it.value - ratio) < 0.001f }?.let { return it.label }
    com.stickers.bank.ui.templates.CANVAS_SIZE_CATEGORIES
        .asSequence().flatMap { it.presets }
        .firstOrNull { kotlin.math.abs(it.ratio - ratio) < 0.001f }
        ?.let { return it.name }
    return if (ratio > 0f) "Custom · %.2f".format(ratio) else "Default"
}

@Composable
fun AdminTemplateCategoriesScreen(
    nav: NavHostController,
    repo: AdminRepository,
    selection: AdminSelection,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<AdminTemplateCategory>>(emptyList()) }
    var leftovers by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    // What the confirm dialog will delete: its title, its message, and the delete itself
    // (returning how many templates it removed, for the toast).
    var pendingDelete by remember { mutableStateOf<Triple<String, String, suspend () -> Int>?>(null) }

    LaunchedEffect(reload) {
        loading = true
        items = loadOrToast(context, emptyList()) { repo.listTemplateCategories() }
        leftovers = loadOrToast(context, emptyList()) { repo.listLeftoverTemplateGroups() }
        loading = false
    }
    // Re-read on coming back from a row's template list, where templates may have been deleted
    // one by one (otherwise a cleared-out leftover group would still be listed here). Skips the
    // first resume, which is just this screen opening and is already covered above.
    var resumedBefore by remember { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (resumedBefore) reload++ else resumedBefore = true
        onPauseOrDispose { }
    }

    Column(Modifier.fillMaxSize()) {
        AdminHeader("Canvas Templates", "Rows on the app's Templates tab", onBack = { nav.popBackStack() }, onAdd = { showAdd = true })
        AdminList(loading, items.isEmpty() && leftovers.isEmpty(), "No template rows yet. Tap + to add one.") {
            items(items, key = { it.id }) { c ->
                NameRow(
                    "${c.name}  ·  ${c.shapeLabel.ifBlank { ratioLabel(c.ratio) }}",
                    onOpen = { selection.templateCategory = c; nav.navigate("templateList") },
                ) {
                    pendingDelete = Triple(
                        "Delete \"${c.name}\"?",
                        "This removes the row and every template in it from the app.",
                    ) { repo.deleteTemplateCategory(c.id, c.name) }
                }
            }
            if (leftovers.isNotEmpty()) {
                item(key = "leftovers_header") {
                    Column(Modifier.padding(top = 10.dp)) {
                        Text("NOT IN ANY ROW", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text(
                            "These templates still show in the app, but their row was deleted (or never existed). " +
                                "Open one to check it, or delete the group.",
                            color = AppOnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                items(leftovers, key = { "leftover_" + it.first }) { (name, count) ->
                    val label = name.ifBlank { "(no category — \"More\" in the app)" }
                    NameRow(
                        "$label  ·  $count template${if (count == 1) "" else "s"}",
                        // A stand-in row (blank id) so the list screen can show and delete them;
                        // it has no shape of its own and nothing new should be filed under it.
                        onOpen = { selection.templateCategory = AdminTemplateCategory(name = name); nav.navigate("templateList") },
                    ) {
                        pendingDelete = Triple(
                            "Delete $count leftover template${if (count == 1) "" else "s"}?",
                            "They'll be removed from the app's Templates tab.",
                        ) { repo.deleteTemplatesInCategory(name) }
                    }
                }
            }
        }
    }
    if (showAdd) AddTemplateCategoryDialog(onDismiss = { showAdd = false }) { name, ratio, shapeLabel ->
        showAdd = false
        scope.launch { repo.addTemplateCategory(name, ratio, shapeLabel); reload++ }
    }
    pendingDelete?.let { (title, message, action) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null }, containerColor = AppSurface,
            title = { Text(title, color = AppOnSurface) },
            text = { Text(message, color = AppOnSurfaceMuted) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch {
                        val removed = runCatching { action() }.getOrElse { e ->
                            Toast.makeText(context, "Couldn't delete: ${e.message}", Toast.LENGTH_LONG).show(); null
                        }
                        if (removed != null) {
                            Toast.makeText(context, "Deleted $removed template${if (removed == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
                        }
                        reload++
                    }
                }) { Text("Delete", color = Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = AppOnSurface) } },
        )
    }
}

@Composable
private fun AddTemplateCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, Float, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var ratio by remember { mutableStateOf(TEMPLATE_RATIO_OPTIONS[0].value) }
    // The shape's own exact name, kept alongside the ratio (not re-derived from it later) so a
    // pick like "YouTube Thumbnail" can't come back mislabeled as "Landscape — 16:9" just
    // because the two happen to share a ratio — see AdminTemplateCategory.shapeLabel.
    var shapeLabel by remember { mutableStateOf(TEMPLATE_RATIO_OPTIONS[0].label) }
    // Opens the SAME full shape picker the app's own users get from "Create your own canvas"
    // (YouTube, Social Media, Print, Business…) — the quick chips below stay for the handful of
    // ratios picked often enough to not need browsing for every time.
    var showFullPicker by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppSurface,
        title = { Text("New template row", color = AppOnSurface) },
        text = {
            Column {
                TextField(
                    text, { text = it }, singleLine = true,
                    placeholder = { Text("Row name (e.g. Birthday, Motivational)", color = AppOnSurfaceMuted) },
                    colors = adminFieldColors(),
                )
                Text(
                    "Canvas shape — locked for every template in this row",
                    color = AppOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TEMPLATE_RATIO_OPTIONS.forEach { opt ->
                        val selected = opt.value == ratio
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(if (selected) Gold else AppSurfaceVariant)
                                .clickable { ratio = opt.value; shapeLabel = opt.label }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                opt.label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = if (selected) AppBackground else AppOnSurface,
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showFullPicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Gold, modifier = Modifier.size(15.dp))
                    Text(
                        "  More shapes — YouTube, Social Media, Print, Business…",
                        color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "Selected: $shapeLabel",
                    color = AppOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim(), ratio, shapeLabel) }) {
                Text("Add", color = Gold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppOnSurfaceMuted) } },
    )
    if (showFullPicker) {
        com.stickers.bank.ui.templates.NewCanvasPicker(
            onDismiss = { showFullPicker = false },
            onPick = { picked, name -> ratio = picked; shapeLabel = name; showFullPicker = false },
            title = "TEMPLATE ROW SHAPE",
            subtitle = "Pick the shape every template in this row will share.",
        )
    }
}

@Composable
fun AdminTemplateListScreen(
    nav: NavHostController,
    repo: AdminRepository,
    selection: AdminSelection,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val category = selection.templateCategory
    var items by remember { mutableStateOf<List<AdminCanvasTemplate>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload, category?.name) {
        if (category == null) return@LaunchedEffect
        loading = true
        items = loadOrToast(context, emptyList()) { repo.listCanvasTemplates(category.name) }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        AdminHeader(
            category?.name ?: "Templates",
            category?.shapeLabel?.ifBlank { null } ?: ratioLabel(category?.ratio ?: 0f),
            onBack = { nav.popBackStack() },
            // A leftover group (blank id — see AdminTemplateCategoriesScreen) has no row to add to.
            onAdd = if (category?.id.isNullOrBlank()) null else ({ selection.editingTemplateId = null; nav.navigate("templateEditor") }),
        )
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Gold) }
            items.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                Text(
                    "No templates in this row yet. Tap + to build one in the editor.",
                    color = AppOnSurfaceMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.id }) { t ->
                    // Tap the card to open it back up in the editor — everything but the trash
                    // icon (its own smaller clickable, hit-tested first) re-enters as an edit of
                    // THIS template rather than starting a new one.
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(AppSurface)
                            .clickable { selection.editingTemplateId = t.id; nav.navigate("templateEditor") },
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(if (t.ratio > 0f) t.ratio else 1f)
                                .clip(RoundedCornerShape(12.dp)).background(AppSurfaceVariant).checkerboard(),
                        ) {
                            AsyncImage(
                                model = t.image, contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Icon(
                                Icons.Rounded.Delete, "Delete", tint = Danger,
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.4f)).padding(2.dp)
                                    .clickable { scope.launch { repo.deleteCanvasTemplate(t.id); reload++ } },
                            )
                        }
                        Text(
                            t.title, color = AppOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
