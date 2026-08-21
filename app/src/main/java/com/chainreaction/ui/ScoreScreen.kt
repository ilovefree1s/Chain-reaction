package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.Character
import com.chainreaction.data.GameState

/** "E", "+2", "-1" — how golfers read a score. */
fun formatRelative(v: Int): String = when {
    v == 0 -> "E"
    v > 0 -> "+$v"
    else -> "$v"
}

/** Under par cool blue, even quiet, over par hot orange — the palette does the talking. */
fun relativeColor(v: Int) = when {
    v < 0 -> NeonIce
    v == 0 -> NeonBody
    else -> NeonOrange
}

@Composable
fun ScoreScreen(
    state: GameState,
    characters: List<Character> = emptyList(),
    modifier: Modifier = Modifier,
    onHole: (Int) -> Unit,
    onScore: (player: Int, delta: Int) -> Unit,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
) {
    val hole = state.currentHole
    val locked = state.locked[hole]
    val par = state.pars[hole]

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // ---- hole navigation ----
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepperButton("‹", enabled = hole > 0) { onHole(hole - 1) }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "HOLE ${hole + 1}",
                    color = NeonWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                )
                // Par is fixed by the course — shown, not editable.
                Text(
                    "PAR $par  ·  OF ${state.holeCount}${if (locked) "  ·  LOCKED" else ""}",
                    color = if (locked) NeonOrange else NeonBody,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
            StepperButton("›", enabled = hole < state.holeCount - 1) { onHole(hole + 1) }
        }

        Spacer(Modifier.height(16.dp))

        // ---- one row per player: hole score entry plus the running total ----
        // The old separate Totals list said the same names again; the LEAD tag
        // and running score live here now instead.
        val bestTotal = state.players.indices.minOfOrNull { state.totalFor(it) }
        state.players.forEachIndexed { i, name ->
            val isMe = i == state.meIndex
            val score = state.scores[hole][i]
            val total = state.totalFor(i)
            val rel = state.relativeToParFor(i)
            val leading = state.lockedHoleCount > 0 && total == bestTotal

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .then(if (isMe) Modifier.neonPanelOrange() else Modifier.neonPanel())
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Only takes up room once somebody has actually picked a character.
                characters.character(state.characterFor(i))?.let {
                    CharacterBadge(it, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = if (isMe) NeonOrange else NeonWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    // LEAD · E · 3 total — the whole story on one line.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (leading) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonBlue),
                            ) {
                                Text(
                                    "LEAD",
                                    color = NeonBg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            "${formatRelative(rel)}  ·  $total total strokes",
                            color = relativeColor(rel),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                StepperButton("−", enabled = !locked && score > 1) { onScore(i, -1) }
                Text(
                    "$score",
                    color = NeonWhite,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(56.dp),
                )
                StepperButton("+", enabled = !locked) { onScore(i, +1) }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- lock / unlock ----
        if (locked) {
            Text(
                "You drew ${state.drawForHole(hole)} on this hole.",
                color = NeonBody,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(8.dp))
            NeonQuietButton("Unlock hole ${hole + 1}", onClick = onUnlock)
        } else {
            NeonBigButton("Lock hole & draw", enabled = true, onClick = onLock)
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.wasDealt(hole)) {
                    "Already dealt — locking again gives no new cards."
                } else {
                    "Locking gives you ${state.drawForHole(hole)} card(s)."
                },
                color = NeonBody,
                fontSize = 16.sp,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}
