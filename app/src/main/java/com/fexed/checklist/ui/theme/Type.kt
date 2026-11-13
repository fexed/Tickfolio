package com.fexed.checklist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fexed.checklist.R

private val HandwritingFontFamily = FontFamily(
    Font(R.font.patrick_hand_regular, FontWeight.Normal)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

fun Typography.withHandwritingFont(): Typography {
    return copy(
        displayLarge = displayLarge.copy(fontFamily = HandwritingFontFamily),
        displayMedium = displayMedium.copy(fontFamily = HandwritingFontFamily),
        displaySmall = displaySmall.copy(fontFamily = HandwritingFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = HandwritingFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = HandwritingFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = HandwritingFontFamily),
        titleLarge = titleLarge.copy(fontFamily = HandwritingFontFamily),
        titleMedium = titleMedium.copy(fontFamily = HandwritingFontFamily),
        titleSmall = titleSmall.copy(fontFamily = HandwritingFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = HandwritingFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = HandwritingFontFamily),
        bodySmall = bodySmall.copy(fontFamily = HandwritingFontFamily),
        labelLarge = labelLarge.copy(fontFamily = HandwritingFontFamily),
        labelMedium = labelMedium.copy(fontFamily = HandwritingFontFamily),
        labelSmall = labelSmall.copy(fontFamily = HandwritingFontFamily)
    )
}
