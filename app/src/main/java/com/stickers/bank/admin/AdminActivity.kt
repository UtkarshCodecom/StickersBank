package com.stickers.bank.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stickers.bank.admin.data.AdminRepository
import com.stickers.bank.admin.ui.AdminCategoriesScreen
import com.stickers.bank.admin.ui.AdminCollectionDetailScreen
import com.stickers.bank.admin.ui.AdminCollectionsScreen
import com.stickers.bank.admin.ui.AdminGroupsScreen
import com.stickers.bank.admin.ui.AdminSelection
import com.stickers.bank.admin.ui.AdminTemplateCategoriesScreen
import com.stickers.bank.admin.ui.AdminTemplateListScreen
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.StickersBankTheme
import com.stickers.bank.ui.theme.AppSurface
import com.stickers.bank.ui.theme.AppSurfaceVariant

object AdminConfig {
    // Change this PIN. (Local gate only — see security note in R2Config.)
    const val PIN = "1947"
}

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StickersBankTheme {
                Surface(Modifier.fillMaxSize().background(AppBackground), color = AppBackground) {
                    AdminRoot()
                }
            }
        }
    }
}

@Composable
private fun AdminRoot() {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        PinGate(onUnlock = { unlocked = true })
    } else {
        AdminNav()
    }
}

@Composable
private fun PinGate(onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("ADMIN", color = Gold, fontWeight = FontWeight.Black, letterSpacing = 6.sp, fontSize = 26.sp)
        Text(
            "Enter PIN to manage the catalog",
            color = AppOnSurfaceMuted, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        TextField(
            value = pin,
            onValueChange = {
                pin = it.filter { c -> c.isDigit() }.take(8); error = false
                if (pin == AdminConfig.PIN) onUnlock()
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = error,
            placeholder = { Text("PIN", color = AppOnSurfaceMuted) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppSurface, unfocusedContainerColor = AppSurface,
                focusedIndicatorColor = Gold, unfocusedIndicatorColor = AppSurfaceVariant,
                cursorColor = Gold, focusedTextColor = AppOnSurface, unfocusedTextColor = AppOnSurface,
            ),
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
        )
        if (error) Text("Wrong PIN", color = com.stickers.bank.ui.theme.Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        Box(
            Modifier.padding(top = 20.dp).clip(RoundedCornerShape(12.dp)).background(Gold)
                .clickable { if (pin == AdminConfig.PIN) onUnlock() else error = true }
                .padding(horizontal = 40.dp, vertical = 12.dp),
        ) { Text("Unlock", color = OnGold, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AdminNav() {
    val nav = rememberNavController()
    val repo = remember { AdminRepository() }
    val selection = remember { AdminSelection() }
    NavHost(
        navController = nav,
        startDestination = "categories",
        modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
    ) {
        composable("categories") { AdminCategoriesScreen(nav, repo, selection) }
        composable("groups") { AdminGroupsScreen(nav, repo, selection) }
        composable("collections") { AdminCollectionsScreen(nav, repo, selection) }
        composable("detail") { AdminCollectionDetailScreen(nav, repo, selection) }
        composable("templateCategories") { AdminTemplateCategoriesScreen(nav, repo, selection) }
        composable("templateList") { AdminTemplateListScreen(nav, repo, selection) }
        composable("templateEditor") {
            val cat = selection.templateCategory
            if (cat != null) {
                // A real template id (set by AdminTemplateListScreen when a card is tapped)
                // opens that template's own saved content for editing; null builds a fresh
                // blank canvas at the row's locked shape, same as before.
                com.stickers.bank.ui.templates.CanvasTemplateEditorScreen(
                    nav = nav,
                    templateId = selection.editingTemplateId
                        ?: com.stickers.bank.ui.templates.blankCanvasId(
                            cat.ratio.takeIf { it > 0f } ?: com.stickers.bank.ui.templates.DEFAULT_TEMPLATE_RATIO,
                        ),
                    adminMode = true,
                    adminCategory = cat.name,
                )
            }
        }
    }
}
