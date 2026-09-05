package com.mo7ammed64.novelnun.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val loading: Boolean = true,
    val title: String = "",
    val paragraphs: List<String> = emptyList(),
    val error: String? = null,
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NovelRepository.get(application)
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state

    private var requestedChapter: Pair<String, String>? = null
    private var loadJob: Job? = null

    fun load(novelUrl: String, chapterUrl: String, forceReload: Boolean = false) {
        val key = novelUrl to chapterUrl
        if (!forceReload && requestedChapter == key &&
            (loadJob?.isActive == true || _state.value.paragraphs.isNotEmpty())
        ) return

        loadJob?.cancel()
        requestedChapter = key
        loadJob = viewModelScope.launch {
            _state.value = ReaderUiState()
            try {
                val cached = repo.findDownload(chapterUrl)
                if (cached != null) {
                    showContent(cached.content, cached.chapterTitle)
                    // Offline reading should update Continue too, without a network request.
                    val novel = Novel(cached.novelSlug, cached.novelTitle, null, novelUrl)
                    saveProgress(novel, Chapter(cached.chapterTitle, chapterUrl, 0), preserveCover = true)
                } else {
                    coroutineScope {
                        // Details normally come straight from the shared session cache. If the
                        // reader was opened directly, slow metadata must not delay chapter text.
                        val metadata = async { repo.getDetails(novelUrl).getOrNull() }
                        showContent(repo.getChapterContent(chapterUrl).getOrThrow(), "Chapter")
                        val details = metadata.await()
                        val chapter = details?.chapters?.firstOrNull { it.url == chapterUrl }
                        if (details != null && chapter != null) {
                            _state.update { it.copy(title = chapter.title) }
                            saveProgress(details.novel, chapter)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.message ?: "تعذر تحميل الفصل") }
            }
        }
    }

    private suspend fun showContent(html: String, title: String) {
        val paragraphs = withContext(Dispatchers.Default) { ChapterText.paragraphs(html) }
        if (paragraphs.isEmpty()) error("لم يتم العثور على نص الفصل. حاول مرة أخرى.")
        _state.value = ReaderUiState(loading = false, title = title, paragraphs = paragraphs)
    }

    private suspend fun saveProgress(novel: Novel, chapter: Chapter, preserveCover: Boolean = false) {
        try {
            val withCover = if (preserveCover) novel.copy(coverUrl = repo.findHistory(novel.slug)?.coverUrl) else novel
            repo.recordProgress(withCover, chapter)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A history-write failure must not replace readable chapter content with an error.
        }
    }
}
