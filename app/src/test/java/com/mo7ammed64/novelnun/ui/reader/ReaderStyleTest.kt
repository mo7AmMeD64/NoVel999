package com.mo7ammed64.novelnun.ui.reader

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.mo7ammed64.novelnun.ui.settings.ReaderAlignment
import com.mo7ammed64.novelnun.ui.settings.ReaderBackground
import com.mo7ammed64.novelnun.ui.settings.ReaderDirection
import com.mo7ammed64.novelnun.ui.settings.ReaderPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderStyleTest {
    @Test fun `direction alignment and spacing are applied to chapter text`() {
        val preferences = ReaderPreferences(fontSize = 24f, lineHeight = 1.5f, letterSpacing = 0.5f,
            direction = ReaderDirection.LTR, alignment = ReaderAlignment.JUSTIFY)
        val style = preferences.textStyle(FontFamily.Serif)
        assertEquals(24.sp, style.fontSize)
        assertEquals(36.sp, style.lineHeight)
        assertEquals(0.5.sp, style.letterSpacing)
        assertEquals(TextDirection.Ltr, style.textDirection)
        assertEquals(TextAlign.Justify, style.textAlign)
        assertEquals(FontFamily.Serif, style.fontFamily)
        assertEquals(TextDirection.Rtl, preferences.copy(direction = ReaderDirection.RTL).textStyle(FontFamily.Default).textDirection)
        assertEquals(TextDirection.ContentOrRtl, ReaderPreferences().textStyle(FontFamily.Default).textDirection)
    }

    @Test fun `every reader background has accessible text contrast`() {
        ReaderBackground.entries.forEach { theme ->
            val foreground = theme.palette.foreground.luminance()
            val background = theme.palette.background.luminance()
            val contrast = (maxOf(foreground, background) + 0.05f) / (minOf(foreground, background) + 0.05f)
            assertTrue("${theme.label} contrast is $contrast", contrast >= 4.5f)
        }
    }
}
