package com.mo7ammed64.novelnun.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.ChapterSearch
import com.mo7ammed64.novelnun.data.model.NovelDetails
import com.mo7ammed64.novelnun.data.repo.NovelDetailsRepository
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val loading: Boolean = true,
    val details: NovelDetails? = null,
    val error: String? = null,
    val query: String = "",
    val continueChapter: Chapter? = null,
    val chapterInputError: String? = null,
)

class DetailsViewModel(
    private val repo: NovelDetailsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailsUiState(query = savedStateHandle[QUERY_KEY] ?: ""))
    val state: StateFlow<DetailsUiState> = _state

    private var requestedUrl: String? = null
    private var loadJob: Job? = null
    private var historyJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val isFavorite: StateFlow<Boolean> = _state
        .map { it.details?.novel?.slug }
        .distinctUntilChanged()
        .flatMapLatest { slug -> slug?.let(repo::observeIsFavorite) ?: flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Re-entering the same back-stack entry must not reset the list or make another request. */
    fun load(seriesUrl: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && requestedUrl == seriesUrl &&
            (_state.value.details != null || loadJob?.isActive == true)
        ) return

        loadJob?.cancel()
        if (requestedUrl != null && requestedUrl != seriesUrl) {
            historyJob?.cancel()
            savedStateHandle[QUERY_KEY] = ""
            _state.value = DetailsUiState()
        }
        requestedUrl = seriesUrl

        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.getDetails(seriesUrl, forceRefresh)
                .onSuccess { details ->
                    ensureActive()
                    _state.update { current ->
                        current.copy(
                            loading = false,
                            details = details,
                            continueChapter = details.chapters.firstOrNull { it.url == current.continueChapter?.url }
                                ?: details.chapters.firstOrNull(),
                            chapterInputError = null,
                        )
                    }
                    observeProgress(details)
                }
                .onFailure { error ->
                    ensureActive()
                    _state.update { it.copy(loading = false, error = error.message ?: "تعذر تحميل الرواية") }
                }
        }
    }

    private fun observeProgress(details: NovelDetails) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            // Room updates Continue as soon as the reader records progress. No network reload
            // or loading placeholder is needed when the user comes back from a chapter.
            repo.observeHistory(details.novel.slug).collect { history ->
                _state.update { current ->
                    current.copy(
                        continueChapter = details.chapters.firstOrNull { it.url == history?.lastChapterUrl }
                            ?: details.chapters.firstOrNull(),
                    )
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        savedStateHandle[QUERY_KEY] = query
        _state.update { it.copy(query = query, chapterInputError = null) }
    }

    fun clearQuery() = onQueryChange("")

    fun filteredChapters(reverseOrder: Boolean = false): List<Chapter> =
        ChapterSearch.filter(_state.value.details?.chapters.orEmpty(), _state.value.query, reverseOrder)

    fun requestedChapter(): Chapter? =
        ChapterSearch.requestedChapter(_state.value.details?.chapters.orEmpty(), _state.value.query)

    fun markChapterNotFound() {
        _state.update { it.copy(chapterInputError = "أدخل رقم فصل متاحًا أو اختر فصلًا من النتائج") }
    }

    fun toggleFavorite() {
        val novel = _state.value.details?.novel ?: return
        viewModelScope.launch { repo.toggleFavorite(novel, isFavorite.value) }
    }

    companion object {
        private const val QUERY_KEY = "chapter_query"

        val Factory = viewModelFactory {
            initializer {
                DetailsViewModel(
                    repo = NovelRepository.get(checkNotNull(this[APPLICATION_KEY])),
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
