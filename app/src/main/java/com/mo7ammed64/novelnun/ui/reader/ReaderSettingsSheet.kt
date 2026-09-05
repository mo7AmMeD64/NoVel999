package com.mo7ammed64.novelnun.ui.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mo7ammed64.novelnun.ui.settings.AppSettings
import com.mo7ammed64.novelnun.ui.settings.FontPickerDialog
import com.mo7ammed64.novelnun.ui.settings.FontTarget
import com.mo7ammed64.novelnun.ui.settings.ReaderAlignment
import com.mo7ammed64.novelnun.ui.settings.ReaderBackground
import com.mo7ammed64.novelnun.ui.settings.ReaderDirection
import com.mo7ammed64.novelnun.ui.settings.ReaderPreferences
import java.util.Locale
import kotlin.math.roundToInt

internal const val DEFAULT_READER_PREVIEW = "في كل صفحة، عالم جديد ينتظر أن تكتشفه.\nEvery chapter opens a new world."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    previewText: String = DEFAULT_READER_PREVIEW,
) {
    val preferences = settings.readerPreferences
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showFontPicker by rememberSaveable { mutableStateOf(false) }
    val textScroll = rememberScrollState()
    val appearanceScroll = rememberScrollState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            modifier = Modifier.fillMaxHeight(0.92f),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Reader settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = settings::resetReaderPreferences) { Text("Reset") }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close reader settings") }
            }
            Text(
                "Saved for all chapters. App appearance stays unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
                listOf("Text", "Appearance").forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) { direction * it / 4 } +
                        fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally(tween(180)) { -direction * it / 6 } + fadeOut(tween(120)))
                },
                label = "Reader settings tab",
            ) { tab ->
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(if (tab == 0) textScroll else appearanceScroll)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ReaderPreview(settings, previewText)
                    if (tab == 0) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                            ListItem(
                                modifier = Modifier.clickable { showFontPicker = true },
                                headlineContent = { Text("Reader font") },
                                supportingContent = {
                                    Text(
                                        if (preferences.fontId == null) "App font · ${settings.currentReaderFont.displayName}"
                                        else settings.currentReaderFont.displayName,
                                    )
                                },
                                leadingContent = { Text("Aa", style = MaterialTheme.typography.titleLarge) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = "Choose or import a font") },
                            )
                        }
                        ReaderSlider("Font size", preferences.fontSize, "${preferences.fontSize.roundToInt()} sp", ReaderPreferences.FONT_SIZE_RANGE, 19) { value ->
                            settings.updateReaderPreferences { it.copy(fontSize = value) }
                        }
                        ReaderSlider("Line spacing", preferences.lineHeight, String.format(Locale.US, "%.1f×", preferences.lineHeight), ReaderPreferences.LINE_HEIGHT_RANGE, 11) { value ->
                            settings.updateReaderPreferences { it.copy(lineHeight = value) }
                        }
                        ReaderSlider("Paragraph spacing", preferences.paragraphSpacing, "${preferences.paragraphSpacing.roundToInt()} dp", ReaderPreferences.PARAGRAPH_SPACING_RANGE, 19) { value ->
                            settings.updateReaderPreferences { it.copy(paragraphSpacing = value) }
                        }
                        ReaderSlider("Letter spacing", preferences.letterSpacing, String.format(Locale.US, "%.1f sp", preferences.letterSpacing), ReaderPreferences.LETTER_SPACING_RANGE, 19) { value ->
                            settings.updateReaderPreferences { it.copy(letterSpacing = value) }
                        }
                        Text(
                            "Keep letter spacing at 0 for natural Arabic letter connections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ChoiceChips("Text direction", ReaderDirection.entries, preferences.direction, { it.label }) { value ->
                            settings.updateReaderPreferences { it.copy(direction = value) }
                        }
                        ChoiceChips("Text alignment", ReaderAlignment.entries, preferences.alignment, { it.label }) { value ->
                            settings.updateReaderPreferences { it.copy(alignment = value) }
                        }
                    } else {
                        BackgroundChoices(preferences.background) { background ->
                            settings.updateReaderPreferences { it.copy(background = background) }
                        }
                        ReaderSlider("Side margins", preferences.horizontalPadding, "${preferences.horizontalPadding.roundToInt()} dp", ReaderPreferences.PADDING_RANGE, 19) { value ->
                            settings.updateReaderPreferences { it.copy(horizontalPadding = value) }
                        }
                        Text(
                            "Background colors apply to the entire reader, including its toolbar. Your app theme is not changed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (showFontPicker) {
            FontPickerDialog(settings, FontTarget.READER, onDismiss = { showFontPicker = false })
        }
    }
}

@Composable
private fun ReaderPreview(settings: AppSettings, text: String) {
    val preferences = settings.readerPreferences
    val palette = preferences.background.palette
    val font = settings.currentReaderFont
    val fontFamily = remember(font) { font.fontFamily }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Live preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = palette.background,
            contentColor = palette.foreground,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides preferences.direction.layoutDirection) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = preferences.horizontalPadding.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(preferences.paragraphSpacing.dp),
                ) {
                    val paragraphs = remember(text) { text.lines().filter(String::isNotBlank).take(2) }
                    paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = preferences.textStyle(fontFamily),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderSlider(
    label: String,
    value: Float,
    valueLabel: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.semantics { contentDescription = label; stateDescription = valueLabel },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundChoices(selected: ReaderBackground, onSelect: (ReaderBackground) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reader background", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReaderBackground.entries.forEach { option ->
                val palette = option.palette
                Column(
                    modifier = Modifier.width(80.dp).selectable(
                        selected = selected == option,
                        onClick = { onSelect(option) },
                        role = Role.RadioButton,
                    ).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = palette.background,
                        border = BorderStroke(
                            if (selected == option) 2.dp else 1.dp,
                            if (selected == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (selected == option) Icon(Icons.Default.Check, contentDescription = null, tint = palette.foreground)
                            else Text("Aa", color = palette.foreground)
                        }
                    }
                    Text(option.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
