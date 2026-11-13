package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = OnSagePrimaryDark,
    primaryContainer = SagePrimaryContainerDark,
    onPrimaryContainer = OnSagePrimaryContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageSecondaryDark,
    secondaryContainer = SageSecondaryContainerDark,
    onSecondaryContainer = OnSageSecondaryContainerDark,
    tertiary = MintTertiaryDark,
    onTertiary = OnMintTertiaryDark,
    tertiaryContainer = MintTertiaryContainerDark,
    onTertiaryContainer = OnMintTertiaryContainerDark,
    background = MintBackgroundDark,
    onBackground = OnMintBackgroundDark,
    surface = MintSurfaceDark,
    onSurface = OnMintSurfaceDark,
    surfaceVariant = MintSurfaceVariantDark,
    onSurfaceVariant = OnMintSurfaceVariantDark,
    surfaceContainerLowest = MintSurfaceContainerLowestDark,
    surfaceContainerLow = MintSurfaceContainerLowDark,
    surfaceContainer = MintSurfaceContainerDark,
    surfaceContainerHigh = MintSurfaceContainerHighDark,
    outline = MintOutlineDark,
    outlineVariant = MintOutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = SagePrimaryLight,
    onPrimary = OnSagePrimaryLight,
    primaryContainer = SagePrimaryContainerLight,
    onPrimaryContainer = OnSagePrimaryContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = OnSageSecondaryLight,
    secondaryContainer = SageSecondaryContainerLight,
    onSecondaryContainer = OnSageSecondaryContainerLight,
    tertiary = MintTertiaryLight,
    onTertiary = OnMintTertiaryLight,
    tertiaryContainer = MintTertiaryContainerLight,
    onTertiaryContainer = OnMintTertiaryContainerLight,
    background = MintBackgroundLight,
    onBackground = OnMintBackgroundLight,
    surface = MintSurfaceLight,
    onSurface = OnMintSurfaceLight,
    surfaceVariant = MintSurfaceVariantLight,
    onSurfaceVariant = OnMintSurfaceVariantLight,
    surfaceContainerLowest = MintSurfaceContainerLowestLight,
    surfaceContainerLow = MintSurfaceContainerLowLight,
    surfaceContainer = MintSurfaceContainerLight,
    surfaceContainerHigh = MintSurfaceContainerHighLight,
    outline = MintOutlineLight,
    outlineVariant = MintOutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun QRcodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
