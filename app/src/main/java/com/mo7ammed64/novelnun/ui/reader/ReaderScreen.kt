package com.mo7ammed64.novelnun.ui.reader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mo7ammed64.novelnun.ui.settings.AppSettings
import com.mo7ammed64.novelnun.ui.settings.readerBackgroundById
import com.mo7ammed64.novelnun.ui.settings.readerBackgrounds
import org.jsoup.Jsoup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    novelUrl: String,
    chapterUrl: String,
    settings: AppSettings,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val background = readerBackgroundById(settings.readerBackground)
    val textAlign = when (settings.readerTextAlign) {
        AppSettings.ALIGN_LEFT -> TextAlign.Left
        AppSettings.ALIGN_CENTER -> TextAlign.Center
        AppSettings.ALIGN_JUSTIFY -> TextAlign.Justify
        else -> TextAlign.Right
    }

    LaunchedEffect(novelUrl, chapterUrl) { viewModel.load(novelUrl, chapterUrl) }

    var topBarVisible by remember { mutableStateOf(true) }

    Scaffold(containerColor = background.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { topBarVisible = !topBarVisible })
                },
        ) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = background.text)
                }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "خطأ", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    val paragraphs = remember(state.html) {
                        Jsoup.parse(state.html).select("p").map { it.text() }.filter { it.isNotBlank() }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy((settings.readerFontSize * 0.66f).dp),
                        ) {
                            items(paragraphs) { paragraph ->
                                Text(
                                    text = paragraph,
                                    color = background.text,
                                    fontFamily = settings.readerFontFamily,
                                    fontSize = settings.readerFontSize.sp,
                                    lineHeight = (settings.readerFontSize * settings.readerLineSpacing).sp,
                                    textAlign = textAlign,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = topBarVisible,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { -it },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it },
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = background.background,
                        titleContentColor = background.text,
                        navigationIconContentColor = background.text,
                        actionIconContentColor = background.text,
                    ),
                    title = {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                state.title,
                                maxLines = 1,
                                color = background.text,
                                textAlign = TextAlign.Right,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    },
                    actions = {
                        IconButton(onClick = { showOptions = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Reader options")
                        }
                    },
                )
            }
        }
    }

    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = { showOptions = false },
            sheetState = sheetState,
        ) {
            ReaderOptionsSheet(settings = settings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderOptionsSheet(settings: AppSettings) {
    val context = LocalContext.current
    var importError by remember { mutableStateOf<String?>(null) }

    val fontImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { importError = settings.importReaderFont(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        Text(
            text = "Reader Options",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Text size -----------------------------------------------------------
        Spacer(Modifier.height(16.dp))
        SheetLabel("Text size", "${settings.readerFontSize.toInt()} sp")
        Slider(
            value = settings.readerFontSize,
            onValueChange = { settings.updateReaderFontSize(it) },
            valueRange = 12f..32f,
            steps = 19,
        )

        // Line spacing --------------------------------------------------------
        SheetLabel("Line spacing", String.format(java.util.Locale.US, "%.1f×", settings.readerLineSpacing))
        Slider(
            value = settings.readerLineSpacing,
            onValueChange = { settings.updateReaderLineSpacing(it) },
            valueRange = 1.2f..2.6f,
            steps = 13,
        )

        // Text alignment --------------------------------------------------------
        SheetLabel("Text alignment", null)
        Spacer(Modifier.height(8.dp))
        val alignments = listOf(
            Triple(AppSettings.ALIGN_RIGHT, Icons.Default.FormatAlignRight, "Right"),
            Triple(AppSettings.ALIGN_CENTER, Icons.Default.FormatAlignCenter, "Center"),
            Triple(AppSettings.ALIGN_JUSTIFY, Icons.Default.FormatAlignJustify, "Justify"),
            Triple(AppSettings.ALIGN_LEFT, Icons.Default.FormatAlignLeft, "Left"),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            alignments.forEachIndexed { index, (id, icon, label) ->
                SegmentedButton(
                    selected = settings.readerTextAlign == id,
                    onClick = { settings.updateReaderTextAlign(id) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = alignments.size),
                    icon = {},
                ) {
                    Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Background ------------------------------------------------------------
        Spacer(Modifier.height(20.dp))
        SheetLabel("Background", readerBackgroundById(settings.readerBackground).displayName)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            readerBackgrounds.forEach { option ->
                val selected = settings.readerBackground == option.id
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(option.background, CircleShape)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { settings.updateReaderBackground(option.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = option.displayName,
                            tint = option.text,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text("Aa", color = option.text, fontSize = 13.sp)
                    }
                }
            }
        }

        // Reader font -------------------------------------------------------------
        Spacer(Modifier.height(20.dp))
        SheetLabel("Chapter font", null)
        importError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = settings.readerFontId == AppSettings.READER_FONT_APP,
                onClick = { settings.updateReaderFont(AppSettings.READER_FONT_APP) },
                label = { Text("App font") },
            )
            settings.availableFonts.forEach { option ->
                FilterChip(
                    selected = settings.readerFontId == option.id,
                    onClick = { settings.updateReaderFont(option.id) },
                    label = {
                        Text(
                            option.displayName.substringBefore(" —"),
                            fontFamily = option.fontFamily,
                        )
                    },
                )
            }
            AssistChip(
                onClick = {
                    fontImporter.launch(
                        arrayOf(
                            "font/*",
                            "application/x-font-ttf",
                            "application/x-font-otf",
                            "application/vnd.ms-opentype",
                            "application/octet-stream",
                        ),
                    )
                },
                label = { Text("Import font file…") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun SheetLabel(title: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
