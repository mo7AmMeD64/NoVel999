package com.mo7ammed64.novelnun.ui.settings

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.mo7ammed64.novelnun.R
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppSettingsTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("novelnun_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        context.filesDir.resolve("fonts").listFiles()?.forEach { it.delete() }
    }

    @Test fun `all reader preferences survive recreation independently of app settings`() {
        val settings = AppSettings(context)
        settings.updateFont(builtInFonts[1])
        settings.updateReverseChapterOrder(true)
        val desired = ReaderPreferences(
            fontId = builtInFonts[3].id,
            fontSize = 24f,
            lineHeight = 1.9f,
            paragraphSpacing = 22f,
            letterSpacing = 0.3f,
            horizontalPadding = 32f,
            direction = ReaderDirection.LTR,
            alignment = ReaderAlignment.JUSTIFY,
            background = ReaderBackground.SEPIA,
        )
        settings.updateReaderPreferences { desired }

        val restored = AppSettings(context)
        assertEquals(desired, restored.readerPreferences)
        assertEquals(builtInFonts[1].id, restored.currentFont.id)
        assertEquals(builtInFonts[3].id, restored.currentReaderFont.id)
        assertTrue(restored.reverseChapterOrder)

        restored.resetReaderPreferences()
        assertEquals(ReaderPreferences(), AppSettings(context).readerPreferences)
        assertEquals(builtInFonts[1].id, restored.currentFont.id)
        assertEquals(restored.currentFont.id, restored.currentReaderFont.id)
        assertTrue(restored.reverseChapterOrder)
    }

    @Test fun `unknown enum preferences and missing font files have safe fallbacks`() {
        context.getSharedPreferences("novelnun_preferences", Context.MODE_PRIVATE).edit()
            .putString("reader_direction", "UNKNOWN")
            .putString("reader_alignment", "UNKNOWN")
            .putString("reader_background", "UNKNOWN")
            .putString("reader_font", "imported:missing.ttf")
            .putFloat("reader_font_size", Float.NaN)
            .commit()
        val settings = AppSettings(context)
        assertEquals(ReaderDirection.AUTO, settings.readerPreferences.direction)
        assertEquals(ReaderAlignment.START, settings.readerPreferences.alignment)
        assertEquals(ReaderBackground.DARK, settings.readerPreferences.background)
        assertEquals(20f, settings.readerPreferences.fontSize, 0f)
        assertEquals(settings.currentFont.id, settings.currentReaderFont.id)
    }

    @Test fun `importing a reader font copies it privately without changing the app font`() = runTest {
        val settings = AppSettings(context)
        val source = context.cacheDir.resolve("Reader Font.ttf")
        source.writeBytes(context.resources.openRawResource(R.font.tajawal_regular).use { it.readBytes() })
        assertNull(settings.importFont(context, Uri.fromFile(source), FontTarget.READER))
        val imported = settings.importedFonts.single()
        assertEquals(imported.id, settings.readerPreferences.fontId)
        assertEquals("cairo", settings.selectedFontId)
        assertTrue(imported.file.exists())
        assertEquals(source.length(), imported.file.length())
        source.delete() // The original document is no longer needed, including after restart.

        val restored = AppSettings(context)
        assertEquals(imported.id, restored.currentReaderFont.id)
        restored.updateFont(restored.importedFonts.single())
        assertNull(restored.deleteImportedFont(restored.importedFonts.single()))
        assertFalse(imported.file.exists())
        assertEquals("cairo", restored.selectedFontId)
        assertNull(restored.readerPreferences.fontId)
        assertTrue(AppSettings(context).importedFonts.isEmpty())
    }

    @Test fun `empty and unreadable imports do not change selections or leave partial files`() = runTest {
        val settings = AppSettings(context)
        val empty = context.cacheDir.resolve("empty.ttf").apply { writeBytes(byteArrayOf()) }
        assertNotNull(settings.importFont(context, Uri.fromFile(empty), FontTarget.READER))
        assertNotNull(settings.importFont(context, Uri.fromFile(context.cacheDir.resolve("missing.ttf")), FontTarget.APP))
        assertNull(settings.readerPreferences.fontId)
        assertEquals("cairo", settings.selectedFontId)
        assertTrue(settings.importedFonts.isEmpty())
        assertTrue(context.filesDir.resolve("fonts").listFiles().orEmpty().isEmpty())
    }
}
