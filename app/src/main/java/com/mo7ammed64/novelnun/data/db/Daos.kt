package com.mo7ammed64.novelnun.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE slug = :slug)")
    fun observeIsFavorite(slug: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE slug = :slug")
    suspend fun deleteBySlug(slug: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastReadAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE slug = :slug LIMIT 1")
    suspend fun findBySlug(slug: String): HistoryEntity?

    @Query("SELECT * FROM history WHERE slug = :slug LIMIT 1")
    fun observeBySlug(slug: String): Flow<HistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE slug = :slug")
    suspend fun deleteBySlug(slug: String)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface DownloadedChapterDao {
    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters WHERE chapterUrl = :url LIMIT 1")
    suspend fun find(url: String): DownloadedChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedChapterEntity)

    @Delete
    suspend fun delete(entity: DownloadedChapterEntity)
}
