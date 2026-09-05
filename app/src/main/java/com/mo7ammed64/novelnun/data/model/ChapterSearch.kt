package com.mo7ammed64.novelnun.data.model

/** Search never changes the source list; clearing the query restores every chapter. */
object ChapterSearch {
    private val numericQuery = Regex("[0-9٠-٩۰-۹]+")

    fun filter(chapters: List<Chapter>, query: String, reverseOrder: Boolean = false): List<Chapter> {
        val trimmed = query.trim()
        val matches = when {
            trimmed.isEmpty() -> chapters
            numericQuery.matches(trimmed) -> {
                val number = ChapterNumbers.parse(trimmed)
                if (number == null) emptyList() else numberedMatches(chapters, number)
            }
            else -> chapters.filter { it.title.contains(trimmed, ignoreCase = true) }
        }
        return if (reverseOrder) matches.asReversed() else matches
    }

    /** A title query can also be opened directly when it has exactly one result. */
    fun requestedChapter(chapters: List<Chapter>, query: String): Chapter? =
        if (query.isBlank()) null else filter(chapters, query).singleOrNull()

    private fun numberedMatches(chapters: List<Chapter>, number: Int): List<Chapter> {
        val matches = chapters.filter { (it.number ?: ChapterNumbers.parse(it.title)) == number }
        if (matches.isNotEmpty()) return matches

        // Positional fallback is only meaningful for an entirely unnumbered source. Never open
        // a different, numbered chapter just because a chapter is missing from the source list.
        if (chapters.any { it.number != null || ChapterNumbers.parse(it.title) != null }) return emptyList()
        return if (number > 0) chapters.getOrNull(number - 1)?.let(::listOf).orEmpty() else emptyList()
    }
}
