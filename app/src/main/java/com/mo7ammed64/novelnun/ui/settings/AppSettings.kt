package com.mo7ammed64.novelnun.ui.settings

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/**
 * Small persistent settings store whose properties are also observable by Compose, so picking a
 * font or flipping the chapter order instantly re-renders the app.
 */
class AppSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    private val fontsDir = File(context.applicationContext.filesDir, "fonts").apply { mkdirs() }

    var selectedFontId: String by mutableStateOf(
        preferences.getString(KEY_FONT, null) ?: DEFAULT_FONT_ID,
    )
        private set

    var importedFonts: List<AppFontOption.Imported> by mutableStateOf(listImportedFonts())
        private set

    /** Bundled + platform + user-imported choices, in display order. */
    val availableFonts: List<AppFontOption>
        get() = builtInFonts + importedFonts

    val currentFont: AppFontOption
        get() = availableFonts.firstOrNull { it.id == selectedFontId } ?: builtInFonts.first()

    val fontFamily: FontFamily
        get() = currentFont.fontFamily

    var reverseChapterOrder: Boolean by mutableStateOf(
        preferences.getBoolean(KEY_REVERSE_CHAPTERS, false),
    )
        private set

    fun updateFont(value: AppFontOption) {
        selectedFontId = value.id
        preferences.edit().putString(KEY_FONT, value.id).apply()
    }

    /**
     * Copies a font picked with the system file picker into the app's private fonts directory,
     * validates that Android can actually load it, and selects it. Returns null on success or a
     * human-readable error message.
     */
    fun importFont(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(context, uri)?.substringAfterLast('/')
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "font"

        var fileName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "font" }
        if (!fileName.endsWith(".ttf", ignoreCase = true) && !fileName.endsWith(".otf", ignoreCase = true)) {
            fileName += ".ttf"
        }
        val target = uniqueFile(fileName)

        return try {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return "تعذر قراءة الملف"

            // Reject files Android can't load as fonts (corrupt download, wrong file type...).
            if (Typeface.createFromFile(target) == null) {
                target.delete()
                return "الملف ليس ملف خط صالح"
            }

            importedFonts = listImportedFonts()
            updateFont(importedFonts.last { it.file == target })
            null
        } catch (e: Exception) {
            target.delete()
            "تعذر استيراد الخط: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun deleteImportedFont(option: AppFontOption.Imported) {
        option.file.delete()
        importedFonts = listImportedFonts()
        if (selectedFontId == option.id) {
            selectedFontId = DEFAULT_FONT_ID
            preferences.edit().putString(KEY_FONT, DEFAULT_FONT_ID).apply()
        }
    }

    fun updateReverseChapterOrder(value: Boolean) {
        reverseChapterOrder = value
        preferences.edit().putBoolean(KEY_REVERSE_CHAPTERS, value).apply()
    }

    private fun listImportedFonts(): List<AppFontOption.Imported> =
        fontsDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in setOf("ttf", "otf") }
            ?.sortedBy { it.name }
            ?.map { file ->
                AppFontOption.Imported(
                    id = "${IMPORTED_ID_PREFIX}${file.name}",
                    displayName = file.nameWithoutExtension,
                    file = file,
                )
            }
            .orEmpty()

    private fun uniqueFile(fileName: String): File {
        var candidate = File(fontsDir, fileName)
        if (!candidate.exists()) return candidate
        val base = candidate.nameWithoutExtension
        val ext = candidate.extension
        var counter = 1
        do {
            candidate = File(fontsDir, "${base}_$counter.$ext")
            counter++
        } while (candidate.exists())
        return candidate
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
    }.getOrNull()

    private companion object {
        const val PREFERENCES_NAME = "novelnun_preferences"
        const val KEY_FONT = "app_font"
        const val KEY_REVERSE_CHAPTERS = "reverse_chapter_order"
        const val DEFAULT_FONT_ID = "cairo"
        const val IMPORTED_ID_PREFIX = "imported:"
    }
}
