package com.stickers.bank.ui.templates

/**
 * The full "create your own canvas" size catalog — every category and named preset a Canva-like
 * picker would offer (social media, print, business, events, food, mobile, education, planners,
 * website...). Only each preset's ratio (width/height) ever reaches the canvas itself; the
 * width/height/unit are kept purely to render the size text ("1080 × 1920 px") next to the name,
 * so mm/in/px can sit in the same list without conversion — the unit cancels out of a ratio.
 */
enum class SizeUnit(val label: String) { PX("px"), MM("mm"), IN("in") }

data class CanvasSizePreset(
    val name: String,
    val width: Float,
    val height: Float,
    val unit: SizeUnit,
) {
    /** width/height, matching the convention every other ratio in this app already uses. */
    val ratio: Float get() = width / height

    val sizeText: String get() = "${fmt(width)} × ${fmt(height)} ${unit.label}"

    private fun fmt(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()
}

data class CanvasSizeCategory(
    val emoji: String,
    val name: String,
    val presets: List<CanvasSizePreset>,
)

private fun px(name: String, w: Float, h: Float) = CanvasSizePreset(name, w, h, SizeUnit.PX)
private fun mm(name: String, w: Float, h: Float) = CanvasSizePreset(name, w, h, SizeUnit.MM)
private fun inch(name: String, w: Float, h: Float) = CanvasSizePreset(name, w, h, SizeUnit.IN)

val CANVAS_SIZE_CATEGORIES: List<CanvasSizeCategory> = listOf(
    // Video-format sizes (YouTube Shorts/Video, Instagram Reel, TikTok Video) are deliberately
    // left out for now — this editor only ever produces a static image, and offering a "video"
    // dimension here just to export a still frame from it is misleading. Revisit once the
    // editor can actually export video.
    CanvasSizeCategory("▶️", "YouTube", listOf(
        px("YouTube Thumbnail", 1280f, 720f),
        px("YouTube Channel Banner", 2560f, 1440f),
        px("YouTube Profile Picture", 800f, 800f),
    )),
    CanvasSizeCategory("📱", "Social Media", listOf(
        px("Instagram Post", 1080f, 1080f),
        px("Instagram Portrait", 1080f, 1350f),
        px("Instagram Landscape", 1080f, 566f),
        px("Instagram Story", 1080f, 1920f),
        px("Facebook Post", 1200f, 630f),
        px("Facebook Story", 1080f, 1920f),
        px("Facebook Cover", 1640f, 856f),
        px("Facebook Profile", 1080f, 1080f),
        px("X / Twitter Post", 1600f, 900f),
        px("X / Twitter Header", 1500f, 500f),
        px("LinkedIn Post", 1200f, 627f),
        px("LinkedIn Cover", 1584f, 396f),
        px("Pinterest Pin", 1000f, 1500f),
        px("Pinterest Board Cover", 600f, 600f),
        px("Snapchat Story", 1080f, 1920f),
        px("WhatsApp Status", 1080f, 1920f),
        px("Reddit Post", 1200f, 628f),
    )),
    CanvasSizeCategory("🖨️", "Paper / Printing", listOf(
        mm("A0", 841f, 1189f),
        mm("A1", 594f, 841f),
        mm("A2", 420f, 594f),
        mm("A3", 297f, 420f),
        mm("A4", 210f, 297f),
        mm("A5", 148f, 210f),
        mm("A6", 105f, 148f),
        mm("A7", 74f, 105f),
        mm("A8", 52f, 74f),
        inch("Letter", 8.5f, 11f),
        inch("Legal", 8.5f, 14f),
        inch("Tabloid", 11f, 17f),
        inch("Executive", 7.25f, 10.5f),
    )),
    CanvasSizeCategory("📢", "Marketing", listOf(
        mm("Flyer A4", 210f, 297f),
        mm("Flyer A5", 148f, 210f),
        mm("Flyer A6", 105f, 148f),
        mm("Poster A4", 210f, 297f),
        mm("Poster A3", 297f, 420f),
        mm("Poster A2", 420f, 594f),
        mm("Poster A1", 594f, 841f),
        px("Billboard", 1200f, 600f),
        px("Web Banner", 1920f, 600f),
        px("Web Ad (Medium Rectangle)", 300f, 250f),
        px("Large Rectangle Ad", 336f, 280f),
        px("Leaderboard Ad", 728f, 90f),
        px("Mobile Banner Ad", 320f, 50f),
    )),
    CanvasSizeCategory("💼", "Business", listOf(
        mm("Business Card", 90f, 50f),
        inch("US Business Card", 3.5f, 2f),
        mm("Letterhead", 210f, 297f),
        mm("Invoice", 210f, 297f),
        mm("Resume / CV", 210f, 297f),
        mm("Report", 210f, 297f),
        mm("Proposal", 210f, 297f),
        px("Presentation 16:9", 1920f, 1080f),
        px("Presentation 4:3", 1024f, 768f),
        mm("Certificate Landscape", 297f, 210f),
        mm("Certificate Portrait", 210f, 297f),
    )),
    CanvasSizeCategory("🎉", "Events", listOf(
        inch("Invitation", 5f, 7f),
        inch("Wedding Invitation", 5f, 7f),
        inch("Birthday Invitation", 5f, 7f),
        inch("Greeting Card", 5f, 7f),
        inch("Thank You Card", 5f, 7f),
        inch("Postcard", 6f, 4f),
        inch("Ticket", 5.5f, 2.125f),
        inch("RSVP Card", 5f, 3.5f),
        inch("Wedding Program", 5.5f, 8.5f),
    )),
    CanvasSizeCategory("🍔", "Restaurant / Food", listOf(
        mm("Restaurant Menu", 210f, 297f),
        mm("Menu Card", 148f, 210f),
        inch("Table Menu", 4f, 6f),
        inch("Table Tent", 4f, 6f),
        mm("Food Poster", 297f, 420f),
        mm("Food Flyer", 210f, 297f),
        px("Food Instagram Post", 1080f, 1080f),
        px("Food Story", 1080f, 1920f),
        px("Restaurant Banner", 1920f, 600f),
    )),
    CanvasSizeCategory("📱", "Mobile / App", listOf(
        px("Mobile Wallpaper", 1080f, 1920f),
        px("iPhone Wallpaper", 1290f, 2796f),
        px("Android Wallpaper", 1440f, 3200f),
        px("App Icon", 1024f, 1024f),
        px("App Store Screenshot", 1290f, 2796f),
        px("Play Store Graphic", 1024f, 500f),
    )),
    CanvasSizeCategory("📚", "Education", listOf(
        mm("Worksheet", 210f, 297f),
        mm("Assignment", 210f, 297f),
        mm("Study Planner", 210f, 297f),
        inch("Flashcard", 3f, 5f),
        mm("Certificate", 210f, 297f),
        mm("Classroom Poster", 297f, 420f),
        mm("School ID Card", 85.6f, 54f),
        mm("Timetable", 210f, 297f),
    )),
    CanvasSizeCategory("📅", "Planners & Documents", listOf(
        mm("Calendar", 210f, 297f),
        mm("Monthly Planner", 210f, 297f),
        mm("Weekly Planner", 210f, 297f),
        mm("Daily Planner", 210f, 297f),
        mm("Journal", 148f, 210f),
        mm("Notebook", 148f, 210f),
        mm("Checklist", 210f, 297f),
        mm("To-Do List", 210f, 297f),
    )),
    CanvasSizeCategory("🌐", "Website", listOf(
        px("Website Desktop", 1920f, 1080f),
        px("Website Banner", 1920f, 600f),
        px("Website Header", 1920f, 400f),
        px("Blog Banner", 1200f, 630f),
        px("Email Header", 600f, 200f),
        px("Email Newsletter", 600f, 1200f),
    )),
)
