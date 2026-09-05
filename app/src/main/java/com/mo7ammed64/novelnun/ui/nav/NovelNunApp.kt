package com.mo7ammed64.novelnun.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mo7ammed64.novelnun.ui.details.DetailsScreen
import com.mo7ammed64.novelnun.ui.files.FilesScreen
import com.mo7ammed64.novelnun.ui.history.HistoryScreen
import com.mo7ammed64.novelnun.ui.home.HomeScreen
import com.mo7ammed64.novelnun.ui.reader.ReaderScreen
import com.mo7ammed64.novelnun.ui.saved.SavedScreen
import com.mo7ammed64.novelnun.ui.search.SearchScreen
import com.mo7ammed64.novelnun.ui.settings.AppSettings
import com.mo7ammed64.novelnun.ui.settings.SettingsScreen

/** Expressive emphasized easing — snappy start, soft landing. */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val NAV_DURATION = 420

/** Forward navigation: the new screen slides in over a gently receding old one. */
private fun forwardEnter(): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(NAV_DURATION, easing = EmphasizedEasing),
        initialOffsetX = { it / 4 },
    ) + fadeIn(animationSpec = tween(NAV_DURATION / 2))

private fun forwardExit(): ExitTransition =
    scaleOut(
        animationSpec = tween(NAV_DURATION, easing = EmphasizedEasing),
        targetScale = 0.94f,
    ) + fadeOut(animationSpec = tween(NAV_DURATION / 2))

/** Back navigation: the top screen slides away, the one underneath scales back up. */
private fun backEnter(): EnterTransition =
    scaleIn(
        animationSpec = tween(NAV_DURATION, easing = EmphasizedEasing),
        initialScale = 0.94f,
    ) + fadeIn(animationSpec = tween(NAV_DURATION / 2))

private fun backExit(): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(NAV_DURATION, easing = EmphasizedEasing),
        targetOffsetX = { it / 4 },
    ) + fadeOut(animationSpec = tween(NAV_DURATION / 2))

/** Rail destinations cross-switch with a subtle shared-axis scale + fade. */
private fun railEnter(): EnterTransition =
    scaleIn(
        animationSpec = tween(NAV_DURATION, easing = EmphasizedEasing),
        initialScale = 0.96f,
    ) + fadeIn(animationSpec = tween(NAV_DURATION, easing = EmphasizedEasing))

private fun railExit(): ExitTransition =
    scaleOut(
        animationSpec = tween(NAV_DURATION / 2, easing = EmphasizedEasing),
        targetScale = 1.02f,
    ) + fadeOut(animationSpec = tween(NAV_DURATION / 2))

@Composable
fun NovelNunApp(
    settings: AppSettings,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showRail = Dest.railDestinations.any { it.route == currentRoute }
    val railRoutes = Dest.railDestinations.map { it.route }.toSet()

    fun isRailRoute(route: String?): Boolean = route != null && route in railRoutes

    fun AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.isRailSwitch(): Boolean =
        isRailRoute(initialState.destination.route) && isRailRoute(targetState.destination.route)

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (showRail) {
                NovelNunNavRail(currentRoute = currentRoute) { dest ->
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = Dest.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { if (isRailSwitch()) railEnter() else forwardEnter() },
                exitTransition = { if (isRailSwitch()) railExit() else forwardExit() },
                popEnterTransition = { backEnter() },
                popExitTransition = { backExit() },
            ) {
                composable(Dest.Home.route) {
                    HomeScreen(
                        onOpenNovel = { url -> navController.navigate(Dest.Details.build(url)) },
                        onOpenHistory = { navController.navigate(Dest.History.route) },
                        onOpenFiles = { navController.navigate(Dest.Files.route) },
                        onOpenLatest = { navController.navigate(Dest.Latest.route) },
                    )
                }
                composable(Dest.Latest.route) {
                    com.mo7ammed64.novelnun.ui.latest.LatestScreen(
                        onBack = { navController.popBackStack() },
                        onOpenNovel = { url -> navController.navigate(Dest.Details.build(url)) },
                    )
                }
                composable(Dest.Search.route) {
                    SearchScreen(onOpenNovel = { url -> navController.navigate(Dest.Details.build(url)) })
                }
                composable(Dest.Saved.route) {
                    SavedScreen(onOpenNovel = { url -> navController.navigate(Dest.Details.build(url)) })
                }
                composable(Dest.Settings.route) { SettingsScreen(settings = settings) }
                composable(Dest.History.route) {
                    HistoryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenNovel = { url -> navController.navigate(Dest.Details.build(url)) },
                    )
                }
                composable(Dest.Files.route) {
                    FilesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenChapter = { novelUrl, chapterUrl ->
                            navController.navigate(Dest.Reader.build(novelUrl, chapterUrl))
                        },
                    )
                }
                composable(Dest.Details.route) { entry ->
                    val url = java.net.URLDecoder.decode(entry.arguments?.getString("url").orEmpty(), "UTF-8")
                    DetailsScreen(
                        seriesUrl = url,
                        settings = settings,
                        onBack = { navController.popBackStack() },
                        onOpenChapter = { novelUrl, chapterUrl ->
                            navController.navigate(Dest.Reader.build(novelUrl, chapterUrl))
                        },
                    )
                }
                composable(Dest.Reader.route) { entry ->
                    val novelUrl = java.net.URLDecoder.decode(entry.arguments?.getString("novelUrl").orEmpty(), "UTF-8")
                    val chapterUrl = java.net.URLDecoder.decode(entry.arguments?.getString("chapterUrl").orEmpty(), "UTF-8")
                    ReaderScreen(
                        novelUrl = novelUrl,
                        chapterUrl = chapterUrl,
                        settings = settings,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
