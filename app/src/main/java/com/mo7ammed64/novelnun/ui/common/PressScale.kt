package com.mo7ammed64.novelnun.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/** Slight press-scale feedback to layer on top of the standard ripple from clickable/Card/etc. */
@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }): Pair<MutableInteractionSource, Modifier> {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, label = "pressScale")
    return interactionSource to Modifier.scale(scale)
}
