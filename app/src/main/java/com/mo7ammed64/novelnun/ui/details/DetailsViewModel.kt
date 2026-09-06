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
    /** Cursor for the next chapter page, or null once every chapter has been loaded. */
    val nextChaptersCursor: String? = null,
    val loadingMoreChapters: Boolean = false,
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

    private var loadedUrl: String? = null

    /**
     * Loads the novel details once per URL. Coming back from the reader re-enters the screen with
     * the same ViewModel, so we keep the already-loaded data (and scroll state) instead of
     * re-fetching and flashing the loading spinner. Pass [force] to explicitly refresh.
     *
     * Only the first page of chapters (up to 300 - the large majority of novels in full) loads up
     * front; longer novels page the rest in via [loadMoreChapters] or on demand when a jump/continue
     * target isn't on the first page yet (see [ensureChapterAvailable]), so opening a novel never
     * waits on its entire chapter list.
     */
    fun load(seriesUrl: String, force: Boolean = false) {
        if (!force && loadedUrl == seriesUrl && _state.value.details != null) {
            // Refresh the "continue" chapter — the user may have just read a new chapter.
            resolveContinueChapter()
            return
        }
        loadedUrl = seriesUrl
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repo.getDetails(seriesUrl)
                .onSuccess { details ->
                    _state.value = _state.value.copy(
                        loading = false,
                        details = details,
                        nextChaptersCursor = details.nextChaptersCursor,
                        chapterInputError = null,
                    )
                    resolveContinueChapter()
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    /**
     * Resolves the "Continue" target from reading history. If the last-read chapter isn't on the
     * already-loaded page(s) yet, this pages in more chapters in the background (via the same
     * mechanism as a chapter-number jump) until it's found, rather than ever falling back to
     * chapter 1 just because the rest of the list hadn't loaded yet.
     */
    private fun resolveContinueChapter() {
        val details = _state.value.details ?: return
        viewModelScope.launch {
            val history = repo.findHistory(details.novel.slug)
            val targetUrl = history?.lastChapterUrl
            val chapter = if (targetUrl != null) {
                ensureChapterAvailable { it.url == targetUrl } ?: _state.value.details?.chapters?.firstOrNull()
            } else {
                _state.value.details?.chapters?.firstOrNull()
            }
            _state.value = _state.value.copy(continueChapter = chapter)
        }
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query, chapterInputError = null)
    }

    /** Clears the chapter search so the full chapter list is visible again. */
    fun clearQuery() {
        _state.value = _state.value.copy(query = "", chapterInputError = null)
    }

    /**
     * Filters by a title when text is entered. Numeric input is treated as a chapter number so the
     * list immediately narrows to the requested chapter instead of merely looking for the string.
     * Text search only covers chapters already loaded - [chapterForNumber] is the one that pages
     * in more chapters on demand for a numeric jump.
     */
    fun filteredChapters(reverseOrder: Boolean = false): List<Chapter> {
        val chapters = orderedChapters(reverseOrder)
        val chronological = orderedChapters(reverseOrder = false)
        val query = _state.value.query.trim()
        if (query.isBlank()) return chapters

        val requestedNumber = chapterNumber(query)
        return if (requestedNumber != null) {
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

    /**
     * Finds the chapter requested in the chapter-number bar. If it isn't on an already-loaded page,
     * this pages in more chapters (via [ensureChapterAvailable]) until it's found or the novel runs
     * out of chapters, so jumping to, say, chapter 2000 of a long novel still works correctly - it
     * just takes a beat longer than a chapter already on the first page.
     */
    suspend fun chapterForNumber(reverseOrder: Boolean = false): Chapter? {
        val requestedNumber = chapterNumber(_state.value.query) ?: return null

        fun matches(chapter: Chapter) =
            chapter.number == requestedNumber || chapterNumber(chapter.title) == requestedNumber

        orderedChapters(reverseOrder = false).firstOrNull(::matches)?.let { return it }
        ensureChapterAvailable(::matches)?.let { return it }
        return orderedChapters(reverseOrder = false).getOrNull(requestedNumber - 1)
    }

    fun markChapterNotFound() {
        _state.value = _state.value.copy(chapterInputError = "لم يتم العثور على هذا الفصل")
    }

    /** Loads one more page of chapters for the "تحميل المزيد" button. */
    fun loadMoreChapters() {
        val seriesUrl = loadedUrl ?: return
        val cursor = _state.value.nextChaptersCursor ?: return
        if (_state.value.loadingMoreChapters) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingMoreChapters = true)
            val startIndex = _state.value.details?.chapters?.size ?: 0
            repo.getMoreChapters(seriesUrl, cursor, startIndex)
                .onSuccess { page -> appendChapterPage(page.chapters, page.nextCursor) }
                .also { _state.value = _state.value.copy(loadingMoreChapters = false) }
        }
    }

    /**
     * Searches already-loaded chapters for [predicate]; if not found and more pages remain, fetches
     * them one at a time (appending each to the visible list as it arrives) until a match turns up
     * or the novel's chapters are exhausted. Bounded so a predicate that never matches can't loop
     * forever.
     */
    private suspend fun ensureChapterAvailable(predicate: (Chapter) -> Boolean): Chapter? {
        _state.value.details?.chapters?.firstOrNull(predicate)?.let { return it }

        val seriesUrl = loadedUrl ?: return null
        var cursor = _state.value.nextChaptersCursor
        var guard = 0
        while (cursor != null && guard < 30) {
            guard++
            val startIndex = _state.value.details?.chapters?.size ?: 0
            val page = repo.getMoreChapters(seriesUrl, cursor, startIndex).getOrNull() ?: break
            appendChapterPage(page.chapters, page.nextCursor)
            page.chapters.firstOrNull(predicate)?.let { return it }
            cursor = page.nextCursor
        }
        return null
    }

    private fun appendChapterPage(newChapters: List<Chapter>, nextCursor: String?) {
        val current = _state.value.details ?: return
        _state.value = _state.value.copy(
            details = current.copy(chapters = current.chapters + newChapters),
            nextChaptersCursor = nextCursor,
        )
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
