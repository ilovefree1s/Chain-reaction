package com.chainreaction.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Always dark. The app is used outdoors and a light ground is unreadable at dusk,
 * so the system light/dark setting is deliberately ignored.
 */
private val ChainReactionColors = darkColorScheme(
    primary = SelfCard,
    onPrimary = Pine,
    secondary = React,
    onSecondary = Pine,
    background = Pine,
    onBackground = OffWhite,
    surface = Panel,
    onSurface = OffWhite,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = Sage,
    error = Attack,
    onError = Pine,
    outline = Sage,
)

@Composable
fun ChainReactionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChainReactionColors,
        typography = AppTypography,
        content = content,
    )
}
