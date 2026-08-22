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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chainreaction.data.CardDeck
import com.chainreaction.data.Rules

/**
 * The price of spinning the Double Wheel off your own bat: [Rules.WHEEL_COST] cards
 * out of your hand, and you choose which. Playing the Double Wheel card skips this entirely —
 * that card's whole text is a free spin, so the card itself is the payment.
 *
 * Deliberately a deliberate act: the wheel is the most dramatic thing in the game and
 * it was previously free and unlimited, which made the hand cap beside the point.
 */
@Composable
fun WheelCostSheet(
    hand: List<Int>,
    onPaid: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val chosen = remember(hand) { mutableListOf<Int>().toMutableStateList() }
        val ready = chosen.size == Rules.WHEEL_COST

        Column(
            Modifier
                .fillMaxSize()
                .background(NeonBg)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // "Back", not "Menu": this is a sheet over the round, not a way out of it.
            NeonHeader("SPIN COSTS ${Rules.WHEEL_COST}", backLabel = "Back", onBack = onDismiss)

            Text(
                "Pick ${Rules.WHEEL_COST} cards to discard, then spin. " +
                    "Playing Double Wheel instead gives you a free spin.",
                color = NeonBody,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            // Same two-up tiles as the hand, so the thing you're paying with looks
            // like the thing you were just holding.
            hand.chunked(2).forEach { pair ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { id ->
                        CardMiniTile(
                            card = CardDeck.card(id),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            selected = id in chosen,
                        ) {
                            // Tapping a chosen card takes it back; once two are down
                            // the rest stop responding rather than silently swapping.
                            if (id in chosen) chosen.remove(id)
                            else if (chosen.size < Rules.WHEEL_COST) chosen.add(id)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "${chosen.size} of ${Rules.WHEEL_COST} chosen",
                color = if (ready) NeonOrange else NeonBody,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(10.dp))
            NeonBigButton("Discard ${Rules.WHEEL_COST} & spin", enabled = ready) {
                onPaid(chosen.toList())
            }
            Spacer(Modifier.height(10.dp))
            NeonQuietButton("Never mind", onClick = onDismiss)
            Spacer(Modifier.height(28.dp))
        }
    }
}
