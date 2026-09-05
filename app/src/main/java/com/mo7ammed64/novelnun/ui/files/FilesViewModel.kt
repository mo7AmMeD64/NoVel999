package com.mo7ammed64.novelnun.ui.files

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mo7ammed64.novelnun.data.db.DownloadedChapterEntity
import com.mo7ammed64.novelnun.data.repo.NovelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = NovelRepository.get(application)

    val downloads: StateFlow<List<DownloadedChapterEntity>> = repo.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(entity: DownloadedChapterEntity) {
        viewModelScope.launch { repo.removeDownload(entity) }
    }
}
