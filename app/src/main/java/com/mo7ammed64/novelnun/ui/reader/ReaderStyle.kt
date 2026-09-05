package com.mo7ammed64.novelnun.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.mo7ammed64.novelnun.ui.settings.ReaderAlignment
import com.mo7ammed64.novelnun.ui.settings.ReaderBackground
import com.mo7ammed64.novelnun.ui.settings.ReaderDirection
import com.mo7ammed64.novelnun.ui.settings.ReaderPreferences

internal data class ReaderPalette(val background: Color, val foreground: Color)

internal val ReaderBackground.palette: ReaderPalette
    get() = when (this) {
        ReaderBackground.DARK -> ReaderPalette(Color(0xFF141317), Color(0xFFE3E2E7))
        ReaderBackground.BLACK -> ReaderPalette(Color(0xFF000000), Color(0xFFDDDDDD))
        ReaderBackground.SEPIA -> ReaderPalette(Color(0xFFF2E6CE), Color(0xFF3E3325))
        ReaderBackground.LIGHT -> ReaderPalette(Color(0xFFFAF9F6), Color(0xFF242426))
        ReaderBackground.SAGE -> ReaderPalette(Color(0xFFE0E8DF), Color(0xFF263C30))
    }

internal val ReaderDirection.layoutDirection: LayoutDirection
    get() = if (this == ReaderDirection.LTR) LayoutDirection.Ltr else LayoutDirection.Rtl

/** Shared by the actual chapter and the live settings preview. */
internal fun ReaderPreferences.textStyle(family: FontFamily): TextStyle = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Normal,
    fontSize = fontSize.sp,
    lineHeight = (fontSize * lineHeight).sp,
    letterSpacing = letterSpacing.sp,
    textDirection = when (direction) {
        ReaderDirection.AUTO -> TextDirection.ContentOrRtl
        ReaderDirection.RTL -> TextDirection.Rtl
        ReaderDirection.LTR -> TextDirection.Ltr
    },
    textAlign = when (alignment) {
        ReaderAlignment.START -> TextAlign.Start
        ReaderAlignment.CENTER -> TextAlign.Center
        ReaderAlignment.JUSTIFY -> TextAlign.Justify
        ReaderAlignment.END -> TextAlign.End
    },
)
