package com.mo7ammed64.novelnun.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.NovelDetails
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ReaderUiState(
    val loading: Boolean = true,
    val title: String = "",
    val html: String = "",
    val error: String? = null,
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NovelRepository.get(application)

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state

    fun load(novelUrl: String, chapterUrl: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            // Serve from the offline cache first if it was downloaded.
            repo.findDownload(chapterUrl)?.let { cached ->
                _state.value = ReaderUiState(loading = false, title = cached.chapterTitle, html = cached.content)
                return@launch
            }

            // Content is what the user is waiting to read; details (full chapter list) are only
            // needed for the title fallback and history bookkeeping - fetch both at once instead
            // of blocking the reader on the details call first.
            val contentDeferred = async { repo.getChapterContent(chapterUrl) }
            val detailsDeferred = async { repo.getDetails(novelUrl) }

            contentDeferred.await()
                .onSuccess { html ->
                    _state.value = _state.value.copy(loading = false, html = html)
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }

            // Resolves shortly after (already in flight) - fills in the title and records
            // progress without ever having blocked the content from showing.
            val details: NovelDetails? = detailsDeferred.await().getOrNull()
            val chapter: Chapter? = details?.chapters?.firstOrNull { it.url == chapterUrl }
            if (chapter != null) {
                _state.value = _state.value.copy(title = chapter.title)
            }
            if (details != null && chapter != null) {
                repo.recordProgress(details.novel, chapter)
            }
        }
    }
}
