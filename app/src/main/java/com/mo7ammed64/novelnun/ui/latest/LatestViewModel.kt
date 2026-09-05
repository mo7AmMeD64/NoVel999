package com.mo7ammed64.novelnun.ui.latest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LatestUiState(
    val loading: Boolean = true,
    val novels: List<Novel> = emptyList(),
    val error: String? = null,
)

class LatestViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NovelRepository.get(application)

    private val _state = MutableStateFlow(LatestUiState())
    val state: StateFlow<LatestUiState> = _state

    init {
        viewModelScope.launch {
            repo.getLatest()
                .onSuccess { list -> _state.value = LatestUiState(loading = false, novels = list) }
                .onFailure { e -> _state.value = LatestUiState(loading = false, error = e.message) }
        }
    }
}
