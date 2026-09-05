package com.mo7ammed64.novelnun.ui.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mo7ammed64.novelnun.ui.settings.AppSettings

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
    val preferences = settings.readerPreferences
    val palette = preferences.background.palette
    val background by animateColorAsState(palette.background, tween(220), label = "Reader background")
    val foreground by animateColorAsState(palette.foreground, tween(220), label = "Reader text color")
    val font = settings.currentReaderFont
    val fontFamily = remember(font) { font.fontFamily }
    val textStyle = remember(preferences, fontFamily) { preferences.textStyle(fontFamily) }
    val listState = rememberLazyListState()
    var showSettings by rememberSaveable(chapterUrl) { mutableStateOf(false) }

    LaunchedEffect(novelUrl, chapterUrl) { viewModel.load(novelUrl, chapterUrl) }

    Scaffold(
        containerColor = background,
        contentColor = foreground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.ContentOrRtl),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Tune, contentDescription = "Reader settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background,
                    titleContentColor = foreground,
                    navigationIconContentColor = foreground,
                    actionIconContentColor = foreground,
                ),
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = foreground)
            }
            state.error != null && state.paragraphs.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text("Unable to load chapter", style = MaterialTheme.typography.titleLarge)
                Text(state.error.orEmpty(), textAlign = TextAlign.Center)
                Button(
                    onClick = { viewModel.load(novelUrl, chapterUrl, forceReload = true) },
                    colors = ButtonDefaults.buttonColors(containerColor = foreground, contentColor = background),
                ) { Text("Retry") }
            }
            else -> CompositionLocalProvider(LocalLayoutDirection provides preferences.direction.layoutDirection) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = preferences.horizontalPadding.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(preferences.paragraphSpacing.dp),
                ) {
                    itemsIndexed(state.paragraphs, key = { index, _ -> "$chapterUrl:$index" }, contentType = { _, _ -> "paragraph" }) { _, paragraph ->
                        Text(
                            text = paragraph,
                            style = textStyle,
                            color = foreground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        val preview = remember(state.paragraphs) { state.paragraphs.take(2).joinToString("\n\n").take(600) }
        ReaderSettingsSheet(
            settings = settings,
            previewText = preview.ifBlank { DEFAULT_READER_PREVIEW },
            onDismiss = { showSettings = false },
        )
    }
}
