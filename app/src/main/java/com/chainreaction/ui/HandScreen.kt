package com.chainreaction.ui

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Counter("Hand", "${state.hand.size}/${Rules.HAND_CAP}", Modifier.weight(1f))
                Counter("Deck", "${state.deck.size}", Modifier.weight(1f))
                Counter("Discard", "${state.discard.size}", Modifier.weight(1f))
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
            // you don't hold. The Double Wheel card is the way round that.
            val canAfford = state.hand.size >= Rules.WHEEL_COST
            Column {
                NeonBlueButton(
                    "Spin the Double Wheel  (${Rules.WHEEL_COST} cards)",
                    enabled = canAfford,
                    onClick = onOpenWheel,
                )
                if (!canAfford) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Spinning costs ${Rules.WHEEL_COST} cards and you're holding " +
                            "${state.hand.size}. Playing Double Wheel spins for free.",
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
private fun Counter(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
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
    }
}
