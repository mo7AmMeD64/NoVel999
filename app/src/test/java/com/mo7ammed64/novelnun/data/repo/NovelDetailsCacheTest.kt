package com.mo7ammed64.novelnun.data.repo

import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.model.NovelDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NovelDetailsCacheTest {
    private fun details(url: String) = NovelDetails(Novel(url, url, null, url), "", null, null, emptyList())

    @Test fun `details and reader share one request even when loading concurrently`() = runTest {
        var requests = 0
        val cache = NovelDetailsCache { url -> requests++; delay(100); details(url) }
        val first = async { cache.get("novel") }
        val second = async { cache.get("novel") }
        assertSame(first.await(), second.await())
        cache.get("novel")
        assertEquals(1, requests)
        cache.get("novel", forceRefresh = true)
        assertEquals(2, requests)
    }

    @Test fun `unrelated novels do not wait for a slow request`() = runTest {
        val release = CompletableDeferred<Unit>()
        val cache = NovelDetailsCache { url ->
            if (url == "slow") release.await()
            details(url)
        }
        val slow = async { cache.get("slow") }
        runCurrent()
        val fast = async { cache.get("fast") }
        runCurrent()
        assertTrue(fast.isCompleted)
        assertEquals("fast", fast.await().novel.url)
        release.complete(Unit)
        slow.await()
    }

    @Test fun `cancelling one caller releases the gate for another reader`() = runTest {
        val release = CompletableDeferred<Unit>()
        var requests = 0
        val cache = NovelDetailsCache { url ->
            if (++requests == 1) release.await()
            details(url)
        }
        val first = launch { cache.get("novel") }
        runCurrent()
        val second = async { cache.get("novel") }
        runCurrent()
        first.cancelAndJoin()
        assertEquals("novel", second.await().novel.url)
        assertEquals(2, requests)
    }

    @Test fun `a failed refresh leaves the last usable data intact`() = runTest {
        var fail = false
        val cache = NovelDetailsCache { url -> if (fail) error("offline") else details(url) }
        val original = cache.get("novel")
        fail = true
        assertTrue(runCatching { cache.get("novel", forceRefresh = true) }.isFailure)
        assertSame(original, cache.get("novel"))
    }

    @Test fun `failed and cancelled initial requests can be retried`() = runTest {
        var requests = 0
        val cache = NovelDetailsCache { url ->
            when (++requests) {
                1 -> error("offline")
                2 -> throw CancellationException("left screen")
                else -> details(url)
            }
        }
        assertTrue(runCatching { cache.get("novel") }.isFailure)
        assertTrue(runCatching { cache.get("novel") }.exceptionOrNull() is CancellationException)
        assertEquals("novel", cache.get("novel").novel.url)
        assertEquals(3, requests)
    }

    @Test fun `the cache evicts the least recently used novel`() = runTest {
        val requests = mutableListOf<String>()
        val cache = NovelDetailsCache(capacity = 2) { url -> requests += url; details(url) }
        cache.get("a")
        cache.get("b")
        cache.get("a")
        cache.get("c")
        cache.get("a")
        cache.get("b")
        assertEquals(listOf("a", "b", "c", "b"), requests)
    }
}
