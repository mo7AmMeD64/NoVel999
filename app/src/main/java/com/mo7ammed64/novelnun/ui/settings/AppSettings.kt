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

    // Reader preferences -----------------------------------------------------

    var readerFontId: String by mutableStateOf(
        preferences.getString(KEY_READER_FONT, READER_FONT_APP) ?: READER_FONT_APP,
    )
        private set

    var readerFontSize: Float by mutableStateOf(
        preferences.getFloat(KEY_READER_FONT_SIZE, 18f),
    )
        private set

    var readerLineSpacing: Float by mutableStateOf(
        preferences.getFloat(KEY_READER_LINE_SPACING, 1.7f),
    )
        private set

    var readerTextAlign: String by mutableStateOf(
        preferences.getString(KEY_READER_ALIGN, ALIGN_RIGHT) ?: ALIGN_RIGHT,
    )
        private set

    var readerBackground: String by mutableStateOf(
        preferences.getString(KEY_READER_BACKGROUND, DEFAULT_READER_BACKGROUND) ?: DEFAULT_READER_BACKGROUND,
    )
        private set

    /** The reader either follows the app font or uses its own dedicated pick. */
    val readerFontFamily: FontFamily
        get() = if (readerFontId == READER_FONT_APP) {
            fontFamily
        } else {
            availableFonts.firstOrNull { it.id == readerFontId }?.fontFamily ?: fontFamily
        }

    fun updateFont(value: AppFontOption) {
        selectedFontId = value.id
        preferences.edit().putString(KEY_FONT, value.id).apply()
    }

    fun updateReaderFont(id: String) {
        readerFontId = id
        preferences.edit().putString(KEY_READER_FONT, id).apply()
    }

    fun updateReaderFontSize(value: Float) {
        readerFontSize = value
        preferences.edit().putFloat(KEY_READER_FONT_SIZE, value).apply()
    }

    fun updateReaderLineSpacing(value: Float) {
        readerLineSpacing = value
        preferences.edit().putFloat(KEY_READER_LINE_SPACING, value).apply()
    }

    fun updateReaderTextAlign(value: String) {
        readerTextAlign = value
        preferences.edit().putString(KEY_READER_ALIGN, value).apply()
    }

    fun updateReaderBackground(value: String) {
        readerBackground = value
        preferences.edit().putString(KEY_READER_BACKGROUND, value).apply()
    }

    /**
     * Copies a font picked with the system file picker into the app's private fonts directory,
     * validates that Android can actually load it, and selects it as the app-wide font.
     * Returns null on success or a human-readable error message.
     */
    fun importFont(context: Context, uri: Uri): String? =
        importFontFile(context, uri).fold(
            onSuccess = { imported ->
                updateFont(imported)
                null
            },
            onFailure = { it.message ?: "Couldn't import the font" },
        )

    /** Imports a font file and selects it for the reader only (the app font is unchanged). */
    fun importReaderFont(context: Context, uri: Uri): String? =
        importFontFile(context, uri).fold(
            onSuccess = { imported ->
                updateReaderFont(imported.id)
                null
            },
            onFailure = { it.message ?: "Couldn't import the font" },
        )

    private fun importFontFile(context: Context, uri: Uri): Result<AppFontOption.Imported> {
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
            } ?: return Result.failure(IllegalStateException("Couldn't read the file"))

            // Reject files Android can't load as fonts (corrupt download, wrong file type...).
            if (Typeface.createFromFile(target) == null) {
                target.delete()
                return Result.failure(IllegalStateException("Not a valid font file"))
            }

            importedFonts = listImportedFonts()
            Result.success(importedFonts.last { it.file == target })
        } catch (e: Exception) {
            target.delete()
            Result.failure(IllegalStateException("Couldn't import the font: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    fun deleteImportedFont(option: AppFontOption.Imported) {
        option.file.delete()
        importedFonts = listImportedFonts()
        if (selectedFontId == option.id) {
            selectedFontId = DEFAULT_FONT_ID
            preferences.edit().putString(KEY_FONT, DEFAULT_FONT_ID).apply()
        }
        if (readerFontId == option.id) {
            updateReaderFont(READER_FONT_APP)
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

    companion object {
        private const val PREFERENCES_NAME = "novelnun_preferences"
        private const val KEY_FONT = "app_font"
        private const val KEY_REVERSE_CHAPTERS = "reverse_chapter_order"
        private const val KEY_READER_FONT = "reader_font"
        private const val KEY_READER_FONT_SIZE = "reader_font_size"
        private const val KEY_READER_LINE_SPACING = "reader_line_spacing"
        private const val KEY_READER_ALIGN = "reader_text_align"
        private const val KEY_READER_BACKGROUND = "reader_background"
        private const val DEFAULT_FONT_ID = "cairo"
        private const val IMPORTED_ID_PREFIX = "imported:"
        private const val DEFAULT_READER_BACKGROUND = "default"

        /** Sentinel meaning “the reader follows the app font”. */
        const val READER_FONT_APP = "app"

        const val ALIGN_RIGHT = "right"
        const val ALIGN_LEFT = "left"
        const val ALIGN_CENTER = "center"
        const val ALIGN_JUSTIFY = "justify"
    }
}
