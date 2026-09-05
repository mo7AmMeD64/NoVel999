package com.mo7ammed64.novelnun.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

/** Rail sections follow their vertical order; deeper pages use a horizontal depth transition. */
internal fun railTransitionDirection(from: String?, to: String?): Int? {
    val fromIndex = Dest.railDestinations.indexOfFirst { it.route == from }
    val toIndex = Dest.railDestinations.indexOfFirst { it.route == to }
    if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return null
    return if (toIndex > fromIndex) 1 else -1
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pageEnter(
    popping: Boolean,
    forwardSign: Int,
): EnterTransition {
    val tabDirection = railTransitionDirection(initialState.destination.route, targetState.destination.route)
    val slide = if (tabDirection != null) {
        slideInVertically(spring(dampingRatio = 0.9f, stiffness = 380f)) { tabDirection * it / 5 }
    } else {
        slideInHorizontally(spring(dampingRatio = 0.95f, stiffness = 380f)) {
            if (popping) -forwardSign * it / 4 else forwardSign * it
        }
    }
    return slide + scaleIn(
        initialScale = if (popping) 0.96f else 0.98f,
        animationSpec = spring(dampingRatio = 0.95f, stiffness = 380f),
    ) + fadeIn(tween(180))
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pageExit(
    popping: Boolean,
    forwardSign: Int,
): ExitTransition {
    val tabDirection = railTransitionDirection(initialState.destination.route, targetState.destination.route)
    val slide = if (tabDirection != null) {
        slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { -tabDirection * it / 8 }
    } else {
        slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) {
            if (popping) forwardSign * it else -forwardSign * it / 4
        }
    }
    return slide + scaleOut(targetScale = 0.96f, animationSpec = tween(220)) + fadeOut(tween(160))
}
