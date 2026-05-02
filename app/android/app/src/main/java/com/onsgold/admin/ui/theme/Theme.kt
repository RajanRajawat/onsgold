package com.onsgold.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GoldPrimary,
    onPrimary = GoldOnPrimary,
    primaryContainer = GoldPrimaryContainer,
    onPrimaryContainer = GoldOnPrimaryContainer,
    secondary = GoldSecondary,
    onSecondary = GoldOnSecondary,
    secondaryContainer = GoldSecondaryContainer,
    onSecondaryContainer = GoldOnSecondaryContainer,
    background = GoldBackground,
    onBackground = GoldOnBackground,
    surface = GoldSurface,
    onSurface = GoldOnSurface,
    error = GoldError,
    onError = GoldOnError,
)

private val DarkColors = darkColorScheme(
    primary = GoldPrimaryDark,
    onPrimary = GoldOnPrimaryDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    background = GoldBackgroundDark,
    onBackground = GoldOnBackgroundDark,
    surface = GoldSurfaceDark,
    onSurface = GoldOnSurfaceDark,
    error = GoldErrorDark,
    onError = GoldOnErrorDark,
)

@Composable
fun OnsGoldAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
