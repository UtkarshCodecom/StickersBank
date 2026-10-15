package com.stickers.bank.ui.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.data.model.Bank
import com.stickers.bank.ui.Routes
import com.stickers.bank.ui.components.PrimaryPillButton
import com.stickers.bank.ui.components.checkerboard
import com.stickers.bank.ui.detail.collectAsStateSafe
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceElevated
import com.stickers.bank.ui.theme.neuInset
import com.stickers.bank.ui.theme.neuRaised

@Composable
fun BankScreen(nav: NavHostController) {
    val banks by ServiceLocator.bankRepository.banks.collectAsStateSafe()
    var showCreate by remember { mutableStateOf(false) }
    // Persisted so scroll position survives opening a bank and coming back.
    val listState = com.stickers.bank.ui.components.rememberSavedListState("bank_list")

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (banks.isEmpty()) {
            CreateBankHero(onCreate = { showCreate = true })
        } else {
            PrimaryPillButton(
                text = "＋  Create bank",
                onClick = { showCreate = true },
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
            )
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(banks, key = { it.id }) { bank ->
                    BankRow(
                        bank = bank,
                        onOpen = { nav.navigate(Routes.bankDetail(bank.id)) },
                        onDelete = { ServiceLocator.bankRepository.deleteBank(bank.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        com.stickers.bank.ui.components.BankNameDialog(
            title = "New bank",
            allowExisting = false,
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                ServiceLocator.bankRepository.createBank(name)
                showCreate = false
            },
        )
    }
}

/**
 * A catchy hero shown in place of the plain "no banks yet" text + a separate top button — one
 * illustrated panel that explains what a bank is AND is the "create" action, instead of two
 * disconnected pieces.
 */
@Composable
private fun CreateBankHero(onCreate: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .neuRaised(cornerRadius = 28.dp, elevation = 9.dp)
            .clip(RoundedCornerShape(28.dp))
            .padding(horizontal = 26.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(74.dp).neuRaised(cornerRadius = 37.dp, elevation = 7.dp, background = Gold).clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Bookmarks, null, tint = OnGold, modifier = Modifier.size(36.dp)) }
        Text(
            "Build Your First Bank",
            color = AppOnSurface, fontWeight = FontWeight.Black, fontSize = 20.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            "Your stickers. Your collection. Your bank.",
            color = AppOnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "Save stickers from anywhere in the app, then send your entire bank to WhatsApp " +
                "or Telegram in one tap.",
            color = AppOnSurfaceMuted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
        )
        Row(
            Modifier
                .neuRaised(cornerRadius = 50.dp, elevation = 7.dp, background = Gold)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onCreate)
                .padding(horizontal = 26.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = OnGold, modifier = Modifier.size(18.dp))
            Text("Create a bank", color = OnGold, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun BankRow(bank: Bank, onOpen: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            // Floor the height so an empty bank isn't a thin strip and a filled one has room to
            // breathe around its preview row.
            .heightIn(min = 156.dp)
            .neuRaised(cornerRadius = 16.dp, elevation = 7.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(bank.name, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${bank.stickers.size}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Icon(
                Icons.Rounded.Delete, "Delete bank", tint = AppOnSurfaceMuted,
                modifier = Modifier.padding(start = 12.dp).size(20.dp).clickable(onClick = onDelete),
            )
        }
        if (bank.stickers.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                bank.stickers.take(5).forEach { s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .neuInset(cornerRadius = 10.dp, elevation = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = s.coilModel(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp), // clear of the inset well's inner shadow
                        )
                    }
                }
                repeat(5 - bank.stickers.take(5).size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CreateBankDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurfaceElevated,
        title = { Text("New bank", color = AppOnSurface) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Bank name", color = AppOnSurfaceMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface,
                    focusedIndicatorColor = Gold,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Gold,
                    focusedTextColor = AppOnSurface,
                    unfocusedTextColor = AppOnSurface,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.ifBlank { "My Bank" }) }) {
                Text("Create", color = Gold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AppOnSurfaceMuted) }
        },
    )
}
