package com.stickers.bank.admin.data

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stickers.bank.admin.r2.R2Uploader
import com.stickers.bank.util.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Admin-side CRUD over Firestore + mass image upload to R2.
 * All calls are suspend and safe to invoke from a coroutine.
 */
class AdminRepository {

    private val db = FirebaseFirestore.getInstance()

    private val categories get() = db.collection("categories")
    private val groups get() = db.collection("groups")
    private val collections get() = db.collection("collections")
    private val stickers get() = db.collection("stickers")
    private val templateCategories get() = db.collection("canvasTemplateCategories")
    private val canvasTemplates get() = db.collection("canvasTemplates")

    private fun now() = System.currentTimeMillis()

    // ---- Categories ----
    suspend fun listCategories(): List<AdminCategory> = withContext(Dispatchers.IO) {
        categories.orderBy("order", Query.Direction.ASCENDING).get().await()
            .toObjects(AdminCategory::class.java)
    }

    suspend fun addCategory(name: String) = withContext(Dispatchers.IO) {
        val doc = categories.document()
        doc.set(AdminCategory(id = doc.id, name = name.trim(), order = now().toInt(), createdAt = now())).await()
    }

    // ---- Groups ----
    suspend fun listGroups(categoryId: String): List<AdminGroup> = withContext(Dispatchers.IO) {
        groups.whereEqualTo("categoryId", categoryId).get().await()
            .toObjects(AdminGroup::class.java).sortedBy { it.order }
    }

    suspend fun addGroup(categoryId: String, name: String) = withContext(Dispatchers.IO) {
        val doc = groups.document()
        doc.set(AdminGroup(id = doc.id, categoryId = categoryId, name = name.trim(), order = now().toInt(), createdAt = now())).await()
    }

    // ---- Collections ----
    suspend fun listCollections(groupId: String): List<AdminCollection> = withContext(Dispatchers.IO) {
        collections.whereEqualTo("groupId", groupId).get().await()
            .toObjects(AdminCollection::class.java).sortedBy { it.order }
    }

    suspend fun addCollection(
        categoryId: String,
        groupId: String,
        name: String,
        animated: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        val doc = collections.document()
        doc.set(
            AdminCollection(
                id = doc.id, categoryId = categoryId, groupId = groupId,
                name = name.trim(), animated = animated, order = now().toInt(), createdAt = now(),
            ),
        ).await()
    }

    // ---- Stickers ----
    suspend fun listStickers(collectionId: String): List<AdminSticker> = withContext(Dispatchers.IO) {
        stickers.whereEqualTo("collectionId", collectionId).get().await()
            .toObjects(AdminSticker::class.java).sortedBy { it.createdAt }
    }

    suspend fun deleteSticker(id: String) = withContext(Dispatchers.IO) {
        stickers.document(id).delete().await()
    }

    suspend fun deleteCategory(id: String) = withContext(Dispatchers.IO) { categories.document(id).delete().await() }
    suspend fun deleteGroup(id: String) = withContext(Dispatchers.IO) { groups.document(id).delete().await() }
    suspend fun deleteCollection(id: String) = withContext(Dispatchers.IO) { collections.document(id).delete().await() }

    /**
     * Mass-upload: for each image, normalise to a 512 webp, push to R2, then write a
     * sticker doc. [tags] are applied to every sticker (plus the collection name as a tag).
     * Returns the number successfully uploaded.
     */
    suspend fun massUpload(
        context: Context,
        collection: AdminCollection,
        categoryName: String,
        groupName: String,
        imageUris: List<Uri>,
        tags: List<String>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val baseTags = (tags + listOf(categoryName, groupName, collection.name))
            .map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        var ok = 0
        imageUris.forEachIndexed { i, uri ->
            runCatching {
                val bmp = BitmapUtils.loadUriBitmap(context, uri) ?: return@runCatching
                val webp = BitmapUtils.toWhatsAppWebp(bmp)
                val key = "stickers/${collection.id}/${now()}_$i.webp"
                val url = R2Uploader.uploadBytes(webp, key)
                val doc = stickers.document()
                doc.set(
                    AdminSticker(
                        id = doc.id,
                        categoryId = collection.categoryId,
                        groupId = collection.groupId,
                        collectionId = collection.id,
                        imageUrl = url,
                        tags = baseTags,
                        createdAt = now(),
                    ),
                ).await()
                ok++
            }
            onProgress(i + 1, imageUris.size)
        }
        ok
    }

