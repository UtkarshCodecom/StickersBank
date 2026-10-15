package com.stickers.bank.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.EmptyState
import com.stickers.bank.ui.components.SbBackHeader
import com.stickers.bank.ui.components.StickerThumb
import com.stickers.bank.ui.detail.StickerDetailSheet
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.theme.AppOnSurfaceMuted

/** Opened from Settings — every sticker the user favorited, from anywhere. */
@Composable
fun FavoritesScreen(nav: NavHostController) {
    val catalog by ServiceLocator.catalogRepository.catalog.collectAsStateSafe()
    val custom by ServiceLocator.customStickerRepository.stickers.collectAsStateSafe()
    val favoriteIds by ServiceLocator.favoritesRepository.ids.collectAsStateSafe()
    var detail by remember { mutableStateOf<Sticker?>(null) }

    val results = remember(catalog, custom, favoriteIds) {
        val all = (custom + catalog.allStickers()).distinctBy { it.id }.associateBy { it.id }
        favoriteIds.mapNotNull { all[it] }
    }

    Column(Modifier.fillMaxSize()) {
        SbBackHeader("Favorites", { nav.popBackStack() })
        Text(
            "${results.size} sticker${if (results.size == 1) "" else "s"}",
            color = AppOnSurfaceMuted, fontSize = 12.sp,
            modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
        )
        if (results.isEmpty()) {
            EmptyState("No favorites yet", "Tap the heart on any sticker's popup to save it here.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                gridItems(results, key = { it.id }) { s -> StickerThumb(sticker = s, onClick = { detail = s }) }
            }
        }
    }

    detail?.let { s ->
        StickerDetailSheet(
            stickers = results,
            initialId = s.id,
            onDismiss = { detail = null },
            onEdit = { nav.navigate(Routes.editor(stickerId = it.id, animated = it.animated)) },
        )
    }
}
