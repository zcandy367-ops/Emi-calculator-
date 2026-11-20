package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Immutable
data class EmiExtendedColors(
    val principalColor: Color,
    val interestColor: Color,
    val prepayColor: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroOnSurface: Color,
    val heroMuted: Color,
    val heroPrincipalAccent: Color,
    val heroInterestAccent: Color,
    val heroHighlightText: Color,
    val heroTrackColor: Color,
    val heroPillBg: Color,
    val heroPillBorder: Color,
    val chartGridLine: Color,
    val chartAxisText: Color,
    val chartSelectionBg: Color,
    val cardBorder: Color,
    val positiveBadgeBg: Color,
    val positiveBadgeFg: Color
)

val LocalEmiColors = staticCompositionLocalOf {
    EmiExtendedColors(
        principalColor = PrincipalEmeraldLight,
        interestColor = InterestAmberLight,
        prepayColor = PrepayTealLight,
        heroStart = HeroCardStartLight,
        heroEnd = HeroCardEndLight,
        heroOnSurface = Color.White,
        heroMuted = Color(0xFFBBE7D8),
        heroPrincipalAccent = Color(0xFF4ADE80),
        heroInterestAccent = Color(0xFFFBBF24),
        heroHighlightText = Color(0xFFFDE68A),
        heroTrackColor = Color.White.copy(alpha = 0.16f),
        heroPillBg = Color.White.copy(alpha = 0.15f),
        heroPillBorder = Color.White.copy(alpha = 0.25f),
        chartGridLine = OutlineLight,
        chartAxisText = OnSurfaceVariantLight,
        chartSelectionBg = PrincipalEmeraldLight.copy(alpha = 0.10f),
        cardBorder = OutlineLight,
        positiveBadgeBg = EmeraldContainerLight,
        positiveBadgeFg = EmeraldOnContainerLight
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateContainerLight,
    onSecondaryContainer = SlateOnContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = AmberOnTertiaryLight,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = AmberOnContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = SlateOnContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberOnContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
private fun animateThemeColor(target: Color, label: String): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 200),
        label = label
    )
    return color
}

@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme = target.copy(
    primary = animateThemeColor(target.primary, "primary"),
    onPrimary = animateThemeColor(target.onPrimary, "onPrimary"),
    primaryContainer = animateThemeColor(target.primaryContainer, "primaryContainer"),
    onPrimaryContainer = animateThemeColor(target.onPrimaryContainer, "onPrimaryContainer"),
    secondary = animateThemeColor(target.secondary, "secondary"),
    secondaryContainer = animateThemeColor(target.secondaryContainer, "secondaryContainer"),
    tertiary = animateThemeColor(target.tertiary, "tertiary"),
    tertiaryContainer = animateThemeColor(target.tertiaryContainer, "tertiaryContainer"),
    background = animateThemeColor(target.background, "background"),
    onBackground = animateThemeColor(target.onBackground, "onBackground"),
    surface = animateThemeColor(target.surface, "surface"),
    onSurface = animateThemeColor(target.onSurface, "onSurface"),
    surfaceVariant = animateThemeColor(target.surfaceVariant, "surfaceVariant"),
    onSurfaceVariant = animateThemeColor(target.onSurfaceVariant, "onSurfaceVariant"),
    surfaceContainer = animateThemeColor(target.surfaceContainer, "surfaceContainer"),
    outline = animateThemeColor(target.outline, "outline")
)

@Composable
fun EmiPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val targetScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = animatedColorScheme(targetScheme)

    val extendedColors = if (darkTheme) {
        EmiExtendedColors(
            principalColor = animateThemeColor(PrincipalEmeraldDark, "extPrincipal"),
            interestColor = animateThemeColor(InterestAmberDark, "extInterest"),
            prepayColor = animateThemeColor(PrepayTealDark, "extPrepay"),
            heroStart = animateThemeColor(HeroCardStartDark, "heroStart"),
            heroEnd = animateThemeColor(HeroCardEndDark, "heroEnd"),
            heroOnSurface = Color(0xFFF1FAF6),
            heroMuted = Color(0xFF9FD6C2),
            heroPrincipalAccent = Color(0xFF4ADE80),
            heroInterestAccent = Color(0xFFFBBF24),
            heroHighlightText = Color(0xFFFDE68A),
            heroTrackColor = Color.White.copy(alpha = 0.14f),
            heroPillBg = Color.White.copy(alpha = 0.13f),
            heroPillBorder = Color.White.copy(alpha = 0.24f),
            chartGridLine = animateThemeColor(OutlineDark, "chartGrid"),
            chartAxisText = animateThemeColor(OnSurfaceVariantDark, "chartAxis"),
            chartSelectionBg = PrincipalEmeraldDark.copy(alpha = 0.14f),
            cardBorder = animateThemeColor(OutlineDark, "cardBorder"),
            positiveBadgeBg = animateThemeColor(EmeraldContainerDark, "posBg"),
            positiveBadgeFg = animateThemeColor(EmeraldOnContainerDark, "posFg")
        )
    } else {
        EmiExtendedColors(
            principalColor = animateThemeColor(PrincipalEmeraldLight, "extPrincipal"),
            interestColor = animateThemeColor(InterestAmberLight, "extInterest"),
            prepayColor = animateThemeColor(PrepayTealLight, "extPrepay"),
            heroStart = animateThemeColor(HeroCardStartLight, "heroStart"),
            heroEnd = animateThemeColor(HeroCardEndLight, "heroEnd"),
            heroOnSurface = Color.White,
            heroMuted = Color(0xFFC2EFE0),
            heroPrincipalAccent = Color(0xFF4ADE80),
            heroInterestAccent = Color(0xFFFBBF24),
            heroHighlightText = Color(0xFFFDE68A),
            heroTrackColor = Color.White.copy(alpha = 0.16f),
            heroPillBg = Color.White.copy(alpha = 0.16f),
            heroPillBorder = Color.White.copy(alpha = 0.26f),
            chartGridLine = animateThemeColor(OutlineLight, "chartGrid"),
            chartAxisText = animateThemeColor(OnSurfaceVariantLight, "chartAxis"),
            chartSelectionBg = PrincipalEmeraldLight.copy(alpha = 0.10f),
            cardBorder = animateThemeColor(OutlineLight, "cardBorder"),
            positiveBadgeBg = animateThemeColor(EmeraldContainerLight, "posBg"),
            positiveBadgeFg = animateThemeColor(EmeraldOnContainerLight, "posFg")
        )
    }

    CompositionLocalProvider(LocalEmiColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

fun Modifier.heroAtmosphere(
    startColor: Color,
    endColor: Color
): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
    val glowCenter = Offset(size.width * 0.88f, size.height * 0.14f)
    val glowRadius = size.maxDimension * 0.65f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
            center = glowCenter,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = glowCenter
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.06f),
        radius = size.minDimension * 0.48f,
        center = Offset(size.width * 0.92f, size.height * 0.12f),
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.04f),
        radius = size.minDimension * 0.72f,
        center = Offset(size.width * 0.92f, size.height * 0.12f),
        style = Stroke(width = 1.dp.toPx())
    )
}
