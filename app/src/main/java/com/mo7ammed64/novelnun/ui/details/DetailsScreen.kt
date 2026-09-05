package com.mo7ammed64.novelnun.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.ui.common.NovelCover
import com.mo7ammed64.novelnun.ui.settings.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    seriesUrl: String,
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenChapter: (novelUrl: String, chapterUrl: String) -> Unit,
    viewModel: DetailsViewModel = viewModel(factory = DetailsViewModel.Factory),
) {
    val state by viewModel.state.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    // Keep this outside the loading/content branches. Navigation saves the list position with
    // this back-stack entry, including when a filtered chapter is opened and then popped.
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val chapters = remember(state.details, state.query, settings.reverseChapterOrder) {
        viewModel.filteredChapters(settings.reverseChapterOrder)
    }

    LaunchedEffect(seriesUrl) { viewModel.load(seriesUrl) }

    fun openChapter(chapter: Chapter) {
        val novel = state.details?.novel ?: return
        keyboard?.hide()
        focusManager.clearFocus()
        onOpenChapter(novel.url, chapter.url)
    }

    fun openRequestedChapter() {
        viewModel.requestedChapter()?.let(::openChapter) ?: viewModel.markChapterNotFound()
    }

    fun showAllChapters() {
        viewModel.clearQuery()
        keyboard?.hide()
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            state.details?.novel?.title ?: "Info",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.ContentOrRtl),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.load(seriesUrl, forceRefresh = true) },
                            enabled = !state.loading,
                        ) { Icon(Icons.Default.Refresh, contentDescription = "Refresh chapters") }
                    },
                )
                Box(Modifier.fillMaxWidth().height(4.dp)) {
                    if (state.loading && state.details != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
    ) { padding ->
        val details = state.details
        when {
            state.loading && details == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            details == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.error ?: "تعذر تحميل الرواية", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.load(seriesUrl, forceRefresh = true) }) { Text("إعادة المحاولة") }
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "novel_header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NovelCover(url = details.novel.coverUrl, modifier = Modifier.width(100.dp).height(150.dp))
                        Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = details.novel.title,
                                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.ContentOrRtl),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                            details.author?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${details.chapters.size} فصل", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                item(key = "synopsis") {
                    if (details.synopsis.isNotBlank()) {
                        Text(
                            text = details.synopsis,
                            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.ContentOrRtl),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item(key = "reading_actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledIconButton(onClick = viewModel::toggleFavorite) {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Remove from saved" else "Save novel",
                                )
                            }
                            Button(
                                onClick = { state.continueChapter?.let(::openChapter) },
                                enabled = state.continueChapter != null,
                                modifier = Modifier.padding(start = 12.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Text(text = "Continue", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        state.continueChapter?.let { chapter ->
                            Text(
                                chapter.title,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.ContentOrRtl),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                item(key = "chapter_search") {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            label = { Text("رقم الفصل أو عنوانه") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = ::showAllChapters) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح البحث وعرض جميع الفصول")
                                    }
                                }
                            },
                            isError = state.chapterInputError != null,
                            supportingText = { state.chapterInputError?.let { Text(it) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { openRequestedChapter() }),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Button(
                            onClick = ::openRequestedChapter,
                            enabled = state.query.isNotBlank(),
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                        ) { Text("انتقال") }
                    }
                }

                item(key = "chapter_heading") {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("الفصول", style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (state.query.isBlank()) "${chapters.size} فصل"
                                else "${chapters.size} من ${details.chapters.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.query.isNotEmpty()) {
                            TextButton(onClick = ::showAllChapters) { Text("عرض جميع الفصول") }
                        }
                    }
                }

                if (chapters.isEmpty()) {
                    item(key = "empty_chapters") {
                        Text(
                            text = if (state.query.isBlank()) "لا توجد فصول" else "لا توجد فصول مطابقة — يمكنك عرض جميع الفصول أعلاه",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    items(chapters, key = { "chapter:${it.url}:${it.index}" }) { chapter ->
                        Surface(
                            onClick = { openChapter(chapter) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (chapter.url == state.continueChapter?.url) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.ContentOrRtl),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Default.PlayArrow, contentDescription = "اقرأ", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
