package com.mo7ammed64.novelnun.ui.settings

import androidx.compose.ui.graphics.Color

/** A reader background theme: page color + matching text colors. */
data class ReaderBackgroundOption(
    val id: String,
    val displayName: String,
    val background: Color,
    val text: Color,
    val secondaryText: Color,
)

/** Backgrounds the user can pick for the reading screen. */
val readerBackgrounds: List<ReaderBackgroundOption> = listOf(
    ReaderBackgroundOption(
        id = "default",
        displayName = "Dark",
        background = Color(0xFF141317),
        text = Color(0xFFE3E2E7),
        secondaryText = Color(0xFFC9C5D1),
    ),
    ReaderBackgroundOption(
        id = "black",
        displayName = "AMOLED Black",
        background = Color(0xFF000000),
        text = Color(0xFFDADADA),
        secondaryText = Color(0xFFA8A8A8),
    ),
    ReaderBackgroundOption(
        id = "gray",
        displayName = "Slate",
        background = Color(0xFF23262B),
        text = Color(0xFFE1E4E8),
        secondaryText = Color(0xFFB4BAC2),
    ),
    ReaderBackgroundOption(
        id = "sepia",
        displayName = "Sepia",
        background = Color(0xFFF3E7D3),
        text = Color(0xFF41321E),
        secondaryText = Color(0xFF6E5B41),
    ),
    ReaderBackgroundOption(
        id = "paper",
        displayName = "Paper",
        background = Color(0xFFF6F4EF),
        text = Color(0xFF23211C),
        secondaryText = Color(0xFF5C594F),
    ),
    ReaderBackgroundOption(
        id = "green",
        displayName = "Eye Care",
        background = Color(0xFFDCEBDB),
        text = Color(0xFF1F2B20),
        secondaryText = Color(0xFF4A5C4B),
    ),
    ReaderBackgroundOption(
        id = "navy",
        displayName = "Deep Blue",
        background = Color(0xFF10161F),
        text = Color(0xFFD8E1EC),
        secondaryText = Color(0xFFA3B2C4),
    ),
)

fun readerBackgroundById(id: String): ReaderBackgroundOption =
    readerBackgrounds.firstOrNull { it.id == id } ?: readerBackgrounds.first()
