package com.mo7ammed64.novelnun.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private fun iconFor(dest: Dest) = when (dest) {
    Dest.Home -> Icons.Default.Home
    Dest.Search -> Icons.Default.Search
    Dest.Saved -> Icons.Default.Favorite
    Dest.Settings -> Icons.Default.Settings
    else -> Icons.Default.Home
}

private fun labelFor(dest: Dest) = when (dest) {
    Dest.Home -> "Home"
    Dest.Search -> "Search"
    Dest.Saved -> "Saved"
    Dest.Settings -> "Settings"
    else -> ""
}

@Composable
fun NovelNunNavRail(currentRoute: String?, onSelect: (Dest) -> Unit) {
    NavigationRail(
        modifier = Modifier.width(80.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Dest.railDestinations.forEach { dest ->
            NavigationRailItem(
                selected = currentRoute == dest.route,
                onClick = { onSelect(dest) },
                icon = { Icon(iconFor(dest), contentDescription = labelFor(dest)) },
                label = { Text(labelFor(dest), style = MaterialTheme.typography.labelMedium) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
