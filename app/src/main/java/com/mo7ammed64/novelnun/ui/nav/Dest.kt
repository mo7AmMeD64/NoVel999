package com.mo7ammed64.novelnun.ui.nav

sealed class Dest(val route: String) {
    data object Home : Dest("home")
    data object Search : Dest("search")
    data object Saved : Dest("saved")
    data object Settings : Dest("settings")
    data object History : Dest("history")
    data object Files : Dest("files")
    data object Latest : Dest("latest")
    data object Details : Dest("details/{url}") {
        fun build(url: String) = "details/${java.net.URLEncoder.encode(url, "UTF-8")}"
    }
    data object Reader : Dest("reader/{novelUrl}/{chapterUrl}") {
        fun build(novelUrl: String, chapterUrl: String) =
            "reader/${java.net.URLEncoder.encode(novelUrl, "UTF-8")}/${java.net.URLEncoder.encode(chapterUrl, "UTF-8")}"
    }

    companion object {
        // A destination object can be accessed before Dest's companion. Constructing this list
        // eagerly during superclass initialization can then capture a null object INSTANCE.
        val railDestinations: List<Dest> by lazy { listOf(Home, Search, Saved, Settings) }
    }
}
