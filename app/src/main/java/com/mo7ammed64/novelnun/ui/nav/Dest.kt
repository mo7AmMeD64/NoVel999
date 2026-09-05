package com.mo7ammed64.novelnun.ui.nav

sealed class Dest(val route: String) {
    data object Home : Dest("home")
    data object Search : Dest("search")
    data object Saved : Dest("saved")
    data object Settings : Dest("settings")
    data object History : Dest("history")
    data object Files : Dest("files")
    data object Details : Dest("details/{url}") {
        fun build(url: String) = "details/${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
    data object Reader : Dest("reader/{novelUrl}/{chapterUrl}") {
        fun build(novelUrl: String, chapterUrl: String) =
            "reader/${java.net.URLEncoder.encode(novelUrl, "UTF-8")}/${java.net.URLEncoder.encode(chapterUrl, "UTF-8")}"
    }

    companion object {
        val railDestinations = listOf(Home, Search, Saved, Settings)
    }
}
