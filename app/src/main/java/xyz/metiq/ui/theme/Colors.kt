package xyz.metiq.ui.theme

import androidx.compose.ui.graphics.Color

// `wave` tints the ripple animation of an active noise color; defaults to the main
// fill at each construction site so it matches unless overridden per variant.
data class NoisePalette(val fill: Color, val onFill: Color, val outline: Color, val wave: Color)

data class MetiqColorTokens(
    val background: Color,
    val foreground: Color,
    val cellBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val subtleFill: Color,
    val sliderActiveFill: Color,
    val scrim: Color,
    val ratingStar: Color,
    val logo: Color,
    val accentHighlight: Color,
    val accentShade: Color,
    val disabledAlpha: Float,
    val accentIconAlpha: Float,
    val waveMaxAlpha: Float,
    val particleBaseAlpha: Float,
    val particleAlphaJitter: Float,
    val noisePink: NoisePalette,
    val noiseBrown: NoisePalette,
    val noiseWhite: NoisePalette,
    val noiseGrey: NoisePalette,
)

object MetiqColors {
    private val PinkFill = Color(0xFFFFC6F2)
    private val BrownFill = Color(0xFFA34E08)
    private val GreyFill = Color(0xFF565656)

    val AmbientSeawaves = Color(0xFF3A7BD5)
    val AmbientRain = Color(0xFF6C5CE7)
    val AmbientFire = Color(0xFFE8662B)
    val AmbientBirds = Color(0xFF4CAF7D)
    val AmbientCafe = Color(0xFFB8862B)
    val AmbientWind = Color(0xFF3AA6B9)

    val Dark = MetiqColorTokens(
        background = Color(0xFF111010),
        foreground = Color(0xFF222121),
        cellBackground = Color(0xFF2E2C2D),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.50f),
        divider = Color.White.copy(alpha = 0.08f),
        subtleFill = Color.White.copy(alpha = 0.12f),
        sliderActiveFill = Color.White.copy(alpha = 0.55f),
        scrim = Color.Black,
        ratingStar = Color(0xFFFFC65A),
        logo = Color(0xFFDBF1B3),
        accentHighlight = Color.White,
        accentShade = Color.Black,
        disabledAlpha = 0.5f,
        accentIconAlpha = 0.7f,
        waveMaxAlpha = 0.9f,
        particleBaseAlpha = 0.2f,
        particleAlphaJitter = 0.4f,
        noisePink = NoisePalette(PinkFill, Color.Black, PinkFill, wave = PinkFill),
        noiseBrown = NoisePalette(BrownFill, Color.White, BrownFill, wave = BrownFill),
        noiseWhite = NoisePalette(Color.White, Color.Black, Color.White, wave = Color.White),
        noiseGrey = NoisePalette(GreyFill, Color.White, GreyFill, wave = GreyFill),
    )

    val Light = MetiqColorTokens(
        background = Color(0xFFE5E7EB),
        foreground = Color(0xFFF5F7FA),
        cellBackground = Color(0xFFECEEF3),
        textPrimary = Color(0xFF111827),
        textSecondary = Color.Black.copy(alpha = 0.50f),
        divider = Color.Black.copy(alpha = 0.08f),
        subtleFill = Color.Black.copy(alpha = 0.12f),
        sliderActiveFill = Color.Black.copy(alpha = 0.55f),
        scrim = Color.Black,
        ratingStar = Color(0xFFFFC65A),
        logo = Color(0xFFADC08B),
        accentHighlight = Color.White,
        accentShade = Color.Black,
        disabledAlpha = 0.5f,
        accentIconAlpha = 0.7f,
        waveMaxAlpha = 0.9f,
        particleBaseAlpha = 0.2f,
        particleAlphaJitter = 0.4f,
        noisePink = NoisePalette(PinkFill, Color.Black, PinkFill, wave = PinkFill),
        noiseBrown = NoisePalette(BrownFill, Color.White, BrownFill, wave = BrownFill),
        noiseWhite = NoisePalette(Color.White, Color.Black, Color.White, wave = Color(0xFFDCDFE7)),
        noiseGrey = NoisePalette(GreyFill, Color.White, GreyFill, wave = GreyFill),
    )
}
