package com.stickers.bank.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickers.bank.ui.theme.Danger
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.neuClickable
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised

@Composable
fun SbSearchBar(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search stickers",
    showLeadingIcon: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    // Idle = raised, like any other neumorphic surface; tapping in presses it to inset, the
    // same "pushed in" cue a real embossed control gives, held for as long as it's focused.
    var focused by remember { mutableStateOf(false) }
    TextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        leadingIcon = if (showLeadingIcon) { { Icon(Icons.Rounded.Search, null, tint = AppOnSurfaceMuted) } } else null,
        trailingIcon = {
            if (value.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close, "Clear", tint = AppOnSurfaceMuted,
                    modifier = Modifier.clickable { onChange("") },
                )
            }
        },
        placeholder = { Text(placeholder, color = AppOnSurfaceMuted, fontSize = 14.sp) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Gold,
            focusedTextColor = AppOnSurface,
            unfocusedTextColor = AppOnSurface,
        ),
        modifier = modifier
            .then(
                if (focused) Modifier.neuInset(cornerRadius = 16.dp, elevation = 6.dp)
                else Modifier.neuRaised(cornerRadius = 16.dp, elevation = 6.dp),
            )
            .onFocusChanged { focused = it.isFocused; onFocusChanged(it.isFocused) },
    )
}

@Composable
fun SbFilterButton(activeCount: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .neuClickable(cornerRadius = 16.dp, elevation = 7.dp, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Tune, "Filters", tint = if (activeCount > 0) Gold else AppOnSurface)
        if (activeCount > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp)
                    .clip(RoundedCornerShape(50)).background(Danger),
                contentAlignment = Alignment.Center,
            ) {
                Text("$activeCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SbActivePill(label: String, color: Color, onRemove: () -> Unit) {
    Row(
        Modifier
            .neuClickable(cornerRadius = 50.dp, elevation = 4.dp, onClick = onRemove)
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Icon(
            Icons.Rounded.Close, "Remove", tint = AppOnSurfaceMuted,
            modifier = Modifier.padding(start = 4.dp).size(14.dp),
        )
    }
}
