package com.mo7ammed64.novelnun.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/** Fonts that are available on Android without downloading anything. */
enum class AppFont(
    val preferenceValue: String,
    val displayName: String,
    val familyName: String,
) {
    SANS_SERIF("sans-serif", "Roboto — افتراضي", "sans-serif"),
    SERIF("serif", "Roboto Serif — كلاسيكي", "roboto-serif"),
    CONDENSED("sans-serif-condensed", "Roboto Condensed — مضغوط", "sans-serif-condensed"),
    MONOSPACE("monospace", "Monospace — ثابت العرض", "monospace"),
    ;

    val fontFamily: FontFamily
        get() = FontFamily(Font(familyName = DeviceFontFamilyName(familyName)))

    companion object {
        fun fromPreference(value: String?): AppFont =
            entries.firstOrNull { it.preferenceValue == value } ?: SERIF
    }
}

/** Small persistent settings store whose properties are also observable by Compose. */
class AppSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    var font: AppFont by mutableStateOf(AppFont.fromPreference(preferences.getString(KEY_FONT, null)))
        private set

    var reverseChapterOrder: Boolean by mutableStateOf(
        preferences.getBoolean(KEY_REVERSE_CHAPTERS, false),
    )
        private set

    fun updateFont(value: AppFont) {
        font = value
        preferences.edit().putString(KEY_FONT, value.preferenceValue).apply()
    }

    fun updateReverseChapterOrder(value: Boolean) {
        reverseChapterOrder = value
        preferences.edit().putBoolean(KEY_REVERSE_CHAPTERS, value).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "novelnun_preferences"
        const val KEY_FONT = "app_font"
        const val KEY_REVERSE_CHAPTERS = "reverse_chapter_order"
    }
}
