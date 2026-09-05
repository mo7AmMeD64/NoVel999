package com.mo7ammed64.novelnun.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mo7ammed64.novelnun.data.model.Novel
import com.mo7ammed64.novelnun.ui.common.NovelListItem

@Composable
fun SavedScreen(onOpenNovel: (String) -> Unit, viewModel: SavedViewModel = viewModel()) {
    val favorites by viewModel.favorites.collectAsState()

    Scaffold { padding ->
        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد روايات محفوظة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(favorites) { fav ->
                    val novel = Novel(slug = fav.slug, title = fav.title, coverUrl = fav.coverUrl, url = fav.url)
                    NovelListItem(novel = novel, onClick = { onOpenNovel(novel.url) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
