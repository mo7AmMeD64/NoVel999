package com.mo7ammed64.novelnun.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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

@Composable
fun NovelNunApp(
    settings: AppSettings,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showRail = Dest.railDestinations.any { it.route == currentRoute }
    val forwardSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showRail,
                enter = expandHorizontally(spring(dampingRatio = 1f, stiffness = 450f), expandFrom = Alignment.Start),
                exit = shrinkHorizontally(spring(dampingRatio = 1f, stiffness = 450f), shrinkTowards = Alignment.Start),
            ) {
                NovelNunNavRail(currentRoute = currentRoute) { dest ->
                    if (currentRoute != dest.route) navController.navigate(dest.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            NavHost(
                navController = navController,
                startDestination = Dest.Home.route,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                enterTransition = { pageEnter(popping = false, forwardSign = forwardSign) },
                exitTransition = { pageExit(popping = false, forwardSign = forwardSign) },
                popEnterTransition = { pageEnter(popping = true, forwardSign = forwardSign) },
                popExitTransition = { pageExit(popping = true, forwardSign = forwardSign) },
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
