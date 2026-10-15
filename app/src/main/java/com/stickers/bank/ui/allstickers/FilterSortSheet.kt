package com.stickers.bank.ui.allstickers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickers.bank.data.model.PopularTag
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.Danger
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceElevated
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.neuRaised
import kotlinx.serialization.Serializable

@Serializable
enum class SortOrder(val label: String) {
    /**
     * Default order — newest uploads first. Deliberately NOT offered as a button; it's
     * simply what you get when "Most tags" is switched off.
     */
    NEWEST("Newest first"),
    MOST_TAGS("Most tags"),
}

/** Which kind of stickers to show. Animated = a sticker that plays (animated webp). */
@Serializable
enum class ContentType(val label: String) {
    ALL("Both"),
    STATIC("Stickers"),
    ANIMATED("Animated"),
}

/** Complete filter state for the all-stickers screen. Categories are just tags. */
@Serializable
data class FilterState(
    val sort: SortOrder = SortOrder.NEWEST,
    val includeTags: Set<String> = emptySet(),
    val excludeTags: Set<String> = emptySet(),
    val contentType: ContentType = ContentType.ALL,
) {
    val activeCount: Int
        get() = includeTags.size + excludeTags.size +
            (if (sort != SortOrder.NEWEST) 1 else 0) +
            (if (contentType != ContentType.ALL) 1 else 0)

    /** True if this sticker passes the content-type filter. */
    fun allowsType(animated: Boolean): Boolean = when (contentType) {
        ContentType.ALL -> true
        ContentType.STATIC -> !animated
        ContentType.ANIMATED -> animated
    }

    fun toggle(tag: String): FilterState = when {
        tag in includeTags -> copy(includeTags = includeTags - tag, excludeTags = excludeTags + tag)
        tag in excludeTags -> copy(excludeTags = excludeTags - tag)
        else -> copy(includeTags = includeTags + tag)
    }

    fun stateOf(tag: String): ChipMode = when {
        tag in includeTags -> ChipMode.INCLUDE
        tag in excludeTags -> ChipMode.EXCLUDE
        else -> ChipMode.OFF
    }
}

enum class ChipMode { OFF, INCLUDE, EXCLUDE }

/** Sort order applied to search results. */
fun SortOrder.comparator(): Comparator<com.stickers.bank.data.model.Sticker> = when (this) {
    SortOrder.NEWEST -> compareByDescending { it.createdAt }
    SortOrder.MOST_TAGS -> compareByDescending { it.tags.size }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSortSheet(
    state: FilterState,
    categories: List<String>,
    popularTags: List<PopularTag>,
    onChange: (FilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AppSurface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filter & Sort", color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                if (state.activeCount > 0) {
                    Text(
                        "Reset",
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onChange(FilterState()) },
                    )
                }
            }

            SectionLabel("SHOW")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContentType.entries.forEach { t ->
                    val sel = state.contentType == t
                    Box(
                        Modifier
                            .padding(vertical = 3.dp)
                            .neuRaised(cornerRadius = 50.dp, elevation = 6.dp, background = if (sel) Gold else AppSurfaceVariant)
                            .clip(RoundedCornerShape(50))
                            .clickable { onChange(state.copy(contentType = t)) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(
                            t.label,
                            color = if (sel) AppBackground else AppOnSurface,
                            fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            if (categories.isNotEmpty()) {
                SectionLabel("CATEGORIES")
                TriChipFlow(categories, state, onChange)
            }

            if (popularTags.isNotEmpty()) {
                SectionLabel("POPULAR TAGS  ·  tap to include, tap again to exclude")
                TriChipFlow(popularTags.map { it.tag }, state, onChange)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriChipFlow(items: List<String>, state: FilterState, onChange: (FilterState) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { tag ->
            TriChip(tag, state.stateOf(tag)) { onChange(state.toggle(tag)) }
        }
    }
}

@Composable
private fun TriChip(label: String, mode: ChipMode, onClick: () -> Unit) {
    val bg = when (mode) {
        ChipMode.INCLUDE -> Gold
        ChipMode.EXCLUDE -> AppSurfaceElevated
        ChipMode.OFF -> AppSurfaceVariant
    }
    Box(
        Modifier
            .padding(vertical = 3.dp)
            .neuRaised(cornerRadius = 50.dp, elevation = 5.dp, background = bg)
            .clip(RoundedCornerShape(50))
            .then(if (mode == ChipMode.EXCLUDE) Modifier.border(1.dp, Danger, RoundedCornerShape(50)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mode == ChipMode.INCLUDE) {
                Icon(Icons.Rounded.Check, null, tint = AppBackground, modifier = Modifier.size(14.dp).padding(end = 0.dp))
                Text(" ", fontSize = 12.sp)
            }
            Text(
                text = (if (mode == ChipMode.EXCLUDE) "− " else "") + label,
                color = when (mode) {
                    ChipMode.INCLUDE -> AppBackground
                    ChipMode.EXCLUDE -> Danger
                    ChipMode.OFF -> AppOnSurfaceMuted
                },
                fontSize = 12.sp,
                fontWeight = if (mode != ChipMode.OFF) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Gold,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}
