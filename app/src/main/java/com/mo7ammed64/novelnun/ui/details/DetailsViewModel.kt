package com.mo7ammed64.novelnun.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.ChapterNumbers
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
    val chapterInputError: String? = null,
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
                    _state.value = _state.value.copy(
                        loading = false,
                        details = details,
                        continueChapter = continueChapter,
                        chapterInputError = null,
                    )
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query, chapterInputError = null)
    }

    /**
     * Filters by a title when text is entered. Numeric input is treated as a chapter number so the
     * list immediately narrows to the requested chapter instead of merely looking for the string.
     */
    fun filteredChapters(reverseOrder: Boolean = false): List<Chapter> {
        val chapters = orderedChapters(reverseOrder)
        val chronological = orderedChapters(reverseOrder = false)
        val query = _state.value.query.trim()
        if (query.isBlank()) return chapters

        val requestedNumber = chapterNumber(query)
        return if (requestedNumber != null) {
            // Match the real chapter number first (parsed from the title when the source provides
            // it); only fall back to the list position, and always on the chronological list so
            // the displayed/reversed order can not shift the result.
            val numberMatches = chapters.filter { chapter ->
                chapter.number == requestedNumber || chapterNumber(chapter.title) == requestedNumber
            }
            if (numberMatches.isNotEmpty()) {
                numberMatches
            } else {
                chronological.getOrNull(requestedNumber - 1)?.let(::listOf).orEmpty()
            }
        } else {
            chapters.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    /** Finds the chapter requested in the chapter-number bar. */
    fun chapterForNumber(reverseOrder: Boolean = false): Chapter? {
        val requestedNumber = chapterNumber(_state.value.query) ?: return null
        val chronological = orderedChapters(reverseOrder = false)

        // Prefer the actual number in the title; the list position is only a fallback for sources
        // that do not include a number in their chapter label. The fallback always indexes the
        // chronological (oldest-first) list, never the reversed display order.
        return chronological.firstOrNull { it.number == requestedNumber }
            ?: chronological.firstOrNull { chapterNumber(it.title) == requestedNumber }
            ?: chronological.getOrNull(requestedNumber - 1)
    }

    fun markChapterNotFound() {
        _state.value = _state.value.copy(chapterInputError = "لم يتم العثور على هذا الفصل")
    }

    private fun orderedChapters(reverseOrder: Boolean): List<Chapter> {
        val chapters = _state.value.details?.chapters.orEmpty()
        return if (reverseOrder) chapters.asReversed() else chapters
    }

    private fun chapterNumber(value: String): Int? = ChapterNumbers.parse(value)

    fun toggleFavorite() {
        val novel = _state.value.details?.novel ?: return
        viewModelScope.launch { repo.toggleFavorite(novel, isFavorite.value) }
    }
}
