package com.mo7ammed64.novelnun.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterSearchTest {
    private val chapters = (1..30).map { Chapter("الفصل $it", "chapter/$it", it - 1, it) }

    @Test fun `all digit styles select the same chapter even in reverse order`() {
        listOf("12", "١٢", "۱۲", " ١٢ ").forEach { query ->
            assertEquals(listOf(chapters[11]), ChapterSearch.filter(chapters, query, reverseOrder = true))
            assertEquals(chapters[11], ChapterSearch.requestedChapter(chapters, query))
        }
    }

    @Test fun `clearing the query restores all chapters and never mutates their order`() {
        assertEquals(1, ChapterSearch.filter(chapters, "12").size)
        assertEquals(chapters, ChapterSearch.filter(chapters, ""))
        assertEquals(chapters.reversed(), ChapterSearch.filter(chapters, "", reverseOrder = true))
        assertEquals(1, chapters.first().number)
    }

    @Test fun `title queries containing digits are still text searches`() {
        val list = listOf(Chapter("A journey in 2026", "story", 0, 12))
        assertEquals(list, ChapterSearch.filter(list, "journey in 2026"))
        assertEquals(list.single(), ChapterSearch.requestedChapter(list, "JOURNEY IN 2026"))
    }

    @Test fun `a missing numbered chapter never opens an unrelated list position`() {
        val missing = chapters.filterNot { it.number == 12 }
        assertTrue(ChapterSearch.filter(missing, "12").isEmpty())
        assertNull(ChapterSearch.requestedChapter(missing, "12"))
    }

    @Test fun `unnumbered chapters use chronological positions not display positions`() {
        val unnumbered = listOf(Chapter("Introduction", "intro", 0), Chapter("A new beginning", "start", 1))
        assertEquals(listOf(unnumbered[1]), ChapterSearch.filter(unnumbered, "2", reverseOrder = true))
        assertTrue(ChapterSearch.filter(unnumbered, "0").isEmpty())
    }

    @Test fun `zero can identify a real prologue and invalid or ambiguous requests do not open`() {
        val prologue = Chapter("الفصل ٠", "prologue", 0)
        assertEquals(prologue, ChapterSearch.requestedChapter(listOf(prologue), "0"))
        listOf("", "not found", "99999999999999999999", "الفصل").forEach { query ->
            assertNull(ChapterSearch.requestedChapter(chapters, query))
        }
    }
}
