package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chainreaction.data.Character
import com.chainreaction.data.Stats

/** How many rows the card and opponent lists show before they stop being a glance. */
private const val LIST_LIMIT = 8

/**
 * Your history, and nobody else's. Everything here is derived from what the app already
 * had to know — scores, and which card left your hand — apart from who you aimed at,
 * which the play flow asks for.
 *
 * Reached from Settings rather than the menu: the menu's four buttons are painted into
 * the artwork, so a fifth would mean redrawing it.
 */
@Composable
fun StatsSheet(
    stats: Stats,
    characters: List<Character>,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        var confirmingClear by remember { mutableStateOf(false) }
        val me = characters.character(stats.profile)

        Column(
            Modifier
                .fillMaxSize()
                .background(NeonBg)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            NeonHeader("STATS", backLabel = "Back", onBack = onDismiss)

            if (stats.gamesPlayed == 0 && stats.totalCardsPlayed == 0) {
                Text(
                    "Nothing yet. Finish a round and it'll show up here.",
                    color = NeonBody,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(20.dp))
                NeonQuietButton("Close", onClick = onDismiss)
                Spacer(Modifier.height(28.dp))
                return@Column
            }

            if (me != null) {
                Row(
                    Modifier.padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CharacterBadge(me, size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(me.name, color = NeonWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            NeonSectionLabel("Rounds")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile("Played", "${stats.gamesPlayed}", Modifier.weight(1f))
                StatTile("Won", "${stats.wins}", Modifier.weight(1f), NeonOrange)
                StatTile("Lost", "${stats.losses}", Modifier.weight(1f))
            }
            if (stats.ties > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    // Ties are their own thing: the app never invents a playoff, so
                    // counting them as wins would be putting words in its mouth.
                    "${stats.ties} tied for the win — neither a win nor a loss.",
                    color = NeonBody,
                    fontSize = 15.sp,
                )
            }

            NeonSectionLabel("On the course")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile("Aces", "${stats.aces}", Modifier.weight(1f), NeonIce)
                StatTile("Cards played", "${stats.totalCardsPlayed}", Modifier.weight(1f))
                StatTile("Gifts given", "${stats.giftsGiven}", Modifier.weight(1f), NeonIce)
            }

            if (stats.playsAgainst.isNotEmpty()) {
                NeonSectionLabel("Most targeted")
                stats.byMostTargeted.take(LIST_LIMIT).forEach { (name, n) ->
                    TallyRow(name, n)
                }
            }

            if (stats.cardsPlayed.isNotEmpty()) {
                NeonSectionLabel("Most played")
                stats.byMostPlayed.take(LIST_LIMIT).forEach { (card, n) ->
                    TallyRow(card.name, n)
                }
                if (stats.cardsDiscarded > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${stats.cardsDiscarded} discarded without playing.",
                        color = NeonBody,
                        fontSize = 15.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            if (confirmingClear) {
                Text(
                    "Clear every stat? This can't be undone.",
                    color = NeonWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        NeonQuietButton("Keep them") { confirmingClear = false }
                    }
                    Column(Modifier.weight(1f)) {
                        NeonBigButton("Clear", enabled = true) {
                            onClear()
                            onDismiss()
                        }
                    }
                }
            } else {
                NeonQuietButton("Clear stats") { confirmingClear = true }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = NeonWhite,
) {
    Column(
        modifier
            .neonPanel()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            label.uppercase(),
            color = NeonBody,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TallyRow(label: String, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .neonPanel()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = NeonWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Text("$count", color = NeonOrange, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}
