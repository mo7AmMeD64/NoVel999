package com.mo7ammed64.novelnun.data.network

import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.ChapterNumbers
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
            .asSequence()
            .filterNot { a ->
                // Skip anything sitting inside a pager/pagination control - some themes leave a
                // hidden "compact" anchor (e.g. just "الفصل 2") per row for responsive layouts,
                // and Jsoup sees it even though CSS hides it. Both look like noise: short text,
                // no real descriptive title or date attached to it.
                a.parents().any { p ->
                    p.hasClass("pagination") || p.hasClass("wp-pagenavi") || p.hasClass("page-numbers") ||
                        p.tagName() == "nav"
                }
            }
            .mapNotNull { a ->
                val href = a.absUrl("href").ifBlank { a.attr("href") }
                val text = a.text().trim()
                if (href.isBlank() || text.isBlank()) null else text to href
            }
            // A genuine chapter row has a full descriptive title (+ often a date); bare "2" style
            // anchors are short noise from the same markup quirk above. Keep "الفصل 2" (7 chars)
            // because some series list their chapters with short labels only.
            .filter { (text, _) -> text.length >= 6 }
            .toList()
            .let { entries ->
                // Dedupe by chapter number, keeping whichever duplicate has the longest (most
                // complete) title. Titles can use western, Arabic-Indic or Persian digits.
                val byNumber = linkedMapOf<Int, Pair<String, String>>()
                val noNumber = mutableListOf<Pair<String, String>>()
                for (entry in entries) {
                    val num = ChapterNumbers.parse(entry.first)
                    if (num == null) {
                        noNumber += entry
                        continue
                    }
                    val existing = byNumber[num]
                    if (existing == null || entry.first.length > existing.first.length) {
                        byNumber[num] = entry
                    }
                }
                byNumber.values.toList() + noNumber
            }
            .let { deduped ->
                // Normalize to chronological (oldest-first) order. The site renders the chapter
                // list newest-first; detect the direction from the numbered entries so the list,
                // the positions used by the jump-to-number field and the "reverse order" setting
                // all behave correctly regardless of how the markup is ordered.
                val numbers = deduped.mapNotNull { ChapterNumbers.parse(it.first) }
                val pairs = numbers.zipWithNext()
                val mostlyDescending =
                    pairs.count { (a, b) -> a > b } > pairs.count { (a, b) -> a < b }
                val chronological = if (mostlyDescending) deduped.asReversed() else deduped

                // Unnumbered rows (prologues, extras) stick with the chapter they follow; rows
                // that appear before any numbered chapter sort to the very top.
                var lastNumber = 0
                val sortKeys = IntArray(chronological.size)
                chronological.forEachIndexed { position, entry ->
                    val number = ChapterNumbers.parse(entry.first)
                    sortKeys[position] = number ?: lastNumber
                    if (number != null) lastNumber = number
                }
                chronological
                    .mapIndexed { position, entry -> sortKeys[position] to entry }
                    .sortedBy { (key, _) -> key } // stable: equal keys keep page order
                    .map { (_, entry) -> entry }
            }
            .mapIndexed { index, (title, href) ->
                Chapter(
                    title = title,
                    url = href,
                    index = index,
                    number = ChapterNumbers.parse(title),
                )
            }

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
