package com.chainreaction.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.Character
import com.chainreaction.data.GameState
import com.chainreaction.data.RoundMode

/**
 * Shown in place of the scorecard once every hole is locked. A finished round should
 * announce itself, not leave you staring at an unlock button.
 */
@Composable
fun ResultsScreen(
    state: GameState,
    characters: List<Character> = emptyList(),
    modifier: Modifier = Modifier,
    onViewScorecard: () -> Unit,
    onFinishRound: () -> Unit,
) {
    val winners = state.winners
    val winnerNames = winners.map { state.players[it] }
    val winningTotal = state.totalFor(winners.first())

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        NeonSectionLabel(
            state.courseName?.let { "$it · round complete" }
                ?: "${state.holeCount} holes · round complete",
        )

        Text(
            winnerLine(winnerNames),
            color = NeonOrange,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            lineHeight = 38.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (state.mode == RoundMode.SKINS) skinsSummary(state, winners.first())
            else "$winningTotal  ·  ${formatRelative(state.relativeToParFor(winners.first()))} " +
                "to a par of ${state.pars.sum()}",
            color = NeonBody,
            fontSize = 16.sp,
        )

        NeonSectionLabel(if (state.mode == RoundMode.SKINS) "Skins" else "Final scores")

        state.standings.forEachIndexed { place, player ->
            val total = state.totalFor(player)
            val won = player in winners
            val behind = total - winningTotal

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .then(if (won) Modifier.neonPanelOrange() else Modifier.neonPanel())
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${place + 1}",
                    color = if (won) NeonOrange else NeonDim,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(32.dp),
                )
                characters.character(state.characterFor(player))?.let {
                    CharacterBadge(it, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        state.players[player],
                        color = if (won) NeonOrange else NeonWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        // Relative to par, then how far off the win — spelled out, because
                        // "E · +1" reads as a contradiction at a glance. In skins the
                        // strokes are still worth showing; they just are not the result.
                        if (state.mode == RoundMode.SKINS) {
                            "$total strokes  ·  ${formatRelative(state.relativeToParFor(player))}"
                        } else if (behind == 0) formatRelative(state.relativeToParFor(player))
                        else "${formatRelative(state.relativeToParFor(player))}  ·  $behind back",
                        color = relativeColor(state.relativeToParFor(player)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    if (state.mode == RoundMode.SKINS) "${state.skinsFor(player)}" else "$total",
                    color = NeonWhite,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        // Clears the round and returns to the menu, so Play means play again.
        NeonBigButton("Finish round", enabled = true, onClick = onFinishRound)
        Spacer(Modifier.height(10.dp))
        // Escape hatch: a mis-scored final hole is still fixable from the scorecard.
        NeonQuietButton("Back to scorecard", onClick = onViewScorecard)
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * The winner's skins, and anything still riding. Skins left over do not go unclaimed:
 * the group goes back to the first tee and plays on until they are taken, which is a
 * house rule the app can state but cannot score — extra holes are not a thing it has.
 */
private fun skinsSummary(state: GameState, winner: Int): String {
    val won = state.skinsFor(winner)
    val carried = state.skinsCarried
    val skins = if (won == 1) "1 skin" else "$won skins"
    return if (carried > 0) "$skins  ·  $carried still up — back to hole 1" else skins
}

private fun winnerLine(names: List<String>): String = when (names.size) {
    1 -> "${names[0]} wins"
    2 -> "${names[0]} and ${names[1]} tie"
    else -> names.dropLast(1).joinToString(", ") + " and " + names.last() + " tie"
}
