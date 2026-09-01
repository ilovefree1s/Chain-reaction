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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chainreaction.data.CardDeck
import com.chainreaction.data.RoundMode
import com.chainreaction.data.Rules
import com.chainreaction.ui.theme.Attack

/** One row of How it works: an icon, a title and the rule itself. */
private data class HouseRule(val icon: NeonIcon, val title: String, val body: String)

/**
 * The house rules, for [mode]. Only scoring differs — everything else is how the deck
 * works, which is the same game either way. Icon and text live together so a new rule
 * can't land beside somebody else's icon.
 */
private fun houseRules(mode: RoundMode): List<HouseRule> = buildList {
    when (mode) {
        RoundMode.STROKE -> add(
            HouseRule(NeonIcon.GOLF, "Stroke play", "Lowest total over the round wins."),
        )
        RoundMode.SKINS -> {
            add(
                HouseRule(
                    NeonIcon.GOLF,
                    "Skins",
                    "Every hole is worth a skin. Win the hole outright and you take it. " +
                        "Most skins at the end wins — strokes only decide each hole.",
                ),
            )
            // Ties are the whole shape of skins, so they get their own rule rather than
            // a clause somebody skims past.
            add(
                HouseRule(
                    NeonIcon.RECYCLE,
                    "Ties roll over",
                    "Tie a hole and nobody wins it. Its skin rides onto the next, so the " +
                        "hole after a tie is worth two, then three, until somebody takes them.",
                ),
            )
            add(
                HouseRule(
                    NeonIcon.CARDS,
                    "Skins buy cards",
                    "Skins are worth cards — to everybody but the winner. Take a hole " +
                        "worth 3 skins and you draw nothing; everyone else draws 3. A tied hole " +
                        "wins nobody a skin but still deals everyone one, so the table is never " +
                        "left without cards to play.",
                ),
            )
            add(
                HouseRule(
                    NeonIcon.SCALES,
                    "No ties around here",
                    "Skins still on the table when the last hole is done don't sit there. " +
                        "Go back to the first tee pad and play on until every one is taken.",
                ),
            )
        }
    }
    addAll(sharedRules)
}

/** True in either format: the deck, the hand and who referees. */
private val sharedRules = listOf(
    HouseRule(NeonIcon.CARDS, "Starting hand", "4 cards, dealt at the start of the round."),
    HouseRule(NeonIcon.SEVEN, "Hand cap", "7 cards. You cannot draw past it — discard first."),
    HouseRule(
        NeonIcon.DECK,
        "Your own deck",
        "Every player shuffles their own ${CardDeck.DEALT.size}-card deck. Duplicates across " +
            "players are expected and fine. This phone only tracks your deck.",
    ),
    HouseRule(
        NeonIcon.RECYCLE,
        "Discard",
        "Played and discarded cards go to your own discard pile. If your deck runs out, " +
            "the discard is reshuffled back in.",
    ),
    HouseRule(
        NeonIcon.WHEEL,
        "Buy a spin",
        "Discard ${Rules.WHEEL_COST} cards to buy a spin on the GAMBLE WHEEL!! Spin it from " +
            "the Hand tab. Playing GAMBLE WHEEL!! spins for free instead.",
    ),
    HouseRule(
        NeonIcon.PEOPLE,
        "Two per player, per hole",
        "At most ${Rules.MAX_CARDS_ON_ONE_PLAYER_PER_HOLE} cards may be played on any one " +
            "player per hole. The app does not enforce this — track it yourselves.",
    ),
    HouseRule(
        NeonIcon.GIFT,
        "Yourself or somebody else",
        "A dual card can be played on you. Nothing says a favour has to go to another player.",
    ),
    HouseRule(
        NeonIcon.RECYCLE,
        "Mulligans",
        "Throwing a mulligan is a free extra throw, not a shot you have to play. " +
            "Take it or leave it — if you would rather play the one you already threw, do.",
    ),
    HouseRule(
        NeonIcon.SCALES,
        "The app doesn't referee",
        "It deals cards and keeps score. It never applies card effects or stroke penalties. " +
            "Cards that rewrite scores get entered by hand with the ± steppers.",
    ),
)

/** In skins the table doesn't apply: cards come from the skins somebody else took. */
private val skinsDrawTable = listOf(
    "Won the hole outright" to "0",
    "Everyone else" to "1 per skin",
    "Tied hole — no skin won" to "1 each",
)

private val drawTable = listOf(
    "Best score, or tied for best" to "0",
    "Middle of the pack" to "1",
    "Last, or tied for last" to "2",
    "Everyone tied" to "1 each",
    "Double bogey or worse" to "+1 bonus",
)

@Composable
private fun RuleRow(rule: HouseRule) {
    Row(
        Modifier
            .fillMaxWidth()
            .neonPanel()
            .padding(14.dp),
    ) {
        NeonChip(rule.icon)
        Column(Modifier.padding(start = 14.dp)) {
            Text(rule.title, color = NeonWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(rule.body, color = NeonBody, fontSize = 16.sp, lineHeight = 22.sp)
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

/**
 * The pick-one pair that swaps which scoring rules are shown. Same shape as Setup's
 * Format buttons, so the two read as the same control.
 */
@Composable
private fun FormatPicker(mode: RoundMode, onPick: (RoundMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RoundMode.entries.forEach { option ->
            Box(Modifier.weight(1f)) {
                if (option == mode) {
                    NeonBigButton(option.label, enabled = true) { onPick(option) }
                } else {
                    NeonQuietButton(option.label) { onPick(option) }
                }
            }
        }
    }
}

/**
 * The house rules and the draw table, for the in-round Rules tab. [mode] opens on the
 * format being played — reading the other one is a tap away, but the round you're in
 * shouldn't have to be selected.
 */
fun LazyListScope.houseRulesItems(shown: RoundMode, onPick: (RoundMode) -> Unit) {
    item { NeonSectionLabel("How it works") }
    item {
        Column {
            FormatPicker(shown, onPick)
            houseRules(shown).forEach { rule ->
                RuleRow(rule)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    item { NeonSectionLabel("Cards drawn at the end of a hole") }
    items(if (shown == RoundMode.SKINS) skinsDrawTable else drawTable) { (finish, cards) ->
        DrawRow(finish, cards)
    }
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

        // Off the menu there is no round to take a format from, so it opens on stroke
        // play — the one you get if nobody chooses.
        var shown by rememberSaveable { mutableStateOf(RoundMode.STROKE) }

        NeonSectionLabel("How it works")
        FormatPicker(shown) { shown = it }
        houseRules(shown).forEach { rule ->
            RuleRow(rule)
            Spacer(Modifier.height(12.dp))
        }

        NeonSectionLabel("Cards drawn at the end of a hole")
        (if (shown == RoundMode.SKINS) skinsDrawTable else drawTable).forEach { (finish, cards) ->
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
    /** The format being played, so the Rules tab opens on the rules in force. */
    mode: RoundMode,
    pars: List<Int>,
    playingCourse: String?,
    modifier: Modifier = Modifier,
    onSaveCourse: (String) -> Unit,
    onEndRound: () -> Unit,
) {
    var confirming by remember { mutableStateOf(false) }
    // Opens on the format being played; reading the other one is a tap away.
    var shownRules by rememberSaveable(mode) { mutableStateOf(mode) }
    // Pre-filled with the course being played, so correcting its pars is a one-tap save.
    var courseName by remember { mutableStateOf(playingCourse.orEmpty()) }
    var savedAs by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        houseRulesItems(shownRules) { shownRules = it }
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
