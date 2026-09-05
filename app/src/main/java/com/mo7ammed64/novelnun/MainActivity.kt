package com.mo7ammed64.novelnun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mo7ammed64.novelnun.ui.nav.NovelNunApp
import com.mo7ammed64.novelnun.ui.theme.NovelNunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovelNunTheme {
                NovelNunApp()
            }
        }
    }
}
