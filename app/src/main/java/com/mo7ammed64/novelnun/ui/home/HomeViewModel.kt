package com.mo7ammed64.novelnun.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val popular: List<Novel> = emptyList(),
    val latest: List<Novel> = emptyList(),
    val loadingPopular: Boolean = true,
    val loadingLatest: Boolean = true,
    val error: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NovelRepository.get(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingPopular = true, loadingLatest = true, error = null)

            repo.getPopular()
                .onSuccess { list -> _state.value = _state.value.copy(popular = list, loadingPopular = false) }
                .onFailure { e -> _state.value = _state.value.copy(loadingPopular = false, error = e.message) }

            repo.getLatest()
                .onSuccess { list -> _state.value = _state.value.copy(latest = list, loadingLatest = false) }
                .onFailure { e -> _state.value = _state.value.copy(loadingLatest = false, error = e.message) }
        }
    }
}
