package xyz.metiq.ui.theme

import androidx.compose.ui.graphics.Color

data class NoisePalette(val fill: Color, val onFill: Color, val outline: Color)

data class MetiqColorTokens(
    val background: Color,
    val foreground: Color,
    val cellBackground: Color,
    val textPrimary: Color,
    val divider: Color,
    val noisePink: NoisePalette,
    val noiseBrown: NoisePalette,
    val noiseWhite: NoisePalette,
    val noiseGrey: NoisePalette,
)

object MetiqColors {
    val DisabledAlpha = 0.32f

    private val PinkFill = Color(0xFFFFC6F2)
    private val BrownFill = Color(0xFFA34E08)
    private val GreyFill = Color(0xFF565656)

    val Dark = MetiqColorTokens(
        background = Color(0xFF111010),
        foreground = Color(0xFF222121),
        cellBackground = Color(0xFF2E2C2D),
        textPrimary = Color.White,
        divider = Color.White.copy(alpha = 0.08f),
        noisePink = NoisePalette(PinkFill, Color.Black, PinkFill),
        noiseBrown = NoisePalette(BrownFill, Color.White, BrownFill),
        noiseWhite = NoisePalette(Color.White, Color.Black, Color.White),
        noiseGrey = NoisePalette(GreyFill, Color.White, GreyFill),
    )
}
