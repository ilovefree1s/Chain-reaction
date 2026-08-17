package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.GameState
import com.chainreaction.ui.theme.OffWhite
import com.chainreaction.ui.theme.Panel
import com.chainreaction.ui.theme.PanelRaised
import com.chainreaction.ui.theme.Pine
import com.chainreaction.ui.theme.Sage
import com.chainreaction.ui.theme.SelfCard

/**
 * Shown in place of the scorecard once every hole is locked. A finished round should
 * announce itself, not leave you staring at an unlock button.
 */
@Composable
fun ResultsScreen(
    state: GameState,
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
        Spacer(Modifier.height(20.dp))
        SectionLabel(
            state.courseName?.let { "$it · round complete" }
                ?: "${state.holeCount} holes · round complete",
        )
        Spacer(Modifier.height(10.dp))

        Text(
            winnerLine(winnerNames),
            style = MaterialTheme.typography.displaySmall,
            color = SelfCard,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$winningTotal  ·  ${formatRelative(state.relativeToParFor(winners.first()))} " +
                "to a par of ${state.pars.sum()}",
            color = Sage,
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel("Final scores")
        Spacer(Modifier.height(10.dp))

        state.standings.forEachIndexed { place, player ->
            val total = state.totalFor(player)
            val won = player in winners
            val behind = total - winningTotal

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (won) PanelRaised else Panel)
                    .then(
                        if (won) Modifier.border(2.dp, SelfCard, RoundedCornerShape(16.dp))
                        else Modifier,
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${place + 1}",
                    color = if (won) SelfCard else Sage,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(32.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        state.players[player],
                        style = MaterialTheme.typography.titleLarge,
                        color = if (won) SelfCard else OffWhite,
                        maxLines = 1,
                    )
                    Text(
                        // Relative to par, then how far off the win — spelled out, because
                        // "E · +1" reads as a contradiction at a glance.
                        if (behind == 0) formatRelative(state.relativeToParFor(player))
                        else "${formatRelative(state.relativeToParFor(player))}  ·  $behind back",
                        color = relativeColor(state.relativeToParFor(player)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    "$total",
                    style = MaterialTheme.typography.displaySmall,
                    color = OffWhite,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        // Clears the round and returns to the menu, so Play means play again.
        BigButton(
            text = "Finish round",
            fill = SelfCard,
            onFill = Pine,
            onClick = onFinishRound,
        )
        Spacer(Modifier.height(10.dp))
        // Escape hatch: a mis-scored final hole is still fixable from the scorecard.
        BigButton(
            text = "Back to scorecard",
            fill = Panel,
            onFill = OffWhite,
            onClick = onViewScorecard,
        )
        Spacer(Modifier.height(32.dp))
    }
}

private fun winnerLine(names: List<String>): String = when (names.size) {
    1 -> "${names[0]} wins"
    2 -> "${names[0]} and ${names[1]} tie"
    else -> names.dropLast(1).joinToString(", ") + " and " + names.last() + " tie"
}
