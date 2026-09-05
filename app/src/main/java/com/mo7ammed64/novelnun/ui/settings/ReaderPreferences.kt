package com.mo7ammed64.novelnun.ui.settings

/** Reader-only preferences. A null font ID follows the app font without changing it. */
data class ReaderPreferences(
    val fontId: String? = null,
    val fontSize: Float = 20f,
    val lineHeight: Float = 1.6f,
    val paragraphSpacing: Float = 16f,
    val letterSpacing: Float = 0f,
    val horizontalPadding: Float = 20f,
    val direction: ReaderDirection = ReaderDirection.AUTO,
    val alignment: ReaderAlignment = ReaderAlignment.START,
    val background: ReaderBackground = ReaderBackground.DARK,
) {
    fun sanitized(): ReaderPreferences = copy(
        fontSize = fontSize.bounded(FONT_SIZE_RANGE, 20f),
        lineHeight = lineHeight.bounded(LINE_HEIGHT_RANGE, 1.6f),
        paragraphSpacing = paragraphSpacing.bounded(PARAGRAPH_SPACING_RANGE, 16f),
        letterSpacing = letterSpacing.bounded(LETTER_SPACING_RANGE, 0f),
        horizontalPadding = horizontalPadding.bounded(PADDING_RANGE, 20f),
    )

    companion object {
        val FONT_SIZE_RANGE = 14f..34f
        val LINE_HEIGHT_RANGE = 1.2f..2.4f
        val PARAGRAPH_SPACING_RANGE = 0f..40f
        val LETTER_SPACING_RANGE = 0f..2f
        val PADDING_RANGE = 8f..48f
    }
}

private fun Float.bounded(range: ClosedFloatingPointRange<Float>, default: Float): Float =
    if (isFinite()) coerceIn(range) else default

enum class ReaderDirection(val label: String) {
    AUTO("Auto"), RTL("Right to left"), LTR("Left to right"),
}

enum class ReaderAlignment(val label: String) {
    START("Start"), CENTER("Center"), JUSTIFY("Justify"), END("End"),
}

enum class ReaderBackground(val label: String) {
    DARK("Dark"), BLACK("Black"), SEPIA("Sepia"), LIGHT("Light"), SAGE("Sage"),
}

enum class FontTarget { APP, READER }
