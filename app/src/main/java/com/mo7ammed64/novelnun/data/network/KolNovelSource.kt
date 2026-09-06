package com.mo7ammed64.novelnun.data.network

import android.content.Context
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.ChapterNumbers
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.model.NovelDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Talks to kolnovel.com's own JSON API (`wp-json/app/v2/...`) - the same one its official Android
 * app (com.benabdellah.KolNovelApp) uses - instead of scraping HTML off the site's theme. Captured
 * from a HAR of that app's traffic. This is far more reliable than scraping: chapter numbers,
 * order and titles come back as clean structured fields, and chapter content is already sanitized
 * (no ads/scripts to strip - the old scraper's spam-removal logic is gone entirely).
 *
 * Two things weren't visible in that capture, so rather than guess at unverified query params:
 * - "Popular": no `sort=rating`/`sort=popular` value was exercised, so this fetches a larger
 *   latest-sorted batch and ranks it client-side by the `score` field the API already returns.
 * - Search: no dedicated search endpoint was exercised, so this filters a fetched batch by title
 *   client-side (digit-normalized, so "142" matches "١٤٢") instead of trusting a guessed
 *   `search=`/`s=` param that the server might silently ignore.
 */
class KolNovelSource(context: Context) {

    private val base = "https://kolnovel.com/wp-json/app/v2"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .cache(Cache(File(context.cacheDir, "kolnovel_http_cache"), 15L * 1024 * 1024))
        .build()

    private suspend fun getJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) NovelNun/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            JSONObject(body)
        }
    }

    private fun novelFromJson(item: JSONObject): Novel {
        val id = item.getInt("id")
        val genres = item.optJSONArray("genres")?.let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }
        }.orEmpty()
        val score = if (item.has("score") && !item.isNull("score")) item.optDouble("score") else null
        return Novel(
            slug = item.optString("slug", id.toString()),
            title = item.optString("title"),
            coverUrl = item.optString("cover_url").ifBlank { null },
            url = "$base/titles/$id",
            rating = score?.let { "%.1f".format(it) },
            latestChapterLabel = item.optString("status").ifBlank { null },
            genres = genres,
        )
    }

    private fun idFromUrl(url: String): Int? = url.substringAfterLast("/").toIntOrNull()

    /** "أخر التحديثات" - latest updated novels. */
    suspend fun fetchLatest(page: Int = 1): List<Novel> {
        val json = getJson("$base/discover?limit=20&sort=latest&genre_match=all")
        val data = json.optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).map { novelFromJson(data.getJSONObject(it)) }
    }

    /** Popular / trending novels, ranked client-side by the score the API returns per item. */
    suspend fun fetchPopular(page: Int = 1): List<Novel> {
        val json = getJson("$base/discover?limit=24&sort=latest&genre_match=all")
        val data = json.optJSONArray("data") ?: JSONArray()
        return (0 until data.length())
            .map { data.getJSONObject(it) }
            .sortedByDescending { if (it.has("score") && !it.isNull("score")) it.optDouble("score") else 0.0 }
            .map { novelFromJson(it) }
    }

    /** "آخر الاضافات" - kept as an alias of [fetchLatest]; the API doesn't distinguish the two. */
    suspend fun fetchRecentlyAdded(): List<Novel> = fetchLatest()

    /** Confirmed from a later HAR capture: /discover has a real `q=` search parameter. */
    suspend fun search(query: String): List<Novel> {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val json = getJson("$base/discover?limit=40&sort=title&genre_match=all&q=$encoded")
        val data = json.optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).map { novelFromJson(data.getJSONObject(it)) }
    }

    suspend fun fetchDetails(seriesUrl: String): NovelDetails? = coroutineScope {
        val id = idFromUrl(seriesUrl) ?: return@coroutineScope null

        // Metadata and the first page of chapters don't depend on each other - fetch both at once
        // instead of waiting on the first before starting the second.
        val titleDeferred = async { getJson("$base/titles/$id").optJSONObject("data") }
        val firstPageDeferred = async { getJson("$base/titles/$id/reading-list?limit=300") }

        val titleJson = titleDeferred.await() ?: return@coroutineScope null
        val novel = novelFromJson(titleJson)

        // Chapters come back chronological (oldest first) already - no ordering heuristics needed.
        val chapters = mutableListOf<Chapter>()
        var listJson = firstPageDeferred.await()
        var page = 0
        while (true) {
            val data = listJson.optJSONArray("data") ?: JSONArray()
            for (i in 0 until data.length()) {
                val entry = data.getJSONObject(i)
                val chapterId = entry.getInt("id")
                val number = entry.optString("number").ifBlank { null }?.let { ChapterNumbers.parse(it) }
                val rawTitle = entry.optString("title").ifBlank { null }
                val displayTitle = when {
                    number != null && rawTitle != null -> "الفصل $number: $rawTitle"
                    number != null -> "الفصل $number"
                    else -> rawTitle ?: "فصل"
                }
                chapters += Chapter(
                    title = displayTitle,
                    url = "$base/reader/$chapterId",
                    index = chapters.size,
                    number = number,
                )
            }
            val meta = listJson.optJSONObject("meta")
            val cursor = meta?.optString("next_cursor")?.takeIf { it.isNotBlank() && it != "null" }
            page++
            if (cursor == null || page >= 15) break
            listJson = getJson("$base/titles/$id/reading-list?limit=300&cursor=$cursor")
        }

        val description = titleJson.optString("description")
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        NovelDetails(
            novel = novel,
            synopsis = description,
            status = titleJson.optString("status").ifBlank { null },
            author = titleJson.optString("author").ifBlank { null },
            chapters = chapters,
        )
    }

    suspend fun fetchChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val id = idFromUrl(chapterUrl) ?: return@withContext ""
        val json = getJson("$base/reader/$id")
        val data = json.optJSONObject("data") ?: return@withContext ""
        val access = data.optJSONObject("access")
        if (access != null && !access.optBoolean("has_access", true)) {
            return@withContext "<p>هذا الفصل مقفل ويتطلب صلاحية وصول غير متوفرة في هذا التطبيق.</p>"
        }
        val entry = data.optJSONObject("entry") ?: return@withContext ""
        entry.optString("content")
    }
}
