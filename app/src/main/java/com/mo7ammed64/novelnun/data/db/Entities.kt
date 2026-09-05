package com.mo7ammed64.novelnun.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A novel the user saved to their library ("Saved" destination). */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val slug: String,
    val title: String,
    val coverUrl: String?,
    val url: String,
    val addedAt: Long = System.currentTimeMillis(),
)

/** Reading progress for a novel, used by "Continue" and the History screen. */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val slug: String,
    val title: String,
    val coverUrl: String?,
    val novelUrl: String,
    val lastChapterUrl: String,
    val lastChapterTitle: String,
    val lastReadAt: Long = System.currentTimeMillis(),
)

/** A chapter saved for offline reading ("Open files" FAB item). */
@Entity(tableName = "downloaded_chapters")
data class DownloadedChapterEntity(
    @PrimaryKey val chapterUrl: String,
    val novelSlug: String,
    val novelTitle: String,
    val chapterTitle: String,
    val content: String,
    val downloadedAt: Long = System.currentTimeMillis(),
)
