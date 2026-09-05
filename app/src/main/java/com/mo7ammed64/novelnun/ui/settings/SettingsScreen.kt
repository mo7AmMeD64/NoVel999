package com.mo7ammed64.novelnun.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(settings: AppSettings) {
    var showFontPicker by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val fontImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { importError = settings.importFont(context, it) }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            ListItem(
                modifier = Modifier.clickable { showFontPicker = true },
                headlineContent = { Text("App font") },
                supportingContent = { Text(settings.currentFont.displayName) },
                trailingContent = { Text("Aa", style = MaterialTheme.typography.titleLarge) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                modifier = Modifier.clickable {
                    settings.updateReverseChapterOrder(!settings.reverseChapterOrder)
                },
                headlineContent = { Text("Reverse chapter order") },
                supportingContent = {
                    Text(
                        if (settings.reverseChapterOrder) "Newest first"
                        else "Oldest first",
                    )
                },
                trailingContent = {
                    Switch(
                        checked = settings.reverseChapterOrder,
                        onCheckedChange = settings::updateReverseChapterOrder,
                    )
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("Reader") },
                supportingContent = {
                    Text("Text size, line spacing, alignment, font and background can be changed from the tune icon inside the reader")
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text("Always dark — immersive, no status bar") },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("Source") },
                supportingContent = { Text("kolnovel.com") },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text("1.1.0") },
            )
        }
    }

    if (showFontPicker) {
        AlertDialog(
            onDismissRequest = { showFontPicker = false },
            title = { Text("Choose app font") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    importError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    settings.availableFonts.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settings.updateFont(option)
                                    importError = null
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.currentFont.id == option.id,
                                onClick = {
                                    settings.updateFont(option)
                                    importError = null
                                },
                            )
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = option.fontFamily,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                            )
                            if (option is AppFontOption.Imported) {
                                IconButton(onClick = {
                                    settings.deleteImportedFont(option)
                                    importError = null
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete font",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                fontImporter.launch(
                                    arrayOf(
                                        "font/*",
                                        "application/x-font-ttf",
                                        "application/x-font-otf",
                                        "application/vnd.ms-opentype",
                                        "application/octet-stream",
                                    ),
                                )
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                        Text(
                            text = "Add a font from your device (ttf / otf)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontPicker = false }) { Text("Close") }
            },
        )
    }
}
