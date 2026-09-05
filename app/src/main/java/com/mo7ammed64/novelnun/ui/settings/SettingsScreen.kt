package com.mo7ammed64.novelnun.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mo7ammed64.novelnun.ui.reader.ReaderSettingsSheet

@Composable
fun SettingsScreen(settings: AppSettings) {
    var showFontPicker by rememberSaveable { mutableStateOf(false) }
    var showReaderSettings by rememberSaveable { mutableStateOf(false) }

    // Settings are intentionally English, even on an Arabic/RTL device. Novel content retains
    // its own direction; this override does not change the rest of the app.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            ) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
                SettingsHeading("Appearance")
                ListItem(
                    modifier = Modifier.clickable { showFontPicker = true },
                    headlineContent = { Text("App font") },
                    supportingContent = { Text(settings.currentFont.displayName) },
                    trailingContent = { Text("Aa", style = MaterialTheme.typography.titleLarge) },
                )
                ListItem(
                    headlineContent = { Text("App theme") },
                    supportingContent = { Text("Dark · Immersive fullscreen") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsHeading("Reading")
                ListItem(
                    modifier = Modifier.clickable { showReaderSettings = true },
                    headlineContent = { Text("Reader preferences") },
                    supportingContent = { Text("Font, spacing, text direction and background") },
                    trailingContent = { Icon(Icons.Default.Tune, contentDescription = null) },
                )
                ListItem(
                    modifier = Modifier.toggleable(
                        value = settings.reverseChapterOrder,
                        role = Role.Switch,
                        onValueChange = settings::updateReverseChapterOrder,
                    ),
                    headlineContent = { Text("Reverse chapter order") },
                    supportingContent = {
                        Text(if (settings.reverseChapterOrder) "Newest to oldest" else "Oldest to newest")
                    },
                    trailingContent = {
                        Switch(checked = settings.reverseChapterOrder, onCheckedChange = null)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsHeading("About")
                ListItem(headlineContent = { Text("Source") }, supportingContent = { Text("kolnovel.com") })
                ListItem(headlineContent = { Text("Version") }, supportingContent = { Text("1.1.0") })
            }
        }
        if (showFontPicker) {
            FontPickerDialog(settings, FontTarget.APP, onDismiss = { showFontPicker = false })
        }
        if (showReaderSettings) {
            ReaderSettingsSheet(settings = settings, onDismiss = { showReaderSettings = false })
        }
    }
}

@Composable
private fun SettingsHeading(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
