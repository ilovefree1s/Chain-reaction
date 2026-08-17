package com.chainreaction.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chainreaction.R
import com.chainreaction.ui.theme.Pine

/** The button sheet is 1024x1536 with a transparent background, so it just sits on the photo. */
private const val SHEET_ASPECT = 1536f / 1024f

/** The title photo is 941x1672. */
private const val PHOTO_ASPECT = 1672f / 941f

/** How much of the screen height the photo/grass cross-fade spans. */
private const val GRASS_FADE = 0.07f

/** Share of the screen height the button stack occupies. */
private const val BUTTONS_HEIGHT_FRACTION = 0.40f

/**
 * Where each button sits within the sheet, measured off the actual pixels (per-row
 * brightness) rather than eyeballed. Boundaries fall at the midpoints between buttons
 * so every tap lands on the nearest one.
 */
private val BUTTON_BANDS = listOf(
    0.130f to 0.3177f, // play
    0.3177f to 0.4984f, // cards
    0.4984f to 0.6781f, // rules
    0.6781f to 0.840f, // settings
)

@Composable
fun MenuScreen(
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onCards: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Pine)
            .clipToBounds(),
    ) {
        // FillWidth, not Crop: cropping to fill a tall screen trims the sides and cuts
        // the ends off the logo. This keeps the artwork whole and lets the bottom fade
        // into the ground instead.
        val photoHeight = maxWidth * PHOTO_ASPECT

        Image(
            painter = painterResource(R.drawable.chainreactionmain),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(photoHeight),
        )

        // Grass carries on below the photo so the frame is filled rather than fading to
        // the ground colour. It starts slightly above the photo's bottom edge and has its
        // own top edge masked out, which cross-fades the join instead of butting them.
        val fade = maxHeight * GRASS_FADE
        val grassTop = (photoHeight - fade).coerceAtMost(maxHeight)
        val grassHeight = maxHeight - grassTop
        if (grassHeight > 0.dp) {
            Image(
                painter = painterResource(R.drawable.moregrass),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(y = grassTop)
                    .fillMaxWidth()
                    .height(grassHeight)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                (fade / grassHeight).coerceIn(0.05f, 0.6f) to Color.Black,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }

        // Gentle darkening so the buttons hold up against bright grass.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.28f),
                    ),
                ),
        )

        BoxWithConstraints(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(BUTTONS_HEIGHT_FRACTION)
                .navigationBarsPadding(),
        ) {
            val sheetHeight = maxHeight
            val sheetWidth = sheetHeight / SHEET_ASPECT

            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(sheetWidth)
                    .height(sheetHeight),
            ) {
                Image(
                    painter = painterResource(R.drawable.mainbuttons),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                // Invisible tap targets tiled over the painted buttons.
                Column(Modifier.fillMaxSize()) {
                    val actions = listOf(onPlay, onCards, onRules, onSettings)
                    Spacer(Modifier.weight(BUTTON_BANDS.first().first))
                    BUTTON_BANDS.forEachIndexed { i, (top, bottom) ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(bottom - top)
                                .clickable(onClick = actions[i]),
                        )
                    }
                    Spacer(Modifier.weight(1f - BUTTON_BANDS.last().second))
                }
            }
        }
    }
}
