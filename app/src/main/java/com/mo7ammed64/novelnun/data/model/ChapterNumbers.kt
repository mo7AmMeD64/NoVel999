package com.mo7ammed64.novelnun.data.model

/**
 * Chapter titles on kolnovel.com mix western digits (`12`), Arabic-Indic digits (`١٢`) and
 * Persian digits (`۱۲`). This helper normalizes all of them and extracts the chapter number.
 */
object ChapterNumbers {

    private val firstNumber = Regex("""\d+""")

    fun parse(text: String): Int? {
        val westernized = text.map { character ->
            when (character) {
                in '٠'..'٩' -> ('0'.code + (character.code - '٠'.code)).toChar()
                in '۰'..'۹' -> ('0'.code + (character.code - '۰'.code)).toChar()
                else -> character
            }
        }.joinToString("")
        return firstNumber.find(westernized)?.value?.toIntOrNull()
    }
}
