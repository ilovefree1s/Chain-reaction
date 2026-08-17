package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chainreaction.data.CardDeck
import com.chainreaction.data.GameCard
import com.chainreaction.ui.theme.Group
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.Panel
import com.chainreaction.ui.theme.Pine
import com.chainreaction.ui.theme.Sage
import kotlinx.coroutines.delay

private enum class Stage { IDLE, CARD_SPIN, CARD_DONE, NAME_SPIN, DONE }

/**
 * Card #48's mechanic. Order matters: the whole group sees what's at stake
 * *before* the name lands, so the effect is revealed first and the exempt
 * player second.
 */
@Composable
fun DoubleWheelSheet(players: List<String>, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Let the sheet paint under the status bar so it reads as a full screen,
            // not a black-topped panel. Insets are handled by safeDrawingPadding below.
            decorFitsSystemWindows = false,
        ),
    ) {
        var stage by remember { mutableStateOf(Stage.IDLE) }
        var cardShown by remember { mutableStateOf<GameCard?>(null) }
        var nameShown by remember { mutableStateOf<String?>(null) }

        // Spin the card wheel — reaction cards are already filtered out of the pool.
        LaunchedEffect(stage) {
            // Tuned to land in roughly 1.6s per stage — long enough to build, short enough to play.
            if (stage == Stage.CARD_SPIN) {
                var step = 30L
                repeat(18) {
                    cardShown = CardDeck.WHEEL_POOL.random()
                    delay(step)
                    step += 7
                }
                cardShown = CardDeck.WHEEL_POOL.random()
                stage = Stage.CARD_DONE
            }
            if (stage == Stage.NAME_SPIN) {
                var step = 40L
                repeat(16) {
                    nameShown = players.random()
                    delay(step)
                    step += 9
                }
                nameShown = players.random()
                stage = Stage.DONE
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Pine)
                .safeDrawingPadding()
                .padding(20.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "DOUBLE WHEEL",
                    style = MaterialTheme.typography.displaySmall,
                    color = Group,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Effect first, then the name. The named player is the only one exempt.",
                    color = Sage,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(24.dp))

                // ---- stage 1: the effect ----
                SectionLabel("1 · The effect")
                Spacer(Modifier.height(10.dp))
                when {
                    cardShown == null -> Placeholder("Spin to reveal the effect")
                    else -> CardTile(cardShown!!)
                }

                Spacer(Modifier.height(20.dp))

                if (stage == Stage.IDLE) {
                    BigButton(
                        text = "Spin the effect",
                        fill = Group,
                        onFill = Pine,
                        onClick = { stage = Stage.CARD_SPIN },
                    )
                }

                // ---- stage 2: the name ----
                if (stage == Stage.CARD_DONE || stage == Stage.NAME_SPIN || stage == Stage.DONE) {
                    SectionLabel("2 · Who's exempt")
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Panel)
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            nameShown ?: "—",
                            style = MaterialTheme.typography.displaySmall,
                            color = if (stage == Stage.DONE) Group else OffWhite,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    if (stage == Stage.CARD_DONE) {
                        BigButton(
                            text = "Spin the name",
                            fill = Group,
                            onFill = Pine,
                            onClick = { stage = Stage.NAME_SPIN },
                        )
                    }
                }

                if (stage == Stage.DONE) {
                    Text(
                        "${nameShown} sits this one out. Everyone else — including whoever " +
                            "played the card — carries out the effect.",
                        color = OffWhite,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(12.dp))
                BigButton(
                    text = if (stage == Stage.DONE) "Done" else "Close",
                    fill = Panel,
                    onFill = OffWhite,
                    onClick = onDismiss,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Sage, style = MaterialTheme.typography.bodyLarge)
    }
}
