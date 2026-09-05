package com.mo7ammed64.novelnun.ui.settings

import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mo7ammed64.novelnun.R
import java.io.File

/**
 * A font the user can pick independently for the app UI or reader. Three flavors:
 *  - [System]   fonts that ship with Android (Roboto family and friends),
 *  - [Bundled]  Arabic fonts packaged inside the app (res/font),
 *  - [Imported] .ttf/.otf files the user imported from device storage.
 */
sealed interface AppFontOption {
    /** Stable value persisted in the settings preferences. */
    val id: String
    val displayName: String

    val fontFamily: FontFamily

    data class System(
        override val id: String,
        override val displayName: String,
        private val familyName: String,
    ) : AppFontOption {
        override val fontFamily: FontFamily
            get() = FontFamily(Font(familyName = DeviceFontFamilyName(familyName)))
    }

    data class Bundled(
        override val id: String,
        override val displayName: String,
        private val regularRes: Int,
        private val boldRes: Int? = null,
    ) : AppFontOption {
        override val fontFamily: FontFamily
            get() = if (boldRes != null) {
                FontFamily(Font(regularRes), Font(boldRes, FontWeight.Bold))
            } else {
                // Variable font: a single file covers the whole weight range; registering it for
                // both Normal and Bold lets Compose apply the matching weight variation.
                FontFamily(Font(regularRes), Font(regularRes, FontWeight.Bold))
            }
    }

    /** A font file the user imported; [file] lives in the app's private fonts directory. */
    data class Imported(
        override val id: String,
        override val displayName: String,
        val file: File,
    ) : AppFontOption {
        override val fontFamily: FontFamily
            get() = FontFamily(Font(file = file))
    }
}

/** Built-in choices: bundled Arabic fonts first, then the platform families. */
val builtInFonts: List<AppFontOption> = listOf(
    AppFontOption.Bundled("cairo", "Cairo — Modern", R.font.cairo),
    AppFontOption.Bundled("tajawal", "Tajawal — Simple and clear", R.font.tajawal_regular, R.font.tajawal_bold),
    AppFontOption.Bundled("almarai", "Almarai — Geometric", R.font.almarai_regular, R.font.almarai_bold),
    AppFontOption.Bundled("amiri", "Amiri — Classic reading", R.font.amiri_regular, R.font.amiri_bold),
    AppFontOption.Bundled("noto_kufi_arabic", "Noto Kufi Arabic — Headlines", R.font.noto_kufi_arabic),
    AppFontOption.Bundled("reem_kufi", "Reem Kufi — Decorative", R.font.reem_kufi),
    AppFontOption.Bundled("lalezar", "Lalezar — Bold", R.font.lalezar),
    AppFontOption.System("sans-serif", "Roboto — System default", "sans-serif"),
    AppFontOption.System("roboto-serif", "Roboto Serif — Classic", "roboto-serif"),
    AppFontOption.System("sans-serif-condensed", "Roboto Condensed — Compact", "sans-serif-condensed"),
    AppFontOption.System("monospace", "Monospace — Fixed width", "monospace"),
)
