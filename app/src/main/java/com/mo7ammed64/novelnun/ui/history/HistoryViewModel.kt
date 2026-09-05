package com.mo7ammed64.novelnun.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mo7ammed64.novelnun.data.db.HistoryEntity
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NovelRepository.get(application)

    val history: StateFlow<List<HistoryEntity>> = repo.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(slug: String) {
        viewModelScope.launch { repo.removeHistory(slug) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearHistory() }
    }
}
