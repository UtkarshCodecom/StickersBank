package com.stickers.bank.ui

object Routes {
    const val FEATURED = "featured"

    // Admin-built starting canvases — tap one and it opens straight in the editor.
    const val CANVAS_TEMPLATES = "canvas_templates"

    const val NEW_ARRIVAL = "new_arrival"
    const val CREATE = "create"
    const val BANK = "bank"

    fun category(id: String) = "category/$id"
    const val CATEGORY = "category/{id}"

    fun collection(categoryId: String, groupId: String, collectionId: String) =
        "collection/$categoryId/$groupId/$collectionId"
    const val COLLECTION = "collection/{categoryId}/{groupId}/{collectionId}"

    fun editor(stickerId: String? = null, animated: Boolean = false) =
        "editor?stickerId=${stickerId ?: ""}&animated=$animated"
    const val EDITOR = "editor?stickerId={stickerId}&animated={animated}"

    // Canvas templates open ONLY here — a separate, purpose-built editor (see
    // CanvasTemplateEditorScreen), not the general sticker/photo editor above.
    fun templateEditor(templateId: String) = "template_editor/$templateId"
    const val TEMPLATE_EDITOR = "template_editor/{templateId}"

    fun bankDetail(id: String) = "bankDetail/$id"
    const val BANK_DETAIL = "bankDetail/{id}"

    // Stickers carrying a popular tag (opened from the Featured tag pills).
    fun tag(tag: String) = "tag/${android.net.Uri.encode(tag)}"
    const val TAG = "tag/{tag}"

    // A standalone Trending card's hand-picked stickers (opened from the Featured trending row).
    fun trending(id: String) = "trending/$id"
    const val TRENDING = "trending/{id}"

    // One canvas-template category in full (opened from a row's "View all").
    fun templateCategory(name: String) = "template_category/${android.net.Uri.encode(name)}"
    const val TEMPLATE_CATEGORY = "template_category/{name}"

    // Every Trending card at once (opened from the "See all" button on Featured).
    const val TRENDING_ALL = "trending_all"

    // The user's favorited stickers — opened from Settings (not a bottom-nav tab, to keep
    // the floating nav from getting crowded).
    const val FAVORITES = "favorites"

    const val SETTINGS = "settings"

    // WhatsApp status saver.
    const val STATUS = "status"

    // The auto-folder of the user's own editor creations (opened from Settings).
    const val MY_STICKERS = "my_stickers"

    val roots = setOf(FEATURED, CANVAS_TEMPLATES, NEW_ARRIVAL, CREATE, BANK, STATUS, SETTINGS)
}

enum class TopTab(val route: String, val label: String, val short: String) {
    Featured(Routes.FEATURED, "VIBE", "Vibe"),
    CanvasTemplates(Routes.CANVAS_TEMPLATES, "CANVAS", "Canvas"),
    NewArrival(Routes.NEW_ARRIVAL, "FRESH DROPS", "Fresh Drops"),
    Create(Routes.CREATE, "CREATE", "Create"),
    Bank(Routes.BANK, "BANK", "Bank"),
    Status(Routes.STATUS, "STATUS", "Status"),
    Settings(Routes.SETTINGS, "SETTINGS", "Settings"),
}
