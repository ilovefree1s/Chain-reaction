package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.CardDeck
import com.chainreaction.data.GameState
import com.chainreaction.data.Rules

@Composable
fun HandScreen(
    state: GameState,
    modifier: Modifier = Modifier,
    onDraw: () -> Unit,
    onResolve: (cardId: Int, played: Boolean, target: String?) -> Unit,
    onOpenWheel: () -> Unit,
) {
    // The card enlarged to its full face, if any. Play and Discard live there.
    var enlarged by remember { mutableStateOf<Int?>(null) }
    // The discard pile, opened from its counter.
    var pileOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            // Intrinsic height, because the discard counter carries an extra line and
            // three tiles of different heights read as a mistake.
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Counter(
                    "Hand",
                    "${state.hand.size}/${Rules.HAND_CAP}",
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Counter(
                    "Deck",
                    "${state.deck.size}",
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                // The only counter worth opening: what's in the pile is a list, not
                // just a number, and mid-round people forget what they've used.
                Counter(
                    "Discard",
                    "${state.discard.size}",
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    sublabel = "Played",
                    onClick = { pileOpen = true },
                )
            }
        }

        // Draw affordance — only present when cards are actually owed.
        if (state.owed > 0) {
            item {
                if (state.handIsFull) {
                    Column {
                        NeonBigButton("Hand is full — discard first", enabled = false) {}
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You're owed ${state.owed} card(s) but you're holding the " +
                                "maximum of ${Rules.HAND_CAP}. Discard, then draw — " +
                                "locking the next hole loses whatever you haven't taken.",
                            color = NeonBody,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    NeonBigButton("Draw a card  (${state.owed} owed)", enabled = true, onClick = onDraw)
                }
            }
        }

        item {
            // The wheel is no longer free: it costs cards, and you can't pay what
            // you don't hold. The GAMBLE WHEEL!! card is the way round that.
            val canAfford = state.hand.size >= Rules.WHEEL_COST
            Column {
                NeonBlueButton(
                    "Spin the GAMBLE WHEEL!!  (${Rules.WHEEL_COST} cards)",
                    enabled = canAfford,
                    onClick = onOpenWheel,
                )
                if (!canAfford) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Spinning costs ${Rules.WHEEL_COST} cards and you're holding " +
                            "${state.hand.size}. Playing GAMBLE WHEEL!! spins for free.",
                        color = NeonBody,
                        fontSize = 16.sp,
                    )
                }
            }
        }

        item { NeonSectionLabel("Your hand") }

        if (state.hand.isEmpty()) {
            item {
                Text(
                    "Nothing in hand. Cards arrive when you lock a hole.",
                    color = NeonBody,
                    fontSize = 16.sp,
                )
            }
        }

        // Two compact tiles per row; tap one to read it big and play it.
        items(state.hand.chunked(2), key = { it.first() }) { pair ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { id ->
                    CardMiniTile(
                        card = CardDeck.card(id),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) { enlarged = id }
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (pileOpen) {
        DiscardPileSheet(state = state, onDismiss = { pileOpen = false })
    }

    // Tap-to-enlarge: the full face, with Play / Discard right there.
    // Swiping left and right walks the rest of the hand.
    enlarged?.let { id ->
        CardFaceDialog(
            cardIds = state.hand,
            initialCardId = id,
            onDismiss = { enlarged = null },
            onResolve = onResolve,
            players = state.players,
            meIndex = state.meIndex,
        )
    }
}

@Composable
private fun Counter(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    /** Second line under the label. Present only where it says what tapping does. */
    sublabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .neonPanel()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = NeonWhite, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(
            label.uppercase(),
            color = NeonBody,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        if (sublabel != null) {
            // Ice, not body grey: this line is the tap target, and it has to look
            // like something rather than like a caption.
            Text(
                sublabel.uppercase(),
                color = NeonIce,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

/**
 * The discard pile, opened from its counter: everything that has left your hand this
 * round, newest first, saying which you played and which you dumped. Mid-round nobody
 * remembers what they've already used, and the pile is the only record of it.
 */
@Composable
private fun DiscardPileSheet(state: GameState, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val entries = state.playLog.reversed()
        val playedCount = state.playLog.count { it.played }

        Column(
            Modifier
                .fillMaxSize()
                .background(NeonBg)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            NeonHeader("DISCARD", backLabel = "Back", onBack = onDismiss)

            if (entries.isEmpty()) {
                Text(
                    "Nothing yet. Cards land here once you play or discard them.",
                    color = NeonBody,
                    fontSize = 16.sp,
                )
            } else {
                Text(
                    "${entries.size} in the pile · $playedCount played, " +
                        "${entries.size - playedCount} discarded.",
                    color = NeonBody,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(14.dp))

                entries.forEach { entry ->
                    Text(
                        if (entry.played) "PLAYED" else "DISCARDED",
                        color = if (entry.played) NeonIce else NeonDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    CardTile(CardDeck.card(entry.id))
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            NeonQuietButton("Close", onClick = onDismiss)
            Spacer(Modifier.height(28.dp))
        }
    }
}
