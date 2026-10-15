package com.stickers.bank.data

import android.content.Context
import com.stickers.bank.data.model.Sticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Stores user-created stickers (from the editor). Images live under
 * filesDir/custom_stickers/, metadata in filesDir/custom_index.json.
 */
class CustomStickerRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val imagesDir = File(context.filesDir, "custom_stickers").apply { mkdirs() }
    private val indexFile = File(context.filesDir, "custom_index.json")

    private val _stickers = MutableStateFlow(load())
    val stickers: StateFlow<List<Sticker>> = _stickers.asStateFlow()

    private fun load(): List<Sticker> = runCatching {
        if (!indexFile.exists()) return emptyList()
        AppJson.decodeFromString(ListSerializer(Sticker.serializer()), indexFile.readText())
            // drop entries whose image file was deleted out from under us
            .filter { File(it.image).exists() }
    }.getOrElse { emptyList() }

    private fun persist(list: List<Sticker>) {
        scope.launch {
            runCatching {
                indexFile.writeText(
                    AppJson.encodeToString(ListSerializer(Sticker.serializer()), list)
                )
            }
        }
    }

    /** A fresh empty file the editor can write its rendered PNG/WebP into. */
    fun newImageFile(extension: String = "webp"): File =
        File(imagesDir, "sticker_${System.currentTimeMillis()}.$extension")

    /** Register an already-written image file as a custom sticker. */
    fun add(imageFile: File, caption: String, tags: List<String>, animated: Boolean): Sticker {
        val sticker = Sticker(
            id = "custom_${System.currentTimeMillis()}",
            image = imageFile.absolutePath,
            caption = caption,
            tags = (tags + "my stickers").distinct(),
            animated = animated,
            custom = true,
            createdAt = System.currentTimeMillis(),
        )
        val list = _stickers.value + sticker
        _stickers.value = list
        persist(list)
        return sticker
    }

    fun remove(id: String) {
        val target = _stickers.value.firstOrNull { it.id == id } ?: return
        runCatching { File(target.image).delete() }
        val list = _stickers.value.filterNot { it.id == id }
        _stickers.value = list
        persist(list)
    }

    /** Delete several stickers at once (their image files are removed too). */
    fun removeAll(ids: Collection<String>) {
        val idSet = ids.toSet()
        if (idSet.isEmpty()) return
        _stickers.value.filter { it.id in idSet }.forEach { runCatching { File(it.image).delete() } }
        val list = _stickers.value.filterNot { it.id in idSet }
        _stickers.value = list
        persist(list)
    }

    /** Delete every custom sticker. */
    fun clear() = removeAll(_stickers.value.map { it.id })
}
