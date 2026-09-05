package com.mo7ammed64.novelnun.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterTextTest {
    @Test fun `paragraphs keep line breaks and decode entities`() {
        assertEquals(listOf("أهلاً بالعالم\nA & B", "Next paragraph"), ChapterText.paragraphs(
            "<p>أهلاً بالعالم<br>A &amp; B</p><p> </p><p>Next paragraph</p>",
        ))
    }

    @Test fun `plain text and chapters without p elements are still readable`() {
        val text = ChapterText.paragraphs("<div>First line<br>Second line</div><script>ads()</script>")
        assertTrue(text.joinToString(" ").contains("First line"))
        assertTrue(text.joinToString(" ").contains("Second line"))
        assertFalse(text.joinToString(" ").contains("ads()"))
        assertEquals(listOf("Offline text"), ChapterText.paragraphs("Offline text"))
    }

    @Test fun `empty content is detectable for retry instead of a blank reader`() {
        assertTrue(ChapterText.paragraphs("<p> </p><style>p { color: red; }</style>").isEmpty())
    }
}
