package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CrossMathSemanticColors(
    val valid: Color,
    val validContainer: Color,
    val onValidContainer: Color,
    val invalid: Color,
    val invalidContainer: Color,
    val onInvalidContainer: Color,
    val streak: Color,
    val streakContainer: Color,
    val onStreakContainer: Color,
    val fixedTileBg: Color,
    val editableTileBg: Color,
    val operatorTileBg: Color,
    val selectedTileBg: Color,
    val swapSourceBg: Color,
    val gridLine: Color
)

private val LightSemanticColors = CrossMathSemanticColors(
    valid = EmeraldValid,
    validContainer = EmeraldContainer,
    onValidContainer = EmeraldOnContainer,
    invalid = TerracottaError,
    invalidContainer = TerracottaContainer,
    onInvalidContainer = TerracottaOnContainer,
    streak = BrassAmber,
    streakContainer = BrassContainer,
    onStreakContainer = BrassOnContainer,
    fixedTileBg = Color(0xFFEDE6DA),
    editableTileBg = Color(0xFFFFFFFF),
    operatorTileBg = Color(0xFFF6F1E9),
    selectedTileBg = CobaltContainer,
    swapSourceBg = BrassContainer,
    gridLine = PaperOutline
)

private val DarkSemanticColors = CrossMathSemanticColors(
    valid = MintValid,
    validContainer = MintContainer,
    onValidContainer = MintOnContainer,
    invalid = CoralError,
    invalidContainer = CoralContainer,
    onInvalidContainer = CoralOnContainer,
    streak = GoldAccent,
    streakContainer = GoldContainer,
    onStreakContainer = GoldOnContainer,
    fixedTileBg = Color(0xFF232D3B),
    editableTileBg = Color(0xFF161D26),
    operatorTileBg = Color(0xFF1E2733),
    selectedTileBg = SkyContainer,
    swapSourceBg = GoldContainer,
    gridLine = ObsidianOutline
)

val LocalCrossMathColors = staticCompositionLocalOf { LightSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimary,
    onPrimary = SkyOnPrimary,
    primaryContainer = SkyContainer,
    onPrimaryContainer = SkyOnContainer,
    secondary = GoldAccent,
    onSecondary = Color(0xFF281900),
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldOnContainer,
    tertiary = MintValid,
    background = ObsidianBackground,
    onBackground = ChalkPrimary,
    surface = ObsidianSurface,
    onSurface = ChalkPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ChalkSecondary,
    outline = ObsidianOutline,
    error = CoralError,
    errorContainer = CoralContainer,
    onErrorContainer = CoralOnContainer
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = CobaltOnPrimary,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = CobaltOnContainer,
    secondary = BrassAmber,
    onSecondary = Color.White,
    secondaryContainer = BrassContainer,
    onSecondaryContainer = BrassOnContainer,
    tertiary = EmeraldValid,
    background = PaperBackground,
    onBackground = InkPrimary,
    surface = PaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = PaperOutline,
    error = TerracottaError,
    errorContainer = TerracottaContainer,
    onErrorContainer = TerracottaOnContainer
)

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Warm Paper"),
    DARK("Slate Night")
}

@Composable
fun CrossMathTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalCrossMathColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
