package com.chainreaction.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.chainreaction.ui.NeonBg
import com.chainreaction.ui.NeonBlue
import com.chainreaction.ui.NeonBody
import com.chainreaction.ui.NeonChipBg
import com.chainreaction.ui.NeonDim
import com.chainreaction.ui.NeonOrange
import com.chainreaction.ui.NeonPanelBg
import com.chainreaction.ui.NeonWhite

/**
 * Always dark. The app is used outdoors and a light ground is unreadable at dusk,
 * so the system light/dark setting is deliberately ignored. The scheme mirrors the
 * neon palette so Material defaults (dialogs, ripples) match the hand-drawn screens.
 */
private val ChainReactionColors = darkColorScheme(
    primary = NeonOrange,
    onPrimary = NeonBg,
    secondary = NeonBlue,
    onSecondary = NeonBg,
    background = NeonBg,
    onBackground = NeonWhite,
    surface = NeonPanelBg,
    onSurface = NeonWhite,
    surfaceVariant = NeonChipBg,
    onSurfaceVariant = NeonBody,
    error = Attack,
    onError = NeonBg,
    outline = NeonDim,
)

@Composable
fun ChainReactionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChainReactionColors,
        typography = AppTypography,
        content = content,
    )
}
