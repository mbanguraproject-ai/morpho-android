package cc.devbangs.morpho.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val F = FontFamily.SansSerif

val MorphoType = Typography(
    displaySmall = TextStyle(fontFamily = F, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = F, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = F, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = F, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = F, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontFamily = F, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontFamily = F, fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp, lineHeight = 19.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontFamily = F, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
    labelSmall = TextStyle(fontFamily = F, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
)
