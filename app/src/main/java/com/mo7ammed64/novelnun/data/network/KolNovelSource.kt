package com.mo7ammed64.novelnun.data.network

import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.model.NovelDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

/**
 * Scrapes kolnovel.com (a self-hosted "lightnovel" WordPress theme site, the same one the
 * upstream Keiyoushi "KolNovel" extension targets). The theme is not publicly documented, so the
 * CSS selectors below are best-effort based on the page structure and may need small adjustments
 * if the site changes its markup - this class is kept isolated from the rest of the app for
 * exactly that reason. All screens degrade to an empty state rather than crashing on a selector
 * miss.
 */
class KolNovelSource {

    private val baseUrl = "https://kolnovel.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private suspend fun fetch(url: String): Document = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Referer", baseUrl)
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            Jsoup.parse(body, baseUrl)
        }
    }

    /** "أخر التحديثات" - latest updated novels, from the series listing page. */
    suspend fun fetchLatest(page: Int = 1): List<Novel> {
        val url = "$baseUrl/series/?status=&type=&order=update&page=$page"
        return parseSeriesCards(fetch(url))
    }

    /** Popular / trending novels, sourced from the highest-rated ordering. */
    suspend fun fetchPopular(page: Int = 1): List<Novel> {
        val url = "$baseUrl/series/?status=&type=&order=rating&page=$page"
        return parseSeriesCards(fetch(url))
    }

    /** "آخر الاضافات" - most recently added novels (used for the top explore row). */
    suspend fun fetchRecentlyAdded(): List<Novel> {
        val url = "$baseUrl/series/?status=&type=&order=latest"
        return parseSeriesCards(fetch(url))
    }

    suspend fun search(query: String): List<Novel> {
        val url = "$baseUrl/?s=${java.net.URLEncoder.encode(query, "UTF-8")}&post_type=wp-manga"
        return parseSeriesCards(fetch(url))
    }

    private fun parseSeriesCards(doc: Document): List<Novel> {
        val candidates = doc.select(
            "div.listupd .bs, div.listupd .bsx, .page-item-detail, article, .maindetail, .box",
        )
        val results = mutableListOf<Novel>()
        for (el in candidates) {
            val link = el.selectFirst("a[href*=/series/]") ?: continue
            val href = link.absUrl("href").ifBlank { link.attr("href") }
            val slug = slugFromUrl(href) ?: continue
            val title = el.selectFirst("h2, h3, .tt, .title")?.text()?.ifBlank { null }
                ?: link.attr("title").ifBlank { null }
                ?: continue
            val cover = el.selectFirst("img")?.let { img ->
                img.absUrl("src").ifBlank { img.absUrl("data-src") }
            }
            val rating = el.selectFirst(".rating, .numscore, .score")?.text()?.ifBlank { null }
            val latestChapter = el.selectFirst(".epxs, .chapter, .lastest, a[href*=chapter]")?.text()?.ifBlank { null }
            if (results.none { it.slug == slug }) {
                results += Novel(
                    slug = slug,
                    title = title.trim(),
                    coverUrl = cover,
                    url = href,
                    rating = rating,
                    latestChapterLabel = latestChapter,
                )
            }
        }
        return results
    }

    private fun slugFromUrl(url: String): String? {
        val trimmed = url.substringAfter("/series/", "").trim('/')
        return trimmed.ifBlank { null }
    }

    suspend fun fetchDetails(seriesUrl: String): NovelDetails? {
        val doc = fetch(seriesUrl)
        val title = doc.selectFirst("h1, .entry-title, .titlepost")?.text() ?: return null
        val cover = doc.selectFirst(".thumb img, .summary_image img, article img")?.let { img ->
            img.absUrl("src").ifBlank { img.absUrl("data-src") }
        }
        val synopsis = doc.selectFirst(".entry-content, .summary__content, .description-summary")
            ?.text().orEmpty()
        val status = doc.selectFirst(".status, .imptdt:contains(الحالة) i")?.text()
        val author = doc.selectFirst(".author-content a, .imptdt:contains(المؤلف) i")?.text()
        val slug = slugFromUrl(seriesUrl) ?: seriesUrl

        val chapters = doc.select("li.wp-manga-chapter a, .eplister a, .chapter-list a")
            .mapIndexedNotNull { index, a ->
                val href = a.absUrl("href").ifBlank { a.attr("href") }
                if (href.isBlank()) return@mapIndexedNotNull null
                Chapter(title = a.text().trim().ifBlank { "الفصل ${index + 1}" }, url = href, index = index)
            }
            .let { list -> if (list.isEmpty()) list else list }

        val novel = Novel(
            slug = slug,
            title = title.trim(),
            coverUrl = cover,
            url = seriesUrl,
            genres = doc.select(".genres-content a, .mgen a").map { it.text() },
        )
        return NovelDetails(
            novel = novel,
            synopsis = synopsis.trim(),
            status = status,
            author = author,
            chapters = chapters,
        )
    }

    /**
     * Fetches and cleans chapter text. Mirrors the upstream KolNovel extension's spam-removal
     * logic: the site hides ad paragraphs behind dynamically generated CSS classes declared in an
     * inline <style> block, plus assorted navigation/ad/social clutter.
     */
    suspend fun fetchChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = fetch(chapterUrl)

        doc.select(".epcontent .code-block").remove()

        val styleText = doc.select("article > style").text()
        Regex("""\.\w+(?=\s*[,{])""").findAll(styleText).forEach { match ->
            doc.select("p${match.value}").remove()
        }

        doc.select(
            ".unlock-buttons, .ads, script, style, .sharedaddy, .su-spoiler-title, noscript, ins, " +
                ".adsbygoogle, iframe, [id*=google], [class*=google], .chapter-navigation, " +
                ".prev-next, .navigation, .post-nav, .related-novels, .recommendations, .sidebar, " +
                ".widget, .author-box, .comments, #comments, .footer, .breadcrumb, .breadcrumbs, " +
                ".share-buttons, .social-share, .rating, .chapter-actions, .download-chapter",
        ).remove()

        val content: Element = doc.select(".epcontent.entry-content, .text-right, .reading-content")
            .maxByOrNull { el -> el.select("p").sumOf { it.text().length } } ?: return@withContext ""

        content.select("p").forEach { p ->
            val text = p.text()
            if (text.isBlank() || text.length < 10) {
                p.remove()
            }
        }

        content.html()
    }
}
