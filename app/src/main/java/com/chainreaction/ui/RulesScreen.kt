package com.chainreaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.chainreaction.data.Rules
import com.chainreaction.ui.theme.Attack

private val houseRules = listOf(
    "Stroke play" to "Lowest total wins.",
    "Starting hand" to "4 cards, dealt at the start of the round.",
    "Hand cap" to "7 cards. You cannot draw past it — discard first.",
    "Your own deck" to
        "Every player shuffles their own ${CardDeck.ALL.size}-card deck. Duplicates across " +
        "players are " +
        "expected and fine. This phone only tracks your deck.",
    "Discard" to
        "Played and discarded cards go to your own discard pile. If your deck runs out, " +
        "the discard is reshuffled back in.",
    "Buy a spin" to
        "Discard ${Rules.WHEEL_COST} cards to buy a spin on the Double Wheel. Spin it from " +
        "the Hand tab. Playing Double Wheel spins for free instead.",
    "Two per player, per hole" to
        "At most ${Rules.MAX_CARDS_ON_ONE_PLAYER_PER_HOLE} cards may be played on any one " +
        "player per hole. The app does not enforce this — track it yourselves.",
    "Gift yourself" to
        "A gift card can be played on you. Nothing says a favour has to go to somebody else.",
    "The app doesn't referee" to
        "It deals cards and keeps score. It never applies card effects or stroke penalties. " +
        "Cards that rewrite scores get entered by hand with the ± steppers.",
)

private val drawTable = listOf(
    "Best score, or tied for best" to "0",
    "Middle of the pack" to "1",
    "Last, or tied for last" to "2",
    "Everyone tied" to "1 each",
    "Double bogey or worse" to "+1 bonus",
)

/** Icon per house rule, in the order of [houseRules]. */
private val ruleIcons = listOf(
    NeonIcon.GOLF, NeonIcon.CARDS, NeonIcon.SEVEN, NeonIcon.DECK,
    NeonIcon.RECYCLE, NeonIcon.WHEEL, NeonIcon.PEOPLE, NeonIcon.GIFT, NeonIcon.SCALES,
)

@Composable
private fun RuleRow(index: Int, title: String, body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .neonPanel()
            .padding(14.dp),
    ) {
        NeonChip(ruleIcons[index])
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, color = NeonWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = NeonBody, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun DrawRow(finish: String, cards: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .neonPanel()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(finish, color = NeonBody, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(cards, color = NeonOrange, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

/** The house rules and the draw table, for the in-round Rules tab. */
fun LazyListScope.houseRulesItems() {
    item { NeonSectionLabel("How it works") }
    items(houseRules.size) { i -> RuleRow(i, houseRules[i].first, houseRules[i].second) }

    item { NeonSectionLabel("Cards drawn at the end of a hole") }
    items(drawTable) { (finish, cards) -> DrawRow(finish, cards) }
}

/** All 54 cards, grouped by timing. */
fun LazyListScope.cardLibraryItems() {
    Rules.TIMINGS.forEach { timing ->
        val group = CardDeck.ALL.filter { it.timing == timing }
        if (group.isNotEmpty()) {
            item { NeonSectionLabel("$timing · ${group.size}") }
            items(group, key = { it.id }) { card -> CardTile(card) }
        }
    }
}

/**
 * Menu destination: the rules in the neon style, drawn in code rather than shipped
 * as artwork — text stays editable and the page costs kilobytes, not megabytes.
 * The in-round Rules tab keeps its own full version with the card list and the
 * round actions.
 */
@Composable
fun RulesScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .background(NeonBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        NeonHeader("RULES", onBack = onBack)

        NeonSectionLabel("How it works")
        houseRules.forEachIndexed { i, (title, body) ->
            RuleRow(i, title, body)
            Spacer(Modifier.height(12.dp))
        }

        NeonSectionLabel("Cards drawn at the end of a hole")
        drawTable.forEach { (finish, cards) ->
            DrawRow(finish, cards)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(28.dp))
    }
}

/** Menu destination: the full deck to browse before a round. */
@Composable
fun CardsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { NeonSectionLabel("All ${CardDeck.ALL.size} cards") }
        cardLibraryItems()
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** The in-round Rules tab: everything above, plus the round-specific actions. */
@Composable
fun RoundRulesScreen(
    holeCount: Int,
    pars: List<Int>,
    playingCourse: String?,
    modifier: Modifier = Modifier,
    onSaveCourse: (String) -> Unit,
    onEndRound: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    // Pre-filled with the course being played, so correcting its pars is a one-tap save.
    var courseName by remember { mutableStateOf(playingCourse.orEmpty()) }
    var savedAs by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        houseRulesItems()
        cardLibraryItems()

        // You usually learn a course's real pars by playing it. Capture them here,
        // once, and the next round on this course starts already set up.
        item {
            NeonSectionLabel("Save this course")
            Text(
                "Keeps these $holeCount pars (par ${pars.sum()}) for next time.",
                color = NeonBody,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(10.dp))
            NeonTextField(
                value = courseName,
                onValueChange = { courseName = it; savedAs = null },
                placeholder = "Course name",
            )
            Spacer(Modifier.height(10.dp))
            NeonBigButton("Save course", enabled = courseName.isNotBlank()) {
                val name = courseName.trim()
                onSaveCourse(name)
                savedAs = name
                courseName = ""
            }
            savedAs?.let {
                Spacer(Modifier.height(8.dp))
                Text("Saved as \"$it\".", color = NeonIce, fontSize = 16.sp)
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            EndRoundButton { confirming = true }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirming) {
        EndRoundDialog(
            onConfirm = onEndRound,
            onDismiss = { confirming = false },
        )
    }
}

/** Quiet frame, red verb — destructive, but it still has a confirm behind it. */
@Composable
private fun EndRoundButton(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(TapTarget)
            .neonPanel()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "END ROUND",
            color = Attack,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun EndRoundDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonPanelBg,
        titleContentColor = NeonWhite,
        textContentColor = NeonBody,
        title = { Text("End the round?") },
        text = {
            Text("Scores, your hand, deck and discard are all cleared. This can't be undone.")
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text("End round", color = Attack) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep playing", color = NeonBody) }
        },
    )
}
