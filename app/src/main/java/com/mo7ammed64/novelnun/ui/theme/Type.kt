package com.mo7ammed64.novelnun.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The font family selected in Settings is supplied to NovelNunTypography at the app root so every
// screen, including navigation labels, uses the same family.
fun NovelNunTypography(fontFamily: FontFamily = FontFamily.SansSerif) = Typography(
    displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp),
    headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
