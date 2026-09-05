package com.mo7ammed64.novelnun.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

            val detailsResult = repo.getDetails(novelUrl)
            val details: NovelDetails? = detailsResult.getOrNull()
            val chapter: Chapter? = details?.chapters?.firstOrNull { it.url == chapterUrl }

            repo.getChapterContent(chapterUrl)
                .onSuccess { html ->
                    _state.value = ReaderUiState(loading = false, title = chapter?.title.orEmpty(), html = html)
                    if (details != null && chapter != null) {
                        repo.recordProgress(details.novel, chapter)
                    }
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }
}
