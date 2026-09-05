package com.mo7ammed64.novelnun.ui.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mo7ammed64.novelnun.data.db.FavoriteEntity
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SavedViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NovelRepository.get(application)

    val favorites: StateFlow<List<FavoriteEntity>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
