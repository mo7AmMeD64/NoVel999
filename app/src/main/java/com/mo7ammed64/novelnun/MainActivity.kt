package com.mo7ammed64.novelnun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mo7ammed64.novelnun.ui.nav.NovelNunApp
import com.mo7ammed64.novelnun.ui.settings.AppSettings
import com.mo7ammed64.novelnun.ui.theme.NovelNunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            val appSettings = remember { AppSettings(applicationContext) }
            NovelNunTheme(appFont = appSettings.font) {
                NovelNunApp(settings = appSettings)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply immersive mode after returning from the launcher or after the user reveals
        // the bars with a swipe. The bars can still be revealed temporarily with a system swipe.
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).run {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
