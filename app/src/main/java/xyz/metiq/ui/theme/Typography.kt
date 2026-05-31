package xyz.metiq.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import xyz.metiq.R

@OptIn(ExperimentalTextApi::class)
private fun satoshi(weight: Int) = Font(
    resId = R.font.satoshi_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Satoshi = FontFamily(
    satoshi(300),
    satoshi(400),
    satoshi(500),
    satoshi(600),
    satoshi(700),
)

private val Base = Typography()

val MetiqTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Satoshi),
    displayMedium = Base.displayMedium.copy(fontFamily = Satoshi),
    displaySmall = Base.displaySmall.copy(fontFamily = Satoshi),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Satoshi),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Satoshi),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Satoshi),
    titleLarge = Base.titleLarge.copy(fontFamily = Satoshi),
    titleMedium = Base.titleMedium.copy(fontFamily = Satoshi),
    titleSmall = Base.titleSmall.copy(fontFamily = Satoshi),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Satoshi),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Satoshi),
    bodySmall = Base.bodySmall.copy(fontFamily = Satoshi),
    labelLarge = Base.labelLarge.copy(fontFamily = Satoshi),
    labelMedium = Base.labelMedium.copy(fontFamily = Satoshi),
    labelSmall = Base.labelSmall.copy(fontFamily = Satoshi),
)
