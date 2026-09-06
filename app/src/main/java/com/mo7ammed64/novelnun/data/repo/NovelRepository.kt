package com.mo7ammed64.novelnun.data.repo

import android.content.Context
import com.mo7ammed64.novelnun.data.db.AppDatabase
import com.mo7ammed64.novelnun.data.db.DownloadedChapterEntity
import com.mo7ammed64.novelnun.data.db.FavoriteEntity
import com.mo7ammed64.novelnun.data.db.HistoryEntity
import com.mo7ammed64.novelnun.data.model.Chapter
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.model.NovelDetails
import com.mo7ammed64.novelnun.data.network.KolNovelSource
import kotlinx.coroutines.flow.Flow

/** Single entry point the UI layer talks to: remote scraping + local persistence. */
class NovelRepository private constructor(context: Context) {

    private val source = KolNovelSource(context)
    private val db = AppDatabase.get(context)

    // Remote --------------------------------------------------------------

    suspend fun getRecentlyAdded(): Result<List<Novel>> = runCatching { source.fetchRecentlyAdded() }
    suspend fun getPopular(): Result<List<Novel>> = runCatching { source.fetchPopular() }
    suspend fun getLatest(): Result<List<Novel>> = runCatching { source.fetchLatest() }
    suspend fun search(query: String): Result<List<Novel>> = runCatching { source.search(query) }
    suspend fun getDetails(seriesUrl: String): Result<NovelDetails> = runCatching {
        source.fetchDetails(seriesUrl) ?: error("تعذر تحميل تفاصيل الرواية")
    }
    suspend fun getChapterContent(chapterUrl: String): Result<String> = runCatching {
        source.fetchChapterContent(chapterUrl)
    }

    // Favorites -------------------------------------------------------------

    fun observeFavorites(): Flow<List<FavoriteEntity>> = db.favoriteDao().observeAll()
    fun observeIsFavorite(slug: String): Flow<Boolean> = db.favoriteDao().observeIsFavorite(slug)

    suspend fun toggleFavorite(novel: Novel, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            db.favoriteDao().deleteBySlug(novel.slug)
        } else {
            db.favoriteDao().insert(
                FavoriteEntity(
                    slug = novel.slug,
                    title = novel.title,
                    coverUrl = novel.coverUrl,
                    url = novel.url,
                ),
            )
        }
    }

    // History / continue reading ---------------------------------------------

    fun observeHistory(): Flow<List<HistoryEntity>> = db.historyDao().observeAll()

    suspend fun findHistory(slug: String): HistoryEntity? = db.historyDao().findBySlug(slug)

    suspend fun recordProgress(novel: Novel, chapter: Chapter) {
        db.historyDao().upsert(
            HistoryEntity(
                slug = novel.slug,
                title = novel.title,
                coverUrl = novel.coverUrl,
                novelUrl = novel.url,
                lastChapterUrl = chapter.url,
                lastChapterTitle = chapter.title,
            ),
        )
    }

    suspend fun removeHistory(slug: String) = db.historyDao().deleteBySlug(slug)
    suspend fun clearHistory() = db.historyDao().clear()

    // Downloaded / offline chapters ------------------------------------------

    fun observeDownloads(): Flow<List<DownloadedChapterEntity>> = db.downloadedChapterDao().observeAll()

    suspend fun downloadChapter(novelSlug: String, novelTitle: String, chapter: Chapter) {
        val content = source.fetchChapterContent(chapter.url)
        db.downloadedChapterDao().insert(
            DownloadedChapterEntity(
                chapterUrl = chapter.url,
                novelSlug = novelSlug,
                novelTitle = novelTitle,
                chapterTitle = chapter.title,
                content = content,
            ),
        )
    }

    suspend fun removeDownload(entity: DownloadedChapterEntity) = db.downloadedChapterDao().delete(entity)
    suspend fun findDownload(chapterUrl: String) = db.downloadedChapterDao().find(chapterUrl)

    companion object {
        @Volatile private var instance: NovelRepository? = null
        fun get(context: Context): NovelRepository = instance ?: synchronized(this) {
            instance ?: NovelRepository(context.applicationContext).also { instance = it }
        }
    }
}
