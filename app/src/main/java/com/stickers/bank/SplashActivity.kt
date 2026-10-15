package com.stickers.bank

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.stickers.bank.data.ServiceLocator
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Launcher entry point: plays [SplashLogoView] on black, then opens [MainActivity].
 *
 * The home screen's data is already loading while this plays — StickersBankApplication.onCreate
 * runs ServiceLocator.init, whose CatalogRepository fires its Firestore queries immediately — so
 * by the time MainActivity appears the catalog is usually there (or served from Firestore's cache).
 */
class SplashActivity : ComponentActivity() {

    private val launched = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private val safetyTimeout = Runnable { openMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Hands off from the system splash (black, with a transparent icon — see
        // Theme.StickersBank.Starting) so Android 12+ never flashes the launcher icon first.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        ServiceLocator.catalogRepository // already loading; referenced so the dependency is explicit

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val logo = SplashLogoView(this).apply { onAnimationEnd = ::openMain }
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
                addView(logo, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            },
        )
        // The whole sequence runs ~5.2 s; this only ever fires if something stalls it.
        handler.postDelayed(safetyTimeout, SAFETY_TIMEOUT_MS)
    }

    /** Safe to call from both the animation's end and the timeout — only the first one launches. */
    private fun openMain() {
        if (!launched.compareAndSet(false, true)) return
        handler.removeCallbacks(safetyTimeout)
        startActivity(
            Intent(this, MainActivity::class.java),
            ActivityOptionsCompat.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle(),
        )
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(safetyTimeout)
        super.onDestroy()
    }

    private companion object {
        const val SAFETY_TIMEOUT_MS = 6_500L
    }
}
