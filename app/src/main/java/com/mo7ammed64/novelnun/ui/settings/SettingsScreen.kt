package com.mo7ammed64.novelnun.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(settings: AppSettings) {
    var showFontPicker by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "الإعدادات",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right,
            )

            ListItem(
                modifier = Modifier.clickable { showFontPicker = true },
                headlineContent = { Text("خط التطبيق") },
                supportingContent = { Text(settings.font.displayName) },
                trailingContent = { Text("Aa", style = MaterialTheme.typography.titleLarge) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                modifier = Modifier.clickable {
                    settings.updateReverseChapterOrder(!settings.reverseChapterOrder)
                },
                headlineContent = { Text("ترتيب الفصول عكسياً") },
                supportingContent = {
                    Text(
                        if (settings.reverseChapterOrder) "من الأحدث إلى الأقدم"
                        else "من الأقدم إلى الأحدث",
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
                headlineContent = { Text("المظهر") },
                supportingContent = { Text("داكن دائماً") },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("المصدر") },
                supportingContent = { Text("kolnovel.com") },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("الإصدار") },
                supportingContent = { Text("1.0.0") },
            )
        }
    }

    if (showFontPicker) {
        AlertDialog(
            onDismissRequest = { showFontPicker = false },
            title = { Text("اختيار خط التطبيق") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppFont.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settings.updateFont(option)
                                    showFontPicker = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.font == option,
                                onClick = {
                                    settings.updateFont(option)
                                    showFontPicker = false
                                },
                            )
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = option.fontFamily,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontPicker = false }) { Text("إغلاق") }
            },
        )
    }
}
