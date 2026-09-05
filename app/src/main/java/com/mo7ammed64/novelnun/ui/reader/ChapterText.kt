package com.mo7ammed64.novelnun.ui.reader

import org.jsoup.Jsoup

internal object ChapterText {
    fun paragraphs(html: String): List<String> {
        val document = Jsoup.parse(html)
        document.select("script, style, noscript").remove()
        val paragraphs = document.select("p").map { it.wholeText().trim() }.filter { it.isNotBlank() }
        // Some offline/source chapters use divs or line breaks instead of paragraph elements.
        return paragraphs.ifEmpty { document.body().wholeText().lines().map(String::trim).filter(String::isNotBlank) }
    }
}
