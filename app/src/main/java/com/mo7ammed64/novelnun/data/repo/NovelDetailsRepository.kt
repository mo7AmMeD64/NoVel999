package com.mo7ammed64.novelnun.data.repo

import com.mo7ammed64.novelnun.data.db.HistoryEntity
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.model.NovelDetails
import kotlinx.coroutines.flow.Flow

/** The details screen's data boundary, also used by its navigation regression tests. */
interface NovelDetailsRepository {
    suspend fun getDetails(seriesUrl: String, forceRefresh: Boolean = false): Result<NovelDetails>
    fun observeHistory(slug: String): Flow<HistoryEntity?>
    fun observeIsFavorite(slug: String): Flow<Boolean>
    suspend fun toggleFavorite(novel: Novel, isCurrentlyFavorite: Boolean)
}
