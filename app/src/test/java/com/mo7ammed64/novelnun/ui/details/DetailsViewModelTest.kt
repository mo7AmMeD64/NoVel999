package com.mo7ammed64.novelnun.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.mo7ammed64.novelnun.data.db.HistoryEntity
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.model.NovelDetails
import com.mo7ammed64.novelnun.data.repo.NovelDetailsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {
    private val store = ViewModelStore()
    private var modelCount = 0

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(repo: FakeRepository, handle: SavedStateHandle = SavedStateHandle()): DetailsViewModel =
        DetailsViewModel(repo, handle).also { store.put("details-${modelCount++}", it) }

    @Test fun `returning from a searched chapter preserves details and query without reloading`() = runTest {
        val repo = FakeRepository()
        val viewModel = model(repo)
        viewModel.load("novel")
        advanceUntilIdle()
        viewModel.onQueryChange("١٢")
        val before = viewModel.state.value.details
        assertEquals("chapter/12", viewModel.requestedChapter()?.url)

        // DetailsScreen's LaunchedEffect runs again after popping the reader.
        viewModel.load("novel")
        advanceUntilIdle()
        assertEquals(1, repo.requests.size)
        assertFalse(viewModel.state.value.loading)
        assertSame(before, viewModel.state.value.details)
        assertEquals("١٢", viewModel.state.value.query)
        assertEquals(1, viewModel.filteredChapters().size)

        viewModel.clearQuery()
        assertEquals(30, viewModel.filteredChapters().size)
        assertEquals(30, viewModel.filteredChapters(reverseOrder = true).first().number)
        assertEquals(1, repo.requests.size)
    }

    @Test fun `recomposition while a request is in flight does not start it twice`() = runTest {
        val repo = FakeRepository().apply { delayMillis = 1000 }
        val viewModel = model(repo)
        viewModel.load("novel")
        viewModel.load("novel")
        runCurrent()
        assertEquals(1, repo.requests.size)
        assertTrue(viewModel.state.value.loading)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.loading)
    }

    @Test fun `continue follows local progress without refetching the novel`() = runTest {
        val repo = FakeRepository()
        val viewModel = model(repo)
        viewModel.load("novel")
        advanceUntilIdle()
        repo.history.value = HistoryEntity("novel", "Novel", null, "novel", "chapter/12", "Chapter 12")
        advanceUntilIdle()
        assertEquals("chapter/12", viewModel.state.value.continueChapter?.url)
        assertEquals(1, repo.requests.size)
    }

    @Test fun `explicit refresh keeps the list and query even if the network fails`() = runTest {
        val repo = FakeRepository()
        val viewModel = model(repo)
        viewModel.load("novel")
        advanceUntilIdle()
        viewModel.onQueryChange("12")
        val before = viewModel.state.value.details
        repo.delayMillis = 1000
        repo.fail = true
        viewModel.load("novel", forceRefresh = true)
        runCurrent()
        assertTrue(viewModel.state.value.loading)
        assertSame(before, viewModel.state.value.details)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.loading)
        assertNotNull(viewModel.state.value.error)
        assertSame(before, viewModel.state.value.details)
        assertEquals("12", viewModel.state.value.query)
        assertEquals(listOf("novel" to false, "novel" to true), repo.requests)
    }

    @Test fun `an initial failure can be retried and clearing a query clears its error`() = runTest {
        val repo = FakeRepository().apply { fail = true }
        val viewModel = model(repo)
        viewModel.load("novel")
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)
        repo.fail = false
        viewModel.load("novel")
        advanceUntilIdle()
        assertNull(viewModel.state.value.error)
        viewModel.onQueryChange("999")
        viewModel.markChapterNotFound()
        viewModel.clearQuery()
        assertNull(viewModel.state.value.chapterInputError)
        assertEquals(30, viewModel.filteredChapters().size)
    }

    @Test fun `query is restored from saved state`() = runTest {
        val handle = SavedStateHandle()
        val first = model(FakeRepository(), handle)
        first.onQueryChange("۱۲")
        val restored = model(FakeRepository(), SavedStateHandle(mapOf("chapter_query" to handle.get<String>("chapter_query"))))
        restored.load("novel")
        advanceUntilIdle()
        assertEquals("۱۲", restored.state.value.query)
        assertEquals(12, restored.requestedChapter()?.number)
    }

    @Test fun `switching novels cancels the old load and never leaks its filter`() = runTest {
        val repo = FakeRepository().apply { delayMillis = 1000 }
        val viewModel = model(repo)
        viewModel.load("first")
        runCurrent()
        viewModel.onQueryChange("12")
        viewModel.load("second")
        advanceUntilIdle()
        assertEquals("second", viewModel.state.value.details?.novel?.url)
        assertEquals("", viewModel.state.value.query)
    }

    private class FakeRepository : NovelDetailsRepository {
        val requests = mutableListOf<Pair<String, Boolean>>()
        val history = MutableStateFlow<HistoryEntity?>(null)
        var delayMillis = 0L
        var fail = false

        override suspend fun getDetails(seriesUrl: String, forceRefresh: Boolean): Result<NovelDetails> {
            requests += seriesUrl to forceRefresh
            delay(delayMillis)
            return if (fail) Result.failure(IllegalStateException("offline")) else Result.success(
                NovelDetails(
                    Novel(seriesUrl, "Novel", null, seriesUrl), "Synopsis", null, null,
                    (1..30).map { Chapter("Chapter $it", "chapter/$it", it - 1, it) },
                ),
            )
        }

        override fun observeHistory(slug: String) = history
        override fun observeIsFavorite(slug: String) = flowOf(false)
        override suspend fun toggleFavorite(novel: Novel, isCurrentlyFavorite: Boolean) = Unit
    }
}
