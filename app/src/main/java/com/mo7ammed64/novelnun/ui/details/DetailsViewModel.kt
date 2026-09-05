package com.mo7ammed64.novelnun.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.NovelDetails
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailsUiState(
    val loading: Boolean = true,
    val details: NovelDetails? = null,
    val error: String? = null,
    val query: String = "",
    val continueChapter: Chapter? = null,
)

class DetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NovelRepository.get(application)

    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state

    val isFavorite: StateFlow<Boolean> by lazy {
        _state.flatMapLatest { s ->
            s.details?.novel?.slug?.let { repo.observeIsFavorite(it) } ?: kotlinx.coroutines.flow.flowOf(false)
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)
    }

    fun load(seriesUrl: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repo.getDetails(seriesUrl)
                .onSuccess { details ->
                    val history = repo.findHistory(details.novel.slug)
                    val continueChapter = details.chapters.firstOrNull { it.url == history?.lastChapterUrl }
                        ?: details.chapters.firstOrNull()
                    _state.value = _state.value.copy(loading = false, details = details, continueChapter = continueChapter)
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun filteredChapters(): List<Chapter> {
        val chapters = _state.value.details?.chapters.orEmpty()
        val query = _state.value.query
        return if (query.isBlank()) chapters else chapters.filter { it.title.contains(query, ignoreCase = true) }
    }

    fun toggleFavorite() {
        val novel = _state.value.details?.novel ?: return
        viewModelScope.launch { repo.toggleFavorite(novel, isFavorite.value) }
    }
}
