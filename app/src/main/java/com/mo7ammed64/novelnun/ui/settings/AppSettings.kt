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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Persistent settings observed by Compose. Reader typography never changes the app UI font. */
class AppSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val fontsDir = File(context.applicationContext.filesDir, "fonts").apply { mkdirs() }

    var selectedFontId: String by mutableStateOf(preferences.getString(KEY_FONT, null) ?: DEFAULT_FONT_ID)
        private set

    var importedFonts: List<AppFontOption.Imported> by mutableStateOf(listImportedFonts())
        private set

    val availableFonts: List<AppFontOption>
        get() = builtInFonts + importedFonts

    val currentFont: AppFontOption
        get() = availableFonts.firstOrNull { it.id == selectedFontId } ?: builtInFonts.first()

    val fontFamily: FontFamily
        get() = currentFont.fontFamily

    var reverseChapterOrder: Boolean by mutableStateOf(preferences.getBoolean(KEY_REVERSE_CHAPTERS, false))
        private set

    var readerPreferences: ReaderPreferences by mutableStateOf(readReaderPreferences())
        private set

    val currentReaderFont: AppFontOption
        get() = availableFonts.firstOrNull { it.id == readerPreferences.fontId } ?: currentFont

    fun updateFont(value: AppFontOption) {
        selectedFontId = value.id
        preferences.edit().putString(KEY_FONT, value.id).apply()
    }

    fun updateReaderFont(value: AppFontOption?) {
        updateReaderPreferences { it.copy(fontId = value?.id) }
    }

    fun updateReaderPreferences(update: (ReaderPreferences) -> ReaderPreferences) {
        val value = update(readerPreferences).sanitized()
        readerPreferences = value
        preferences.edit()
            .putString("reader_font", value.fontId)
            .putFloat("reader_font_size", value.fontSize)
            .putFloat("reader_line_height", value.lineHeight)
            .putFloat("reader_paragraph_spacing", value.paragraphSpacing)
            .putFloat("reader_letter_spacing", value.letterSpacing)
            .putFloat("reader_horizontal_padding", value.horizontalPadding)
            .putString("reader_direction", value.direction.name)
            .putString("reader_alignment", value.alignment.name)
            .putString("reader_background", value.background.name)
            .apply()
    }

    fun resetReaderPreferences() = updateReaderPreferences { ReaderPreferences() }

    private fun readReaderPreferences(): ReaderPreferences = ReaderPreferences(
        fontId = preferences.getString("reader_font", null),
        fontSize = preferences.getFloat("reader_font_size", 20f),
        lineHeight = preferences.getFloat("reader_line_height", 1.6f),
        paragraphSpacing = preferences.getFloat("reader_paragraph_spacing", 16f),
        letterSpacing = preferences.getFloat("reader_letter_spacing", 0f),
        horizontalPadding = preferences.getFloat("reader_horizontal_padding", 20f),
        direction = enumPreference("reader_direction", ReaderDirection.AUTO),
        alignment = enumPreference("reader_alignment", ReaderAlignment.START),
        background = enumPreference("reader_background", ReaderBackground.DARK),
    ).sanitized()

    private inline fun <reified T : Enum<T>> enumPreference(key: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == preferences.getString(key, null) } ?: default

    /**
     * The system document picker grants temporary read access. Copy and validate on IO so the
     * font remains available after a restart, without storage permissions or UI-thread stalls.
     * Returns an English error for both font pickers, or null on success.
     */
    suspend fun importFont(context: Context, uri: Uri, target: FontTarget = FontTarget.APP): String? {
        var copiedFile: File? = null
        val result = try {
            withContext(Dispatchers.IO) {
                val displayName = queryDisplayName(context, uri)?.substringAfterLast('/')
                    ?: uri.lastPathSegment?.substringAfterLast('/') ?: "font.ttf"
                val extension = displayName.substringAfterLast('.', "ttf").lowercase()
                    .takeIf { it == "ttf" || it == "otf" } ?: "ttf"
                val base = displayName.substringBeforeLast('.', displayName)
                    .replace(Regex("[^A-Za-z0-9_-]"), "_")
                    .replace(Regex("_+"), "_").trim('_').take(100).ifBlank { "font" }
                val file = uniqueFile("$base.$extension")
                copiedFile = file

                val input = context.contentResolver.openInputStream(uri)
                    ?: throw FontImportException("Could not read the selected file.")
                input.use {
                    file.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_FONT_BYTES) throw FontImportException("Font files must be 20 MB or smaller.")
                            output.write(buffer, 0, count)
                        }
                    }
                }
                if (file.length() == 0L || Typeface.createFromFile(file) == null) {
                    throw FontImportException("The selected file is not a valid font.")
                }
                AppFontOption.Imported("$IMPORTED_ID_PREFIX${file.name}", file.nameWithoutExtension, file)
            }
        } catch (cancelled: CancellationException) {
            copiedFile?.delete()
            throw cancelled
        } catch (error: Exception) {
            copiedFile?.delete()
            return if (error is FontImportException) error.message
            else "Could not import this font. Choose a valid .ttf or .otf file."
        }

        importedFonts = (importedFonts + result).sortedBy { it.file.name }
        when (target) {
            FontTarget.APP -> updateFont(result)
            FontTarget.READER -> updateReaderFont(result)
        }
        return null
    }

    fun deleteImportedFont(option: AppFontOption.Imported): String? {
        if (option.file.exists() && !option.file.delete()) return "Could not delete this font. Please try again."
        importedFonts = importedFonts.filterNot { it.id == option.id }
        if (selectedFontId == option.id) {
            selectedFontId = DEFAULT_FONT_ID
            preferences.edit().putString(KEY_FONT, DEFAULT_FONT_ID).apply()
        }
        if (readerPreferences.fontId == option.id) updateReaderFont(null)
        return null
    }

    fun updateReverseChapterOrder(value: Boolean) {
        reverseChapterOrder = value
        preferences.edit().putBoolean(KEY_REVERSE_CHAPTERS, value).apply()
    }

    private fun listImportedFonts(): List<AppFontOption.Imported> = fontsDir.listFiles()
        ?.filter { it.isFile && it.length() > 0 && it.extension.lowercase() in setOf("ttf", "otf") }
        ?.sortedBy { it.name }
        ?.map { file -> AppFontOption.Imported("$IMPORTED_ID_PREFIX${file.name}", file.nameWithoutExtension, file) }
        .orEmpty()

    private fun uniqueFile(fileName: String): File {
        var candidate = File(fontsDir, fileName)
        val base = candidate.nameWithoutExtension
        val extension = candidate.extension
        var counter = 1
        while (candidate.exists()) candidate = File(fontsDir, "${base}_${counter++}.$extension")
        return candidate
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }.getOrNull()

    private class FontImportException(message: String) : Exception(message)

    private companion object {
        const val PREFERENCES_NAME = "novelnun_preferences"
        const val KEY_FONT = "app_font"
        const val KEY_REVERSE_CHAPTERS = "reverse_chapter_order"
        const val DEFAULT_FONT_ID = "cairo"
        const val IMPORTED_ID_PREFIX = "imported:"
        const val MAX_FONT_BYTES = 20L * 1024 * 1024
    }
}
