package com.mo7ammed64.novelnun.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mo7ammed64.novelnun.ui.common.rememberPressScale

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

/** A spring-driven active pill connects the compact rail's sections instead of blinking on/off. */
@Composable
fun NovelNunNavRail(currentRoute: String?, onSelect: (Dest) -> Unit) {
    val requestedIndex = Dest.railDestinations.indexOfFirst { it.route == currentRoute }
    var lastIndex by remember { mutableIntStateOf(requestedIndex.coerceAtLeast(0)) }
    SideEffect { if (requestedIndex >= 0) lastIndex = requestedIndex }
    // Preserve the selection while the rail slides out on the way to a deeper page.
    val selectedIndex = if (requestedIndex >= 0) requestedIndex else lastIndex
    val itemHeight = 52.dp + with(LocalDensity.current) { 20.sp.toDp() }
    val indicatorOffset by animateDpAsState(
        targetValue = (itemHeight + 12.dp) * selectedIndex + 4.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "Navigation selection",
    )

    Surface(modifier = Modifier.width(80.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(Modifier.padding(top = 32.dp).verticalScroll(rememberScrollState())) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).offset(y = indicatorOffset).size(width = 56.dp, height = 36.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {}
            Column(
                modifier = Modifier.selectableGroup(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Dest.railDestinations.forEachIndexed { index, dest ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "Navigation icon color",
                    )
                    val iconScale by animateFloatAsState(
                        if (selected) 1.12f else 1f,
                        spring(dampingRatio = 0.7f, stiffness = 500f),
                        label = "Navigation icon scale",
                    )
                    val (interaction, pressScale) = rememberPressScale()
                    Column(
                        modifier = Modifier.width(80.dp).height(itemHeight).then(pressScale)
                            .clip(RoundedCornerShape(20.dp))
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                interactionSource = interaction,
                                indication = androidx.compose.material3.ripple(),
                                onClick = { if (!selected) onSelect(dest) },
                            ).padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(width = 56.dp, height = 36.dp), contentAlignment = Alignment.Center) {
                            Icon(iconFor(dest), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp).scale(iconScale))
                        }
                        Text(
                            labelFor(dest),
                            style = MaterialTheme.typography.labelMedium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
