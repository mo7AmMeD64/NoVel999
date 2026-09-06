package com.mo7ammed64.novelnun.data.model

/** A novel/series card as shown in listings (Popular, Latest, Search results). */
data class Novel(
    val slug: String,
    val title: String,
    val coverUrl: String?,
    val url: String,
    val rating: String? = null,
    val latestChapterLabel: String? = null,
    val genres: List<String> = emptyList(),
    val description: String? = null,
)

/** Full details for a novel's info page. */
data class NovelDetails(
    val novel: Novel,
    val synopsis: String,
    val status: String?,
    val author: String?,
    val chapters: List<Chapter>,
    /** Opaque cursor for fetching the next page of chapters, or null if this is all of them. */
    val nextChaptersCursor: String? = null,
)

/** One additional page of chapters fetched after the initial [NovelDetails] load. */
data class ChapterPage(
    val chapters: List<Chapter>,
    val nextCursor: String?,
)

/** A single chapter entry in a novel's chapter list. */
data class Chapter(
    val title: String,
    val url: String,
    val index: Int,
    /** Chapter number parsed from the title (western/Arabic-Indic/Persian digits), if any. */
    val number: Int? = null,
)
