package com.mo7ammed64.novelnun.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderPreferencesTest {
    @Test fun `defaults are valid and resettable`() {
        assertEquals(ReaderPreferences(), ReaderPreferences().sanitized())
    }

    @Test fun `values outside the supported slider ranges are clamped`() {
        val preferences = ReaderPreferences(
            fontSize = 100f, lineHeight = 0f, paragraphSpacing = -10f,
            letterSpacing = 20f, horizontalPadding = 1000f,
        ).sanitized()
        assertEquals(34f, preferences.fontSize, 0f)
        assertEquals(1.2f, preferences.lineHeight, 0f)
        assertEquals(0f, preferences.paragraphSpacing, 0f)
        assertEquals(2f, preferences.letterSpacing, 0f)
        assertEquals(48f, preferences.horizontalPadding, 0f)
    }

    @Test fun `invalid floating point values fall back to readable defaults`() {
        val preferences = ReaderPreferences(
            fontSize = Float.NaN, lineHeight = Float.POSITIVE_INFINITY,
            paragraphSpacing = Float.NEGATIVE_INFINITY, letterSpacing = Float.NaN,
            horizontalPadding = Float.NaN,
        ).sanitized()
        assertEquals(ReaderPreferences(), preferences)
    }
}
