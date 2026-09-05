package com.mo7ammed64.novelnun.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Shared picker; importing here selects a font only for the requested target. */
@Composable
fun FontPickerDialog(settings: AppSettings, target: FontTarget, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importError by rememberSaveable { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val selectedId = when (target) {
        FontTarget.APP -> settings.currentFont.id
        FontTarget.READER -> settings.readerPreferences.fontId?.takeIf { id -> settings.availableFonts.any { it.id == id } }
    }

    val fontImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                importing = true
                importError = null
                try {
                    importError = settings.importFont(context, uri, target)
                } finally {
                    importing = false
                }
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = { if (!importing) onDismiss() },
            title = { Text(if (target == FontTarget.APP) "App font" else "Reader font") },
            text = {
                Column {
                    Text(
                        if (target == FontTarget.READER) "Only chapter text is affected."
                        else "Used throughout the app interface.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    if (importing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Importing font…", modifier = Modifier.padding(vertical = 8.dp))
                    }
                    importError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp).selectableGroup()) {
                        if (target == FontTarget.READER) {
                            item(key = "follow_app") {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                        selected = selectedId == null,
                                        enabled = !importing,
                                        role = Role.RadioButton,
                                        onClick = { settings.updateReaderFont(null); importError = null },
                                    ).padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = selectedId == null, onClick = null, enabled = !importing)
                                    Column(Modifier.padding(start = 12.dp)) {
                                        Text("Use app font")
                                        Text(settings.currentFont.displayName, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        items(settings.availableFonts, key = { it.id }) { option ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                                    selected = selectedId == option.id,
                                    enabled = !importing,
                                    role = Role.RadioButton,
                                    onClick = {
                                        when (target) {
                                            FontTarget.APP -> settings.updateFont(option)
                                            FontTarget.READER -> settings.updateReaderFont(option)
                                        }
                                        importError = null
                                    },
                                ).padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedId == option.id, onClick = null, enabled = !importing)
                                Text(
                                    option.displayName,
                                    fontFamily = option.fontFamily,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                )
                                if (option is AppFontOption.Imported) {
                                    IconButton(
                                        enabled = !importing,
                                        onClick = { importError = settings.deleteImportedFont(option) },
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete ${option.displayName}")
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        ".ttf / .otf files · Up to 20 MB",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !importing,
                    // Many document providers don't assign font MIME types; validate the file
                    // ourselves rather than hiding valid fonts in the picker.
                    onClick = { fontImporter.launch(arrayOf("*/*")) },
                ) { Text("Import font") }
            },
            confirmButton = { TextButton(enabled = !importing, onClick = onDismiss) { Text("Done") } },
        )
    }
}
