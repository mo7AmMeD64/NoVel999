package com.mo7ammed64.novelnun.data.repo

import com.mo7ammed64.novelnun.data.model.NovelDetails
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Small session cache shared by Details and Reader. Requests for the same URL share a gate;
 * unrelated novels never wait for each other's network calls. Only successful loads are cached.
 */
internal class NovelDetailsCache(
    private val capacity: Int = 8,
    private val fetch: suspend (String) -> NovelDetails,
) {
    init { require(capacity > 0) }

    private class Gate(val mutex: Mutex = Mutex(), var users: Int = 0)
    private val monitor = Any()
    private val gates = mutableMapOf<String, Gate>()
    private val entries = LinkedHashMap<String, NovelDetails>(capacity, 0.75f, true)

    suspend fun get(url: String, forceRefresh: Boolean = false): NovelDetails {
        val gate = synchronized(monitor) { gates.getOrPut(url) { Gate() }.also { it.users++ } }
        try {
            return gate.mutex.withLock {
                val cached = synchronized(monitor) { if (forceRefresh) null else entries[url] }
                if (cached != null) return@withLock cached
                fetch(url).also { details ->
                    synchronized(monitor) {
                        entries[url] = details
                        if (entries.size > capacity) entries.remove(entries.keys.first())
                    }
                }
            }
        } finally {
            // Also release gates after failed/cancelled requests, so neither failures nor locks
            // accumulate. An explicit refresh doesn't discard the last usable cached details.
            synchronized(monitor) { if (--gate.users == 0) gates.remove(url) }
        }
    }
}
