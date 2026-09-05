package com.mo7ammed64.novelnun.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.ui.common.NovelCover
import com.mo7ammed64.novelnun.ui.settings.AppSettings

@Composable
fun DetailsScreen(
    seriesUrl: String,
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenChapter: (novelUrl: String, chapterUrl: String) -> Unit,
    viewModel: DetailsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    LaunchedEffect(seriesUrl) { viewModel.load(seriesUrl) }

    Scaffold { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.details == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text(state.error ?: "تعذر تحميل الرواية", color = MaterialTheme.colorScheme.error) }

            else -> {
                val details = state.details!!

                fun openRequestedChapter() {
                    viewModel.chapterForNumber(settings.reverseChapterOrder)?.let { chapter ->
                        onOpenChapter(details.novel.url, chapter.url)
                    } ?: viewModel.markChapterNotFound()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                            NovelCover(url = details.novel.coverUrl, modifier = Modifier.size(192.dp))
                            Text(
                                text = details.novel.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 12.dp).weight(1f),
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Info",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (details.synopsis.isNotBlank()) {
                        item {
                            androidx.compose.runtime.CompositionLocalProvider(
                                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
                            ) {
                                Text(
                                    text = details.synopsis,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledIconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                )
                            }
                            Button(
                                onClick = {
                                    state.continueChapter?.let { onOpenChapter(details.novel.url, it.url) }
                                },
                                modifier = Modifier.padding(start = 12.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Text(text = "  Continue")
                            }
                        }
                    }

                    item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                        ) {
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = viewModel::onQueryChange,
                                label = { Text("رقم الفصل") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                isError = state.chapterInputError != null,
                                supportingText = {
                                    state.chapterInputError?.let { Text(it) }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Go,
                                ),
                                keyboardActions = KeyboardActions(onGo = { openRequestedChapter() }),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            Button(
                                onClick = ::openRequestedChapter,
                                enabled = state.query.isNotBlank(),
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                            ) {
                                Text("انتقال")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "الفصول",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    val chapters = viewModel.filteredChapters(settings.reverseChapterOrder)
                    if (chapters.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (state.query.isBlank()) "لا توجد فصول" else "لا توجد فصول مطابقة",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(chapters) { chapter: Chapter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                androidx.compose.runtime.CompositionLocalProvider(
                                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
                                ) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                IconButton(onClick = { onOpenChapter(details.novel.url, chapter.url) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "اقرأ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