    /**
     * Mass-upload animated .mp4 files: uploads each raw (no re-encoding) to R2 and writes a
     * sticker doc with animated=true. Returns the number successfully uploaded.
     */
    suspend fun massUploadAnimated(
        context: Context,
        collection: AdminCollection,
        categoryName: String,
        groupName: String,
        videoUris: List<Uri>,
        tags: List<String>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val baseTags = (tags + listOf(categoryName, groupName, collection.name))
            .map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        var ok = 0
        videoUris.forEachIndexed { i, uri ->
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching
                val key = "animations/${collection.id}/${now()}_$i.mp4"
                val url = R2Uploader.uploadBytes(bytes, key, contentType = "video/mp4")
                val doc = stickers.document()
                doc.set(
                    AdminSticker(
                        id = doc.id,
                        categoryId = collection.categoryId,
                        groupId = collection.groupId,
                        collectionId = collection.id,
                        imageUrl = url,
                        tags = baseTags,
                        animated = true,
                        createdAt = now(),
                    ),
                ).await()
                ok++
            }
            onProgress(i + 1, videoUris.size)
        }
        ok
    }

    // ---- Canvas template categories (named rows, each with a locked-in shape) ----

    suspend fun listTemplateCategories(): List<AdminTemplateCategory> = withContext(Dispatchers.IO) {
        templateCategories.orderBy("order", Query.Direction.ASCENDING).get().await()
            .toObjects(AdminTemplateCategory::class.java)
    }

    /**
     * Idempotent by name (retyping an existing category just re-locks its ratio) — mirrors the
     * web admin's addCanvasTemplateCategory. Any templates already filed under this name are
     * re-synced to the new ratio so a whole row always stays one consistent shape.
     */
    suspend fun addTemplateCategory(name: String, ratio: Float, shapeLabel: String): String = withContext(Dispatchers.IO) {
        val clean = name.trim()
        val existing = templateCategories.get().await().documents
            .firstOrNull { (it.getString("name") ?: "").trim().equals(clean, ignoreCase = true) }
        val id = if (existing != null) {
            existing.reference.update(mapOf("ratio" to ratio.toDouble(), "shapeLabel" to shapeLabel)).await()
            existing.id
        } else {
            val doc = templateCategories.document()
            doc.set(
                AdminTemplateCategory(id = doc.id, name = clean, ratio = ratio, shapeLabel = shapeLabel, order = now().toInt(), createdAt = now()),
            ).await()
            doc.id
        }
        val snap = canvasTemplates.whereEqualTo("category", clean).get().await()
        snap.documents.forEach { it.reference.update("ratio", ratio.toDouble()).await() }
        id
    }

    /**
     * Deletes the row AND every template filed under it. The app's Templates tab groups
     * templates by their own category name, not by these rows — so deleting just the row (as
     * this used to) left its templates still showing in the app, while this admin, which only
     * lists templates under rows that exist, showed nothing. Returns how many templates went too.
     */
    suspend fun deleteTemplateCategory(id: String, name: String): Int = withContext(Dispatchers.IO) {
        val removed = deleteTemplatesInCategory(name)
        templateCategories.document(id).delete().await()
        removed
    }

    /** Deletes every template filed under [name] (matched the way the app groups them). */
    suspend fun deleteTemplatesInCategory(name: String): Int = withContext(Dispatchers.IO) {
        val docs = canvasTemplates.get().await().documents.filter { sameTemplateCategory(it.getString("category"), name) }
        docs.forEach { it.reference.delete().await() }
        docs.size
    }

    /**
     * Template groups (category name → template count) that match no row — left behind by a row
     * deleted before deletes took its templates with them, or filed under a name that never had
     * a row. The app still shows them, so they're listed here to be opened or cleared. A blank
     * name means no category at all (the app shows those under "More").
     */
    suspend fun listLeftoverTemplateGroups(): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        val rows = templateCategories.get().await().documents
            .mapTo(HashSet()) { (it.getString("name") ?: "").trim().lowercase() }
        canvasTemplates.get().await().documents
            .groupBy { (it.getString("category") ?: "").trim() }
            .filterKeys { it.lowercase() !in rows }
            .map { (name, docs) -> name to docs.size }
            .sortedBy { it.first.lowercase() }
    }

    /** Same matching as the app's own row grouping: trimmed, and case-insensitive here so a row
     * still finds templates saved with slightly different capitalisation. */
    private fun sameTemplateCategory(a: String?, b: String) = (a ?: "").trim().equals(b.trim(), ignoreCase = true)

    // ---- Canvas templates (Templates tab) ----

    suspend fun listCanvasTemplates(category: String): List<AdminCanvasTemplate> = withContext(Dispatchers.IO) {
        canvasTemplates.get().await().documents.filter { sameTemplateCategory(it.getString("category"), category) }.map { d ->
            AdminCanvasTemplate(
                id = d.id,
                title = d.getString("title").orEmpty(),
                image = d.getString("image").orEmpty(),
                category = d.getString("category").orEmpty(),
                ratio = (d.getDouble("ratio") ?: 0.0).toFloat(),
                order = (d.getLong("order") ?: 0L).toInt(),
                createdAt = d.getLong("createdAt") ?: 0L,
            )
        }.sortedBy { it.order }
    }

    suspend fun deleteCanvasTemplate(id: String) = withContext(Dispatchers.IO) {
        canvasTemplates.document(id).delete().await()
    }

    /**
     * Uploads one piece of template art (base photo, an image/shape layer, or the card
     * thumbnail) and returns its public R2 URL. Kept aspect-correct — [BitmapUtils.toWhatsAppWebp]
     * would force a square crop, which is wrong for anything but the thumbnail's own shape.
     */
    suspend fun uploadTemplateArt(bitmap: android.graphics.Bitmap, templateKey: String, tag: String): String =
        withContext(Dispatchers.IO) {
            val webp = BitmapUtils.toAspectWebp(bitmap)
            R2Uploader.uploadBytes(webp, "canvas_templates/$templateKey/${tag}_${now()}.webp")
        }

    /**
     * Writes one `canvasTemplates` doc. [config] is the raw Firestore-shaped map the app's
     * CatalogRepository.parseTemplateConfig expects back — built by the editor itself (see
     * CanvasTemplateEditorScreen.buildAdminConfig) since it already holds every layer's live
     * state; this call just uploads the thumbnail and writes the doc.
     */
    suspend fun addCanvasTemplate(
        title: String,
        thumbnail: android.graphics.Bitmap,
        category: String,
        tags: List<String>,
        ratio: Float,
        config: Map<String, Any?>,
    ): String = withContext(Dispatchers.IO) {
        val clean = category.trim()
        val key = "${clean.ifBlank { "uncategorized" }}_${now()}"
        val imageUrl = uploadTemplateArt(thumbnail, key, "thumb")
        val doc = canvasTemplates.document()
        doc.set(
            mapOf(
                "title" to title.trim(),
                "image" to imageUrl,
                "textColor" to "",
                "bgColor" to (config["bgColor"] ?: ""),
                "config" to config,
                "category" to clean,
                "tags" to tags.map { it.trim().lowercase() }.filter { it.isNotBlank() },
                "ratio" to ratio.toDouble(),
                "order" to now().toInt(),
                "createdAt" to now(),
            ),
        ).await()
        doc.id
    }

    /**
     * Re-saves an EXISTING `canvasTemplates` doc in place — same shape as [addCanvasTemplate],
     * but a partial `.update()` on [id] instead of a new document, so editing a template updates
     * it rather than leaving the original behind alongside a duplicate. Category/tags/order/
     * createdAt are left untouched; only what the editor can actually change is written.
     */
    suspend fun updateCanvasTemplate(
        id: String,
        title: String,
        thumbnail: android.graphics.Bitmap,
        ratio: Float,
        config: Map<String, Any?>,
    ): Unit = withContext(Dispatchers.IO) {
        val imageUrl = uploadTemplateArt(thumbnail, "${id}_edit", "thumb")
        canvasTemplates.document(id).update(
            mapOf(
                "title" to title.trim(),
                "image" to imageUrl,
                "bgColor" to (config["bgColor"] ?: ""),
                "config" to config,
                "ratio" to ratio.toDouble(),
            ),
        ).await()
    }
}
