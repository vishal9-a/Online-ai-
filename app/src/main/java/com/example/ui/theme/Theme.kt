package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PromptToonDarkColorScheme = darkColorScheme(
    primary = ElectricCoral,
    onPrimary = StudioObsidian,
    primaryContainer = ElectricCoralDark,
    onPrimaryContainer = ColorTokens.CoralLight,
    secondary = CyberMint,
    onSecondary = StudioObsidian,
    secondaryContainer = CyberMintDark,
    onSecondaryContainer = CyberMint,
    tertiary = StudioGold,
    onTertiary = StudioObsidian,
    tertiaryContainer = StudioGoldDark,
    onTertiaryContainer = StudioGold,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSlate,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = StudioObsidian,
    surfaceContainerLow = StudioSlate,
    surfaceContainer = StudioCard,
    surfaceContainerHigh = StudioCardElevated,
    outline = StudioOutline,
    outlineVariant = ColorTokens.SoftOutline,
    error = FriendlyRose,
    onError = StudioObsidian,
    errorContainer = FriendlyRoseDark,
    onErrorContainer = ColorTokens.RoseLight
)

private object ColorTokens {
    val CoralLight = androidx.compose.ui.graphics.Color(0xFFFFB4A3)
    val RoseLight = androidx.compose.ui.graphics.Color(0xFFFFB5C2)
    val SoftOutline = androidx.compose.ui.graphics.Color(0xFF23293B)
}

val PromptToonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PromptToonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PromptToonDarkColorScheme,
        typography = PromptToonTypography,
        shapes = PromptToonShapes,
        content = content
    )
}
