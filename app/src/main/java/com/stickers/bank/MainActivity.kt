package com.stickers.bank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stickers.bank.data.ServiceLocator
import com.stickers.bank.ui.StickersBankApp
import com.stickers.bank.ui.SystemChrome
import com.stickers.bank.ui.onboarding.WelcomeScreen
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.Neo
import com.stickers.bank.ui.theme.NeoPalettes
import com.stickers.bank.ui.theme.StickersBankTheme

/** The app itself. Launched by [SplashActivity], which owns the launch animation. */
class MainActivity : ComponentActivity() {
    override fun onStop() {
        super.onStop()
        // Going to the background is the last reliable moment to send what's been counted.
        ServiceLocator.analytics.flush()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A real launch — not a rotation or the system recreating this activity.
        if (savedInstanceState == null) ServiceLocator.analytics.appOpened()
        setContent {
            // Keep Neo.current in sync with the persisted choice — live, so picking a new
            // theme in Settings re-themes every screen immediately.
            LaunchedEffect(Unit) {
                ServiceLocator.themePreferences.themeId.collect { Neo.current = NeoPalettes.byId(it) }
            }
            StickersBankTheme {
                val seenWelcome by ServiceLocator.onboardingPreferences.seenWelcome.collectAsState()
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(AppBackground)) {
                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                            StickersBankApp()
                        }
                        if (SystemChrome.lightStatusBar) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                                    .align(Alignment.TopCenter)
                                    .background(Color.White),
                            )
                        }
                    }
                    // First launch only — bottom sheet slides up over the app.
                    if (!seenWelcome) {
                        WelcomeScreen(onContinue = { ServiceLocator.onboardingPreferences.markWelcomeSeen() })
                    }
                    // "Rate us on the Play Store", after the user's come back from sending stickers.
                    com.stickers.bank.ui.RatePromptHost()
                }
            }
        }
    }
}
